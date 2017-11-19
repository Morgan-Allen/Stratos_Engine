

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
    boolean consumes = accessible() && Visit.arrayIncludes(needed(), g);
    float need = consumes ? stockNeeded(g) : 0;
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
    //  Different construction approach...
    Task building2 = TaskBuilding2.configBuilding2(this, actor);
    if (building2 != null) {
      actor.assignTask(building2);
      return;
    }
    //
    //  Try and find a nearby building to construct:
    if (TaskBuilding.pickBuildTask(actor, this, map.buildings)) {
      return;
    }
    //
    //  Go here if you aren't already:
    if (! actorIsHereWithPrompt(actor)) return;
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
  
  
}






