

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;




public class BuildingForHunt extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public BuildingForHunt(Type type) {
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
    if (delivery != null) {
      return delivery;
    }
    
    Task coming = returnActorHere(actor);
    if (coming != null) return coming;
    
    TaskHunting hunting = new TaskHunting(actor);
    hunting = hunting.configHunting(this, produced());
    if (hunting != null) return hunting;
    
    TaskExplore exploring = TaskExplore.configExploration(actor);
    if (exploring != null) return exploring;
    
    return null;
  }
  
}







