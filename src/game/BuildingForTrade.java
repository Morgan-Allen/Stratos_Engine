


package game;
import util.*;
import static game.GameConstants.*;
import static game.ActorUtils.*;



public class BuildingForTrade extends Building implements Trader {
  
  
  /**  Data fields, setup and save/load methods-
    */
  Tally <Good> prodLevel = new Tally();
  Tally <Good> needLevel = new Tally();
  List <Good> tradeFixed = new List();
  Base tradePartner = null;
  boolean tradeOff = false;
  Good needed[] = NO_GOODS, produced[] = NO_GOODS;
  
  
  public BuildingForTrade(BuildType type) {
    super(type);
  }
  
  
  public BuildingForTrade(Session s) throws Exception {
    super(s);
    s.loadTally(prodLevel);
    s.loadTally(needLevel);
    s.loadObjects(tradeFixed);
    tradePartner = (Base) s.loadObject();
    tradeOff = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveTally(prodLevel);
    s.saveTally(needLevel);
    s.saveObjects(tradeFixed);
    s.saveObject(tradePartner);
    s.saveBool(tradeOff);
  }

  
  
  /**  Updating demands-
    */
  public void setProdLevels(boolean matchStock, Object... args) {
    prodLevel.setWith(args);
    if (matchStock) for (Good g : prodLevel.keys()) {
      setInventory(g, Nums.abs(prodLevel.valueFor(g)));
    }
  }
  
  
  public void setNeedLevels(boolean matchStock, Object... args) {
    needLevel.setWith(args);
    if (matchStock) for (Good g : needLevel.keys()) {
      setInventory(g, Nums.abs(needLevel.valueFor(g)));
    }
  }
  
  
  public void setTradePartner(Base partner) {
    this.tradePartner = partner;
  }
  
  
  public void toggleTrading(boolean allowed) {
    this.tradeOff = ! allowed;
  }
  

  void updateOnPeriod(int period) {
    super.updateOnPeriod(period);
    
    Batch <Good> impB = new Batch(), expB = new Batch();
    for (Good g : map.world.goodTypes) {
      float need   = needLevel  .valueFor(g);
      float accept = prodLevel.valueFor(g);
      if (need   > 0) impB.add(g);
      if (accept > 0) expB.add(g);
    }
    produced = impB.toArray(Good.class);
    needed   = expB.toArray(Good.class);
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
  
  
  
  /**  Selecting behaviour for walkers-
    */
  public Task selectActorBehaviour(Actor actor) {
    
    if (actor.type().isVessel() && ! tradeOff) {
      Task trading = selectTraderBehaviour(actor);
      if (trading != null) return trading;
      
      Task coming = returnActorHere(actor);
      if (coming != null) return coming;
    }
    else {
      Task delivery = TaskDelivery.pickNextDelivery(actor, this, produced());
      if (delivery != null) return delivery;
      
      Task building = TaskBuilding.nextBuildingTask(this, actor);
      if (building != null) return building;
    }
    
    return null;
  }
  
  
  Task selectTraderBehaviour(Actor trader) {
    
    class Order { Tally <Good> cargo; Trader goes; float rating; }
    List <Trader> targets = new List();
    List <Order> orders = new List();
    Base homeCity = base();
    World world = homeCity.world;
    
    for (Building b : map.buildings) {
      if (b == this || ! (b instanceof Trader)) continue;
      if (b.base() != base()) {
        if      (tradePartner == null    ) continue;
        else if (b.base() != tradePartner) continue;
      }
      targets.add((Trader) b);
    }
    
    if (tradePartner != null && tradePartner.activeMap() != trader.map()) {
      targets.add(tradePartner);
    }
    else for (Base c : world.bases) {
      if (c.activeMap() == map       ) continue;
      if (c == homeCity              ) continue;
      if (c.isEnemyOf(homeCity)      ) continue;
      if (c.distance (homeCity) == -1) continue;
      targets.add(c);
    }
    
    for (Trader t : targets) {
      World w = map.world;
      Base c = (t == t.base()) ? ((Base) t) : null;
      Tally <Good> cargoAway = TaskTrading.configureCargo(this, t, false, w);
      Tally <Good> cargoBack = TaskTrading.configureCargo(t, this, true , w);
      
      float distRating = TaskTrading.distanceRating(this, t);
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
      
      if (homeCity.isVassalOf(c)) {
        rating *= 2.5f;
      }
      if (homeCity.isLordOf(c)) {
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
      
      if (reports()) {
        I.say("\n"+this+" assigning delivery to "+trader);
        I.say("  Cargo is: "+o.cargo);
        I.say("  Destination is: "+o.goes);
      }
      
      TaskTrading t = new TaskTrading(trader);
      t = t.configTrading(this, o.goes, o.cargo);
      return t;
    }
    
    return null;
  }
  
  
  
  
  
}














