

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class Element implements Session.Saveable, Target, Flood.Fill {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final static int
    FLAG_SITED  = 1 << 0,
    FLAG_ON_MAP = 1 << 1,
    FLAG_BUILT  = 1 << 2,
    FLAG_RAZING = 1 << 3,
    FLAG_EXIT   = 1 << 4
  ;
  
  Type type;
  
  CityMap map;
  private Tile at;
  private float growLevel = 0;
  private int   buildBits = 0;
  private int   stateBits = 0;
  
  private List <Actor> focused = null;
  Object pathFlag;  //  Note- this is used purely during path-searches,
                    //  and doesn't have to be saved or loaded.
  
  
  Element(Type type) {
    this.type = type;
  }
  
  
  public Element(Session s) throws Exception {
    s.cacheInstance(this);
    
    type   = (Type) s.loadObject();
    map    = (CityMap) s.loadObject();
    at     = loadTile(map, s);
    growLevel = s.loadFloat();
    buildBits = s.loadInt();
    stateBits = s.loadInt();
    
    if (s.loadBool()) s.loadObjects(focused = new List());
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(type);
    s.saveObject(map);
    saveTile(at, map, s);
    s.saveFloat(growLevel);
    s.saveInt(buildBits);
    s.saveInt(stateBits);
    
    s.saveBool(focused != null);
    if (focused != null) s.saveObjects(focused);
  }
  
  
  
  /**  Entering and exiting the map-
    */
  boolean canPlace(CityMap map, int x, int y) {
    for (Coord c : Visit.grid(x, y, type.wide, type.high, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t == null || t.above != null) return false;
    }
    return true;
  }
  
  
  void setLocation(Tile at, CityMap map) {
    this.at  = at;
    this.map = map;
    stateBits |= FLAG_SITED;
  }
  
  
  boolean sited() {
    return (stateBits & FLAG_SITED) != 0;
  }
  
  
  void enterMap(CityMap map, int x, int y, float buildLevel) {
    stateBits |= FLAG_ON_MAP;
    setLocation(map.tileAt(x, y), map);
    
    for (Good g : materials()) {
      float need = materialNeed(g);
      setMaterialLevel(g, need * buildLevel);
    }
    
    for (Tile t : map.tilesUnder(at.x, at.y, type.wide, type.high)) {
      if (t.above != null) t.above.exitMap(map);
      map.setAbove(t, this);
    }
  }
  
  
  void exitMap(CityMap map) {
    if (true       ) setFlagging(false, type.flagKey);
    if (type.isCrop) setFlagging(false, NEED_PLANT  );
    
    for (Tile t : map.tilesUnder(at.x, at.y, type.wide, type.high)) {
      if (t.above == this) map.setAbove(t, null);
    }
    
    setLocation(null, null);
    stateBits |= FLAG_EXIT;
  }
  
  
  void setDestroyed() {
    return;
  }
  
  
  public boolean onMap() {
    return (stateBits & FLAG_ON_MAP) != 0;
  }
  
  
  public Tile at() {
    return at;
  }
  

  public float pathHeight() {
    return type.deep;
  }
  
  
  public int pathType() {
    return complete() ? type.pathing : PATH_NONE;
  }
  
  
  public boolean allowsEntryFrom(Pathing p) {
    return false;
  }
  
  
  public void flagWith(Object o) {
    pathFlag = o;
  }
  
  
  public Object flaggedWith() {
    return pathFlag;
  }
  


  /**  Growth and construction methods-
    */
  void updateGrowth() {
    if (type.growRate > 0 && growLevel != -1) {
      float inc = SCAN_PERIOD * type.growRate / RIPEN_PERIOD;
      setGrowLevel(Nums.clamp(growLevel + inc, 0, 1));
    }
  }
  
  
  void setGrowLevel(float level) {
    this.growLevel = level;
    if (type.growRate > 0) {
      setFlagging(growLevel >= 1, type.flagKey);
    }
    if (type.isCrop) {
      setFlagging(growLevel == -1, NEED_PLANT);
    }
  }
  
  
  float growLevel() {
    return growLevel;
  }
  
  
  Good[] materials() {
    return type.builtFrom;
  }
  
  
  float materialNeed(Good g) {
    int index = Visit.indexOf(g, type.builtFrom);
    return index == -1 ? 0 : type.builtAmount[index];
  }
  
  
  void takeDamage(float damage) {
    
    float totalNeed = 0, totalHave = 0;
    for (Good g : materials()) {
      totalNeed += materialNeed(g);
      totalHave += materialLevel(g);
    }
    float totalHealth = type.maxHealth * totalHave / totalNeed;
    
    for (Good g : materials()) {
      float level = materialLevel(g);
      float sub = level;
      sub *= damage / totalHealth;
      sub *= level / totalHave;
      setMaterialLevel(g, level - sub);
    }
    
    if (buildLevel() <= 0) {
      exitMap(map);
      setDestroyed();
    }
  }
  
  
  float buildLevel() {
    float totalNeed = 0, totalHave = 0;
    for (Good g : materials()) {
      totalNeed += materialNeed(g);
      totalHave += materialLevel(g);
    }
    return totalHave / Nums.max(1, totalNeed);
  }
  
  
  float setMaterialLevel(Good material, float level) {
    int index = Visit.indexOf(material, materials());
    if (index == -1) return 0;
    
    int amount, shift = index * 8;
    if (level == -1) {
      amount = (buildBits >> shift) & 0xff;
      return amount / 10f;
    }
    
    amount = (int) (Nums.clamp(level, 0, 10) * 10);
    buildBits &= ~(0xff   << shift);
    buildBits |=  (amount << shift);
    
    updateBuildState();
    return amount / 10f;
  }
  
  
  void updateBuildState() {
    float buildLevel = buildLevel();
    boolean wasBuilt = complete();
    
    if (type.growRate == 0) {
      setFlagging(buildLevel >= 1, type.flagKey);
    }
    
    if (buildLevel >= 1) {
      stateBits |= FLAG_BUILT;
      if (! wasBuilt) onCompletion();
    }
  }
  
  
  void onCompletion() {
    //  Underlying tiles may have become blocked now-
    for (Tile t : map.tilesUnder(at.x, at.y, type.wide, type.high)) {
      map.pathCache.checkPathingChanged(t);
    }
  }
  
  
  float materialLevel(Good material) {
    return setMaterialLevel(material, -1);
  }
  
  
  float incMaterialLevel(Good material, float inc) {
    float level = materialLevel(material) + inc;
    return setMaterialLevel(material, level);
  }
  
  
  void flagTeardown(boolean yes) {
    if (yes) stateBits |= FLAG_RAZING;
    else stateBits &= ~ FLAG_RAZING;
  }
  
  
  boolean complete() {
    return (stateBits & FLAG_BUILT) != 0;
  }
  
  
  boolean razing() {
    return (stateBits & FLAG_RAZING) != 0;
  }
  
  
  void setFlagging(boolean is, Type key) {
    if (key == null || type.mobile) return;
    map.flagType(key, at.x, at.y, is);
  }
  
  
  
  /**  Support methods for sight and fog-levels:
    */
  public float sightLevel() {
    return map.fog.sightLevel(at);
  }
  
  
  public float maxSightLevel() {
    return map.fog.maxSightLevel(at);
  }
  
  
  float ambience() {
    return type.ambience;
  }
  

  
  /**  Handling focus for actor activities-
    */
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
  
  
  public void targetedBy(Actor w) {
    return;
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  protected boolean reports() {
    return I.talkAbout == this;
  }
  
  
  public String toString() {
    return type.name;
  }
  

  public int debugTint() {
    return type.tint;
  }
}







