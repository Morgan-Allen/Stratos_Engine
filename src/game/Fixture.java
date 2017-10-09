

package game;
import util.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



public class Fixture implements Session.Saveable, Target {
  
  
  /**  Data fields, construction and save/load methods-
    */
  ObjectType type;
  
  CityMap map;
  CityMap.Tile at;
  int facing = N;
  
  float buildLevel;
  boolean complete;
  
  List <Walker> focused = null;
  
  
  Fixture(ObjectType type) {
    this.type = type;
  }
  
  
  public Fixture(Session s) throws Exception {
    s.cacheInstance(this);
    
    type   = (ObjectType) s.loadObject();
    map    = (CityMap) s.loadObject();
    at     = CityMap.loadTile(map, s);
    facing = s.loadInt();
    
    buildLevel = s.loadFloat();
    complete   = s.loadBool();
    
    if (s.loadBool()) s.loadObjects(focused = new List());
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(type);
    s.saveObject(map);
    CityMap.saveTile(at, map, s);
    s.saveInt(facing);
    
    s.saveFloat(buildLevel);
    s.saveBool(complete);
    
    s.saveBool(focused != null);
    if (focused != null) s.saveObjects(focused);
  }
  
  
  
  /**  Entering and exiting the map-
    */
  void enterMap(CityMap map, int x, int y, float buildLevel) {
    this.map = map;
    this.at  = map.tileAt(x, y);
    this.buildLevel = buildLevel;
    
    for (Coord c : Visit.grid(x, y, type.wide, type.high, 1)) {
      CityMap.Tile t = map.tileAt(c.x, c.y);
      t.above = this;
    }
  }
  
  
  void exitMap(CityMap map) {
    for (Coord c : Visit.grid(at.x, at.y, type.wide, type.high, 1)) {
      CityMap.Tile t = map.tileAt(c.x, c.y);
      t.above = null;
    }
    this.map = null;
    this.at  = null;
  }
  
  
  void setDestroyed() {
    return;
  }
  
  
  public CityMap.Tile at() {
    return at;
  }
  
  
  
  /**  Handling focus for walker activities-
    */
  public void targetedBy(Walker w) {
    return;
  }
  
  
  /*
  public void setFocused(Walker w, boolean is) {
    if (is) {
      if (focused == null) focused = new List();
      focused.include(w);
    }
    else if (focused != null) {
      focused.remove(w);
      if (focused.size() == 0) focused = null;
    }
  }
  
  
  public boolean hasFocus() {
    return focused != null;
  }
  //*/

  
  
  /**  Life cycle, combat and survival methods-
    */
  void updateGrowth() {
    if (type.growRate > 0) {
      buildLevel += SCAN_PERIOD * type.growRate / RIPEN_PERIOD;
      if (buildLevel >= 1) buildLevel = 1;
    }
  }
  
  
  void takeDamage(float damage) {
    buildLevel -= damage / type.maxHealth;
    
    if (buildLevel <= 0) {
      exitMap(map);
      setDestroyed();
    }
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  public String toString() {
    return type.name;
  }
}







