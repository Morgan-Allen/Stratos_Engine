

package game;
import static game.GameConstants.*;
import util.*;



public class BuildingForTrade extends Building implements Trader {
  
  
  /**  Data fields, setup and save/load methods-
    */
  Tally <Good> prodLevel = new Tally();
  Tally <Good> needLevel = new Tally();
  
  Base tradePartner = null;
  boolean freeMarket = false;
  boolean exports    = true ;
  boolean tradeOff   = false;
  Good needed[] = NO_GOODS, produced[] = NO_GOODS;
  
  
  
  public BuildingForTrade(BuildType type) {
    super(type);
  }
  
  
  public BuildingForTrade(Session s) throws Exception {
    super(s);
    
    s.loadTally(prodLevel);
    s.loadTally(needLevel);
    
    tradePartner = (Base) s.loadObject();
    freeMarket = s.loadBool();
    exports    = s.loadBool();
    tradeOff   = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveTally(prodLevel);
    s.saveTally(needLevel);
    
    s.saveObject(tradePartner);
    s.saveBool(freeMarket);
    s.saveBool(exports);
    s.saveBool(tradeOff);
  }

  
  
  /**  Updating demands-
    */
  public void setProdLevels(boolean matchStock, Object... args) {
    prodLevel.setWith(args);
    if (matchStock) for (Good g : prodLevel.keys()) {
      setInventory(g, Nums.abs(prodLevel.valueFor(g)));
    }
    updateDemandTypes();
  }
  
  
  public void setNeedLevels(boolean matchStock, Object... args) {
    needLevel.setWith(args);
    if (matchStock) for (Good g : needLevel.keys()) {
      setInventory(g, Nums.abs(needLevel.valueFor(g)));
    }
    updateDemandTypes();
  }
  
  
  public void setTradePartner(Base partner) {
    this.tradePartner = partner;
  }
  
  public void toggleTrading(boolean allowed) {
    this.tradeOff = ! allowed;
  }
  
  public void toggleExports(boolean allowed) {
    this.exports = allowed;
  }
  

  void updateOnPeriod(int period) {
    super.updateOnPeriod(period);
    
    if (freeMarket) {
      updateDemandsLocally();
    }
    updateDemandTypes();
  }
  
  
  private void updateDemandTypes() {
    Batch <Good> needB = new Batch(), prodB = new Batch();
    for (Good g : map.world.goodTypes) {
      float need   = needLevel.valueFor(g);
      float accept = prodLevel.valueFor(g);
      if (need   > 0) needB.add(g);
      if (accept > 0) prodB.add(g);
    }
    needed   = needB.toArray(Good.class);
    produced = prodB.toArray(Good.class);
  }
  
  
  private void updateDemandsLocally() {
    float maxDist = type().maxDeliverRange;
    
    //  TODO:  Scale down to maximum cargo capacity!
    
    needLevel.clear();
    prodLevel.clear();
    
    for (Building b : map().buildings()) if (b.base() == base()) {
      if (b.type().isTradeBuilding()) continue;
      
      float dist = Area.distance(this, b);
      if (dist > maxDist) continue;
      if (! map.pathCache.pathConnects(this, b, true, false)) continue;
      
      for (Good g : b.produced()) {
        float amount = b.maxStock(g);
        needLevel.add(amount, g);
      }
      for (Good g : b.needed()) {
        float amount = b.maxStock(g);
        prodLevel.add(amount, g);
      }
      
      Tally <Good> homeUse = b.homeUsed();
      if (homeUse != null) for (Good g : homeUse.keys()) {
        float amount = homeUse.valueFor(g);
        prodLevel.add(amount, g);
      }
    }
  }
  
  
  public Tally <Good> needLevels() { return needLevel; }
  public Tally <Good> prodLevels() { return prodLevel; }
  public Tally <Good> inventory () { return super.inventory(); }
  
  public Good[] needed  () { return needed  ; }
  public Good[] produced() { return produced; }
  
  
  public float stockNeeded(Good need) {
    return needLevel.valueFor(need) + prodLevel.valueFor(need);
  }
  
  public float stockLimit(Good made) {
    return needLevel.valueFor(made) + prodLevel.valueFor(made);
  }
  
