

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;
import game.GameConstants.Target;



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
    String key = "need_build_"+m.name;
    return map.demands.get(key);
  }
  
  
  static float checkNeedForBuilding(Element b, Good w, CityMapDemands map) {
    
    Tile at = b.at();
    boolean raze = map.map.planning.objectAt(at) != b.type;
    
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
      return b.buildLevel() * need;
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
  static TaskBuilding2 configBuilding2(Building store, Actor a) {
    TaskBuilding2 task = new TaskBuilding2(a, store, null);
    return task.pickNextTarget(false) ? task : null;
  }
  
  
  boolean pickNextTarget(boolean near) {
    CityMap map = actor.map;
    Tile at = actor.at();
    
    CityMapDemands demands = demandsFor(material, map);
    if (demands == null) return false;
    
    int storeRange = store.type.maxDeliverRange;
    int maxRange = near ? actor.type.sightRange : storeRange;
    
    for (CityMapDemands.Entry e : demands.nearbyEntries(at.x, at.y)) {
      Element source = (Element) e.source;
      if (CityMap.distance(source.at(), at) > maxRange) break;
      if (CityMap.distance(store .at(), at) > storeRange) continue;
      if (! decideNextAction(source, demands)) continue;
      return true;
    }
    
    return false;
  }
  
  
  boolean decideNextAction(Element b, CityMapDemands d) {
    
    boolean onSite = actor.inside == store;
    float atStore = store.inventory.valueFor(material);
    float needBuild = checkNeedForBuilding(b, material, d);
    
    if (needBuild > 0) {
      //  Decide whether to obtain the materials from your store, based
      //  on whether they're available and not already present on-site.
      
      float carried = getCarryAmount(material, b, false);
      
      if (carried <= 0 && atStore > 0 && onSite) {
        //  Go to the store and pick up the material!
        configTask(store, store, null, JOB.BUILDING, 1);
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
  protected void onTarget(Target target) {
    if (target == store) {
      //
      //  Pick up some of the material, then go on to the site-
      float amount = Nums.min(5, store.inventory.valueFor(material));
      actor.pickupGood(material, amount, store);
      configTask(store, null, site, JOB.BUILDING, 1);
    }
    else if (target == site) {
      CityMapDemands demands = demandsFor(material, actor.map);
      advanceBuilding(site);
      
      if (checkNeedForBuilding(site, material, demands) != 0) {
        configTask(store, null, site, JOB.BUILDING, 1);
      }
      else {
        pickNextTarget(true);
      }
    }
  }
  
  
  void advanceBuilding(Element b) {
    
    CityMapDemands demands = demandsFor(material, actor.map);
    float totalNeed = 0, totalDone = 0;
    boolean didWork = false;
    
    for (Good g : b.type.buildsWith) {
      float amountGap = checkNeedForBuilding(b, g, demands);
      float amountGot = getCarryAmount(g, b, false);
      
      if (amountGap <= 0 || amountGot <= 0) continue;
      
      if (g == material) {
        float puts = Nums.clamp(amountGap, -0.1f, 0.1f);
        puts = Nums.min(puts, amountGot);
        incBuildAmount(puts, g, b);
        incCarryAmount(0 - puts, g, b, false);
        didWork = true;
      }
      
      totalNeed += b.type.materialNeed(g);
      totalDone += getBuildAmount(b, g);
    }
    
    if (actor.reports()) {
      I.say("Trying to build "+b);
      I.say("  Progress? "+didWork+" "+I.percent(b.buildLevel()));
      I.say("  Did:      "+totalDone+"/"+totalNeed);
    }
    
    if (didWork) {
      b.setBuildLevel(1.1f * (totalDone / totalNeed));
    }
  }
  
  
  float incCarryAmount(float a, Good m, Element b, boolean siteOnly) {
    float total = 0;
    
    if (actor.carried(m) > 0 && a < 0 && ! siteOnly) {
      float sub = Nums.min(actor.carryAmount, 0 - a);
      actor.setCarried(material, actor.carryAmount - sub);
      a += sub;
    }
    total += actor.carried(m);
    
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










