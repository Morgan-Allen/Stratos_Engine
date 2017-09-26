

package game;
import static game.BuildingSet.*;

import game.BuildingSet.Good;
import util.*;



public class TradeBuilding extends CraftBuilding implements Trader.Partner {
  
  
  Tally <Good> stockLevels = new Tally();
  City tradePartner = null;
  Good imports[] = NO_GOODS, exports[] = NO_GOODS;
  
  
  public TradeBuilding(ObjectType type) {
    super(type);
  }
  
  
  public TradeBuilding(Session s) throws Exception {
    super(s);
    s.loadTally(stockLevels);
    tradePartner = (City) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveTally(stockLevels);
    s.saveObject(tradePartner);
  }

  
  
  void updateDemands() {
    
    Batch <Good> impB = new Batch(), expB = new Batch();
    for (Good g : stockLevels.keys()) {
      float level = stockLevels.valueFor(g);
      if (level > 0) impB.add(g);
      if (level < 0) expB.add(g);
    }
    imports = impB.toArray(Good.class);
    exports = expB.toArray(Good.class);
    
    super.updateDemands();
  }
  
  
  
  
  
  public Tally <Good> demands  () { return demands  ; }
  public Tally <Good> inventory() { return inventory; }
  Good[] needed  () { return imports; }
  Good[] produced() { return exports; }
  float stockNeeded(Good imp) { return     stockLevels.valueFor(imp); }
  float stockLimit (Good exp) { return 0 - stockLevels.valueFor(exp); }
  
  
  void selectWalkerBehaviour(Walker walker) {
    Trader trader = (Trader) I.cast(walker, Trader.class);
    if (trader != null) selectTraderBehaviour(trader);
  }
  
  
  void selectTraderBehaviour(Trader trader) {
    
    class Order { Tally <Good> cargo; Trader.Partner goes; float rating; }
    List <Trader.Partner> targets = new List();
    List <Order> orders  = new List();
    
    for (Building b : map.buildings) {
      if (! b.type.hasFeature(IS_TRADER)) continue;
      targets.add((Trader.Partner) b);
    }
    if (tradePartner != null) {
      targets.add(tradePartner);
    }
    
    for (Trader.Partner t : targets) {
      Tally <Good> cargo = new Tally();
      float rating = 0;
      
      for (Good good : ALL_GOODS) {
        //
        //  If you have a surplus, and they have a demand, do a delivery.
        float amountO  = inventory.valueFor(good);
        float demandO  = demands  .valueFor(good);
        float surplus  = amountO - demandO;
        float amountD  = t.inventory().valueFor(good);
        float demandD  = t.demands()  .valueFor(good);
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














