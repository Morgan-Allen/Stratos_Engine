

package game;
import static game.GameConstants.*;
import util.*;



public class TaskDelivery extends Task {
  
  
  /**  Data fields, construction and save/load methods-
    */
  Pathing from;
  Pathing goes;
  Good carried;
  float amount;
  
  
  public TaskDelivery(Actor actor) {
    super(actor);
  }
  
  
  public TaskDelivery(Session s) throws Exception {
    super(s);
    from    = (Pathing) s.loadObject();
    goes    = (Pathing) s.loadObject();
    carried = (Good   ) s.loadObject();
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
    Actor actor, Building from, int minStock, Good... produced
  ) {
    int maxRange = from.type().maxDeliverRange;
    return pickNextDelivery(actor, from, from, maxRange, minStock, produced);
  }
  
  
  static TaskDelivery pickNextDelivery(
    Actor actor, Pathing from, Employer e, int maxRange,
    int minStock, Good... produced
  ) {
    //
    //  Basic sanity-checks first:
    if (! (from.type().isBuilding() || from.type().isVessel())) return null;
    
    class Order { Pathing goes; Good good; float amount; }
    Pick <Order> pickD = new Pick();
    Carrier venue = (Carrier) from;
    //
    //  Find someone to deliver to:
    for (Good made : produced) {
      int amount = (int) venue.inventory().valueFor(made);
      if (amount <= minStock) continue;
      
      //  TODO:  Iterate over suitable building-types here.
      Building goes = findNearestDemanding(null, made, maxRange, from);
      if (goes == null) continue;
      
      int demand = Nums.round(goes.demandFor(made), 1, true);
      amount = Nums.min(amount, 10    );
      amount = Nums.min(amount, demand);
      if (amount <= 0) continue;
      
      float penalty = Area.distancePenalty(
        from.mainEntrance(), goes.mainEntrance()
      );
      Order o = new Order();
      o.goes   = goes  ;
      o.good   = made  ;
      o.amount = amount;
      pickD.compare(o, amount * penalty);
    }
    
    if (! pickD.empty()) {
      Order o = pickD.result();
      TaskDelivery task = new TaskDelivery(actor);
      task.configDelivery(from, o.goes, JOB.DELIVER, o.good, o.amount, e);
      return task;
    }
    else {
      return null;
    }
  }
  
  
  static Building findNearestOfType(
    Type type, int maxDist, Pathing from
  ) {
    return findNearestDemanding(type, null, null, maxDist, from, false);
  }
  
  
  static Building findNearestWithFeature(
    Good feature, int maxDist, Pathing from
  ) {
    return findNearestDemanding(null, feature, null, maxDist, from, false);
  }
  
  
  static Building findNearestDemanding(
    Type type, Good needed, int maxDist, Pathing from
  ) {
    return findNearestDemanding(type, null, needed, maxDist, from, false);
  }
  
  
  static Building findNearestDemanding(
    Type type, Good feature,
    Good needed, int maxDist,
    Pathing from, boolean excludeFrom
  ) {
    Area map = ((Element) from).map();
    Pick <Building> pick = new Pick();
    boolean trades = from.type().isTradeBuilding();
    
    for (Building b : map.buildings) {
      if (type != null && b.type() != type) continue;
      if (excludeFrom && b == from) continue;
      
      //
      //  Check to ensure that traders don't deliver to other traders using a
      //  standard delivery mechanism, and that any requisite features are
      //  present-
      boolean otherTrades = b.type().isTradeBuilding();
      boolean featured    = b.type().hasFeature(feature);
      if (trades && otherTrades        ) continue;
      if (feature != null && ! featured) continue;
      
      //
      //  We need to allow for structures that aren't built yet, as well as
      //  those that are-
      AreaTile fromT = from.complete() ? from.mainEntrance() : from.at();
      AreaTile goesT = b   .complete() ? b   .mainEntrance() : b   .at();
      float dist = Area.distance(fromT, goesT);
      if (maxDist > 0 && dist > maxDist) continue;
      
      float rating = 1;
      
      if (needed != null) {
        float demand   = b.demandFor(needed);
        float maxStock = b.maxStock (needed);
        if (demand <= 0 || maxStock <= 0) continue;
        if (otherTrades) rating /= 2;
        rating *= demand / maxStock;
      }
      
      rating *= Area.distancePenalty(dist);
      
      pick.compare(b, rating);
    }
    
    return pick.result();
  }
  
  
  
  /**  Priority-evaluation:
    */
  protected float successPriority() {
    Actor actor = (Actor) active;
    float base = super.successPriority();
    if (carried.isEdible && goes == actor.home()) {
      float max = actor.hungerLevel();
      return base + ((Task.PARAMOUNT - base) * max);
    }
    else return base;
  }
  
  
  
  
  /**  Scripting and events-handling-
    */
  TaskDelivery configDelivery(
    Pathing from, Pathing goes, Task.JOB jobType,
    Good carried, float amount, Employer e
  ) {
    this.from    = from;
    this.goes    = goes;
    this.carried = carried;
    this.amount  = amount;
    configTravel(from, jobType, e);
    return this;
  }
  
  
  void configTravel(Pathing site, Task.JOB jobType, Employer e) {
    if (site.complete()) {
      configTask(e, site, null, jobType, 0);
    }
    else {
      configTask(e, null, site.at(), jobType, 0);
    }
  }
  
  
  protected void onVisit(Pathing visits) {
    Actor actor = (Actor) this.active;
    
    //  TODO:  Fill this in...!
    
    if (visits == from) {
      Carrier venue = (Carrier) from;
      
      amount = Nums.min(amount, venue.inventory().valueFor(carried));
      amount = Nums.max(amount, 0);
      
      boolean privateSale = amount > 0 && goes.type().hasFeature(IS_HOUSING);
      if (type == JOB.DELIVER && privateSale) {
        float cashPaid = amount * venue.shopPrice(carried, this);
        venue.inventory().add(cashPaid, CASH);
      }
      if (type == JOB.SHOPPING && privateSale) {
        float cashPaid = amount * venue.shopPrice(carried, this);
        venue.inventory().add(cashPaid, CASH);
      }
      
      if (amount > 0.1f) {
        actor.pickupGood(carried, amount, venue);
        configTravel(goes, type, origin);
      }
    }
    
    if (visits == goes) {
      Carrier venue = (Carrier) goes;
      actor.offloadGood(carried, venue);
    }
  }
  
  
  protected void onTarget(Target targets) {
    if (targets == from.at()) onVisit(from);
    if (targets == goes.at()) onVisit(goes);
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    return "Delivering "+carried+" from "+from+" to "+goes;
  }
  
  
  private float[] totalMaterial() {
    Actor actor = (Actor) this.active;
    float total[] = new float[4];
    total[0] += total[1] = ((Carrier) from).inventory().valueFor(carried);
    total[0] += total[2] = actor.carried(carried);
    total[0] += total[3] = ((Carrier) goes).inventory().valueFor(carried);
    return total;
  }
  
  
  private void checkTotalsDiff(float oldT[], float newT[]) {
    if (Nums.abs(oldT[0] - newT[0]) > 0.001f) {
      I.say("Diff while: "+this);
      I.say("  Old: "+oldT);
      I.say("  New: "+newT);
      I.say("  ???");
    }
  }
}




