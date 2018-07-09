

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
  
  
  public static TaskResting nextRelaxing(Actor actor) {
    Area map = actor.map();
    Pick <Building> pickV = new Pick(0);
    
    for (Building b : map.buildings()) {
      if (! b.type().hasFeature(DIVERSION)) continue;
      if (TaskCombat.hostile(actor, b)) continue;
      
      float dist = Area.distance(actor, b);
      if (dist > MAX_WANDER_RANGE) continue;
      
      float rating = 1.0f;
      rating *= Area.distancePenalty(dist);
      rating *= b.type().featureAmount;
      rating *= (actor.base().relations.loyalty(b.base()) + 1) / 2;
      rating *= Rand.num();
      pickV.compare(b, rating);
    }
    
    if (pickV.empty()) return null;
    
    Building goes = pickV.result();

    TaskResting t = new TaskResting(actor);
    return (TaskResting) t.configTask(null, goes, null, JOB.RESTING, 10);
  }
  
  
  public static TaskResting nextResting(Actor actor) {
    if (actor == null) return null;
    return nextResting(actor, actor.haven());
  }
  
  
  public static TaskResting nextResting(Actor actor, Pathing haven) {
    if (actor == null || haven == null) return null;
    
    TaskResting t = new TaskResting(actor);
    return (TaskResting) t.configTask(null, haven, null, JOB.RESTING, 10);
  }
  
  
  public static Batch <Good> menuAt(Pathing visits, Actor actor) {
    Batch <Good> menu = new Batch();
    if (! (visits.type().isBuilding() || visits.type().isVessel())) {
      return menu;
    }
    for (Good g : ((Carrier) visits).inventory().keys()) {
      if (g.isEdible) menu.add(g);
    }
    return menu;
  }
  
  
  static float restUrgency(Actor actor, Pathing rests) {
    if (actor == null || rests == null || ! rests.complete()) return -1;
    
    Batch <Good> menu = menuAt(rests, actor);
    float hurtRating = actor.health.fatigue() + actor.health.injury();
    hurtRating += menu.size() > 0 ? actor.health.hunger() : 0;
    return hurtRating / actor.health.maxHealth();
  }

  
  
  
  /**  Priority-evaluation-
    */
  protected float successPriority() {
    Actor actor = (Actor) active;
    float diligence = (actor.traits.levelOf(TRAIT_DILIGENCE) + 1) / 2;
    float urgency = restUrgency(actor, visits);
    urgency *= 1.5f - diligence;
    return urgency * PARAMOUNT;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected void onVisit(Pathing visits) {
    Actor actor = (Actor) active;
    
    if (actor.health.hunger() >= 1f / HUNGER_REGEN) {
      Batch <Good> menu = menuAt(visits, actor);
      boolean adult = actor.health.adult();
      
      if (menu.size() > 0) for (Good g : menu) {
        float eats = 1f / (menu.size() * HUNGER_REGEN);
        if (! adult) eats /= 2;
        
        Carrier haven = (Carrier) visits;
        eats = Nums.min(eats, haven.inventory().valueFor(g));
        haven.inventory().add(0 - eats, g);
        actor.health.liftHunger(eats / FOOD_UNIT_PER_HP);
      }
    }
    
    if (visits instanceof Building) {
      Building b = (Building) visits;
      for (Good g : b.needed()) {
        float carries = actor.outfit.carried(g);
        if (carries > 0) {
          actor.outfit.setCarried(g, 0);
          b.addInventory(carries, g);
        }
      }
    }
  }
  
  
  protected void onVisitEnds(Pathing visits) {
    //Actor actor = (Actor) active;
    if (priority() >= IDLE) {
      configTask(origin, visits, null, JOB.RESTING, 10);
    }
  }
}


