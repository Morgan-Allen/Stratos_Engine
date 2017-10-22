

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



public class Element implements Session.Saveable, Target {
  
  
  /**  Data fields, construction and save/load methods-
    */
  Type type;
  
  CityMap map;
  Tile at;
  int facing = N;
  
  float buildLevel;
  
  List <Actor> focused = null;
  
  
  Element(Type type) {
    this.type = type;
  }
  
  
  public Element(Session s) throws Exception {
    s.cacheInstance(this);
    
    type   = (Type) s.loadObject();
    map    = (CityMap) s.loadObject();
    at     = loadTile(map, s);
    facing = s.loadInt();
    
    buildLevel = s.loadFloat();
    
    if (s.loadBool()) s.loadObjects(focused = new List());
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(type);
    s.saveObject(map);
    saveTile(at, map, s);
    s.saveInt(facing);
    
    s.saveFloat(buildLevel);
    
    s.saveBool(focused != null);
    if (focused != null) s.saveObjects(focused);
  }
  
  
  
  /**  Entering and exiting the map-
    */
  boolean canPlace(CityMap map, int x, int y) {
    for (Coord c : Visit.grid(x, y, type.wide, type.high, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t == null || t.paved || t.above != null) return false;
    }
    return true;
  }
  
  
  void enterMap(CityMap map, int x, int y, float buildLevel) {
    this.map = map;
    this.at  = map.tileAt(x, y);
    setBuildLevel(buildLevel);
    
    for (Coord c : Visit.grid(x, y, type.wide, type.high, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      t.above = this;
    }
  }
  
  
  void exitMap(CityMap map) {
    setFlagging(false);
    for (Coord c : Visit.grid(at.x, at.y, type.wide, type.high, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      t.above = null;
    }
    this.map = null;
    this.at  = null;
  }
  
  
  void setDestroyed() {
    return;
  }
  
  
  public Tile at() {
    return at;
  }
  
  
  
  /**  Handling focus for actor activities-
    */
  public float sightLevel() {
    return map.fog.sightLevel(at);
  }
  
  
  public float maxSightLevel() {
    return map.fog.maxSightLevel(at);
  }
  
  
  public void targetedBy(Actor w) {
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
      incBuildLevel(SCAN_PERIOD * type.growRate / RIPEN_PERIOD);
    }
  }
  
  
  void takeDamage(float damage) {
    if (incBuildLevel(0 - damage / type.maxHealth) <= 0) {
      exitMap(map);
      setDestroyed();
    }
  }
  
  
  float buildLevel() {
    return buildLevel;
  }
  
  
  float incBuildLevel(float inc) {
    return setBuildLevel(buildLevel + inc);
  }
  
  
  float setBuildLevel(float level) {
    buildLevel = Nums.clamp(level, 0, 1.1f);
    setFlagging(buildLevel == 1);
    return buildLevel;
  }
  
  
  void setFlagging(boolean is) {
    if (type.flagKey == null || type.mobile) return;
    map.flagType(type.flagKey, at.x, at.y, is);
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  protected boolean reports() {
    return I.talkAbout == this;
  }
  
  
  public String toString() {
    return type.name;
  }
}







