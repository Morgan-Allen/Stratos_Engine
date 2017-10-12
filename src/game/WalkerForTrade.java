

package game;
import util.*;
import static game.CityMap.*;
import static game.CityBorders.*;
import static game.GameConstants.*;



public class WalkerForTrade extends Walker implements Journeys {
  
  
  
  /**  Data fields, construction and save/load methods-
    */
  Tally <Good> cargo = new Tally();
  float profits;
  
  City   homeCity ;
  Trader tradeFrom;
  Trader tradeGoes;
  
  
  public WalkerForTrade(ObjectType type) {
    super(type);
    //I.say("JUST INITIALISED TRADE-WALKER: "+this.hashCode());
  }
  
  
  public WalkerForTrade(Session s) throws Exception {
    super(s);
    s.loadTally(cargo);
    homeCity  = (City  ) s.loadObject();
    tradeFrom = (Trader) s.loadObject();
    tradeGoes = (Trader) s.loadObject();
    profits   = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveTally(cargo);
    s.saveObject(homeCity );
    s.saveObject(tradeFrom);
    s.saveObject(tradeFrom);
    s.saveFloat(profits);
  }
  
  
  
  /**  TODO:  This is a temporary hack!
    */
  void updateAging() {
    return;
  }
  
  
  void update() {
    super.update();
  }
  
  
  void beginNextBehaviour() {
    super.beginNextBehaviour();
  }
  
  

  /**  Behaviour scripting...
    */
  void beginDelivery(Building from, Building goes, Tally <Good> taken) {
    if (goes == null || goes.entrance == null) return;
    if (! (from instanceof Trader)) return;
    
    cargo.clear();
    
    takeOnGoods((Trader) from, taken, false);
    embarkOnVisit(goes, 0, Task.JOB.TRADING, from);
  }
  
  
  void beginTravel(Building from, City goes, Tally <Good> taken) {
    if (goes == null || ! (from instanceof Trader)) return;
    
    Tile exits = findTransitPoint(map, goes);
    if (exits == null) return;
    
    this.cargo.clear();
    profits   = 0;
    homeCity  = map.city;
    tradeFrom = (Trader) from;
    tradeGoes = goes;
    takeOnGoods(tradeFrom, taken, false);
    
    embarkOnTarget(exits, 0, Task.JOB.TRADING, from);
  }
  
  
  protected void onVisit(Building visits) {
    Trader partner = (Trader) I.cast(visits, Trader.class);
    
    if (visits == tradeFrom) {
      offloadGoods(partner, false);
      if (homeCity != null) homeCity.currentFunds += profits;
      profits   = 0;
      tradeFrom = null;
      tradeGoes = null;
    }
    else {
      offloadGoods(partner, false);
    }
  }
  
  
  void offloadGoods(Trader store, boolean doPayment) {
    if (store == null) return;
    
    int totalValue = 0;
    for (Good g : cargo.keys()) {
      float amount = cargo.valueFor(g);
      store.inventory().add(amount, g);
      totalValue += amount * g.price;
    }
    
    if (doPayment) profits += totalValue;
    cargo.clear();
    
    if (reports()) {
      I.say("\n"+this+" depositing goods at "+store);
      I.say("  Cargo: "+cargo);
      I.say("  Value: "+totalValue);
    }
  }
  
  
  void takeOnGoods(Trader store, Tally <Good> taken, boolean doPayment) {
    if (store == null) return;
    
    cargo.clear();
    int totalCost = 0;
    
    for (Good g : taken.keys()) {
      float amount = taken.valueFor(g);
      cargo.set(g, amount);
      store.inventory().add(0 - amount, g);
      totalCost += amount * g.price;
    }
    if (doPayment) profits -= totalCost;
    
    if (reports()) {
      I.say("\n"+this+" taking on goods from "+store);
      I.say("  New cargo: "+cargo);
      I.say("  Cost: "+totalCost+" Profit: "+profits);
    }
  }
  
  
  protected void onTarget(Target target) {
    if (target == null || ! (tradeGoes instanceof City)) return;
    map.city.world.beginJourney(map.city, (City) tradeGoes, this);
    exitMap();
  }
  
  
  public void onArrival(City goes, World.Journey journey) {
    if (homeCity != null && goes != homeCity) {
      Tally <Good> taken = configureCargo(goes, tradeFrom, false);
      offloadGoods(goes,        true);
      takeOnGoods (goes, taken, true);
      homeCity.world.beginJourney(journey.goes, homeCity, this);
    }
    else super.onArrival(goes, journey);
  }
}







