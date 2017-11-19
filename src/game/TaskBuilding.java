

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;




public class TaskBuilding extends Task {
  
  
  Building builds;
  
  
  public TaskBuilding(Actor actor, Building builds) {
    super(actor);
    this.builds = builds;
  }
  
  
  public TaskBuilding(Session s) throws Exception {
    super(s);
    builds = (Building) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(builds);
  }
  
  
  
  static float needForBuilding(Building b, Good buildsWith[]) {
    if (b.buildLevel() >= 1) return -1;
    float sumNeeds = 0;
    
    for (Good w : buildsWith) {
      int   need       = b.type.materialNeed(w);
      float amountDone = b.materials.valueFor(w);
      float amountGot  = b.inventory.valueFor(w);
      if (amountDone >= need || amountGot <= 0) continue;
      sumNeeds += Nums.max(0, need - amountDone); 
    }
    
    return sumNeeds;
  }
  
  
  static Building selectBuildTarget(
    Building from, Good buildsWith[], Series <Building> buildings
  ) {
    int maxRange = MAX_WANDER_RANGE;
    Pick <Building> pickB = new Pick();

    for (Building b : buildings) {
      float dist = CityMap.distance(from.entrance(), b.entrance());
      if (dist > maxRange) continue;
      
      float need = needForBuilding(b, buildsWith);
      if (need <= 0) continue;
      
      float distRating = CityMap.distancePenalty(dist);
      pickB.compare(b, need * distRating);
    }
    
    return pickB.result();
  }
  
  
  static TaskBuilding configBuilding(Building site, Actor a, Building from) {
    TaskBuilding task = new TaskBuilding(a, site);
    
    Tile t = site.entrance();
    if (task.configTask(from, null, t, JOB.BUILDING, 1) != null) {
      return task;
    }
    
    return null;
  }
  
  
  static Series <Building> nearbyBuildings(Actor a) {
    Tile t = a.at();
    Box2D area = new Box2D(t.x, t.y, 0, 0);
    area.expandBy(2);
    Batch <Building> matches = new Batch();
    
    for (Coord c : Visit.grid(area)) {
      Tile u = a.map.tileAt(c);
      if (u != null && u.above != null && u.above.type.isBuilding()) {
        matches.include((Building) u.above);
      }
    }
    return matches;
  }
  
  
  static boolean pickBuildTask(
    Actor a, Building from, Series <Building> across
  ) {
    Building builds = selectBuildTarget(from, from.type.buildsWith, across);
    if (builds == null) return false;
    
    TaskBuilding building = TaskBuilding.configBuilding(builds, a, from);
    if (building == null) return false;
    
    a.assignTask(building);
    return true;
  }
  
  
  protected void onTarget(Target target) {
    if (target == builds.entrance()) {
      Building from = (Building) origin;
      advanceBuilding(actor, from.type.buildsWith, builds);
      //
      //  Check to see if we can keep building the same structure...
      if (needForBuilding(builds, from.type.buildsWith) > 0) {
        Tile t = builds.entrance();
        configTask(from, null, t, JOB.BUILDING, 1);
      }
      //
      //  ...or another structure nearby.  If that fails, return to base.
      else if (! pickBuildTask(actor, from, nearbyBuildings(actor))) {
        from.returnActorHere(actor);
      }
    }
  }
  
  
  void advanceBuilding(Actor builds, Good buildsWith[], Building b) {
    float totalNeed = 0, totalDone = 0;
    boolean didWork = false;
    
    for (Good g : buildsWith) {
      int   need       = b.type.materialNeed(g);
      float amountDone = b.materials.valueFor(g);
      float amountGot  = b.inventory.valueFor(g);
      
      totalNeed += need;
      totalDone += amountDone;
      
      if (amountDone >= need || amountGot <= 0) continue;
      
      float puts = Nums.min(0.1f, amountGot);
      b.materials.add(puts    , g);
      b.inventory.add(0 - puts, g);
      didWork = true;
    }
    
    if (builds.reports()) {
      I.say("Trying to build "+b);
      I.say("  Progress? "+didWork+" "+I.percent(b.buildLevel()));
      I.say("  Did:      "+totalDone+"/"+totalNeed);
    }
    
    if (didWork) {
      b.setBuildLevel(1.1f * (totalDone / totalNeed));
    }
  }
  
  
  void updateTileOccupation(Building b) {
    
    
    
  }
  
  
  //  TODO:  Take building-tiers into account for this...
  /*
  static void updateBuildLevel(Building b) {
    float totalNeed = 0, totalDone = 0;
    for (Good g : b.type.builtFrom) {
      int   need       = b.type.materialNeed(g);
      float amountDone = b.materials.valueFor(g);
      
      totalNeed += need;
      totalDone += amountDone;
    }
    b.buildLevel = 1.5f * (totalDone / totalNeed);
  }
  //*/
  
  
  
}















