

package game;
import static game.BuildingSet.*;

import game.BuildingSet.Good;
import util.*;



public class TradeBuilding extends CraftBuilding implements TradeWalker.Partner {
  
  
  /**  Data fields, setup and save/load methods-
    */
  Tally <Good> stockLevel = new Tally();
  City tradePartner = null;
  Good needed[] = NO_GOODS, produced[] = NO_GOODS;
  
  
  public TradeBuilding(ObjectType type) {
    super(type);
  }
  
  
  public TradeBuilding(Session s) throws Exception {
    super(s);
    s.loadTally(stockLevel);
    tradePartner = (City) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveTally(stockLevel);
    s.saveObject(tradePartner);
  }

  
  
  /**  Updating demands-
    */
  void updateDemands() {
    
    Batch <Good> impB = new Batch(), expB = new Batch();
    for (Good g : stockLevel.keys()) {
      float level = stockLevel.valueFor(g);
      if (level > 0) impB.add(g);
      if (level < 0) expB.add(g);
    }
    produced = impB.toArray(Good.class);
    needed   = expB.toArray(Good.class);
    
    super.updateDemands();
  }
  
  
  public Tally <Good> stockLevel() { return stockLevel; }
  public Tally <Good> inventory () { return inventory ; }
  Good[] needed  () { return needed; }
  Good[] produced() { return produced; }
  float stockNeeded(Good need) { return 0 - stockLevel.valueFor(need); }
  float stockLimit (Good made) { return     stockLevel.valueFor(made); }
  
  
  
  /**  Selecting behaviour for walkers-
    */
  void selectWalkerBehaviour(Walker walker) {
    TradeWalker trader = (TradeWalker) I.cast(walker, TradeWalker.class);
    if (trader != null) selectTraderBehaviour(trader);
    else super.selectWalkerBehaviour(walker);
  }
  
  
  void selectTraderBehaviour(TradeWalker trader) {
    
    class Order { Tally <Good> cargo; TradeWalker.Partner goes; float rating; }
    List <TradeWalker.Partner> targets = new List();
    List <Order> orders = new List();
    
    for (Building b : map.buildings) {
      if (! b.type.hasFeature(IS_TRADER)) continue;
      targets.add((TradeWalker.Partner) b);
    }
    if (tradePartner != null) {
      targets.add(tradePartner);
    }
    
    for (TradeWalker.Partner t : targets) {
      Tally <Good> cargo = new Tally();
      float rating = 0;
      
      for (Good good : ALL_GOODS) {
        
        float amountO  = inventory ().valueFor(good);
        float demandO  = stockLevel().valueFor(good);
        float surplus  = amountO - demandO;
        float amountD  = t.inventory ().valueFor(good);
        float demandD  = t.stockLevel().valueFor(good);
        float shortage = demandD - amountD;
        
        if (surplus > 0 && shortage > 0) {
          float size = Nums.min(surplus, shortage);
          cargo.set(good, size);
          rating += size;
        }
      }
      
      if (cargo.size() > 0) {
        Order order = new Order();
        order.cargo  = cargo;
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














