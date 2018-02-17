


package game;
import util.*;
import static game.GameConstants.*;



public class TaskAssessTax extends Task {
  
  
  Building store;
  int maxCollect;
  
  
  public TaskAssessTax(Actor actor, Building store, int maxCollect) {
    super(actor);
    this.store      = store;
    this.maxCollect = maxCollect;
  }
  
  
  public TaskAssessTax(Session s) throws Exception {
    super(s);
    store      = (Building) s.loadObject();
    maxCollect = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(store     );
    s.saveInt   (maxCollect);
  }
  
  
  static TaskAssessTax nextAssessment(
    Actor actor, Building from, int maxCollect
  ) {
    //  TODO:  Maybe use a different Good for this, to distinguish from
    //  personal savings?
    
    float cashCarried = actor.carried(CASH);
    Pick <Building> pick = new Pick();
    Tile entrance = from.mainEntrance();
    
    for (Building b : from.map.buildings) {
      if (! b.type().hasFeature(IS_HOUSING)) continue;
      float distW = AreaMap.distance(actor.at(), b.mainEntrance());
      float distB = AreaMap.distance(entrance  , b.mainEntrance());
      if (distB > from.type().maxDeliverRange) continue;
      
      int amount = (int) b.inventory(CASH);
      if (amount <= 0) continue;
      
      pick.compare(b, amount * AreaMap.distancePenalty(distW));
    }
    
    if (cashCarried > maxCollect || (pick.empty() && cashCarried > 0)) {
      Task task = new TaskAssessTax(actor, from, maxCollect);
      task = task.configTask(from, from, null, JOB.RETURNING, 0);
      return (TaskAssessTax) task;
    }
    
    if (! pick.empty()) {
      Task task = new TaskAssessTax(actor, from, maxCollect);
      task = task.configTask(from, pick.result(), null, JOB.COLLECTING, 0);
      return (TaskAssessTax) task;
    }
    
    return null;
  }
  
  
  protected void onVisit(Building visits) {
    Actor actor = (Actor) this.active;
    
    if (actor.jobType() == JOB.COLLECTING) {
      float cash = visits.inventory(CASH);
      actor.pickupGood(CASH, cash, visits);
      
      TaskAssessTax next = nextAssessment(actor, store, maxCollect);
      if (next != null) actor.assignTask(next);
    }
    
    if (actor.jobType() == JOB.RETURNING && visits == store) {
      float cash = actor.carried(CASH);
      actor.clearCarried();
      store.base().incFunds((int) cash);
    }
  }
}






