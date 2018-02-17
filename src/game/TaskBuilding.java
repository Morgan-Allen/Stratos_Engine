

package game;
import util.*;
import static game.AreaMap.*;
import static game.GameConstants.*;



public class TaskBuilding extends Task {
  
  
  final Building store;
  final Good material;
  final Element site;
  
  
  public TaskBuilding(
    Actor actor, Building store, Good material, Element site
  ) {
    super(actor);
    this.store    = store;
    this.material = material;
    this.site     = site;
  }
  
  
  public TaskBuilding(Session s) throws Exception {
    super(s);
    store    = (Building) s.loadObject();
    material = (Good    ) s.loadObject();
    site     = (Element ) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(store   );
    s.saveObject(material);
    s.saveObject(site    );
  }
  

  
  /**  These methods are also used by the planning-map to flag any
    *  structures that need attention:
    */
  public static CityMapDemands demandsFor(Good m, AreaMap map) {
    
    //  TODO:  Improve the lookup speed here?
    
    String key = "build_"+m.name;
    CityMapDemands d = map.demands.get(key);
    if (d == null) {
      d = new CityMapDemands(map, key);
      map.demands.put(key, d);
    }
    return d;
  }
  
  
  static float checkNeedForBuilding(
    Element b, Good w, AreaMap map, boolean doUpdate
  ) {
    Tile at = b.at();
    if (at == null) return 0;
    
    Element onPlan  = map.planning.objectAt(at);
    boolean natural = b.type().isNatural();
    boolean raze    = onPlan != b && (onPlan != null || ! natural);
    
    b.flagTeardown(raze);
    
    float need       = raze ? 0 : b.materialNeed(w);
    float amountDone = b.onMap() ? b.materialLevel(w) : 0;
    float amountGap  = need - amountDone;
    
    if (natural && ! raze           ) amountGap = 0;
    if (need == 0 && amountDone <= 0) amountGap = 0;
    
    if (doUpdate) {
      CityMapDemands demands = demandsFor(w, map);
      demands.setAmount(amountGap, b, at.x, at.y);
    }
    
    return amountGap;
  }
  
  
  
  /**  Setup and config methods exclusive to the task itself:
    */
  static Task nextBuildingTask(Building store, Actor a) {
    if (! a.map().world.settings.toggleBuilding) return null;
    if (Visit.empty(store.type().buildsWith)   ) return null;
    {
      Task clearing = nextBuildingTask(store, a, VOID, false);
      if (clearing != null) return clearing;
    }
    for (Good g : store.type().buildsWith) {
      Task building = nextBuildingTask(store, a, g, false);
      if (building != null) return building;
    }
    return null;
  }
  
  
  static Task nextBuildingTask(
    Building store, Actor actor, Good material, boolean near
  ) {
    if (! actor.map().world.settings.toggleBuilding) return null;
    AreaMap map = actor.map;
    Tile at = actor.at();
    //
    //  Iterate over any structures demanding your attention and see if
    //  they're close enough to attend to:
    CityMapDemands demands = demandsFor(material, map);
    if (demands != null) {
      int storeRange = store.type().maxDeliverRange;
      int maxRange = near ? actor.type().sightRange : storeRange;
      
      for (CityMapDemands.Entry e : demands.nearbyEntries(at.x, at.y)) {
        Element source = (Element) e.source;
        if (AreaMap.distance(source.at(), at) > maxRange  ) continue;
        if (AreaMap.distance(store .at(), at) > storeRange) continue;
        
        Pathing from = Task.pathOrigin(actor);
        Target goes = source;
        if (! map.pathCache.pathConnects(from, goes, true, false)) continue;
        
        TaskBuilding task = new TaskBuilding(actor, store, material, source);
        if (task.decideNextAction(source, map)) return task;
      }
    }
    //
    //  If there's no target to attend to, but you have surplus material left
    //  over, return it to your store:
    if (actor.carried(material) > 0) {
      TaskBuilding task = new TaskBuilding(actor, store, material, null);
      if (task.configTravel(store, JOB.RETURNING, store)) return task;
    }
    //
    //  Otherwise, nothing doing-
    return null;
  }
  
  
  boolean decideNextAction(Element b, AreaMap map) {
    //
    //  Avoid piling up on the same target:
    if (b != site && b.type().isFixture() && (
      Task.hasTaskFocus(b, JOB.BUILDING) ||
      Task.hasTaskFocus(b, JOB.SALVAGE )
    )) {
      return false;
    }
    
    float atStore   = store.inventory(material);
    float needBuild = checkNeedForBuilding(b, material, map, false);
    float carried   = getCarryAmount(material, b, false);
    
    if (needBuild > 0) {
      //  Decide whether to obtain the materials from your store, based
      //  on whether they're available and not already present on-site.
      if (carried <= 0 && atStore > 0) {
        //  Go to the store and pick up the material!
        return configTravel(store, JOB.COLLECTING, store);
      }
      else if (carried <= 0) {
        //  Impossible.  Skip onto the next target.
        return false;
      }
      else {
        //  Begin your visit for construction-
        return configTravel(b, JOB.BUILDING, store);
      }
    }
    else if (needBuild < 0) {
      //  If there's salvage to be done, start that:
      return configTravel(b, JOB.SALVAGE, store);
    }
    else {
      return false;
    }
  }
  
  
  
