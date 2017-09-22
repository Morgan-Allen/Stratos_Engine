

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
  
  List <Walker> focused = null;
  
  
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
    
    if (s.loadBool()) s.loadObjects(focused = new List());
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(type);
    s.saveObject(map);
    s.saveInt(x);
    s.saveInt(y);
    s.saveInt(facing);
    
    s.saveFloat(buildLevel);
    s.saveBool(complete);
    
    s.saveBool(focused != null);
    if (focused != null) s.saveObjects(focused);
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
  
  
  
  /**  Handling focus for walker activities-
    */
  void setFocused(Walker w, boolean is) {
    if (is) {
      if (focused == null) focused = new List();
      focused.include(w);
    }
    else {
      focused.remove(w);
      if (focused.size() == 0) focused = null;
    }
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  public String toString() {
    return type.name;
  }
}







