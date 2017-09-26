

package game;
import game.BuildingSet.*;
import util.*;



public class City implements Session.Saveable {
  
  
  /**  Data fields, construction and save/load methods-
    */
  World world;
  
  float mapX, mapY;
  Table <City, Integer> distances = new Table();
  
  Tally <Good> stockLevels = new Tally();
  Tally <Good> inventory   = new Tally();
  
  boolean active;
  CityMap map;
  
  
  City(World world) {
    this.world = world;
  }
  
  
  public City(Session s) throws Exception {
    s.cacheInstance(this);
    
    world = (World) s.loadObject();
    
    mapX  = s.loadFloat();
    mapY  = s.loadFloat();
    for (int n = s.loadInt(); n-- > 0;) {
      distances.put((City) s.loadObject(), s.loadInt());
    }
    
    s.loadTally(stockLevels);
    s.loadTally(inventory);
    
    active = s.loadBool();
    map    = (CityMap) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(world);
    
    s.saveFloat(mapX);
    s.saveFloat(mapY);
    s.saveInt(distances.size());
    for (City c : distances.keySet()) {
      s.saveObject(c);
      s.saveInt(distances.get(c));
    }
    
    s.saveTally(stockLevels);
    s.saveTally(inventory);
    
    s.saveBool(active);
    s.saveObject(map);
  }
  
  
  
  /**  Supplemental setup methods-
    */
  static void setupRoute(City a, City b, int distance) {
    a.distances.put(b, distance);
    b.distances.put(a, distance);
  }
  
  
  void attachMap(CityMap map) {
    this.map    = map ;
    this.active = true;
  }
  
  
  
  /**  Regular updates-
    */
  void updateFrom(CityMap map) {
    
    
    return;
  }
  
}



