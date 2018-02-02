

package game;
import gameUI.play.*;
import graphics.common.*;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class Element implements Session.Saveable, Target, Selection.Focus {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final static int
    FLAG_SITED  = 1 << 0,
    FLAG_ON_MAP = 1 << 1,
    FLAG_BUILT  = 1 << 2,
    FLAG_RAZING = 1 << 3,
    FLAG_EXIT   = 1 << 4,
    FLAG_DEST   = 1 << 5
  ;
  
  private static int nextVarID = 0;
  
  private Type type;
  private int varID;
  
  CityMap map;
  private Tile at;
  private float growLevel = 0;
  private int   buildBits = 0;
  private int   stateBits = 0;
  
  private List <Actor> focused = null;
  private Object pathFlag;  //  Note- used during temporary search.
  
  private Sprite sprite = null;
  
  
  public Element(Type type) {
    this.type  = type;
    this.varID = nextVarID++;
  }
  
  
  public Element(Session s) throws Exception {
    s.cacheInstance(this);
    
    type   = (Type) s.loadObject();
    varID  = s.loadInt();
    map    = (CityMap) s.loadObject();
    at     = loadTile(map, s);
    
    growLevel = s.loadFloat();
    buildBits = s.loadInt();
    stateBits = s.loadInt();
    
    if (s.loadBool()) s.loadObjects(focused = new List());
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(type);
    s.saveInt(varID);
    s.saveObject(map);
    saveTile(at, map, s);
    
    s.saveFloat(growLevel);
    s.saveInt(buildBits);
    s.saveInt(stateBits);
    
    s.saveBool(focused != null);
    if (focused != null) s.saveObjects(focused);
  }
  
  
  
  public Type type() {
    return type;
  }
  
  
  public int varID() {
    return varID;
  }
  
  
  protected void assignType(Type newType) {
    this.type = newType;
  }
  
  
  
  /**  Entering and exiting the map-
    */
  public Visit <Tile> footprint(CityMap map) {
    if (at == null) return map.tilesAround(0, 0, 0, 0);
    return map.tilesUnder(at.x, at.y, type.wide, type.high);
  }
  
  
  public Visit <Tile> perimeter(CityMap map) {
    if (at == null) return map.tilesAround(0, 0, 0, 0);
    return map.tilesAround(at.x, at.y, type.wide, type.high);
  }
  
  
  public Box2D area() {
    if (at == null) return null;
    return new Box2D(at.x - 0.5f, at.y - 0.5f, type.wide, type.high);
  }
  
  
  public Tile centre() {
    if (at == null) return null;
    return map.tileAt(
      at.x + (type.wide / 2),
      at.y + (type.high / 2)
    );
  }
  
  
  public boolean canPlace(CityMap map, int x, int y, int margin) {
    int w = type.wide, h = type.high, m = margin;
    for (Tile t : map.tilesUnder(x - m, y - m, w + (m * 2), h + (m * 2))) {
      if (t == null) return false;
      if (t.above != null && ! t.above.type().isClearable()) return false;
      if (t.terrain.pathing != PATH_FREE) return false;
    }
    return true;
  }
  
  
  public void setLocation(Tile at, CityMap map) {
    this.at  = at;
    this.map = map;
    stateBits |= FLAG_SITED;
  }
  
  
  boolean sited() {
    return (stateBits & FLAG_SITED) != 0;
  }
  
  
  public void enterMap(CityMap map, int x, int y, float buildLevel, City owns) {
    stateBits |= FLAG_ON_MAP;
    setLocation(map.tileAt(x, y), map);
    
    ///I.say("\nEntered map at "+at+": "+this);
    if (! type.mobile) {
      for (Good g : materials()) {
        float need = materialNeed(g);
        setMaterialLevel(g, need * buildLevel);
      }
      
      for (Tile t : footprint(map)) {
        ///I.say("  Footprint: "+t);
        if (t.above != null) t.above.exitMap(map);
        map.setAbove(t, this);
      }
    }
  }
  
  
  public void exitMap(CityMap map) {
    
    if (! type.mobile) {
      if (true       ) setFlagging(false, type.flagKey);
      if (type.isCrop) setFlagging(false, NEED_PLANT  );
      for (Tile t : footprint(map)) {
        if (t.above == this) map.setAbove(t, null);
      }
      setDestroyed();
    }
    
    setLocation(null, map);
    this.map = null;
    stateBits |=  FLAG_EXIT;
    stateBits &= ~FLAG_ON_MAP;
  }
  
  
  
  /**  Associated query methods-
    */
  public void setDestroyed() {
    stateBits |= FLAG_DEST;
  }
  
  
  public boolean destroyed() {
    return (stateBits & FLAG_DEST) != 0;
  }
  
  
  public City homeCity() {
    if (! onMap()) return null;
    return map.locals;
  }
  
  
  public CityMap map() {
    return map;
  }
  
  
  public boolean onMap() {
    return (stateBits & FLAG_ON_MAP) != 0;
  }
  
  
  public Tile at() {
    return at;
  }
  
  
  public boolean isTile() {
    return false;
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
  
  
  public void setGrowLevel(float level) {
    this.growLevel = level;
    if (type.growRate > 0) {
      setFlagging(growLevel >= 1, type.flagKey);
    }
    if (type.isCrop) {
      setFlagging(growLevel == -1, NEED_PLANT);
    }
  }
  
  
  public float growLevel() {
    return growLevel;
  }
  
  
  public Good[] materials() {
    return type.builtFrom;
  }
  
  
  public float materialNeed(Good g) {
    int index = Visit.indexOf(g, type.builtFrom);
    return index == -1 ? 0 : type.builtAmount[index];
  }
  
  
  public void takeDamage(float damage) {
    
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
  
  
  public float buildLevel() {
    float totalNeed = 0, totalHave = 0;
    for (Good g : materials()) {
      totalNeed += materialNeed(g);
      totalHave += materialLevel(g);
    }
    return totalHave / Nums.max(1, totalNeed);
  }
  
  
  public float setMaterialLevel(Good material, float level) {
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
  
  
  public float materialLevel(Good material) {
    return setMaterialLevel(material, -1);
  }
  
  
  public float incMaterialLevel(Good material, float inc) {
    float level = materialLevel(material) + inc;
    return setMaterialLevel(material, level);
  }
  
  
  public void flagTeardown(boolean yes) {
    if (yes) stateBits |= FLAG_RAZING;
    else stateBits &= ~ FLAG_RAZING;
  }
  
  
  public boolean complete() {
    return (stateBits & FLAG_BUILT) != 0;
  }
  
  
  public boolean razing() {
    return (stateBits & FLAG_RAZING) != 0;
  }
  
  
  public void setFlagging(boolean is, Type key) {
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
  
  
  public String fullName() {
    return type.name;
  }
  
  
  public void whenClicked(Object context) {
    PlayUI.pushSelection(this);
  }
  
  
  public boolean trackSelection() {
    return true;
  }
  
  
  public Vec3D trackPosition() {
    if (at == null) return new Vec3D();
    return new Vec3D(at.x + (type.wide / 2f), at.y + (type.high / 2f), 0);
  }
  
  
  public boolean testSelection(PlayUI UI, City base, Viewport port) {
    float height = type.deep;
    float radius = 1.5f * Nums.max(type.wide, type.high) / 2;
    float selRad = (height + radius) / 2;
    Vec3D selPos = trackPosition();
    if (! port.mouseIntersects(selPos, selRad, UI)) return false;
    return true;
  }


  public boolean setSelected(PlayUI UI) {
    return false;
  }
  
  
  public Sprite sprite() {
    if (sprite == null) {
      sprite = type.makeSpriteFor(this);
      if (sprite != null) type.prepareMedia(sprite, this);
    }
    return sprite;
  }
  
  
  public boolean canRender(City base, Viewport view) {
    final Sprite s = sprite();
    if (s == null) return false;
    
    s.position.set(at.x + (type.wide / 2f), at.y + (type.high / 2f), 0);
    float height = type.deep;
    float radius = 1.5f * Nums.max(type.wide, type.high) / 2;
    return s != null && view.intersects(s.position, radius + height + 1);
  }
  
  
  public void renderElement(Rendering rendering, City base) {
    final Sprite s = sprite();
    if (onMap()) {
      final float fog = renderedFog(base);
      s.fog    = fog;
      s.colour = Colour.WHITE;
    }
    s.readyFor(rendering);
  }
  
  
  protected float renderedFog(City base) {
    //return 1;//sightLevel();
    return sightLevel();
    //final Vec3D p = trackPosition();
    //return base.fogMap().renderedFog(p.x, p.y, this);
  }
  
  
  public void renderPreview(Rendering rendering, boolean canPlace, Tile puts) {
    final Sprite s = sprite();
    if (s == null) return;
    s.position.set(puts.x + (type.wide / 2f), puts.y + (type.high / 2f), 0);
    s.colour = canPlace ? Colour.SOFT_GREEN : Colour.SOFT_RED;
    renderElement(rendering, null);
  }
  
}







