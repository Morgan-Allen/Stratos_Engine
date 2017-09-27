

package game;
import static game.GameConstants.*;
import static game.CityMap.*;
import util.*;



public class WalkerForTrade extends Walker implements World.Journeys {
  
  
  
  /**  Data fields, construction and save/load methods-
    */
  Tally <Good> cargo = new Tally();
  float profits;
  City tradesWith;
  boolean offMap = false;
  
  
  public WalkerForTrade(ObjectType type) {
    super(type);
  }
  
  
  public WalkerForTrade(Session s) throws Exception {
    super(s);
    s.loadTally(cargo);
    tradesWith = (City) s.loadObject();
    profits    = s.loadFloat();
    offMap     = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveTally(cargo);
    s.saveObject(tradesWith);
    s.saveFloat(profits);
    s.saveBool(offMap);
  }
  
  
  
  /**  Behaviour scripting...
    */
  void beginDelivery(Building from, Building goes, Tally <Good> taken) {
    if (goes == null || goes.entrance == null) return;
    if (! (from instanceof Trader)) return;
    
    cargo.clear();
    takeOnGoods((Trader) from, taken, false);
    embarkOnVisit(goes, 0, JOB_TRADING);
  }
  
  
  void beginTravel(Building from, City goes, Tally <Good> taken) {
    if (goes == null || ! (from instanceof Trader)) return;
    
    Tile exits = findTransitPoint(map, goes);
    if (exits == null) return;
    
    this.cargo.clear();
    takeOnGoods((Trader) from, taken, false);
    profits    = 0;
    tradesWith = goes;
    
    embarkOnTarget(exits, 0, JOB_TRADING);
  }
  
  
  protected void onVisit(Building visits) {
    Trader partner = (Trader) I.cast(visits, Trader.class);
    City homeCity = home.map.city;

    if (visits == home) {
      offloadGoods(partner, false);
      homeCity.currentFunds += profits;
      profits    = 0;
      tradesWith = null;
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
    
    I.say("\n"+this+" depositing goods at "+store);
    I.say("  Cargo: "+cargo);
    I.say("  Value: "+totalValue);
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
    
    I.say("\n"+this+" taking on goods from "+store);
    I.say("  New cargo: "+cargo);
    I.say("  Cost: "+totalCost+" Profit: "+profits);
  }
  
  
  protected void onTarget(Tile target) {
    if (target == null || tradesWith == null) return;
    tradesWith.world.beginJourney(home.map.city, tradesWith, this);
    exitMap();
  }
  
  
  public void onArrival(City goes, World.Journey journey) {
    if (home.destroyed()) return;
    
    City homeCity = home.map.city;
    if (goes == homeCity) {
      Tile entry = findTransitPoint(homeCity.map, tradesWith);
      enterMap(homeCity.map, entry.x, entry.y);
      startReturnHome();
    }
    else {
      Tally <Good> taken = configureCargo(goes, (Trader) home, false);
      offloadGoods(goes,        true);
      takeOnGoods (goes, taken, true);
      tradesWith.world.beginJourney(tradesWith, homeCity, this);
    }
  }
  
  
  
  /**  Public Utility methods-
    */
  static Tally <Good> configureCargo(
    Trader from, Trader goes, boolean cityOnly
  ) {
    Tally <Good> cargo = new Tally();
    
    if (from == null || goes == null          ) return cargo;
    if (cityOnly && goes.tradeOrigin() != goes) return cargo;
    
    for (Good good : ALL_GOODS) {
      float amountO  = from.inventory ().valueFor(good);
      float demandO  = from.tradeLevel().valueFor(good);
      float amountD  = goes.inventory ().valueFor(good);
      float demandD  = goes.tradeLevel().valueFor(good);
      float surplus  = amountO - Nums.max(0, demandO);
      float shortage = Nums.max(0, demandD) - amountD;
      
      if (surplus > 0 && shortage > 0) {
        float size = Nums.min(surplus, shortage);
        cargo.set(good, size);
      }
    }
    
    return cargo;
  }
  
  
  static float distanceRating(Trader from, Trader goes) {
    
    City fromC = from.tradeOrigin(), goesC = goes.tradeOrigin();
    Integer distance = fromC.distances.get(goesC);
    float distRating = distance == null ? 100 : (1 + distance);
    
    if (
      from instanceof Building &&
      goes instanceof Building &&
      fromC == goesC
    ) {
      Building fromB = (Building) from, goesB = (Building) goes;
      float mapDist = PathSearch.distance(fromB.entrance, goesB.entrance);
      distRating += mapDist / Walker.MAX_WANDER_TIME;
    }
    
    return distRating;
  }
  
  
  static Tile findTransitPoint(CityMap map, City with) {
    
    Pick <Tile> pick = new Pick();
    Vec2D cityDir = new Vec2D(
      with.mapX - map.city.mapX,
      with.mapY - map.city.mapY
    ).normalise(), temp = new Vec2D();
    
    for (Coord c : Visit.perimeter(1, 1, map.size - 2, map.size - 2)) {
      if (map.blocked(c.x, c.y)) continue;
      
      temp.set(c.x - (map.size / 2), c.y - (map.size / 2)).normalise();
      float rating = 1 + temp.dot(cityDir);
      if (map.paved(c.x, c.y)) rating *= 2;
      
      Tile u = map.tileAt(c.x, c.y);
      pick.compare(u, rating);
    }
    
    return pick.result();
  }
}







