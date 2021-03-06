


package game;
import util.*;
import static game.GameConstants.*;

import game.GameConstants.Pathing;



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
    float cashCarried = actor.outfit.carried(CASH);
    Pick <Building> pick = new Pick();
    AreaTile entrance = from.mainEntrance();
    
    for (Building b : from.map.buildings) {
      float distW = AreaMap.distance(actor.at(), b.mainEntrance());
      float distB = AreaMap.distance(entrance  , b.mainEntrance());
      if (distB > from.type().maxDeliverRange) continue;
      
      int amount = (int) b.inventory(CASH);
      if (amount == 0) continue;
      
      pick.compare(b, Nums.abs(amount) * AreaMap.distancePenalty(distW));
    }
    
    if (cashCarried > maxCollect || (pick.empty() && cashCarried != 0)) {
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
  
  
  protected float successPriority() {
    Actor   actor = (Actor) active;
    Carrier venue = (Carrier) visits;
    float   cash  = actor.outfit.carried(CASH);
    
    if (visits != store) {
      cash = Nums.max(cash, venue.inventory().valueFor(CASH));
    }
    
    //  TODO:  This needs to be reconsidered.  Seems a bit extreme?
    
    float urgency = Nums.clamp(cash / maxCollect, 0, 1);
    return PARAMOUNT + (PARAMOUNT * urgency);
  }
  
  
  protected void onVisit(Pathing visits) {
    Actor actor = (Actor) this.active;
    Carrier venue = (Carrier) visits;
    
    //  NOTE:  Operations with cash need some special handling to allow for
    //  negative numbers...
    
    if (actor.jobType() == JOB.COLLECTING) {
      float cash = venue.inventory().valueFor(CASH);
      
      actor.inventory().add(cash, CASH);
      venue.inventory().set(CASH, 0);
      
      TaskAssessTax next = nextAssessment(actor, store, maxCollect);
      if (next != null) actor.assignTask(next, this);
    }
    
    if (actor.jobType() == JOB.RETURNING && visits == store) {
      float cash = actor.outfit.carried(CASH);
      actor.outfit.setCarried(CASH, 0);
      store.base().incFunds((int) cash);
    }
  }
}






