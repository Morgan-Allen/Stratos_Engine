

package game;
import static game.GameConstants.*;
import static game.BaseRelations.*;
import util.*;



public class BaseTrading {
  
  
  final Base base;
  
  Tally <Good> needLevel = new Tally();
  Tally <Good> prodLevel = new Tally();
  Tally <Good> inventory = new Tally();
  
  Tally <Type> makeTotals = new Tally();
  Tally <Type> usedTotals = new Tally();
  
  List <ActorAsVessel> traders = new List();
  List <Actor> migrants = new List();
  
  
  
  BaseTrading(Base base) {
    this.base = base;
  }
  
  
  void loadState(Session s) throws Exception {
    s.loadTally(needLevel );
    s.loadTally(prodLevel );
    s.loadTally(inventory );    
    s.loadTally(makeTotals);
    s.loadTally(usedTotals);
    
    s.loadObjects(traders );
    s.loadObjects(migrants);
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveTally(needLevel );
    s.saveTally(prodLevel );
    s.saveTally(inventory );
    s.saveTally(makeTotals);
    s.saveTally(usedTotals);
    
    s.saveObjects(traders );
    s.saveObjects(migrants);
  }
  
  

  
  
  public void setTradeLevel(Good g, float need, float accept) {
    needLevel.set(g, need  );
    prodLevel.set(g, accept);
  }
  
  
  public float needLevel(Good g) {
    return needLevel.valueFor(g);
  }
  
  
  public float prodLevel(Good g) {
    return prodLevel.valueFor(g);
  }
  
  
  public void initInventory(Object... args) {
    inventory.setWith(args);
  }
  
  
  public float inventory(Good g) {
    return inventory.valueFor(g);
  }
  
  
  public float setInventory(Good g, float amount) {
    return inventory.set(g, amount);
  }
  
  
  public float totalMade(Good g) {
    return makeTotals.valueFor(g);
  }
  
  
  public float totalUsed(Good g) {
    return usedTotals.valueFor(g);
  }
  

  public Tally <Good> needLevels() { return needLevel; }
  public Tally <Good> prodLevels() { return prodLevel; }
  public Tally <Good> inventory () { return inventory; }
  
  

  
  
  public boolean allowExport(Good g, Trader buys) {
    if (buys.base().relations.homeland() == base) return true;
    if (suppliesDue(base, buys.base(), g) > 0) return true;
    return prodLevel.valueFor(g) > 0;
  }
  
  
  public float shopPrice(Good good, Task purchase) {
    return good.price;
  }
  
  

  
  
  public static void setSuppliesDue(Base a, Base b, Tally <Good> suppliesDue) {
    if (suppliesDue == null) suppliesDue = new Tally();
    Relation r = a.relations.relationWith(b);
    r.suppliesDue = suppliesDue;
  }
  
  
  public static Tally <Good> suppliesDue(Base a, Base b) {
    Relation r = a.relations.relationWith(b);
    if (r == null) return new Tally();
    return r.suppliesDue;
  }
  
  
  public static float goodsSent(Base a, Base b, Good g) {
    Relation r = a.relations.relationWith(b);
    return r == null ? 0 : r.suppliesSent.valueFor(g);
  }
  
  
  public static float suppliesDue(Base a, Base b, Good g) {
    Relation r = a.relations.relationWith(b);
    return r == null ? 0 : r.suppliesDue.valueFor(g);
  }
  
  
  
  //  Selling to where goods are abundant gets you a lower price.
  //  Buying from where goods are scarce imposes a higher price.
  //  Selling to where goods are scarce gets you a higher price.
  //  Buying from where goods are abundant imposes a lower price.
  
  float scarcityMultiple(Base other, Good g) {
    if (other != base.relations.homeland()) return 1.0f;
    float mult = 1.0f;
    float needS = other.trading.needLevel(g);
    float prodS = other.trading.prodLevel(g);
    if (needS > 0) mult += SCARCE_MARGIN / 100f;
    if (prodS > 0) mult += PLENTY_MARGIN / 100f;
    return mult;
  }
  
  
  public float importPrice(Good g, Base sells) {
    float mult = scarcityMultiple(sells, g);
    mult += TRAVEL_MARGIN / 100f;
    return (int) (g.price * mult);
  }
  
  
  public float exportPrice(Good g, Base buys) {
    float mult = scarcityMultiple(buys, g);
    return (int) (g.price * mult);
  }
  
  
  
  
  
