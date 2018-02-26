

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
  //  TODO:  Move the shopping code over here from BuildingForHome
  
  
  static TaskDelivery pickNextDelivery(
    Actor actor, Building from, Good... produced
  ) {
    //
    //  Find someone to deliver to:
    class Order { Building goes; Good good; float amount; }
    Pick <Order> pickD = new Pick();
    int maxRange = from.type().maxDeliverRange;
    
    for (Good made : produced) {
      int amount = (int) from.inventory(made);
      if (amount <= 0) continue;
      
      //  TODO:  Iterate over suitable building-types here.
      Building goes = findNearestDemanding(null, made, maxRange, from);
      if (goes == null) continue;
      
      int demand = Nums.round(goes.demandFor(made), 1, true);
      amount = Nums.min(amount, 10    );
      amount = Nums.min(amount, demand);
      if (amount <= 0) continue;
      
      float penalty = AreaMap.distancePenalty(
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
    boolean trades = from.type().isTradeBuilding();
    
    for (Building b : from.map.buildings) if (b != from) {
      if (type != null && b.type() != type) continue;
      
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
      Tile fromT = from.accessible() ? from.mainEntrance() : from.at();
      Tile goesT = b   .accessible() ? b   .mainEntrance() : b   .at();
      float dist = AreaMap.distance(fromT, goesT);
      if (maxDist > 0 && dist > maxDist) continue;
      
      float rating = 1;
      if (needed != null) rating *= b.demandFor(needed);
      if (rating <= 0) continue;
      if (otherTrades) rating /= 2;
      
      pick.compare(b, rating * AreaMap.distancePenalty(dist));
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
    configTravel(from, jobType, e);
    return this;
  }
  
  
  void configTravel(Building site, Task.JOB jobType, Employer e) {
    if (site.complete()) {
      configTask(e, site, null, jobType, 0);
    }
    else {
      configTask(e, null, site.at(), jobType, 0);
    }
  }
  
  
  protected void onVisit(Building visits) {
    Actor actor = (Actor) this.active;
    
    if (visits == from) {
      amount = Nums.min(amount, visits.inventory(carried));
      amount = Nums.max(amount, 0);
      
      boolean privateSale = amount > 0 && goes.type().hasFeature(IS_HOUSING);
      if (type == JOB.DELIVER && privateSale) {
        float cashPaid = amount * from.shopPrice(carried, this);
        from.addInventory(cashPaid, CASH);
      }
      if (type == JOB.SHOPPING && privateSale) {
        float cashPaid = amount * from.shopPrice(carried, this);
        from.addInventory(cashPaid, CASH);
      }
      
      if (amount > 0) {
        actor.pickupGood(carried, amount, from);
        configTravel(goes, type, origin);
      }
    }
    if (visits == goes) {
      actor.offloadGood(carried, goes);
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
    total[0] += total[1] = from.inventory(carried);
    total[0] += total[2] = actor.carried(carried);
    total[0] += total[3] = goes.inventory(carried);
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




