

package game;
import static game.BuildingSet.*;
import util.*;



public class Trader extends Walker implements World.Journeys {
  
  
  /**  Interface code-
    */
  static interface Partner {
    Tally <Good> demands  ();
    Tally <Good> inventory();
  }
  
  
  static Tile findTransitPoint(CityMap map, City with) {
    return null;
  }
  
  
  
  /**  Data fields, construction and save/load methods-
    */
  Tally <Good> cargo = new Tally();
  float profits;
  City tradesWith;
  boolean offMap = false;
  
  
  public Trader(ObjectType type) {
    super(type);
  }
  
  
  public Trader(Session s) throws Exception {
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




