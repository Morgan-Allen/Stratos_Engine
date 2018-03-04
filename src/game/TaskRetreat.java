


package game;
import util.*;
import static game.GameConstants.*;




public class TaskRetreat extends Task {
  
  
  Building hides;
  
  
  public TaskRetreat(Actor actor, Building hides) {
    super(actor);
    this.hides = hides;
  }
  
  
  public TaskRetreat(Session s) throws Exception {
    super(s);
    this.hides = (Building) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(hides);
  }
  
  
  
  
  static TaskRetreat configRetreat(Actor actor) {
    
    Building home = actor.home();
    if (home == null) home = actor.work();
    AreaMap map = actor.map;
    Pick <Building> pickHide = new Pick();
    
    for (Building b : map.buildings) {
      if (b != home && b.base() != actor.base()) continue;
      if (b != home && ! b.type().hasFeature(IS_REFUGE)) continue;
      
      float rating = 1.0f;
      rating *= AreaMap.distancePenalty(actor, b);
      if (b == home) rating *= 2;
      
      pickHide.compare(b, rating);
    }
    
    if (pickHide.empty()) return null;
    
    Building hides = pickHide.result();
    TaskRetreat hiding = new TaskRetreat(actor, hides);
    return (TaskRetreat) hiding.configTask(null, hides, null, JOB.RETREAT, 10);
  }
  
  
  
  protected float successChance() {
    //  TODO:  Rate your chance of escape based on speed relative to other
    //  actors!
    
    return 1.0f;
  }
  
  
  protected float successPriority() {
    Actor actor = (Actor) this.active;
    
    float dangerSum = 0, allySum = 0;
    float range = actor.sightRange();
    Batch <Active> aware = new Batch();
    
    for (Active other : actor.map.activeInRange(actor.at(), range)) {
      aware.add(other);
    }
    for (Active other : actor.focused()) {
      if (Task.inCombat(other) && other.task().inContact()) {
        aware.include(other);
      }
    }
    
    for (Active other : aware) {
      if (other.indoors()) continue;
      
      boolean hostile      = TaskCombat.hostile(other, actor);
      boolean allied       = TaskCombat.allied (other, actor);
      Target  otherFocus   = Task.focusTarget(other.task());
      float   harmLevel    = otherFocus == null ? 0 : other.task().harmLevel();
      boolean focusHostile = TaskCombat.hostile(actor, otherFocus);
      boolean focusAllied  = TaskCombat.allied (actor, otherFocus);
      
      float hostility = Nums.max(hostile ? 0.5f : 0, focusAllied  ? harmLevel : 0);
      float alliance  = Nums.max(allied  ? 0.5f : 0, focusHostile ? harmLevel : 0);
      
      float power = TaskCombat.attackPower(other);
      if (hostility > 0) dangerSum += power * hostility;
      if (alliance  > 0) allySum   += power * alliance ;
    }
    
    if (dangerSum <= 0) return 0;
    
    float lossChance = 0, sumFactors = dangerSum + allySum;
    if (sumFactors > 0) lossChance = dangerSum / sumFactors;
    lossChance += actor.injury () * 1.0f / actor.maxHealth();
    lossChance += actor.fatigue() * 0.5f / actor.maxHealth();
    
    return lossChance * PARAMOUNT;
  }


  protected void onVisit(Building visits) {
    super.onVisit(visits);
  }
  
  
  protected void onTarget(Target target) {
    super.onTarget(target);
  }
  
}


