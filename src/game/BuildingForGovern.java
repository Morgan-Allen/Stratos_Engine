

package game;
import static game.GameConstants.*;
import util.*;



public class BuildingForGovern extends BuildingForTrade {
  
  
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
  
  
  
  /**  Accomodating actors-
    */
  public boolean allowsResidence(Actor actor) {
    int maxR = type().maxResidents;
    if (maxR <= 0) return false;
    if (actor.work() == this) return true;
    else return false;
  }
  
  
  
  /**  Assigning actor behaviours:
    */
  public Task selectActorBehaviour(Actor actor) {
    
    if (actor.type().isCommoner()) {
      //  TODO:  THIS IS AN UGLY HACK AND SHOULD BE FIXED ASAP!
      
      if (actor.traits.levelOf(SKILL_WRITE) > 0) {
        Task taxing = TaskAssessTax.nextAssessment(actor, this, 100);
        if (taxing != null) return taxing;
        
        Task tending = TaskWaiting.configWaiting(actor, this);
        if (tending != null) return tending;
      }
      
      else {
        Task delivery = TaskDelivery.pickNextDelivery(actor, this, 0, produced());
        if (delivery != null) return delivery;
        
        Task building = TaskBuilding.nextBuildingTask(this, actor);
        if (building != null) return building;
        
        Task tending = TaskWaiting.configWaiting(actor, this);
        if (tending != null) return tending;
      }
    }
    
    if (actor.type().isSoldier()) {
      Task patrol = TaskPatrol.nextGuardPatrol(actor, this);
      if (patrol != null) return patrol;
    }
    
    if (actor.type().isNoble()) {
      return TaskWaiting.configWaiting(actor, this, TaskWaiting.TYPE_VIP_STAY);
    }
    
    return null;
  }
  
  
  
}







