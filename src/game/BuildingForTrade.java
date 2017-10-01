

package game;
import util.*;
import static game.GameConstants.*;



public class BuildingForTrade extends BuildingForCrafts implements Trader {
  
  
  /**  Data fields, setup and save/load methods-
    */
  Tally <Good> tradeLevel = new Tally();
  City tradePartner = null;
  List <Good> tradeFixed = new List();
  Good needed[] = NO_GOODS, produced[] = NO_GOODS;
  
  
  public BuildingForTrade(ObjectType type) {
    super(type);
  }
  
  
  public BuildingForTrade(Session s) throws Exception {
    super(s);
    s.loadTally(tradeLevel);
    s.loadObjects(tradeFixed);
    tradePartner = (City) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveTally(tradeLevel);
    s.saveObjects(tradeFixed);
    s.saveObject(tradePartner);
  }

  
  
  /**  Updating demands-
    */
  public void setTradeLevels(boolean matchStock, Object... args) {
    Object split[][] = Visit.splitByModulus(args, 2);
    for (int i = split[0].length; i-- > 0;) {
      Good  g = (Good   ) split[0][i];
      float a = (Integer) split[1][i];
      tradeLevel.set(g, a);
      if (matchStock) inventory.set(g, Nums.abs(a));
    }
  }
  

  void updateOnPeriod(int period) {
    super.updateOnPeriod(period);
    
    Batch <Good> impB = new Batch(), expB = new Batch();
    for (Good g : tradeLevel.keys()) {
      float level = tradeLevel.valueFor(g);
      if (level != 0) impB.add(g);
      if (level != 0) expB.add(g);
    }
    produced = impB.toArray(Good.class);
    needed   = expB.toArray(Good.class);
  }
  
  
  public Tally <Good> tradeLevel() { return tradeLevel; }
  public Tally <Good> inventory () { return inventory ; }
  
  Good[] needed  () { return needed  ; }
  Good[] produced() { return produced; }
  
  float stockNeeded(Good need) { return Nums.abs(tradeLevel.valueFor(need)); }
  float stockLimit (Good made) { return Nums.abs(tradeLevel.valueFor(made)); }
  
  
  void advanceProduction() {
    return;
  }
  
  
  public City tradeOrigin() {
    return map.city;
  }
  
  
  
  /**  Selecting behaviour for walkers-
    */
  public void selectWalkerBehaviour(Walker walker) {
    if (walker.type.category == ObjectType.IS_TRADE_WLK) {
      WalkerForTrade trader = (WalkerForTrade) walker;
      selectTraderBehaviour(trader);
    }
    else {
      super.selectWalkerBehaviour(walker);
    }
  }
  
  
  void selectTraderBehaviour(WalkerForTrade trader) {
    
    if (trader.inside != this) {
      trader.returnTo(this);
      return;
    }
    
    class Order { Tally <Good> cargo; Trader goes; float rating; }
    List <Trader> targets = new List();
    List <Order> orders = new List();
    
    for (Building b : map.buildings) {
      if (b == this || ! (b instanceof Trader)) continue;
      targets.add((Trader) b);
    }
    if (tradePartner != null) {
      targets.add(tradePartner);
    }
    
    for (Trader t : targets) {
      Tally <Good> cargoAway = WalkerForTrade.configureCargo(this, t, false);
      Tally <Good> cargoBack = WalkerForTrade.configureCargo(t, this, true );
      
      float distRating = WalkerForTrade.distanceRating(this, t);
      float rating = 0;
      
      if (cargoAway.size() > 0) {
        for (Good good : cargoAway.keys()) {
          rating += cargoAway.valueFor(good) / distRating;
        }
      }
      if (cargoBack.size() > 0) {
        for (Good good : cargoBack.keys()) {
          rating += cargoBack.valueFor(good) / distRating;
        }
      }
      if (rating > 0) {
        Order order = new Order();
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
      
      if (o.goes instanceof City) {
        City goes = (City) o.goes;
        trader.beginTravel(this, goes, o.cargo);
      }
      else {
        Building goes = (Building) o.goes;
        trader.beginDelivery(this, goes, o.cargo);
      }
    }
    
  }
  
  
  
  
  
}














