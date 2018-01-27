

package game;
import util.*;
import static game.GameConstants.*;



public class BuildingForCrafts extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  float craftProgress;
  
  
  public BuildingForCrafts(Type type) {
    super(type);
  }
  
  
  public BuildingForCrafts(Session s) throws Exception {
    super(s);
    craftProgress = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat(craftProgress);
  }
  
  
  
  /**  Life-cycle, update and economic functions-
    */
  public void enterMap(CityMap map, int x, int y, float buildLevel) {
    super.enterMap(map, x, y, buildLevel);
  }
  
  
  public float demandFor(Good g) {
    boolean consumes = accessible() && Visit.arrayIncludes(needed(), g);
    float need = consumes ? stockNeeded(g) : 0;
    return super.demandFor(g) + need;
  }
  
  
  boolean canAdvanceCrafting() {
    boolean anyRoom = false, allMaterials = true;
    
    for (Good made : produced()) {
      if (inventory(made) < stockLimit(made)) anyRoom = true;
    }
    for (Good need : needed()) {
      if (inventory(need) <= 0) allMaterials = false;
    }
    
    return anyRoom && allMaterials;
  }
  
  
  public float craftProgress() {
    return craftProgress;
  }
  
  
  
  /**  Handling actor behaviours:
    */
  public void selectActorBehaviour(Actor actor) {
    //
    //  Different construction approach...
    Task building = TaskBuilding.nextBuildingTask(this, actor);
    if (building != null) {
      actor.assignTask(building);
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
    if (canAdvanceCrafting()) {
      TaskCrafting task = TaskCrafting.configCrafting(actor, this);
      actor.assignTask(task);
      return;
    }
  }
  
  
}






