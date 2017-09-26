

package game;
import static game.BuildingSet.*;
import util.*;



public class TradeWalker extends Walker implements World.Journeys {
  
  
  /**  Interface code-
    */
  static interface Partner {
    Tally <Good> stockLevel();
    Tally <Good> inventory ();
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
  
  
  
  /**  Data fields, construction and save/load methods-
    */
  Tally <Good> cargo = new Tally();
  float profits;
  City tradesWith;
  boolean offMap = false;
  
  
  public TradeWalker(ObjectType type) {
    super(type);
  }
  
  
  public TradeWalker(Session s) throws Exception {
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
  void beginDelivery(Building from, Building goes, Tally <Good> cargo) {
    if (from == null || goes == null || goes.entrance == null) return;
    
    for (Good g : cargo.keys()) {
      float amount = cargo.valueFor(g);
      from.inventory.add(0 - amount, g);
      this.cargo.add(amount, g);
    }
    
    pathToward(goes, null, JOB_DELIVER);
  }
  
  
  void beginTravel(Building from, City goes, Tally <Good> cargo) {
    if (from == null || goes == null) return;
    
    Tile exits = findTransitPoint(map, goes);
    if (exits == null) return;
    
    for (Good g : cargo.keys()) {
      float amount = cargo.valueFor(g);
      from.inventory.add(0 - amount, g);
      this.cargo.add(amount, g);
    }
    
    profits = 0;
    this.tradesWith = goes;
    pathToward(null, exits, JOB_TRADING);
  }
  
  
  protected void onTarget(Tile target) {
    if (target == null || tradesWith == null) return;
    tradesWith.world.beginJourney(home.map.city, tradesWith);
    exitMap();
  }
  
  
  public void onArrival(City goes) {
    City homeCity = home.map.city;
    if (goes == homeCity) {
      Tile entry = findTransitPoint(homeCity.map, tradesWith);
      enterMap(homeCity.map, entry.x, entry.y);
      startReturnHome();
    }
    else {
      offloadGoods(tradesWith);
      tradesWith.world.beginJourney(tradesWith, homeCity);
    }
  }
  
  
  protected void onVisit(Building visits) {
    Partner partner = (Partner) I.cast(visits, Partner.class);
    
    if (visits == home) {
      offloadGoods(partner);
    }
    else {
      offloadGoods(partner);
    }
  }
  
  
  void offloadGoods(Partner store) {
    if (store == null) return;
    
    int totalValue = 0;
    for (Good g : cargo.keys()) {
      float amount = cargo.valueFor(g);
      store.inventory().add(amount, g);
      totalValue += amount * g.price;
    }
    
    profits += totalValue;
    cargo.clear();
  }
  
}




