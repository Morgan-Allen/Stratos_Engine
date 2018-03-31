

package game;
import util.*;
import static game.Area.*;
import static game.GameConstants.*;



//  TODO:  Extend TaskCombat?


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
  static TaskHunting configHunting(
    Actor actor, Building store, Good... meatTypes
  ) {
    Pick <Actor> forHunt = new Pick();
    for (Actor a : actor.map().actors) {
      if (! a.type().isAnimal()                 ) continue;
      if (a.type().predator || a.growLevel() < 1) continue;
      if (a.maxSightLevel(actor.base()) == 0    ) continue;
      if (hasTaskFocus(a, JOB.HUNTING)          ) continue;
      
      Good meat = a.type().meatType;
      if (! Visit.arrayIncludes(meatTypes, meat)) continue;
      if (store.inventory(meat) >= store.type().maxStock) continue;
      
      float dist = Area.distance(actor.at(), a.at());
      if (dist > MAX_EXPLORE_DIST) continue;
      
      //  TODO:  Check to make sure there's pathing access!
      
      forHunt.compare(a, Area.distancePenalty(dist));
    }
    if (forHunt.empty()) return null;
    
    TaskHunting hunt = new TaskHunting(actor);
    hunt.store = store;
    hunt.prey  = forHunt.result();
    return (TaskHunting) hunt.configTask(store, null, hunt.prey, JOB.HUNTING, 0);
  }
  
  
  
  protected void onTarget(Target target) {
    Actor actor = (Actor) this.active;
    if (target == prey) {
      AreaTile site = prey.at();
      
      boolean melee = actor.meleeDamage() > actor.rangeDamage();
      actor.performAttack(prey, melee);
      
      if (! prey.health.dead()) {
        configTask(store, null, prey, JOB.HUNTING, 0);
      }
      else {
        configTask(store, null, site, JOB.DELIVER, 0);
      }
    }
    else {
      float yield = ActorAsAnimal.meatYield(prey);
      actor.outfit.incCarried(prey.type().meatType, yield);
      configTask(store, store, null, JOB.DELIVER, 0);
    }
  }
  
  
  protected void onVisit(Building visits) {
    Actor actor = (Actor) this.active;
    if (visits != store) return;
    actor.outfit.offloadGood(prey.type().meatType, visits);
  }
  
}








