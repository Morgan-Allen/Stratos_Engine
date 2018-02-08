

package game;
import static game.GameConstants.*;



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
      if (building != null) {
        return building;
      }
    }
    
    if (actor.type().isSoldier()) {
      //  TODO:  Assign some patrolling tasks, or just rely on the actor's own
      //  behaviour-script.
    }
    
    if (actor.type().isTrader()) {
      Task taxing = TaskAssessTax.nextAssessment(actor, this, 100);
      if (taxing != null) return taxing;
    }
    
    if (actor.type().isNoble()) {
      return actor.restingTask(this);
    }
    
    return null;
  }
  
  
  
}







