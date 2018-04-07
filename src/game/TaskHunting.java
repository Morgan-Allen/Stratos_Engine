

package game;
import util.*;
import static game.GameConstants.*;
import static game.TaskCombat.*;



public class TaskHunting extends Task {
  
  
  /**  Data fields, construction and save/load methods-
    */
  Building store;
  Actor prey;
  int attackMode = ATTACK_NONE;
  
  
  public TaskHunting(Actor actor, Building store, Actor prey) {
    super(actor);
    this.store = store;
    this.prey  = prey;
  }
  
  
  public TaskHunting(Session s) throws Exception {
    super(s);
    store = (Building) s.loadObject();
    prey  = (Actor   ) s.loadObject();
    attackMode = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(store);
    s.saveObject(prey );
    s.saveInt(attackMode);
  }
  
  
  
  /**  External factory methods and priority-evaluation-
    */
  static TaskHunting configHunting(
    Actor actor, Building store, Good... meatTypes
  ) {
    Pick <Actor> forHunt = new Pick();
    for (Actor a : actor.map().actors()) {
      if (! a.type().isAnimal()                 ) continue;
      if (a.type().predator || a.growLevel() < 1) continue;
      if (a.maxSightLevel(actor.base()) == 0    ) continue;
      if (hasTaskFocus(a, JOB.HUNTING)          ) continue;
      
      Good meat = a.type().meatType;
      if (! Visit.arrayIncludes(meatTypes, meat)) continue;
      if (store.inventory(meat) >= store.type().maxStock) continue;
      
      float dist = Area.distance(actor.at(), a.at());
      if (dist > MAX_EXPLORE_DIST) continue;
      
      //  TODO:  Check to make sure there's pathing access...
      forHunt.compare(a, Area.distancePenalty(dist));
    }
    
    if (forHunt.empty()) return null;
    Actor prey = forHunt.result();
    boolean melee = actor.meleeDamage() > actor.rangeDamage();
    
    TaskHunting hunt = new TaskHunting(actor, store, prey);
    hunt.attackMode = melee ? ATTACK_MELEE : ATTACK_RANGE;
    return (TaskHunting) hunt.configTask(store, null, prey, JOB.HUNTING, 0);
  }
  
  
  protected float successPriority() {
    if (type != JOB.HUNTING) return ROUTINE;
    Actor actor = (Actor) this.active;
    float combat = TaskCombat.attackPriority(actor, prey, false);
    return Nums.max(ROUTINE, combat);
  }
  
  
  protected float successChance() {
    if (type != JOB.HUNTING) return 1;
    Actor actor = (Actor) this.active;
    return TaskCombat.attackChance(actor, prey);
  }
  
  
  public boolean emergency() {
    if (type != JOB.HUNTING) return false;
    Actor actor = (Actor) this.active;
    if (Area.distance(active, prey) > actor.sightRange()) return false;
    return true;
  }

  
  
  /**  Behaviour-execution-
    */
  float actionRange() {
    if (type != JOB.HUNTING) return super.actionRange();
    if (attackMode == ATTACK_MELEE) return 1.5f;
    else return Nums.max(1.5f, ((Element) active).attackRange());
  }
  
  
  protected void onTarget(Target target) {
    Actor actor = (Actor) this.active;
    
    if (target == prey && type == JOB.HUNTING) {
      if (prey.health.alive()) {
        actor.performAttack(prey, attackMode == ATTACK_MELEE);
      }
      if (prey.health.dead()) {
        AreaTile site = prey.at();
        configTask(store, null, site, JOB.COLLECTING, 0);
      }
      else {
        configTask(store, null, prey, JOB.HUNTING, 0);
      }
    }
    else if (type == JOB.COLLECTING) {
      float yield = ActorAsAnimal.meatYield(prey);
      actor.outfit.incCarried(prey.type().meatType, yield);
      configTask(store, store, null, JOB.DELIVER, 0);
    }
  }


  protected void onVisit(Building visits) {
    Actor actor = (Actor) this.active;
    if (visits == store) {
      actor.outfit.offloadGood(prey.type().meatType, visits);
    }
  }
  
}








