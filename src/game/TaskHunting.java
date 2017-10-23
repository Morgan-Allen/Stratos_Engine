

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class TaskHunting extends Task {
  
  
  /**  Data fields, construction and save/load methods-
    */
  Building store;
  Actor prey;
  
  
  public TaskHunting(Actor actor) {
    super(actor);
  }
  
  
  public TaskHunting(Session s) throws Exception {
    super(s);
    store = (Building) s.loadObject();
    prey  = (Actor   ) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(store);
    s.saveObject(prey );
  }
  
  
  
  /**  Scripting and events-
    */
  TaskHunting configHunting(Building store) {
    
    Pick <Actor> forHunt = new Pick();
    for (Actor a : actor.map.walkers) {
      if (a.maxSightLevel() == 0               ) continue;
      if (a.type.category != Type.IS_ANIMAL_ACT) continue;
      if (a.type.predator || a.growLevel() < 1 ) continue;
      if (hasTaskFocus(a, JOB.HUNTING)         ) continue;
      
      float dist = CityMap.distance(actor.at(), a.at());
      if (dist > MAX_EXPLORE_DIST) continue;
      
      forHunt.compare(a, CityMap.distancePenalty(dist));
    }
    if (forHunt.empty()) return null;
    
    this.store = store;
    this.prey  = forHunt.result();
    
    return (TaskHunting) configTask(store, null, prey, JOB.HUNTING, 0);
  }
  
  
  
  protected void onTarget(Target target) {
    if (target == prey) {
      Tile site = prey.at();
      actor.performAttack(prey);
      
      if (! prey.dead()) {
        configTask(store, null, prey, JOB.HUNTING, 0);
      }
      else {
        configTask(store, null, site, JOB.DELIVER, 0);
      }
    }
    else {
      float yield = ActorAsAnimal.meatYield(prey);
      actor.setCarried(MEAT, yield);
      configTask(store, store, null, JOB.DELIVER, 0);
    }
  }
  
  
  protected void onVisit(Building visits) {
    if (visits != store) return;
    actor.offloadGood(MEAT, visits);
  }
  
}