  /**  Methods invoked once you arrive at the site:
    */
  boolean configTravel(
    Element goes, Task.JOB jobType, Employer e
  ) {
    if (goes.complete() && goes.type().isBuilding()) {
      return configTask(e, (Building) goes, null, jobType, 0) != null;
    }
    else {
      //
      //  You need to configure a visit to an adjacent tile here...
      AreaMap map = goes.map;
      Tile c = goes.at();
      Type t = goes.type();
      Pathing from = Task.pathOrigin(active);
      Pick <Tile> pick = new Pick();
      
      for (Tile a : map.tilesAround(c.x, c.y, t.wide, t.high)) {
        if (! map.pathCache.pathConnects(from, a, false, false)) continue;
        
        float rating = 1;
        rating *= AreaMap.distancePenalty(from, a);
        if (Task.hasTaskFocus(a, jobType, active)) rating /= 3;
        
        pick.compare(a, rating);
      }
      
      if (pick.empty()) {
        return false;
      }
      else {
        return configTask(e, null, pick.result(), jobType, 0) != null;
      }
    }
  }
  
  
  void toggleFocus(boolean activeNow) {
    super.toggleFocus(activeNow);
    if (site != null) site.setFocused(active, activeNow);
  }
  
  
  protected void onVisit(Building visits) {
    onTarget(visits);
  }
  
  
  protected void onTarget(Target target) {
    Actor actor = (Actor) this.active;
    //
    //  Pick up some of the material initially-
    if (type == JOB.COLLECTING && adjacent(target, store)) {
      float amount = Nums.min(5, store.inventory(material));
      actor.pickupGood(material, amount, store);
    }
    //
    //  Then go on to the site and advance actual construction-
    if ((type == JOB.BUILDING || type == JOB.SALVAGE) && adjacent(target, site)) {
      advanceBuilding(site, actor.map);
    }
    //
    //  And drop off any surplus material at the end-
    if (type == JOB.RETURNING && adjacent(target, store)) {
      actor.offloadGood(material, store);
    }
    //
    //  But if you're not done yet, find the next step to take...
    else if (! decideNextAction(site, actor.map)) {
      Task next = nextBuildingTask(store, actor, material, true);
      actor.assignTask(next);
    }
  }
  
  
  void advanceBuilding(Element b, AreaMap map) {
    Actor actor = (Actor) this.active;
    //
    //  First, we ascertain how much raw material we have, and how much
    //  building needs to be done-
    Tile at = b.at();
    float amountGap = checkNeedForBuilding(b, material, map, true);
    float amountGot = getCarryAmount(material, b, false);
    //
    //  We do some basic sanity checks to ensure our effort isn't
    //  wasted, and introduce the structure if required:
    if (amountGap == 0 || (amountGap > 0 && amountGot <= 0)) {
      return;
    }
    if (amountGap > 0 && ! b.onMap()) {
      Base owns = store.base();
      if (b.type().isBuilding()) owns = ((Building) b).base();
      b.enterMap(map, at.x, at.y, 0, owns);
    }
    //
    //  We see how much work we can do in this action, either to raze or
    //  to build, and adjust progress/stocks accordingly-
    Trait skill      = SKILL_BUILD;
    float oldM       = b.materialLevel(material);
    float skillBonus = actor.levelOf(skill) / MAX_SKILL_LEVEL;
    float putLimit   = 0.1f * (1 + skillBonus);
    float puts       = Nums.clamp(amountGap, -putLimit, putLimit);
    
    actor.gainXP(skill, 1 * BUILD_XP_PERCENT / 100f);
    
    puts = Nums.min(puts, amountGot);
    puts = b.incMaterialLevel(material, puts) - oldM;
    
    //  NOTE:  This is important for avoiding rare loop-conditions with very
    //  small remainders of a material...
    if (puts == 0 && amountGot > 0) puts = amountGot;
    
    incCarryAmount(0 - puts, material, b, false);
    
    //
    //  If we've depleted the structure entirely, remove it from the
    //  world, report and exit:
    if (b.buildLevel() <= 0 && b.onMap() && puts <= 0) {
      b.exitMap(map);
    }
  }
  
  
  float incCarryAmount(float inc, Good m, Element b, boolean siteOnly) {
    Actor actor = (Actor) this.active;
    //
    //  The default 'nothing' material gets special treatment:
    if (m == VOID) return 100;
    //
    //  If we're recovering material, the actor keeps it:
    float total = 0;
    if (inc > 0) {
      actor.incCarried(material, inc);
      inc = 0;
    }
    //
    //  If we're depleting the material, take it from the actor first:
    if (actor.carried(m) > 0 && inc < 0 && ! siteOnly) {
      float sub = Nums.min(actor.carried(m), 0 - inc);
      actor.incCarried(material, 0 - sub);
      inc += sub;
    }
    total += actor.carried(m);
    //
    //  And take from the building's stock as necessary:
    if (b.type().isBuilding()) {
      Building site = (Building) b;
      if (inc < 0) {
        float sub = Nums.min(0 - inc, site.inventory(m));
        site.addInventory(0 - sub, m);
      }
      if (inc > 0) {
        site.addInventory(inc, m);
      }
      total += site.inventory(m);
    }
    return total;
  }
  
  
  float getCarryAmount(Good m, Element b, boolean siteOnly) {
    return incCarryAmount(0, m, b, siteOnly);
  }
  

  
  /**  Rendering, debug and interface methods:
    */
  public String toString() {
    return type.name()+" "+site+" from "+target;
  }
  
  
  private float[] totalMaterial() {
    Actor actor = (Actor) this.active;
    float total[] = new float[5];
    total[0] += total[1] = site == null ? 0 : site.materialLevel(material);
    total[0] += total[2] = actor.carried(material);
    total[0] += total[3] = store.inventory(material);
    if (site != null && site.type().isBuilding() && site != store) {
      total[0] += total[4] = ((Building) site).inventory(material);
    }
    return total;
  }
  
  
  private void checkTotalsDiff(float oldT[], float newT[]) {
    if (Nums.abs(oldT[0] - newT[0]) > 0.001f && material != VOID) {
      I.say("Diff in "+material+" for "+site);
      I.say("  Old: "+oldT);
      I.say("  New: "+newT);
      I.say("  ???");
    }
  }
}






