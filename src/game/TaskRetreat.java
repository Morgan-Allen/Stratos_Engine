


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
    CityMap map = actor.map;
    Pick <Building> pickHide = new Pick();
    
    for (Building b : map.buildings) {
      if (b != home && b.homeCity() != actor.homeCity()) continue;
      if (b != home && ! b.type().hasFeature(IS_REFUGE)) continue;
      
      float rating = 1.0f;
      rating *= CityMap.distancePenalty(actor, b);
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
    
    float dangerSum = 0, allySum = 0;
    float range = actor.sightRange();
    Batch <Actor> aware = new Batch();
    
    for (Actor other : actor.map.actorsInRange(actor.at(), range)) {
      aware.add(other);
    }
    for (Actor other : actor.focused()) {
      if (other.task().inContact() && other.inCombat()) aware.include(other);
    }
    
    for (Actor other : aware) {
      
      boolean hostile      = TaskCombat.hostile(other, actor);
      boolean allied       = TaskCombat.allied (other, actor);
      Element otherFocus   = Task.focusTarget(other.task());
      float   harmLevel    = other.task().harmLevel();
      boolean focusHostile = TaskCombat.hostile(actor, otherFocus);
      boolean focusAllied  = TaskCombat.allied (actor, otherFocus);
      
      float hostility = Nums.max(hostile ? 0.5f : 0, focusAllied  ? harmLevel : 0);
      float alliance  = Nums.max(allied  ? 0.5f : 0, focusHostile ? harmLevel : 0);
      
      float power = TaskCombat.attackPower(other);
      if (hostility > 0) dangerSum += power * hostility;
      if (alliance  > 0) allySum   += power * alliance ;
    }
    
    float lossChance = dangerSum / (dangerSum + allySum);
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


