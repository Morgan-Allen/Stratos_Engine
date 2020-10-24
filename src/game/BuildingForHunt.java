

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
  
  public void loadState(Session s) throws Exception {
    super.loadState(s);
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Handling walker behaviours-
    */
  public void selectActorBehaviour(Actor actor) {
    
    Task delivery = TaskDelivery.pickNextDelivery(actor, this, produced());
    if (actor.idle() && delivery != null) {
      actor.assignTask(delivery);
    }
    
    if (actor.idle() && ! actorIsHere(actor)) {
      returnActorHere(actor);
    }
    
    if (actor.idle() && inventory.valueFor(MEAT) < type.maxStock) {
      TaskHunting hunting = new TaskHunting(actor);
      hunting = hunting.configHunting(this);
      if (hunting != null) actor.assignTask(hunting);
    }
    
    if (actor.idle()) {
      TaskExplore exploring = new TaskExplore(actor);
      exploring = exploring.configExploration();
      if (exploring != null) actor.assignTask(exploring);
    }
  }
  
}