  public float importPrice(Good g, Base sells) {
    return base().importPrice(g, sells);
  }
  
  public float exportPrice(Good g, Base buys) {
    return base().exportPrice(g, buys);
  }
  
  public float shopPrice(Good g, TaskDelivery s) {
    return super.shopPrice(g, s);
  }
  
  public boolean allowExport(Good g, Trader buys) {
    if (tradePartner != null && buys.base() != tradePartner) return false;
    return exports && prodLevel.valueFor(g) > 0;
  }
  
  public boolean allowExports() {
    return exports;
  }
  
  
  
  /**  Selecting behaviour for walkers-
    */
  public Task selectActorBehaviour(Actor actor) {
    
    if (actor.type().isVessel()) {
      if (! tradeOff) {
        Task trading = selectTraderBehaviour(
          this, actor, tradePartner, true, map()
        );
        if (trading != null) return trading;
      }
      Task coming = returnActorHere(actor);
      if (coming != null) return coming;
    }
    else {
      Task delivery = TaskDelivery.pickNextDelivery(actor, this, 0, produced());
      if (delivery != null) return delivery;
      
      Task building = TaskBuilding.nextBuildingTask(this, actor);
      if (building != null) return building;
      
      Task tending = TaskWaiting.configWaiting(actor, this);
      if (tending != null) return tending;
    }
    
    return null;
  }
  
  
  public static TaskTrading selectTraderBehaviour(
    Trader from, Actor trading,
    Base tradePartner, boolean cityBackOnly, Area map
  ) {
    boolean reports = trading.reports();
    
    class Order { Tally <Good> cargo; Trader goes; float rating; }
    List <Trader> targets = new List();
    List <Order> orders = new List();
    Base homeCity = from.base();
    World world = homeCity.world;
    int moveMode = trading.type().moveMode;
    
    for (Building b : map.buildings()) {
      if (b == from || ! (b instanceof Trader)) continue;
      if (b.base() != homeCity) {
        if      (tradePartner == null    ) continue;
        else if (b.base() != tradePartner) continue;
      }
      targets.add((Trader) b);
    }
    
    if (tradePartner != null && tradePartner.activeMap() != trading.map()) {
      targets.add(tradePartner);
    }
    else for (Base c : world.bases) {
      if (c.activeMap() == map) continue;
      if (c == homeCity       ) continue;
      if (c.relations.isEnemyOf(homeCity)   ) continue;
      if (c.distance(homeCity, moveMode) < 0) continue;
      targets.add(c);
    }
    
    for (Trader t : targets) {
      World w = map.world;
      Base c = (t == t.base()) ? ((Base) t) : null;
      
      Tally <Good> cargoAway, cargoBack;
      cargoAway = TaskTrading.configureCargo(from, t, false       , w);
      cargoBack = TaskTrading.configureCargo(t, from, cityBackOnly, w);
      
      float distRating = TaskTrading.distanceRating(from, t);
      float rating = 0;
      
      if (cargoAway.size() > 0) {
        for (Good good : cargoAway.keys()) {
          rating += cargoAway.valueFor(good) * distRating;
        }
      }
      if (cargoBack.size() > 0) {
        for (Good good : cargoBack.keys()) {
          rating += cargoBack.valueFor(good) * distRating;
        }
      }
      
      if (homeCity.relations.isVassalOf(c)) {
        rating *= 2.5f;
      }
      if (homeCity.relations.isLordOf(c)) {
        rating *= 1.5f;
      }
      if (rating > 0) {
        Order order  = new Order();
        order.cargo  = cargoAway;
        order.goes   = t;
        order.rating = rating;
        orders.add(order);
      }
    }
    
    Pick <Order> pick = new Pick(0);
    for (Order o : orders) {
      pick.compare(o, o.rating);
    }
    
    if (! pick.empty()) {
      Order o = pick.result();
      if (reports) {
        I.say("\n"+from+" assigning delivery to "+trading);
        I.say("  Cargo is: "+o.cargo);
        I.say("  Destination is: "+o.goes);
      }
      return TaskTrading.configTrading(from, o.goes, trading, o.cargo);
    }
    
    return null;
  }
  
}














