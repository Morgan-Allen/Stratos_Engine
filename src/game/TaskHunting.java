

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
  static Building findDropoffPoint(Good meat) {
    return null;
  }
  
  
  static TaskHunting nextHunting(Actor actor) {
    return nextHunting(actor, null);
  }
  
  
  static TaskHunting nextHunting(
    Actor actor, Building store, Good... meatTypes
  ) {
    Pick <Actor> forHunt = new Pick();
    boolean forMeat = store != null && ! Visit.empty(meatTypes);
    
    for (Actor a : actor.considered()) {
      float dist = AreaMap.distance(actor, a);
      if (dist > MAX_EXPLORE_DIST) continue;
      
      if (forMeat) {
        if (hasTaskFocus(a, JOB.HUNTING)) continue;
        if (! a.type().isAnimal()) continue;
        if (a.type().predator || a.growLevel() < 1) continue;
        
        Good meat = a.type().meatType;
        if (! Visit.arrayIncludes(meatTypes, meat)) continue;
        if (store.inventory(meat) >= store.type().maxStock) continue;
      }
      
      forHunt.compare(a, AreaMap.distancePenalty(dist));
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
    float combat = TaskCombat.attackPriority(actor, prey, false, false);
    if (store != null) combat = Nums.max(ROUTINE, combat);
    return combat;
  }
  
  
  protected float successChance() {
    if (type != JOB.HUNTING) return 1;
    Actor actor = (Actor) this.active;
    return TaskCombat.attackChance(actor, prey);
  }
  
  
  protected float failCostPriority() {
    if (type != JOB.HUNTING) return 0;
    return PARAMOUNT;
  }
  
  
  public boolean emergency() {
    if (type != JOB.HUNTING) return false;
    Actor actor = (Actor) this.active;
    if (AreaMap.distance(active, prey) > actor.sightRange()) return false;
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
      if (prey.health.alive()) {
        configTask(store, null, prey, JOB.HUNTING, 0);
      }
      else if (store != null) {
        AreaTile site = prey.at();
        configTask(store, null, site, JOB.COLLECTING, 0);
      }
    }
    else if (type == JOB.COLLECTING && prey.onMap()) {
      float yield = ActorAsAnimal.meatYield(prey);
      prey.exitMap(actor.map());
      actor.outfit.incCarried(prey.type().meatType, yield);
      configTask(store, store, null, JOB.DELIVER, 0);
    }
  }
  
  
  protected void onVisit(Pathing visits) {
    Actor actor = (Actor) this.active;
    if (visits == store) {
      actor.outfit.offloadGood(prey.type().meatType, (Carrier) visits);
    }
  }
  
}








