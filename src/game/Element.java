

package game;
import gameUI.play.*;
import graphics.common.*;
import util.*;
import static game.Area.*;
import static game.GameConstants.*;



public class Element implements Session.Saveable, Target, Selection.Focus {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final static int
    FLAG_SITED   = 1 << 0,
    FLAG_ON_PLAN = 1 << 1,
    FLAG_ON_MAP  = 1 << 2,
    FLAG_BUILT   = 1 << 3,
    FLAG_RAZING  = 1 << 4,
    FLAG_EXIT    = 1 << 5,
    FLAG_DEST    = 1 << 6
  ;
  
  private static int nextVarID = 0;
  
  private Type type;
  private int varID;
  
  Area map;
  private AreaTile at;
  private float growLevel = 0;
  private int   buildBits = 0;
  private int   stateBits = 0;
  private int   facing = TileConstants.N;
  
  private Base base;
  
  private List <Active> focused = null;
  private Object pathFlag;  //  Note- used during temporary search.
  
  protected Sprite sprite = null;
  
  
  public Element(Type type) {
    this.type  = type;
    this.varID = nextVarID++;
  }
  
  
  public Element(Session s) throws Exception {
    s.cacheInstance(this);
    
    type   = (Type) s.loadObject();
    varID  = s.loadInt();
    map    = (Area) s.loadObject();
    at     = loadTile(map, s);
    
    growLevel = s.loadFloat();
    buildBits = s.loadInt();
    stateBits = s.loadInt();
    facing    = s.loadInt();
    
    base = (Base) s.loadObject();
    
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
    s.saveInt(facing);
    
    s.saveObject(base);
    
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
  
  
  public void assignBase(Base base) {
    this.base = base;
  }
  
  
  
  /**  Utility methods for location-planning:
    */
  final static int
    CANNOT_PLACE = -1,
    MUST_CLEAR   =  0,
    IS_OKAY      =  1
  ;
  
  
  public Visit <AreaTile> footprint(Area map, boolean withClearance) {
    if (at == null) return map.tilesAround(0, 0, 0, 0);
    int m = withClearance ? type.clearMargin : 0, m2 = m * 2;
    return map.tilesUnder(at.x - m, at.y - m, type.wide + m2, type.high + m2);
  }
  
  
  public Visit <AreaTile> perimeter(Area map) {
    if (at == null) return map.tilesAround(0, 0, 0, 0);
    return map.tilesAround(at.x, at.y, type.wide, type.high);
  }
  
  
  public Box2D area() {
    if (at == null) return null;
    return new Box2D(at.x - 0.5f, at.y - 0.5f, type.wide, type.high);
  }
  
  
  public AreaTile centre() {
    if (at == null) return null;
    return map.tileAt(at.x + (type.wide / 2), at.y + (type.high / 2));
  }
  
  
  public void setLocation(AreaTile at, Area map) {
    this.at  = at;
    this.map = map;
    stateBits |= FLAG_SITED;
  }
  
  
  boolean sited() {
    return (stateBits & FLAG_SITED) != 0;
  }
  
  
  void setOnPlan(boolean is) {
    if (is) stateBits |= FLAG_ON_PLAN;
    else stateBits &= ~FLAG_ON_PLAN;
  }
  
  
  boolean onPlan() {
    return (stateBits & FLAG_ON_PLAN) != 0;
  }
  

  public void setFacing(int facing) {
    this.facing = facing;
  }
  
  
  public int facing() {
    return facing;
  }
  
  
  boolean canPlaceOver(AreaTile t) {
    int pathing = t.terrain.pathing;
    if (type.buildOnWater && pathing == Type.PATH_WATER) return true;
    else if (pathing > Type.PATH_FREE) return false;
    else return true;
  }
  
  
  int canPlaceOver(Element above, int footMask) {
    if (above == null || above == this) return IS_OKAY;
    Type aboveT = above.type();
    if (aboveT.pathing <= Type.PATH_FREE && footMask == 0) return IS_OKAY;
    else if (aboveT.isClearable()) return MUST_CLEAR;
    else return CANNOT_PLACE;
  }
  
  
  boolean checkPlacingConflicts(Area map, Batch <Element> conflicts) {
    
    if (! sited()) return false;
    boolean footprintOkay = true;
    
    for (AreaTile t : footprint(map, true)) {
      if (t == null) {
        footprintOkay = false;
      }
      
      int footMask = type.footprint(t, this);
      if (footMask == -1) continue;
      
      Element onPlan    = map.planning.objectAt(t);
      Element onMap     = map.above(t);
      boolean paves     = pathType() <= Type.PATH_FREE;
      boolean tileOkay  = canPlaceOver(t) || footMask == 0 || onMap == this;
      boolean planOkay  = map.planning.reserveCounter[t.x][t.y] == 0 || paves;
      int     checkPlan = canPlaceOver(onPlan, footMask);
      int     checkMap  = canPlaceOver(onMap , footMask);
      
      if (footMask == 0 || onPlan == this) {
        planOkay = true;
      }
      if (! (tileOkay && planOkay)) {
        footprintOkay = false;
      }
      if (checkPlan == CANNOT_PLACE) {
        footprintOkay = false;
      }
      if (checkMap  == CANNOT_PLACE) {
        footprintOkay = false;
      }
      if (checkPlan != IS_OKAY && conflicts != null) {
        conflicts.include(onPlan);
      }
      if (checkMap  != IS_OKAY && conflicts != null) {
        conflicts.include(onMap );
      }
    }
    
    return footprintOkay;
  }
  
  
  public boolean canPlace(Area map) {
    return checkPlacingConflicts(map, null);
  }
  

  
  /**  Entering and exiting the map-
    */
  public void enterMap(Area map, int x, int y, float buildLevel, Base owns) {
    if (onMap()) {
      I.complain("\nALREADY ON MAP: "+this);
      return;
    }
    if (owns == null) {
      I.complain("\nCANNOT ASSIGN NULL OWNER! "+this);
      return;
    }
    
    stateBits |= FLAG_ON_MAP;
    setLocation(map.tileAt(x, y), map);
    assignBase(owns);
    
    if (! type.mobile) {
      imposeFootprint();
      map.planning.placeObject(this);
    }
    setBuildLevel(buildLevel);
  }
  
  
  public void setBuildLevel(float buildLevel) {
    if (type.mobile) {
      if (buildLevel >= 1) {
        stateBits |= FLAG_BUILT;
      }
    }
    else {
      for (Good g : materials()) {
        float need = materialNeed(g);
        setMaterialLevel(g, need * buildLevel);
      }
    }
  }
  
  
  protected void imposeFootprint() {
    for (AreaTile t : footprint(map, true)) if (t != null) {
      int footMask = type.footprint(t, this);
      int check = canPlaceOver(t.above, footMask);
      
      if (t.above != null && check != IS_OKAY) {
        t.above.exitMap(map);
      }
      if (footMask == 1) {
        map.setAbove(t, this);
        map.pathCache.checkPathingChanged(t);
      }
    }
  }
  
  
  public void exitMap(Area map) {
    
    if (! type.mobile) {
      if (true       ) setFlagging(false, type.flagKey);
      if (type.isCrop) setFlagging(false, NEED_PLANT  );
      
      removeFootprint();
      
      map.planning.unplaceObject(this);
      setDestroyed();
    }
    
    setLocation(null, map);
    this.map = null;
    stateBits |=  FLAG_EXIT;
    stateBits &= ~FLAG_ON_MAP;
  }
  
  
  protected void removeFootprint() {
    for (AreaTile t : footprint(map, false)) {
      if (t.above == this) map.setAbove(t, null);
      map.pathCache.checkPathingChanged(t);
    }
  }
  
  
  
  /**  Associated query methods-
    */
  public void setDestroyed() {
    stateBits |= FLAG_DEST;
  }
  
  
  public boolean destroyed() {
    return (stateBits & FLAG_DEST) != 0;
  }
  
  
  public Base base() {
    return base;
  }
  
  
  public Base guestBase() {
    return base();
  }
  
  
  public Area map() {
    return map;
  }
  
  
  public AreaTile at() {
    return at;
  }
  
  
  public Vec3D exactPosition(Vec3D store) {
    if (at == null) return null;
    return at.exactPosition(store);
  }
  
  
  public float radius() {
    return Nums.max(type.wide, type.high) * Nums.ROOT2 / 2f;
  }
  
  
  public float height() {
    return type.deep;
  }
  
  
  public boolean onMap() {
    return (stateBits & FLAG_ON_MAP) != 0;
  }
  
  
  public boolean isTile() {
    return false;
  }
  
  
  public boolean indoors() {
    return false;
  }
  
  
  public int pathType() {
    return type.pathing;
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
    return type.buildNeed(g);
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
    for (Good g : materials()) {
      TaskBuilding.checkNeedForBuilding(this, g, map, true);
    }
    if (buildLevel >= 1) {
      stateBits |= FLAG_BUILT;
      if (! wasBuilt) onCompletion();
    }
  }
  
  
  void onCompletion() {
    return;
  }
  
  
  public float materialLevel(Good material) {
    return setMaterialLevel(material, -1);
  }
  
  
  public float incMaterialLevel(Good material, float inc) {
    float level = materialLevel(material) + inc;
    return setMaterialLevel(material, Nums.max(0, level));
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
  public float sightLevel(Base views) {
    if (map == null) return 0;
    AreaFog fog = map.fogMap(views, false);
    if (fog == null) return 1;
    AreaTile from = (type.wide == 1 && type.high == 1) ? at : centre();
    return fog.sightLevel(from);
  }
  
  
  public float maxSightLevel(Base views) {
    if (map == null) return 0;
    AreaFog fog = map.fogMap(views, false);
    if (fog == null) return 1;
    AreaTile from = (type.wide == 1 && type.high == 1) ? at : centre();
    return fog.maxSightLevel(from);
  }
  
  
  float ambience() {
    return type.ambience;
  }
  
  
  public float sightRange() {
    return type.sightRange;
  }
  
  
  
  /**  Support methods for combat-
    */
  public int meleeDamage() {
    return type.meleeDamage;
  }
  
  
  public int rangeDamage() {
    return type.rangeDamage;
  }
  
  
  public int attackRange() {
    return type.rangeDist;
  }
  
  
  public int armourClass() {
    return type.armourClass;
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
  

  
  /**  Handling focus for actor activities-
    */
  static List setMember(Active a, boolean is, List l) {
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
  
  
  public void setFocused(Active a, boolean is) {
    focused = setMember(a, is, focused);
  }

  
  public Series <Active> focused() {
    return focused == null ? NONE_ACTIVE : focused;
  }
  
  
  public boolean hasFocus() {
    return focused != null;
  }
  
  
  public void targetedBy(Active w) {
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
    if (! onMap()) return false;
    return true;
  }
  
  
  public Vec3D trackPosition() {
    if (at == null) return new Vec3D();
    return new Vec3D(at.x + (type.wide / 2f), at.y + (type.high / 2f), 0);
  }
  
  
  public boolean testSelection(PlayUI UI, Base base, Viewport port) {
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
  
  
  public Vec3D renderedPosition(Vec3D store) {
    if (store == null) store = new Vec3D();
    store.set(at.x + (type.wide / 2f), at.y + (type.high / 2f), 0);
    return store;
  }
  
  
  public boolean canRender(Base base, Viewport view) {
    final Sprite s = sprite();
    if (s == null) return false;
    
    renderedPosition(s.position);
    float height = height();
    float radius = radius();
    return s != null && view.intersects(s.position, radius + height + 1);
  }
  
  
  public void renderElement(Rendering rendering, Base base) {
    final Sprite s = sprite();
    if (onMap()) {
      final float fog = renderedFog(base);
      s.fog    = fog;
      s.colour = Colour.WHITE;
    }
    s.readyFor(rendering);
  }
  
  
  protected float renderedFog(Base base) {
    return ((sightLevel(base) * 2) + maxSightLevel(base)) / 3;
  }
  
  
  public void renderPreview(Rendering rendering, boolean canPlace, AreaTile puts) {
    final Sprite s = sprite();
    if (s == null) return;
    renderedPosition(s.position);
    s.colour = canPlace ? Colour.SOFT_GREEN : Colour.SOFT_RED;
    renderElement(rendering, null);
  }
  
  
}







