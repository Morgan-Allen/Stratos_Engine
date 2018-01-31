

package game;
import util.*;
import static game.Task.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class BuildingForGovern extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public BuildingForGovern(Type type) {
    super(type);
  }
  
  
  public BuildingForGovern(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Assigning actor behaviours:
    */
  public void selectActorBehaviour(Actor actor) {
    
    if (actor.type().isCommoner()) {
      Task building = TaskBuilding.nextBuildingTask(this, actor);
      if (building != null) {
        actor.assignTask(building);
        return;
      }
    }
    
    if (actor.type().isSoldier()) {
      //  TODO:  Assign some patrolling tasks, or just rely on the actor's own
      //  behaviour-script.
    }
    
    if (actor.type().isTrader()) {
      Task taxing = TaskAssessTax.nextAssessment(actor, this, 100);
      if (taxing != null) actor.assignTask(taxing);
    }
    
    if (actor.type().isNoble()) {
      actor.beginResting(this);
    }
  }
  
  
  
}







