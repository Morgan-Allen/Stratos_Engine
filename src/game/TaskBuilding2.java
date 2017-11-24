

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class TaskBuilding2 extends Task {
  
  
  
  Building store;
  Good material;
  Element site;
  boolean razing;
  
  
  public TaskBuilding2(Actor actor, Building store, Good material) {
    super(actor);
    this.store    = store;
    this.material = material;
  }
  
  
  public TaskBuilding2(Session s) throws Exception {
    super(s);
    store    = (Building) s.loadObject();
    material = (Good    ) s.loadObject();
    site     = (Element ) s.loadObject();
    razing   = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(store   );
    s.saveObject(material);
    s.saveObject(site    );
    s.saveBool  (razing  );
  }
  

  
  /**  These methods are also used by the planning-map to flag any
    *  structures that need attention:
    */
  static CityMapDemands demandsFor(Good m, CityMap map) {
    
    //  TODO:  Improve the lookup speed here?
    
    String key = "build_"+m.name;
    CityMapDemands d = map.demands.get(key);
    if (d == null) {
      d = new CityMapDemands(map, key);
      map.demands.put(key, d);
    }
    return d;
  }
  
  
  static float checkNeedForBuilding(Element b, Good w, CityMap map) {
    Tile at = b.at();
    if (at == null) return 0;
    
    Element onPlan  = map.planning.objectAt(at);
    boolean natural = b.type.isNatural();
    boolean raze    = onPlan != b && (onPlan != null || ! natural);
    
    b.flagTeardown(raze);
    
    float need       = raze ? 0 : b.materialNeed(w);
    float amountDone = b.materialLevel(w);
    float amountGap  = need - amountDone;
    
    if (natural && ! raze           ) amountGap = 0;
    if (need == 0 && amountDone <= 0) amountGap = 0;
    
    CityMapDemands demands = demandsFor(w, map);
    demands.setAmount(amountGap, b, at.x, at.y);
    
    return amountGap;
  }
  
  
  
  /**  Setup and config methods exclusive to the task itself:
    */
  static Task nextBuildingTask(Building store, Actor a) {
    if (Visit.empty(store.type.buildsWith)) return null;
    
    Task clearing = TaskBuilding2.configBuilding(store, a, VOID);
    if (clearing != null) return clearing;
    
    for (Good g : store.type.buildsWith) {
      Task building = TaskBuilding2.configBuilding(store, a, g);
      if (building != null) return building;
    }
    
    return null;
  }
  
  
  static TaskBuilding2 configBuilding(
    Building store, Actor a, Good material
  ) {
    TaskBuilding2 task = new TaskBuilding2(a, store, material);
    return task.pickNextTarget(false) ? task : null;
  }
  
  
  boolean pickNextTarget(boolean near) {
    CityMap map = actor.map;
    Tile at = actor.at();
    //
    //  Iterate over any structures demanding your attention and see if
    //  they're close enough to attend to:
    CityMapDemands demands = demandsFor(material, map);
    if (demands == null) return false;
    
    int storeRange = store.type.maxDeliverRange;
    int maxRange = near ? actor.type.sightRange : storeRange;
    
    for (CityMapDemands.Entry e : demands.nearbyEntries(at.x, at.y)) {
      Element source = (Element) e.source;
      if (CityMap.distance(source.at(), at) > maxRange  ) break;
      if (CityMap.distance(store .at(), at) > storeRange) continue;
      if (decideNextAction(source, map)) return true;
    }
    //
    //  If there's no target to attend to, but you have surplus material
    //  left over, return it to your store:
    if (actor.carried == material) {
      configTask(store, store, null, JOB.RETURNING, 1);
      site   = null;
      razing = false;
      return true;
    }
    return false;
  }
  
  
  boolean decideNextAction(Element b, CityMap map) {
    //
    //  Avoid piling up on the same target:
    if (b != site && b.type.isFixture() && (
      Task.hasTaskFocus(b, JOB.BUILDING) ||
      Task.hasTaskFocus(b, JOB.SALVAGE )
    )) {
      return false;
    }
    
    float atStore   = store.inventory.valueFor(material);
    float needBuild = checkNeedForBuilding(b, material, map);
    
    if (needBuild > 0) {
      //  Decide whether to obtain the materials from your store, based
      //  on whether they're available and not already present on-site.
      float carried = getCarryAmount(material, b, false);
      
      if (carried <= 0 && atStore > 0) {
        //  Go to the store and pick up the material!
        configTask(store, store, null, JOB.RETURNING, 1);
        site   = b;
        razing = false;
        return true;
      }
      else if (carried <= 0) {
        //  Impossible.  Skip onto the next target.
        return false;
      }
      else {
        //  Begin your visit.  (Note that we cannot target the site
        //  directly, as it may not yet be in the world.)
        configTask(store, null, b.at(), JOB.BUILDING, 1);
        site   = b;
        razing = false;
        return true;
      }
    }
    else if (needBuild < 0) {
      //  If there's salvage to be done, start that:
      configTask(store, null, b.at(), JOB.SALVAGE, 1);
      site   = b;
      razing = true;
      return true;
    }
    else {
      return false;
    }
  }
  
  
  
  /**  Methods invoked once you arrive at the site:
    */
  protected void onVisit(Building visits) {
    //  TODO:  This might not work if the store isn't built yet!
    if (visits == store && site != null) {
      //
      //  Pick up some of the material, then go on to the site-
      float amount = Nums.min(5, store.inventory.valueFor(material));
      actor.pickupGood(material, amount, store);
      configTask(store, null, site, JOB.BUILDING, 1);
    }
    if (visits == store && site == null) {
      //
      //  Drop off any surplus material at the end-
      actor.offloadGood(material, store);
    }
  }
  
  
  protected void onTarget(Target target) {
    if (target == site.at()) {
      advanceBuilding(site, actor.map);
      if (! decideNextAction(site, actor.map)) {
        pickNextTarget(true);
      }
    }
  }
  
  
  void advanceBuilding(Element b, CityMap map) {
    //
    //  First, we ascertain how much raw material we have, and how much
    //  building needs to be done-
    Tile at = b.at();
    float amountGap = checkNeedForBuilding(b, material, map);
    float amountGot = getCarryAmount(material, b, false);
    //
    //  We do some basic sanity checks to ensure our effort isn't
    //  wasted, and introduce the structure if required:
    if (amountGap == 0 || (amountGap > 0 && amountGot <= 0)) {
      return;
    }
    if (amountGap > 0 && ! b.onMap()) {
      b.enterMap(map, at.x, at.y, 0);
    }
    //
    //  We see how much work we can do in this action, either to raze or
    //  to build, and adjust progress/stocks accordingly-
    float oldM = b.materialLevel(material);
    float puts = Nums.clamp(amountGap, -0.1f, 0.1f);
    puts = Nums.min(puts, amountGot);
    puts = b.incMaterialLevel(material, puts) - oldM;
    incCarryAmount(0 - puts, material, b, false);
    //
    //  If we've depleted the structure entirely, remove it from the
    //  world, report and exit:
    if (b.buildLevel() <= 0 && b.onMap() && puts <= 0) {
      b.exitMap(map);
    }
  }
  
  
  float incCarryAmount(float a, Good m, Element b, boolean siteOnly) {
    //
    //  The default 'nothing' material gets special treatment:
    if (m == VOID) return 100;
    //
    //  If we're recovering material, the actor keeps it:
    float total = 0;
    if (a > 0) {
      actor.incCarried(material, a);
      a = 0;
    }
    //
    //  If we're depleting the material, take it from the actor first:
    if (actor.carried(m) > 0 && a < 0 && ! siteOnly) {
      float sub = Nums.min(actor.carryAmount, 0 - a);
      actor.incCarried(material, 0 - sub);
      a += sub;
    }
    total += actor.carried(m);
    //
    //  And take from the building's stock as necessary:
    if (b.type.isBuilding()) {
      Building site = (Building) b;
      if (a != 0) site.inventory.add(a, m);
      total += site.inventory.valueFor(m);
    }
    return total;
  }
  
  
  float getCarryAmount(Good m, Element b, boolean siteOnly) {
    return incCarryAmount(0, m, b, siteOnly);
  }
  
  
  
}










