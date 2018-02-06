

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
    this.homeCity  = from.homeCity();
    
    configTravel(from, JOB.TRADING, from);
    return this;
  }
  
  
  
  /**  Handling actor events-
    */
  void configTravel(Building site, Task.JOB jobType, Employer e) {
    if (site.complete()) {
      configTask(e, site, null, jobType, 0);
    }
    else {
      configTask(e, null, site.at(), jobType, 0);
    }
  }
  
  
  protected void onVisit(Building visits) {
    //
    //  If you're embarking on a fresh journey, take on your assigned cargo,
    //  then determine if you're visiting a city or another trading post:
    if (visits == tradeFrom && actor.carried().empty()) {
      if (reports()) {
        I.say("\n"+actor+" SETTING OFF FROM "+visits);
      }
      City city = tradeGoes.homeCity();
      takeOnGoods(tradeFrom, taken);
      
      if (tradeGoes != city) {
        Building goes = (Building) tradeGoes;
        configTravel(goes, Task.JOB.TRADING, origin);
      }
      else {
        Tile exits = findTransitPoint(visits.map, tradeFrom.homeCity(), city);
        configTask(origin, null, exits, Task.JOB.TRADING, 0);
      }
    }
    //
    //  If you've reached another trading post, simply offload your goods.
    //  (The case of reaching a city off-map is handled below.)
    else if (visits == tradeGoes) {
      if (reports()) {
        I.say("\n"+actor+" REACHED "+tradeGoes);
      }
      offloadGoods(tradeGoes);
    }
    //
    //  If you've returned from a journey, offload any goods you might have
    //  brought back, including any profits from exports.
    else if (visits == tradeFrom && ! actor.carried().empty()) {
      float profits = actor.carried(CASH);
      homeCity.incFunds((int) profits);
      if (reports()) {
        I.say("\n"+actor+" RETURNED TO "+visits);
        I.say("  ADDING TRADE PROFITS: "+profits);
      }
      offloadGoods(tradeFrom);
      actor.assignCargo(null);
    }
  }
  
  
  protected void onTarget(Target target) {
    //
    //  We might be visiting an incomplete structure:
    if (target.at().above == tradeFrom) {
      onVisit((Building) tradeFrom);
      return;
    }
    if (target.at().above == tradeGoes) {
      onVisit((Building) tradeGoes);
      return;
    }
    //
    //  If you've arrived at the edge of the map, begin your journey to a
    //  foreign city-
    City city = tradeGoes.homeCity();
    city.world.beginJourney(city, (City) tradeGoes, actor);
    actor.exitMap(actor.map);
  }
  
  
  protected void onArrival(City goes, World.Journey journey) {
    //
    //  If you've arrived at your destination city, offload your cargo, take on
    //  fresh goods, and record any profits in the process:
    if (goes != homeCity) {
      Tally <Good> taken = configureCargo(goes, tradeFrom, false, goes.world);
      offloadGoods(goes       );
      takeOnGoods (goes, taken);
      homeCity.world.beginJourney(journey.goes, homeCity, actor);
    }
    //
    //  If you're arrived back on your home map, return to your post-
    else {
      Building store = (Building) tradeFrom;
      configTravel(store, Task.JOB.TRADING, origin);
    }
  }
  


  /**  Other utility methods:
    */
  City oppositeCity(Trader point) {
    if (point == tradeFrom) return tradeGoes.homeCity();
    if (point == tradeGoes) return tradeFrom.homeCity();
    return null;
  }
  
  
  float tributeQuantityRemaining(City.Relation r, Good good) {
    if (r == null) return 0;
    float demand = r.suppliesDue .valueFor(good);
    float paid   = r.suppliesSent.valueFor(good);
    return Nums.max(0, demand - paid);
  }
  
  
  void takeOnGoods(Trader store, Tally <Good> taken) {
    if (store == null) return;
    
    //  You don't have to pay for goods if the city you're taking them from
    //  owes them as tribute!
    City city = store.homeCity(), opposite = oppositeCity(store);
    boolean tributeDue = city.isLoyalVassalOf(opposite);
    City.Relation r = city.relationWith(opposite);
    
    Tally <Good> cargo = actor.carried();
    Tally <Good> stock = store.inventory();
    int totalCost = 0;
    
    for (Good g : taken.keys()) {
      float amount = Nums.min(taken.valueFor(g), stock.valueFor(g));
      cargo.add(    amount, g);
      stock.add(0 - amount, g);
      
      if (tributeDue) {
        float tribLeft = tributeQuantityRemaining(r, g);
        float payFor   = Nums.max(0, amount - tribLeft);
        totalCost += payFor * g.price;
      }
      else {
        totalCost += amount * g.price;
      }
      
      r.suppliesSent.add(amount, g);
    }
    
    boolean doPayment = city != homeCity;
    cargo.add(doPayment ? (0 - totalCost) : 0, CASH);
    actor.assignCargo(cargo);
    
    if (reports()) {
      I.say("\n"+actor+" taking on goods from "+store);
      I.say("  New cargo: "+cargo);
      I.say("  Cost: "+totalCost+" Profit: "+cargo.valueFor(CASH));
    }
  }
  
  
  void offloadGoods(Trader store) {
    if (store == null) return;
    
    //  You don't receive money for goods if the city you deliver to is owed
    //  them as tribute.
    City city = store.homeCity(), opposite = oppositeCity(store);
    boolean tributeDue = opposite.isLoyalVassalOf(city);
    City.Relation r = opposite.relationWith(city);
    
    float cash = actor.carried(CASH);
    int totalValue = 0;
    
    for (Good g : actor.carried().keys()) {
      if (g == CASH) continue;
      float amount = actor.carried(g);
      store.inventory().add(amount, g);
      
      if (tributeDue) {
        float tribLeft = tributeQuantityRemaining(r, g);
        float payFor   = Nums.max(0, amount - tribLeft);
        totalValue += payFor * g.price;
      }
      else {
        totalValue += amount * g.price;
      }
    }
    
    if (reports()) {
      I.say("\n"+actor+" depositing goods at "+store);
      I.say("  Cargo: "+actor.carried());
      I.say("  Value: "+totalValue+" Profit: "+actor.carried(CASH));
    }
    
    boolean doPayment = city != homeCity;
    actor.clearCarried();
    actor.incCarried(CASH, cash + (doPayment ? totalValue : 0));
  }
  
  
  boolean reports() {
    //return tradeFrom instanceof City || tradeGoes instanceof City;
    return super.reports();
  }
  
}




