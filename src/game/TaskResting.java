

package game;
import static game.GameConstants.*;
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
  public static Building findRestVenue(Actor actor, Area map) {
    Pick <Building> pick = new Pick();
    for (Building b : map.buildings()) {
      
      if (b.base() != actor.base() || ! b.complete()) continue;
      if (b != actor.home() && ! b.type().hasFeature(IS_REFUGE)) continue;
      
      float rating = 1f;
      rating *= Area.distancePenalty(actor, b);
      rating *= restUrgency(actor, b);
      
      if (b == actor.home()) rating *= 3;
      else rating /= 2 + b.visitors().size();
      
      pick.compare(b, rating);
    }
    return pick.result();
  }
  
  
  public static TaskResting configResting(Actor actor, Building rests) {
    if (actor == null || rests == null) return null;
    
    TaskResting t = new TaskResting(actor);
    return (TaskResting) t.configTask(rests, rests, null, JOB.RESTING, 10);
  }
  
  
  public static Batch <Good> menuAt(Building visits, Actor actor) {
    Batch <Good> menu = new Batch();
    for (Good g : visits.inventory().keys()) {
      if (g.isEdible) menu.add(g);
    }
    return menu;
  }
  
  
  static float restUrgency(Actor actor, Building rests) {
    if (actor == null || rests == null || ! rests.complete()) return -1;
    
    Batch <Good> menu = menuAt(rests, actor);
    float hurtRating = actor.health.fatigue() + actor.health.injury();
    hurtRating += menu.size() > 0 ? actor.health.hunger() : 0;
    return hurtRating / actor.health.maxHealth();
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float successPriority() {
    Actor actor = (Actor) active;
    return restUrgency(actor, visits) * PARAMOUNT;
  }
  
  
  protected void onVisit(Building visits) {
    Actor actor = (Actor) active;
    if (actor.health.hunger() >= 1f / HUNGER_REGEN) {
      Batch <Good> menu = menuAt(visits, actor);
      boolean adult = actor.health.adult();
      
      if (menu.size() > 0) for (Good g : menu) {
        float eats = 1f / (menu.size() * HUNGER_REGEN);
        if (! adult) eats /= 2;
        eats = Nums.min(eats, visits.inventory(g));
        visits.addInventory(0 - eats, g);
        actor.health.liftHunger(eats / FOOD_UNIT_PER_HP);
      }
    }
  }
  
  
  protected void onVisitEnds(Building visits) {
    if (priority() >= IDLE) {
      configTask(visits, visits, null, JOB.RESTING, 10);
    }
  }
}






