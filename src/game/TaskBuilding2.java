

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class TaskBuilding2 extends Task {
  
  
  
  final static int MAX_TRIES = 100;
  
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
    String key = "build_"+m.name;
    CityMapDemands d = map.demands.get(key);
    if (d == null) {
      d = new CityMapDemands(map, key);
      map.demands.put(key, d);
    }
    return d;
  }
  
  
  static float checkNeedForBuilding(Element b, Good w, CityMapDemands map) {
    if (! b.onMap()) return 0;
    Tile at = b.at();
    boolean raze = map.map.planning.objectAt(at) != b;
    
    int   need       = raze ? 0 : b.type.materialNeed(w);
    float amountDone = getBuildAmount(b, w);
    float amountGap  = need - amountDone;
    
    if (need == 0 && amountDone <= 0) amountGap = 0;
    map.setAmount(amountGap, b, at.x, at.y);
    
    return amountGap;
  }
  
  
  static float incBuildAmount(float a, Good m, Element b) {
    if (b.type.isFixture()) {
      float need = b.type.materialNeed(m);
      if (need == 0) return 0;
      if (a != 0) b.incBuildLevel(a / need);
      return Nums.clamp(b.buildLevel(), 0, 1) * need;
    }
    if (b.type.isBuilding()) {
      Building site = (Building) b;
      if (a != 0) site.materials.add(a, m);
      return site.materials.valueFor(m);
    }
    return 0;
  }
  
  
  static float getBuildAmount(Element b, Good m) {
    return incBuildAmount(0, m, b);
  }
  
  
  
  /**  Setup and config methods exclusive to the task itself:
    */
  static Task nextBuildingTask(Building store, Actor a) {
    
    Task razing = TaskBuilding2.configBuilding(store, a, NOTHING);
    if (razing != null) return razing;
    
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
      if (decideNextAction(source, demands)) return true;
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
  
  
  boolean decideNextAction(Element b, CityMapDemands d) {
    //
    //  Avoid piling up on the same target:
    if (
      b != site && b.type.isFixture() &&
      Task.hasTaskFocus(b, JOB.BUILDING)
    ) {
      return false;
    }
    
    boolean onSite = actor.inside == store;
    float atStore = store.inventory.valueFor(material);
    float needBuild = checkNeedForBuilding(b, material, d);
    
    if (needBuild > 0) {
      //  Decide whether to obtain the materials from your store, based
      //  on whether they're available and not already present on-site.
      float carried = getCarryAmount(material, b, false);
      
      if (carried <= 0 && atStore > 0 && onSite) {
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
        //  Begin your visit.
        configTask(store, null, b, JOB.BUILDING, 1);
        site   = b;
        razing = false;
        return true;
      }
    }
    else if (needBuild < 0) {
      configTask(store, null, b, JOB.BUILDING, 1);
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
    if (target == site) {
      CityMapDemands demands = demandsFor(material, actor.map);
      advanceBuilding(site, demands);
      
      if (! decideNextAction(site, demands)) {
        pickNextTarget(true);
      }
    }
  }
  
  
  void advanceBuilding(Element b, CityMapDemands demands) {
    //
    //  First, we ascertain how much raw material we have, and how much
    //  building needs to be done-
    CityMap map = demands.map;
    Tile at = b.at();
    float amountPut = 0;
    float amountGap = checkNeedForBuilding(b, material, demands);
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
    float puts = Nums.clamp(amountGap, -0.1f, 0.1f);
    puts = Nums.min(puts, amountGot);
    incBuildAmount(puts, material, b);
    incCarryAmount(0 - puts, material, b, false);
    amountPut = puts;
    //
    //  Buildings get some special treatment, since their level of
    //  completion may be a function of multiple materials-
    float totalNeed = 0, totalDone = 0;
    for (Good g : b.type.builtFrom) {
      totalNeed += b.type.materialNeed(g);
      totalDone += getBuildAmount(b, g);
    }
    if (amountPut != 0 && b.type.isBuilding()) {
      b.setBuildLevel(1f * (totalDone / totalNeed));
    }
    //
    //  If we've depleted the structure entirely, remove it from the
    //  world, report and exit:
    if (b.buildLevel() <= 0 && b.onMap() && amountPut <= 0) {
      checkNeedForBuilding(b, material, demands);
      b.exitMap(map);
    }
    if (actor.reports()) {
      I.say("Trying to build "+b);
      I.say("  Progress? "+amountPut+" "+I.percent(b.buildLevel()));
      I.say("  Did:      "+totalDone+"/"+totalNeed);
    }
  }
  
  
  float incCarryAmount(float a, Good m, Element b, boolean siteOnly) {
    //
    //  The default 'nothing' material gets special treatment:
    if (m == NOTHING) return 100;
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










