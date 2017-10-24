

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class Element implements Session.Saveable, Target {
  
  
  /**  Data fields, construction and save/load methods-
    */
  Type type;
  
  CityMap map;
  private Tile at;
  private float buildLevel = -1;
  
  private List <Actor> focused = null;
  
  
  Element(Type type) {
    this.type = type;
  }
  
  
  public Element(Session s) throws Exception {
    s.cacheInstance(this);
    
    type   = (Type) s.loadObject();
    map    = (CityMap) s.loadObject();
    at     = loadTile(map, s);
    buildLevel = s.loadFloat();
    
    if (s.loadBool()) s.loadObjects(focused = new List());
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(type);
    s.saveObject(map);
    saveTile(at, map, s);
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
    setLocation(map.tileAt(x, y));
    setBuildLevel(buildLevel);
    
    for (Coord c : Visit.grid(x, y, type.wide, type.high, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t.above != null) t.above.exitMap(map);
      t.above = this;
    }
  }
  
  
  void exitMap(CityMap map) {
    if (true       ) setFlagging(false, type.flagKey);
    if (type.isCrop) setFlagging(false, NEED_PLANT  );
    
    for (Coord c : Visit.grid(at.x, at.y, type.wide, type.high, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      t.above = null;
    }
    
    setLocation(null);
    this.map = null;
  }
  
  
  void setLocation(Tile at) {
    this.at = at;
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
  
  
  static List <Actor> setMember(Actor a, boolean is, List <Actor> l) {
    if (is) {
      if (l == null) l = new List();
      l.include(a);
    }
    else if (l != null) {
      l.remove(a);
      if (l.size() == 0) l = null;
    }
    return l;
  }
  
  
  public void setFocused(Actor a, boolean is) {
    focused = setMember(a, is, focused);
  }

  
  public Series <Actor> focused() {
    return focused == null ? NO_ACTORS : focused;
  }
  
  
  public boolean hasFocus() {
    return focused != null;
  }

  
  
  /**  Life cycle, combat and survival methods-
    */
  void updateGrowth() {
    if (type.growRate > 0 && buildLevel != -1) {
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
    if (level == -1) {
      buildLevel = -1;
    }
    else {
      buildLevel = Nums.clamp(level, 0, 1.1f);
    }
    if (true) {
      setFlagging(buildLevel >= 1, type.flagKey);
    }
    if (type.isCrop) {
      setFlagging(buildLevel == -1, NEED_PLANT);
    }
    return buildLevel;
  }
  
  
  void setFlagging(boolean is, Type key) {
    if (key == null || type.mobile) return;
    map.flagType(key, at.x, at.y, is);
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







