

package game;
import util.*;
import static util.TileConstants.*;



public class Fixture implements Session.Saveable {
  
  
  /**  Data fields, construction and save/load methods-
    */
  ObjectType type;
  
  City map;
  int x, y, facing = N;
  
  float buildLevel;
  boolean complete;
  
  
  Fixture(ObjectType type) {
    this.type = type;
  }
  
  
  public Fixture(Session s) throws Exception {
    s.cacheInstance(this);
    
    type   = (ObjectType) s.loadObject();
    map    = (City) s.loadObject();
    x      = s.loadInt();
    y      = s.loadInt();
    facing = s.loadInt();
    
    buildLevel = s.loadFloat();
    complete   = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(type);
    s.saveObject(map);
    s.saveInt(x);
    s.saveInt(y);
    s.saveInt(facing);
    
    s.saveFloat(buildLevel);
    s.saveBool(complete);
  }
  
  
  
  /**  Entering and exiting the map-
    */
  void enterMap(City map, int x, int y) {
    this.map = map;
    this.x   = x  ;
    this.y   = y  ;
    
    for (Coord c : Visit.grid(x, y, type.wide, type.high, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      t.above = this;
    }
  }
  
  
  void exitMap(City map) {
    for (Coord c : Visit.grid(x, y, type.wide, type.high, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      t.above = null;
    }
  }
  
  
  
  /**  Regular updates-
    */
  void updateGrowth() {
    return;
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  public String toString() {
    return type.name;
  }
}







