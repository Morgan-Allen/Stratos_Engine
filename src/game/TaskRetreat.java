


package game;
import static game.GameConstants.*;
import util.*;




public class TaskRetreat extends Task {
  
  
  /**  Data fields, construction and save/load methods-
    */
  Pathing hides;
  float priorityBonus = 0;
  
  
  public TaskRetreat(Actor actor, Pathing hides) {
    super(actor);
    this.hides = hides;
  }
  
  
  public TaskRetreat(Session s) throws Exception {
    super(s);
    this.hides = (Pathing) s.loadObject();
    this.priorityBonus = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(hides);
    s.saveFloat(priorityBonus);
  }
  
  
  
  /**  Utility method for calculating fear-levels...
    */
  static float fearLevel(Actor actor, List <Active> storeBackup) {
    
    float dangerSum = 0, allySum = 0;
    Batch <Active> aware = new Batch();
    boolean report = false;
    
    for (Active other : actor.seen()) {
      aware.add(other);
    }
    for (Active other : actor.focused()) {
      if (Task.inCombat((Element) other) && other.task().inContact()) {
        aware.include(other);
      }
    }
    
    if (report) {
      I.say("\nComputing fear-level for "+actor);
      I.say("  Aware of (host/ally/powr)");
    }
    
    for (Active other : aware) {
      if (other.indoors() || other.jobType() == JOB.RETREAT) continue;
      
      boolean hostile      = TaskCombat.hostile(other, actor);
      boolean allied       = TaskCombat.allied (other, actor);
      Target  otherFocus   = mainTaskFocus((Element) other);
      float   harmLevel    = otherFocus == null ? 0 : other.task().harmLevel();
      boolean focusHostile = TaskCombat.hostile(actor, otherFocus);
      boolean focusAllied  = TaskCombat.allied (actor, otherFocus);
      
      float hostility = Nums.max(hostile ? 0.5f : 0, focusAllied  ? harmLevel : 0);
      float alliance  = Nums.max(allied  ? 0.5f : 0, focusHostile ? harmLevel : 0);
      float power     = TaskCombat.attackPower((Element) other);
      
      if (hostility > 0) dangerSum += power * hostility;
      if (alliance  > 0) allySum   += power * alliance ;
      
      if (power > 0 && allied && other.mobile()) {
        storeBackup.add((Actor) other);
      }
      
      if (report) {
        I.say("    "+other+" ("+hostility+"/"+alliance+"/"+power+")");
        I.add(" "+other.jobType()+" -> "+otherFocus+"\\"+harmLevel);
      }
    }
    
    if (dangerSum <= 0) {
      if (report) I.say("No danger!");
      return 0;
    }
    
    float lossChance = 0, sumFactors = dangerSum + allySum;
    if (sumFactors > 0) lossChance = dangerSum / sumFactors;
    
    float maxHealth = actor.health.maxHealth();
    float injury    = actor.health.injury () * 1f / maxHealth;
    float fatigue   = actor.health.fatigue() * 1f / maxHealth;
    
    lossChance += injury  * injury  * 1.5f;
    lossChance += fatigue * fatigue * 0.5f;
    
    if (report) {
      I.say("  Overall factors:");
      I.say("    Ally/danger ratio: "+allySum+"/"+dangerSum);
      I.say("    Injury:  "+actor.health.injury() +"/"+maxHealth);
      I.say("    Fatigue: "+actor.health.fatigue()+"/"+maxHealth);
      I.say("    Loss chance: "+lossChance);
    }
    
    return lossChance;
  }
  
  
  
  /**  External factory methods-
    */
  static TaskRetreat configRetreat(Actor actor, Pathing hides, float priority) {
    AreaMap map = actor.map;
    if (! map.world.settings.toggleRetreat) return null;
    
    TaskRetreat hiding = new TaskRetreat(actor, hides);
    hiding.priorityBonus = priority;
    return (TaskRetreat) hiding.configTask(null, hides, null, JOB.RETREAT, 10);
  }
  
  
  static TaskRetreat configRetreat(Actor actor) {
    AreaMap map = actor.map;
    if (! map.world.settings.toggleRetreat) return null;
    
    Pathing home = actor.home();
    if (home == null) home = (Pathing) actor.work();
    Pick <Building> pickHide = new Pick();
    
    for (Building b : map.buildings) {
      if (b != home && b.base() != actor.base()) continue;
      if (b != home && ! b.type().hasFeature(IS_REFUGE)) continue;
      if (! b.complete()) continue;
      
      float rating = 1.0f;
      rating *= AreaMap.distancePenalty(actor, b);
      if (b == home) rating *= 2;
      
      pickHide.compare(b, rating);
    }
    
    if (pickHide.empty()) return null;
    
    Building hides = pickHide.result();
    TaskRetreat hiding = new TaskRetreat(actor, hides);
    
    if (hiding.priority() <= 0) return null;
    
    return (TaskRetreat) hiding.configTask(null, hides, null, JOB.RETREAT, 10);
  }
  
  
  
  /**  Evaluating task-priority-
    */
  protected float successChance() {
    //  TODO:  Rate your chance of escape based on speed relative to other
    //  actors?
    return 1.0f;
  }
  
  
  protected float successPriority() {
    Actor actor = (Actor) this.active;
    float bravery = (actor.traits.levelOf(TRAIT_BRAVERY) + 1) / 2;
    float fearLevel = actor.fearLevel();
    //
    //  Retreat priority stays pretty low as long as the odds are even or
    //  better, then spikes toward being paramount-
    float priority = fearLevel * (1.5f - bravery);
    if (priority < 0.5f) priority *= IDLE;
    if (priority > 0.5f) priority = IDLE + ((priority - 0.5f) * PARAMOUNT * 2);
    priority += priorityBonus;
    return priority;
  }
  
  
  public boolean emergency() {
    return true;
  }
  
  
  
  /**  Actual behaviour-execution-
    */
  int motionMode() {
    return Actor.MOVE_RUN;
  }
  
  
  boolean checkAndUpdateTask() {
    if (! super.checkAndUpdateTask()) return false;
    
    Mission mission = active.mission();
    if (mission != null) mission.toggleRecruit((Actor) active, false);
    
    return true;
  }


  protected void onVisit(Pathing visits) {
    super.onVisit(visits);
  }
  
  
  protected void onTarget(Target target) {
    super.onTarget(target);
  }
  
}







