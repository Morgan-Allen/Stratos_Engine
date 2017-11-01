

package game;
import util.*;
import static game.GameConstants.*;



public class TaskDelivery extends Task {
  
  
  /**  Data fields, construction and save/load methods-
    */
  Building from;
  Building goes;
  Good carried;
  float amount;
  
  
  public TaskDelivery(Actor actor) {
    super(actor);
  }
  
  
  public TaskDelivery(Session s) throws Exception {
    super(s);
    from    = (Building) s.loadObject();
    goes    = (Building) s.loadObject();
    carried = (Good) s.loadObject();
    amount  = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(from);
    s.saveObject(goes);
    s.saveObject(carried);
    s.saveFloat(amount);
  }
  
  
  
  /**  Other config utilities:
    */
  static TaskDelivery pickNextDelivery(
    Actor actor, Building from, Good... produced
  ) {
    //
    //  Find someone to deliver to:
    class Order { Building goes; Good good; float amount; }
    Pick <Order> pickD = new Pick();
    int maxRange = from.type.maxDeliverRange;
    
    for (Good made : produced) {
      int amount = (int) from.inventory.valueFor(made);
      if (amount <= 0) continue;
      
      //  TODO:  Iterate over suitable building-types here.
      Building goes = findNearestDemanding(null, made, maxRange, from);
      if (goes == null) continue;
      
      int demand = Nums.round(goes.demandFor(made), 1, true);
      amount = Nums.min(amount, 10    );
      amount = Nums.min(amount, demand);
      if (amount <= 0) continue;
      
      float penalty = CityMap.distancePenalty(from.entrance(), goes.entrance());
      Order o = new Order();
      o.goes   = goes  ;
      o.good   = made  ;
      o.amount = amount;
      pickD.compare(o, amount * penalty);
    }
    
    if (! pickD.empty()) {
      Order o = pickD.result();
      TaskDelivery task = new TaskDelivery(actor);
      task.configDelivery(from, o.goes, JOB.DELIVER, o.good, o.amount, from);
      return task;
    }
    else {
      return null;
    }
  }
  
  
  static Building findNearestOfType(
    Type type, int maxDist, Building from
  ) {
    return findNearestDemanding(type, null, null, -1, from);
  }
  
  
  static Building findNearestWithFeature(
    Good feature, int maxDist, Building from
  ) {
    return findNearestDemanding(null, feature, null, -1, from);
  }
  
  
  static Building findNearestDemanding(
    Type type, Good needed, int maxDist, Building from
  ) {
    return findNearestDemanding(type, null, needed, maxDist, from);
  }
  
  
  static Building findNearestDemanding(
    Type type, Good feature,
    Good needed, int maxDist,
    Building from
  ) {
    Pick <Building> pick = new Pick();
    boolean trades = from.type.isTradeBuilding();
    
    for (Building b : from.map.buildings) {
      if (type != null && b.type != type) continue;
      
      boolean otherTrades = b.type.isTradeBuilding();
      if (trades && otherTrades) continue;
      
      boolean featured = b.type.hasFeature(feature);
      if (feature != null && ! featured) continue;
      
      float dist = CityMap.distance(from.entrance(), b.entrance());
      if (maxDist > 0 && dist > maxDist) continue;
      
      float rating = 1;
      if (needed != null) rating *= b.demandFor(needed);
      if (rating <= 0) continue;
      if (otherTrades) rating /= 2;
      
      pick.compare(b, rating * CityMap.distancePenalty(dist));
    }
    
    return pick.result();
  }
  
  
  
  /**  Scripting and events-handling-
    */
  TaskDelivery configDelivery(
    Building from, Building goes, Task.JOB jobType,
    Good carried, float amount, Employer e
  ) {
    this.from    = from;
    this.goes    = goes;
    this.carried = carried;
    this.amount  = amount;
    return (TaskDelivery) configTask(e, from, null, jobType, 0);
  }
  
  
  protected void onVisit(Building visits) {
    if (visits == from) {
      actor.pickupGood(carried, amount, from);
      this.configTask(origin, goes, null, type, 0);
    }
    if (visits == goes) {
      actor.offloadGood(carried, goes);
    }
  }
  
}



