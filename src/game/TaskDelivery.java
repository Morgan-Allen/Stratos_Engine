

package game;
import static game.GameConstants.*;
import static game.Area.*;
import util.*;



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
  //  TODO:  Move the shopping code over here from BuildingForHome!
  
  
  static TaskDelivery pickNextShopping(
    Actor actor, Building home, Tally <Good> homeUsed
  ) {
    class Order { Building b; Good g; float amount; }
    Pick <Order> pickS = new Pick();
    
    for (Good cons : homeUsed.keys()) {
      float need = 1 + homeUsed.valueFor(cons);
      need -= home.inventory(cons);
      need -= totalFetchedFor(home, cons);
      if (need <= 0) continue;
      
      for (Building b : home.map().buildings) {
        if (! b.type().hasFeature(IS_VENDOR)) continue;
        
        float dist = distance(home, b);
        if (dist > 50) continue;
        
        float amount = b.inventory(cons);
        amount -= totalFetchedFrom(b, cons);
        if (amount < 1) continue;
        
        float rating = need * amount * distancePenalty(dist);
        Order o  = new Order();
        o.b      = b;
        o.g      = cons;
        o.amount = Nums.min(need, amount);
        pickS.compare(o, rating);
      }
    }
    if (! pickS.empty()) {
      Order o = pickS.result();
      TaskDelivery d = new TaskDelivery(actor);
      d = d.configDelivery(o.b, home, JOB.SHOPPING, o.g, o.amount, home);
      if (d != null) return d;
    }
    
    return null;
  }
  
  
  static float totalFetchedFor(Building home, Good good) {
    float total = 0;
    
    List <Actor> all = home.residents.copy();
    for (Actor a : home.workers) all.include(a);
    
    for (Actor a : all) {
      if (a.task() instanceof TaskDelivery) {
        TaskDelivery fetch = (TaskDelivery) a.task();
        if (fetch.carried == good && fetch.goes == home) total += fetch.amount;
      }
      for (Task t : ((ActorAsPerson) a).todo()) if (t instanceof TaskDelivery) {
        TaskDelivery fetch = (TaskDelivery) t;
        if (fetch.carried == good && fetch.goes == home) total += fetch.amount;
      }
    }
    return total;
  }
  
  
  static float totalFetchedFrom(Building store, Good good) {
    float total = 0;
    for (Active a : store.focused()) {
      if (! (a.task() instanceof TaskDelivery)) continue;
      TaskDelivery fetch = (TaskDelivery) a.task();
      if (fetch.from    != store) continue;
      if (fetch.carried == good ) total += fetch.amount;
    }
    return total;
  }
  
  
  
  static TaskDelivery pickNextDelivery(
    Actor actor, Building from, int minStock, Good... produced
  ) {
    //
    //  Find someone to deliver to:
    class Order { Building goes; Good good; float amount; }
    Pick <Order> pickD = new Pick();
    int maxRange = from.type().maxDeliverRange;
    
    for (Good made : produced) {
      int amount = (int) from.inventory(made);
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
    return findNearestDemanding(type, null, null, maxDist, from, false);
  }
  
  
  static Building findNearestWithFeature(
    Good feature, int maxDist, Building from
  ) {
    return findNearestDemanding(null, feature, null, maxDist, from, false);
  }
  
  
  static Building findNearestDemanding(
    Type type, Good needed, int maxDist, Building from
  ) {
    return findNearestDemanding(type, null, needed, maxDist, from, false);
  }
  
  
  static Building findNearestDemanding(
    Type type, Good feature,
    Good needed, int maxDist,
    Building from, boolean excludeFrom
  ) {
    Pick <Building> pick = new Pick();
    boolean trades = from.type().isTradeBuilding();
    
    for (Building b : from.map.buildings) {
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
      float max = actor.health.hungerLevel();
      return base + ((Task.PARAMOUNT - base) * max);
    }
    else return base;
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
      
      if (amount > 0.1f) {
        actor.outfit.pickupGood(carried, amount, from);
        configTravel(goes, type, origin);
      }
    }
    if (visits == goes) {
      actor.outfit.offloadGood(carried, goes);
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
    total[0] += total[2] = actor.outfit.carried(carried);
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




