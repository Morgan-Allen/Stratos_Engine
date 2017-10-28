

package game;
import util.*;
import static game.CityMap.*;
import static game.CityBorders.*;
import static game.GameConstants.*;



public class TaskTrading extends Task {
  
  
  /**  Data fields, construction/setup and save/load methods-
    */
  Tally <Good> taken;
  City   homeCity ;
  Trader tradeFrom;
  Trader tradeGoes;
  
  
  public TaskTrading(Actor actor) {
    super(actor);
  }
  
  
  public TaskTrading(Session s) throws Exception {
    super(s);
    s.loadTally(taken);
    homeCity  = (City  ) s.loadObject();
    tradeFrom = (Trader) s.loadObject();
    tradeGoes = (Trader) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveTally(taken);
    s.saveObject(homeCity );
    s.saveObject(tradeFrom);
    s.saveObject(tradeFrom);
  }
  
  
  TaskTrading configTrading(
    BuildingForTrade from, Trader goes, Tally <Good> taken
  ) {
    this.taken     = taken;
    this.tradeFrom = from ;
    this.tradeGoes = goes ;
    this.homeCity  = from.map.city;
    return (TaskTrading) configTask(from, from, null, Task.JOB.TRADING, 0);
  }
  
  
  
  /**  Handling actor events-
    */
  protected void onVisit(Building visits) {
    //
    //  If you're embarking on a fresh journey, take on your assigned cargo,
    //  then determine if you're visiting a city or another trading post:
    if (visits == tradeFrom && actor.cargo == null) {
      if (reports()) {
        I.say("\nSETTING OFF FROM "+visits);
      }
      City city = tradeGoes.tradeOrigin();
      takeOnGoods(tradeFrom, taken, false);
      
      if (tradeGoes != city) {
        Building goes = (Building) tradeGoes;
        configTask(origin, goes, null, Task.JOB.TRADING, 0);
      }
      else {
        Tile exits = findTransitPoint(visits.map, city);
        configTask(origin, null, exits, Task.JOB.TRADING, 0);
      }
    }
    //
    //  If you've reached another trading post, simply offload your goods.
    //  (The case of reaching a city off-map is handled below.)
    else if (visits == tradeGoes) {
      if (reports()) {
        I.say("\nREACHED "+tradeGoes);
      }
      offloadGoods(tradeGoes, false);
    }
    //
    //  If you've returned from a journey, offload any goods you might have
    //  brought back, including any profits from exports.
    else if (visits == tradeFrom && actor.cargo != null) {
      float profits = actor.cargo.valueFor(CASH);
      homeCity.currentFunds += profits;
      
      if (reports()) {
        I.say("\nRETURNED TO "+visits);
        I.say("  ADDING TRADE PROFITS: "+profits);
      }
      offloadGoods(tradeFrom, false);
      actor.assignCargo(null);
    }
  }
  
  
  protected void onTarget(Target target) {
    //
    //  If you've arrived at the edge of the map, begin your journey to a
    //  foreign city-
    City city = tradeGoes.tradeOrigin();
    city.world.beginJourney(city, (City) tradeGoes, actor);
    actor.exitMap();
  }
  
  
  protected void onArrival(City goes, World.Journey journey) {
    //
    //  If you've arrived at your destination city, offload your cargo, take on
    //  fresh goods, and record any profits in the process:
    if (goes != homeCity) {
      Tally <Good> taken = configureCargo(goes, tradeFrom, false);
      offloadGoods(goes,        true);
      takeOnGoods (goes, taken, true);
      homeCity.world.beginJourney(journey.goes, homeCity, actor);
    }
    //
    //  If you're arrived back on your home map, return to your post-
    else {
      Building store = (Building) tradeFrom;
      configTask(origin, store, null, Task.JOB.TRADING, 0);
    }
  }
  


  /**  Other utility methods:
    */
  City.Relation cityRelation(Trader trades, Trader with) {
    return trades.tradeOrigin().relationWith(with.tradeOrigin());
  }
  
  
  float tributeQuantityRemaining(City.Relation r, Good good) {
    if (r == null) return 0;
    float demand = r.tributeDue.valueFor(good);
    float paid   = r.goodsSent .valueFor(good);
    return demand - paid;
  }
  
  
  void takeOnGoods(Trader store, Tally <Good> taken, boolean doPayment) {
    if (store == null) return;
    
    //  You don't have to pay for goods if the city you're taking them from
    //  owes them as tribute!
    City.Relation r = cityRelation(store, homeCity);
    boolean tributeDue = r != null && r.nextTributeDate != -1;
    
    Tally <Good> cargo = actor.cargo == null ? new Tally() : actor.cargo;
    Tally <Good> stock = store.inventory();
    int totalCost = 0;
    
    for (Good g : taken.keys()) {
      float amount = Nums.min(taken.valueFor(g), stock.valueFor(g));
      cargo.add(    amount, g);
      stock.add(0 - amount, g);
      
      if (tributeDue) {
        float tribLeft = tributeQuantityRemaining(r, g);
        float paysFor  = Nums.max(0, amount - tribLeft);
        totalCost += paysFor * g.price;
        r.goodsSent.add(amount, g);
      }
      else {
        totalCost += amount * g.price;
      }
    }
    
    cargo.add(doPayment ? (0 - totalCost) : 0, CASH);
    actor.assignCargo(cargo);
    
    if (reports()) {
      I.say("\nTaking on goods from "+store);
      I.say("  New cargo: "+cargo);
      I.say("  Cost: "+totalCost+" Profit: "+cargo.valueFor(CASH));
    }
  }
  
  
  void offloadGoods(Trader store, boolean doPayment) {
    if (store == null) return;
    
    //  You don't receive money for goods if the city you deliver to is owed
    //  them as tribute.
    City.Relation r = cityRelation(homeCity, store);
    boolean tributeDue = r != null && r.nextTributeDate != -1;
    
    float cash = actor.cargo.valueFor(CASH);
    int totalValue = 0;
    
    for (Good g : actor.cargo.keys()) {
      if (g == CASH) continue;
      float amount = actor.cargo.valueFor(g);
      store.inventory().add(amount, g);
      
      if (tributeDue) {
        float tribLeft = tributeQuantityRemaining(r, g);
        float paysFor  = Nums.max(0, amount - tribLeft);
        totalValue += paysFor * g.price;
        r.goodsSent.add(amount, g);
      }
      else {
        totalValue += amount * g.price;
      }
    }
    
    actor.cargo.clear();
    actor.cargo.add(cash + (doPayment ? totalValue : 0), CASH);
    
    if (reports()) {
      I.say("\nDepositing goods at "+store);
      I.say("  Cargo: "+actor.cargo);
      I.say("  Value: "+totalValue+" Profit: "+actor.cargo.valueFor(CASH));
    }
  }
  
}