  public void updateLocalStocks() {
    inventory .clear();
    needLevel .clear();
    prodLevel .clear();
    
    for (Building b : base.activeMap().buildings()) if (b.base() == base) {
      for (Good g : base.world.goodTypes()) {
        inventory.add(b.inventory(g), g);
      }
      if (b.type().category != Type.IS_TRADE_BLD) {
        for (Good g : b.needed()) {
          needLevel.add(b.stockLimit(g), g);
        }
        for (Good g : b.produced()) {
          prodLevel.add(b.stockLimit(g), g);
        }
      }
    }
  }
  
  
  public void updateOffmapStocks(int updateGap) {
    float usageInc = updateGap * 1f / YEAR_LENGTH;
    
    for (Good g : base.world.goodTypes) {
      float demand = needLevel.valueFor(g);
      float supply = prodLevel.valueFor(g);
      float amount = inventory.valueFor(g);
      amount -= usageInc * demand;
      amount += usageInc * supply;
      if (amount > demand + supply) amount -= usageInc * 5;
      inventory.set(g, Nums.max(0, amount));
    }
    
    Base lord = base.relations.currentLord();
    boolean tribute = base.relations.isLoyalVassalOf(lord);
    
    if (tribute && lord.isOffmap()) {
      Relation r = base.relations.relationWith(lord);
      for (Good g : r.suppliesDue.keys()) {
        float sent = r.suppliesDue.valueFor(g) * usageInc * 1.1f;
        r.suppliesSent.add(sent, g);
      }
    }
  }
  
  
  
  
  public void updateOffmapTraders() {
    if (Visit.empty(base.world.shipTypes)) return;
    if (! base.world.settings.toggleShipping) return;
    
    for (Base b : base.world.bases()) {
      
      POSTURE p = base.relations.posture(b);
      boolean shouldTrade =
        p != POSTURE.NEUTRAL &&
        p != POSTURE.ENEMY   &&
        b.activeMap() != null
      ;
      
      ActorAsVessel trader = traderFor(b);
      boolean isHome = trader != null && base.visitors.includes(trader);
      
      if (trader != null && isHome && ! shouldTrade) {
        base.toggleVisitor(trader, false);
        traders.remove(trader);
        for (Actor a : trader.crew) base.toggleVisitor(a, false);
        continue;
      }
      
      if (shouldTrade && trader == null) {
        ActorType forShip = (ActorType) base.world.shipTypes[0];
        trader = (ActorAsVessel) forShip.generate();
        trader.assignBase(base);
        trader.bonds.assignGuestBase(b);
        trader.setBuildLevel(1);
        traders.add(trader);
        base.toggleVisitor(trader, true);
      }
      
      if (trader != null && shouldTrade && isHome && trader.readyForTakeoff()) {
        TaskTrading trading = BuildingForTrade.selectTraderBehaviour(
          base, trader, b, true, b.activeMap()
        );
        if (trading != null) {
          I.say(trader+" begins task: "+trading);
          trader.assignTask(trading, this);
          trading.beginFromOffmap(base);
        }
        else {
          I.say(trader+" could not find trading behaviour!");
        }
      }
    }
  }


  public Series <ActorAsVessel> traders() {
    return traders;
  }
  
  
  public ActorAsVessel traderFor(Base other) {
    for (ActorAsVessel t : traders) if (t.guestBase() == other) return t;
    return null;
  }
  
  
  public int hireCost(ActorType workerType) {
    return workerType.hireCost;
  }
  
  
  public void addMigrant(Actor migrant) {
    this.migrants.add(migrant);
    base.toggleVisitor(migrant, true);
  }
  
  
  public void removeMigrant(Actor migrant) {
    this.migrants.remove(migrant);
    base.toggleVisitor(migrant, false);
  }
  
  
  public Series <Actor> migrants() {
    return migrants;
  }
}









