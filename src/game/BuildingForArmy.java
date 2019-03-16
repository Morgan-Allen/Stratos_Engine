

package game;
import util.*;
import static game.AreaMap.*;
import static game.GameConstants.*;



public class BuildingForArmy extends Building {
  
  
  /**  Data fields, setup and save/load methods-
    */
  public BuildingForArmy(BuildType type) {
    super(type);
  }
  
  
  public BuildingForArmy(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Regular updates and worker assignments-
    */
  public Task selectActorBehaviour(Actor actor) {
    
    TaskPatrol patrol = TaskPatrol.nextGuardPatrol(actor, this);
    if (patrol != null) return patrol;
    
    return null;
  }
  
}









