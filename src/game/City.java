

package game;
import game.BuildingSet.*;
import util.*;



public class City implements Session.Saveable {
  
  
  Tally <Good> demanded = new Tally();
  Tally <Good> supplied = new Tally();
  
  World world;
  boolean active;
  CityMap map;
  
  
  City(World world) {
    this.world = world;
  }
  
  
  public City(Session s) throws Exception {
    s.cacheInstance(this);
    
    s.loadTally(demanded);
    s.loadTally(supplied);
    
    world  = (World) s.loadObject();
    active = s.loadBool();
    map    = (CityMap) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveTally(demanded);
    s.saveTally(supplied);
    
    s.saveObject(world);
    s.saveBool(active);
    s.saveObject(map);
  }
  
  
  
  void updateFrom(CityMap map) {
    
    
    return;
  }
  
  
  
}








