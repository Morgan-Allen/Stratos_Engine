

package game;
import static game.GameConstants.*;

import game.GameConstants.Good;
import util.*;




public class TaskResting extends Task {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public TaskResting(Actor actor) {
    super(actor);
  }
  
  
  public TaskResting(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Factory methods for outside access and other helper functions-
    */
  static TaskResting configResting(Actor actor, Building rests) {
    if (actor == null || rests == null) return null;
    
    TaskResting t = new TaskResting(actor);
    return (TaskResting) t.configTask(rests, rests, null, JOB.RESTING, 10);
  }
  
  
  static Batch <Good> menuAt(Building visits, Actor actor) {
    Batch <Good> menu = new Batch();
    if (actor.type().foodsAllowed == null) {
      return menu;
    }
    if (visits != null) for (Good g : actor.type().foodsAllowed) {
      if (visits.inventory(g) > 0) menu.add(g);
    }
    return menu;
  }
  
  
  static float restUrgency(Actor actor, Building rests) {
    if (actor == null || rests == null || ! rests.complete()) return -1;
    
    Batch <Good> menu = menuAt(rests, actor);
    float hurtRating = actor.fatigue() + actor.injury();
    hurtRating += menu.size() > 0 ? actor.hunger() : 0;
    return hurtRating / actor.maxHealth();
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float successPriority() {
    Actor actor = (Actor) active;
    return restUrgency(actor, visits) * PARAMOUNT;
  }
  
  
  protected void onVisit(Building visits) {
    Actor actor = (Actor) active;
    if (actor.hunger() >= 1f / HUNGER_REGEN) {
      Batch <Good> menu = menuAt(visits, actor);
      boolean adult = actor.adult();
      
      if (menu.size() > 0) for (Good g : menu) {
        float eats = 1f / (menu.size() * HUNGER_REGEN);
        if (! adult) eats /= 2;
        visits.addInventory(0 - eats, g);
        actor.hunger -= eats / FOOD_UNIT_PER_HP;
      }
    }
  }
  
  
  protected void onVisitEnds(Building visits) {
    Actor actor = (Actor) active;
    if (priority() >= IDLE) {
      actor.assignTask(configResting(actor, visits));
    }
  }
}






