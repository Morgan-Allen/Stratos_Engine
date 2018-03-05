

package game;
import util.*;
import static game.AreaMap.*;
import static game.GameConstants.*;




public class BuildingForHunt extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public BuildingForHunt(BuildType type) {
    super(type);
  }
  
  
  public BuildingForHunt(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Handling walker behaviours-
    */
  public Task selectActorBehaviour(Actor actor) {
    
    Task delivery = TaskDelivery.pickNextDelivery(actor, this, produced());
    if (delivery != null) return delivery;
    
    TaskHunting hunt = TaskHunting.configHunting(actor, this, produced());
    if (hunt != null) return hunt;
    
    TaskExplore exploring = TaskExplore.configExploration(actor);
    if (exploring != null) return exploring;
    
    return null;
  }
  
}




