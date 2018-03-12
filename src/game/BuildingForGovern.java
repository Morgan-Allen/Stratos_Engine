

package game;
import static game.GameConstants.*;
import util.*;



public class BuildingForGovern extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public BuildingForGovern(BuildType type) {
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
  public Task selectActorBehaviour(Actor actor) {
    
    if (actor.type().isCommoner()) {
      Task building = TaskBuilding.nextBuildingTask(this, actor);
      if (building != null) return building;
      
      Task tending = TaskWaiting.configWaiting(actor, this);
      if (tending != null) return tending;
    }
    
    if (actor.type().isSoldier()) {
      Task patrol = TaskPatrol.nextGuardPatrol(actor, this, Task.ROUTINE);
      if (patrol != null) return patrol;
    }
    
    if (actor.type().isTrader()) {
      Task taxing = TaskAssessTax.nextAssessment(actor, this, 100);
      if (taxing != null) return taxing;
      
      Task tending = TaskWaiting.configWaiting(actor, this);
      if (tending != null) return tending;
    }
    
    if (actor.type().isNoble()) {
      return TaskWaiting.configWaiting(actor, this, TaskWaiting.TYPE_VIP_STAY);
    }
    
    return null;
  }
  
  
  
}







