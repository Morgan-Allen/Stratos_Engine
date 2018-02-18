

package game;
import util.*;
import static game.CityBorders.*;
import static game.GameConstants.*;



public class TaskTrading extends Task {
  
  
  /**  Data fields, construction/setup and save/load methods-
    */
  Tally <Good> taken;
  Base   homeCity ;
  Trader tradeFrom;
  Trader tradeGoes;
  
  
  public TaskTrading(Actor actor) {
    super(actor);
  }
  
  
  public TaskTrading(Session s) throws Exception {
    super(s);
    s.loadTally(taken);
    homeCity  = (Base  ) s.loadObject();
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
    this.homeCity  = from.base();
    
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
    Actor actor = (Actor) this.active;
    //
    //  If you're embarking on a fresh journey, take on your assigned cargo,
    //  then determine if you're visiting a city or another trading post:
    if (visits == tradeFrom && actor.carried().empty()) {
      if (reports()) {
        I.say("\n"+actor+" SETTING OFF FROM "+visits);
      }
      Base city = tradeGoes.base();
      takeOnGoods(tradeFrom, taken);
      
      if (tradeGoes != city) {
        Building goes = (Building) tradeGoes;
        configTravel(goes, Task.JOB.TRADING, origin);
      }
      else {
        Tile exits = findTransitPoint(visits.map, tradeFrom.base(), city);
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
      actor.clearCarried();
    }
  }
  
  
  protected void onTarget(Target target) {
    Actor actor = (Actor) this.active;
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
    Base city = tradeGoes.base();
    city.world.beginJourney(city, (Base) tradeGoes, actor);
    actor.exitMap(actor.map);
  }
  
  
  protected void onArrival(Base goes, World.Journey journey) {
    Actor actor = (Actor) this.active;
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
  Base oppositeCity(Trader point) {
    if (point == tradeFrom) return tradeGoes.base();
    if (point == tradeGoes) return tradeFrom.base();
    return null;
  }
  
  
  float tributeQuantityRemaining(Base.Relation r, Good good) {
    if (r == null) return 0;
    float demand = r.suppliesDue .valueFor(good);
    float paid   = r.suppliesSent.valueFor(good);
    return Nums.max(0, demand - paid);
  }
  
  
  void takeOnGoods(Trader store, Tally <Good> taken) {
    Actor actor = (Actor) this.active;
    if (store == null) return;
    
    //  You don't have to pay for goods if the city you're taking them from
    //  owes them as tribute!
    Base base = store.base(), opposite = oppositeCity(store);
    boolean tributeDue = base.isLoyalVassalOf(opposite);
    Base.Relation r = base.relationWith(opposite);
    
    Tally <Good> cargo = new Tally();
    Tally <Good> stock = store.inventory();
    cargo.add(actor.carried);
    int totalPaid = 0, totalGets = 0;
    
    for (Good g : taken.keys()) {
      float priceP = opposite.importPrice(g, base);
      float priceG = base.exportPrice(g, opposite);
      float amount = Nums.min(taken.valueFor(g), stock.valueFor(g));
      cargo.add(    amount, g);
      stock.add(0 - amount, g);
      
      if (tributeDue) {
        float tribLeft = tributeQuantityRemaining(r, g);
        float payFor   = Nums.max(0, amount - tribLeft);
        totalPaid += payFor * priceP;
        totalGets += payFor * priceG;
      }
      else {
        totalPaid += amount * priceP;
        totalGets += amount * priceG;
      }
      r.suppliesSent.add(amount, g);
    }
    
    boolean doPayment = base != homeCity;
    if (doPayment) {
      cargo.add(0 - totalPaid, CASH);
      store.inventory().add(totalGets, CASH);
    }
    actor.assignCargo(cargo);
    
    if (reports()) {
      I.say("\n"+actor+" taking on goods from "+store);
      I.say("  New cargo: "+cargo);
      I.say("  Profit: "+cargo.valueFor(CASH));
      I.say("  Cost paid: "+totalPaid+" Cost gets: "+totalGets);
    }
  }
  
  
  void offloadGoods(Trader store) {
    Actor actor = (Actor) this.active;
    if (store == null) return;
    
    //  You don't receive money for goods if the city you deliver to is owed
    //  them as tribute.
    Base base = store.base(), opposite = oppositeCity(store);
    boolean tributeDue = opposite.isLoyalVassalOf(base);
    Base.Relation r = opposite.relationWith(base);
    
    //float cash = actor.carried(CASH);
    //int totalValue = 0;
    int totalPaid = 0, totalGets = 0;
    
    for (Good g : actor.carried().keys()) {
      if (g == CASH) continue;
      
      float priceP = opposite.importPrice(g, base);
      float priceG = base.exportPrice(g, opposite);
      float amount = actor.carried(g);
      store.inventory().add(amount, g);
      
      if (tributeDue) {
        float tribLeft = tributeQuantityRemaining(r, g);
        float payFor   = Nums.max(0, amount - tribLeft);
        totalPaid += payFor * priceP;
        totalGets += payFor * priceG;
      }
      else {
        totalPaid += amount * priceP;
        totalGets += amount * priceG;
      }
    }
    
    if (reports()) {
      I.say("\n"+actor+" depositing goods at "+store);
      I.say("  Cargo: "+actor.carried());
      I.say("  Profit: "+actor.carried(CASH));
      I.say("  Cost paid: "+totalPaid+" Cost gets: "+totalGets);
    }
    
    boolean doPayment = base != homeCity;
    actor.clearCarried();
    //actor.incCarried(CASH, cash + (doPayment ? totalValue : 0));
    
    if (doPayment) {
      actor.incCarried(CASH, totalGets);
      store.inventory().add(0 - totalPaid, CASH);
    }
  }
  
  
  static Tally <Good> configureCargo(
    Trader from, Trader goes, boolean cityOnly, World world
  ) {
    
    //  TODO:  You need to cap the total sum of goods transported based on
    //  cargo limits.
    
    Tally <Good> cargo = new Tally();
    boolean fromCity = from.base() == from;
    boolean goesCity = goes.base() == goes;
    boolean fromFlex = from.base() == goes.base().homeland();
    boolean goesFlex = goes.base() == from.base().homeland();
    
    
    boolean report = false;
    if (report) I.say("\nDoing cargo config for "+from+" -> "+goes);
    
    if (from == null || goes == null        ) return cargo;
    if (cityOnly && ! (fromCity || goesCity)) return cargo;
    Base.Relation fromR = goes.base().relationWith(from.base());
    Base.Relation goesR = from.base().relationWith(goes.base());
    
    for (Good good : world.goodTypes) {
      float amountFrom = from.inventory ().valueFor(good);
      float amountGoes = goes.inventory ().valueFor(good);
      float needFrom   = from.needLevels().valueFor(good);
      float needGoes   = goes.needLevels().valueFor(good);
      
      if (report) {
        I.say("  "+I.padToLength(good.name, 12)+": ");
        I.add(amountFrom+"/"+needFrom+" -> "+amountGoes+"/"+needGoes);
      }
      
      if (fromCity) {
        needFrom = Nums.max(needFrom, fromR.suppliesDue.valueFor(good));
      }
      if (goesCity) {
        needGoes = Nums.max(needGoes, goesR.suppliesDue.valueFor(good));
      }
      
      float surplus  = amountFrom - needFrom;
      float shortage = needGoes - amountGoes;
      
      if (fromFlex) surplus  = Nums.max(surplus , shortage);
      if (goesFlex) shortage = Nums.max(shortage, surplus );
      
      if (surplus > 0 && shortage > 0) {
        float size = Nums.min(surplus, shortage);
        cargo.set(good, size);
      }
    }
    
    if (report) {
      I.say("  Cargo: "+cargo);
      I.say("");
    }
    
    return cargo;
  }
  
  
  static float distanceRating(Trader from, Trader goes) {
    final Base fromB = from.base(), goesB = goes.base();
    if (
      from instanceof Building &&
      goes instanceof Building &&
      fromB == goesB
    ) {
      float mapDist = AreaMap.distance(
        ((Building) from).mainEntrance(),
        ((Building) goes).mainEntrance()
      );
      return 1f / (1 + (mapDist / MAX_TRADER_RANGE));
    }
    else {
      return CityBorders.distanceRating(fromB, goesB);
    }
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  boolean reports() {
    //return tradeFrom instanceof City || tradeGoes instanceof City;
    return super.reports();
  }
  
}




