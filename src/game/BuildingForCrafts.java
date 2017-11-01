

package game;
import util.*;
import static game.Task.*;
import static game.GameConstants.*;



public class BuildingForCrafts extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  float craftProgress;
  boolean stalled = false;
  
  
  BuildingForCrafts(Type type) {
    super(type);
  }
  
  
  public BuildingForCrafts(Session s) throws Exception {
    super(s);
    craftProgress = s.loadFloat();
    stalled = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat(craftProgress);
    s.saveBool(stalled);
  }
  
  
  
  /**  Life-cycle, update and economic functions-
    */
  void enterMap(CityMap map, int x, int y, float buildLevel) {
    super.enterMap(map, x, y, buildLevel);
  }
  
  
  void update() {
    super.update();
    advanceProduction();
  }
  
  
  float demandFor(Good g) {
    float need = Visit.arrayIncludes(needed(), g) ? stockNeeded(g) : 0;
    return super.demandFor(g) + need;
  }
  
  
  void advanceProduction() {
    if (craftProgress > 1) return;
    
    boolean anyRoom = false, allMaterials = true;
    
    for (Good made : produced()) {
      if (inventory.valueFor(made) < stockLimit(made)) anyRoom = true;
    }
    for (Good need : needed()) {
      if (inventory.valueFor(need) <= 0) allMaterials = false;
    }
    
    stalled = (! allMaterials) || (! anyRoom);
    
    
    //  TODO:  Move this out into dedicated Tasks...
    
    if (! stalled) {
      float prog = 1f / type.craftTime;
      
      for (Good need : needed()) {
        inventory.add(0 - prog, need);
      }
      craftProgress = Nums.min(craftProgress + prog, 1);
      
      if (craftProgress >= 1) {
        for (Good made : produced()) {
          if (inventory.valueFor(made) >= stockLimit(made)) continue;
          inventory.add(1, made);
          map.city.makeTotals.add(1, made);
        }
        craftProgress = 0;
      }
    }
  }
  
  
  float craftProgress() {
    return craftProgress;
  }
  
  
  
  /**  Handling actor behaviours:
    */
  public void selectActorBehaviour(Actor actor) {
    //
    //  Try and find a nearby building to construct:
    Building builds = selectBuildTarget(this, type.buildsWith, map.buildings);
    if (builds != null) {
      actor.embarkOnVisit(builds, 10, JOB.BUILDING, this);
      return;
    }
    //
    //  Failing that, go here if you aren't already:
    if (actor.inside != this) {
      actor.returnTo(this);
      return;
    }
    //
    //  If you're already home, see if any deliveries are required:
    Task delivery = TaskDelivery.pickNextDelivery(actor, this, produced());
    if (delivery != null) {
      actor.assignTask(delivery);
      return;
    }
    //
    //  And failing all that, start crafting:
    if (! stalled) {
      actor.embarkOnVisit(this, -1, JOB.CRAFTING, this);
    }
  }
  
  
  public void actorEnters(Actor actor, Building enters) {
    return;
  }
  
  
  public void actorVisits(Actor actor, Building visits) {
    if (actor.jobType() == JOB.BUILDING) {
      advanceBuilding(actor, type.buildsWith, visits);
    }
  }
  
  
  
  /**  Supplementary methods for building construction-
    */
  static Building selectBuildTarget(
    Building from, Good buildsWith[], Series <Building> buildings
  ) {
    int maxRange = MAX_WANDER_RANGE;
    Pick <Building> pickB = new Pick();
    
    for (Good w : buildsWith) {
      for (Building b : buildings) {
        if (b.buildLevel() >= 1) continue;
        
        int   need       = b.type.materialNeed(w);
        float amountDone = b.materials.valueFor(w);
        float amountGot  = b.inventory.valueFor(w);
        float dist       = CityMap.distance(from.entrance(), b.entrance());
        float distRating = CityMap.distancePenalty(dist);
        if (amountDone >= need || amountGot <= 0) continue;
        if (dist > maxRange) continue;
        
        pickB.compare(b, (need - amountDone) * distRating);
      }
    }
    return pickB.result();
  }
  
  
  static void advanceBuilding(Actor builds, Good buildsWith[], Building b) {
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
    
    if (didWork) {
      b.setBuildLevel(1.1f * (totalDone / totalNeed));
      
      if (builds.reports()) {
        I.say("\nBuilding "+b+"...");
        I.say("  Did: "+totalDone+"/"+totalNeed);
        I.say("  Build level: "+b.buildLevel());
      }
    }
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






