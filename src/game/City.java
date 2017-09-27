

package game;
import game.GameConstants.*;
import util.*;



public class City implements Session.Saveable, Trader {
  
  
  /**  Data fields, construction and save/load methods-
    */
  String name;
  
  World world;
  
  float mapX, mapY;
  Table <City, Integer> distances = new Table();
  
  int currentFunds = 0;
  Tally <Good> tradeLevel = new Tally();
  Tally <Good> inventory  = new Tally();
  
  boolean active;
  CityMap map;
  
  
  City(World world) {
    this.world = world;
  }
  
  
  public City(Session s) throws Exception {
    s.cacheInstance(this);
    
    name = s.loadString();
    
    world = (World) s.loadObject();
    
    mapX = s.loadFloat();
    mapY = s.loadFloat();
    for (int n = s.loadInt(); n-- > 0;) {
      distances.put((City) s.loadObject(), s.loadInt());
    }
    
    currentFunds = s.loadInt();
    s.loadTally(tradeLevel);
    s.loadTally(inventory);
    
    active = s.loadBool();
    map    = (CityMap) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveString(name);
    
    s.saveObject(world);
    
    s.saveFloat(mapX);
    s.saveFloat(mapY);
    s.saveInt(distances.size());
    for (City c : distances.keySet()) {
      s.saveObject(c);
      s.saveInt(distances.get(c));
    }
    
    s.saveInt(currentFunds);
    s.saveTally(tradeLevel);
    s.saveTally(inventory);
    
    s.saveBool(active);
    s.saveObject(map);
  }
  
  
  
  /**  Supplemental setup methods for trade and geography-
    */
  static void setupRoute(City a, City b, int distance) {
    a.distances.put(b, distance);
    b.distances.put(a, distance);
  }
  
  
  void setWorldCoords(float mapX, float mapY) {
    this.mapX = mapX;
    this.mapY = mapY;
  }
  
  
  void attachMap(CityMap map) {
    this.map    = map ;
    this.active = true;
  }
  
  
  public Tally <Good> tradeLevel() { return tradeLevel; }
  public Tally <Good> inventory () { return inventory ; }
  
  
  public City tradeOrigin() {
    return this;
  }
  
  
  
  /**  Regular updates-
    */
  void updateFrom(CityMap map) {
    
    return;
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  public String toString() {
    return name;
  }
}






