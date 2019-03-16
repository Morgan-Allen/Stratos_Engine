

package game;
import graphics.common.*;
import util.*;
import static game.GameConstants.*;
import java.lang.reflect.*;
import game.GameConstants.Good;




public class Type extends Constant {
  
  
  /**  Common data fields and setup functions-
    */
  final public static int
    PATH_NONE   = -1,
    PATH_PAVE   =  1,
    PATH_FREE   =  2,
    PATH_HINDER =  3,
    PATH_WALLS  =  4,
    PATH_BLOCK  =  5,
    PATH_WATER  =  6
  ;
  final public static int
    MOVE_NONE  = -1,
    MOVE_LAND  =  1,
    MOVE_WATER =  2,
    MOVE_AIR   =  3
  ;
  final public static int
    NO_MARGIN   = 0,
    THIN_MARGIN = 1,
    WIDE_MARGIN = 2
  ;
  
  
  public String name = "";
  public String traitRangeNames[];
  public int tint = BLACK_COLOR;
  
  public ImageAsset icon = null;
  public ModelAsset model = null;
  public ModelAsset modelVariants[] = {};
  
  
  public Type flagKey = null;
  public Good features[] = NO_GOODS;
  
  public int wide = 1, high = 1, deep = 1;
  private byte[][] footprint = {{ 1 }};
  int clearMargin;
  
  public boolean rulerBuilt     = true ;
  public boolean uniqueBuilding = false;
  public boolean buildAsArea    = false;
  public boolean buildOnWater   = false;
  
  public Good    yields      = null;
  public float   yieldAmount = 0;
  public Good    builtFrom  [] = EMPTY_MATERIAL;
  public Integer builtAmount[] = { 1 };
  
  public int     pathing  = PATH_BLOCK;
  public boolean mobile   = false;
  public float   growRate = 0;
  public int     ambience = 0;
  public boolean isCrop   = false;
  public boolean isWall   = false;
  
  public Good weaponType = null;
  public Good armourType = null;
  public Good useItemTypes[] = {};
  public int maxHealth   = AVG_MAX_HEALTH;
  public int meleeDamage = 0;
  public int rangeDamage = 0;
  public int plasDamage  = 0;
  public int rangeDist   = 0;
  public int armourClass = 0;
  public int shieldBonus = 0;
  public int sightRange  = AVG_SIGHT;
  
  
  
  public Type(Class baseClass, String ID, int category) {
    super(baseClass, ID, category);
  }
  
  
  Object[] castArray(Object arr[], Class c) {
    Object n[] = (Object[]) Array.newInstance(c, arr.length);
    for (int i = arr.length; i-- > 0;) n[i] = arr[i];
    return n;
  }
  
  
  public void setDimensions(int w, int h, int d) {
    setDimensions(w, h, d, NO_MARGIN);
  }
  
  
  public void setDimensions(int w, int h, int d, int clearMargin) {
    this.wide = w;
    this.high = h;
    this.deep = d;
    this.clearMargin = clearMargin;
    this.maxHealth = (int) (BUILD_TILE_HP * w * h * ((d + 1) / 2f));
    
    int m = this.clearMargin;
    int spanW = w + (m * 2), spanH = h + (m * 2);
    footprint = new byte[spanW][spanH];
    for (Coord c : Visit.grid(0, 0, w, h, 1)) footprint[c.x + m][c.y + m] = 1;
  }
  
  
  public int footprint(AreaTile at, Element e) {
    if (at == null || e == null) return -1;
    
    int m = this.clearMargin;
    int spanW = wide + (m * 2), spanH = high + (m * 2);
    
    AreaTile o = e.at();
    int x = at.x + m - o.x, y = at.y + m - o.y;
    if (x < 0 || x >= spanW || y < 0 || y >= spanH) return -1;
    return footprint[x][y];
  }
  
  
  public void setBuildMaterials(Object... args) {
    //
    //  Note:  1 unit of 'nothing' is always included in the list of
    //  build-materials so that a foundation can be laid and allow
    //  other materials to arrive.
    if (! Visit.arrayIncludes(args, VOID)) {
      Object ground[] = { VOID, 1 };
      args = Visit.compose(Object.class, ground, args);
    }
    Object split[][] = Visit.splitByModulus(args, 2);
    builtFrom   = (Good   []) castArray(split[0], Good   .class);
    builtAmount = (Integer[]) castArray(split[1], Integer.class);
  }
  
  
  public float buildNeed(Good g) {
    int index = Visit.indexOf(g, builtFrom);
    if (index != -1) return builtAmount[index];
    return 0;
  }
  
  
  
  /**  Building-specific data fields and setup methods-
    */
  public boolean rulerCanBuild(Base ruler, AreaMap map) {
    if (! rulerBuilt) return false;
    if (uniqueBuilding) {
      for (Building b : map.buildings()) if (b.type() == this) return false;
    }
    return true;
  }
  
  
  public boolean isNatural() {
    return
      category == IS_FIXTURE ||
      category == IS_TERRAIN ||
      category == IS_ANIMAL_ACT
    ;
  }
  
  
  public boolean isClearable() {
    return category == IS_FIXTURE || category == IS_GOOD;
  }
  
  
  public boolean isTerrain() {
    return category == IS_TERRAIN;
  }
  
  
  public boolean isFlora() {
    return category == IS_FIXTURE && growRate > 0;
  }
  
  
  public boolean isFauna() {
    return category == IS_ANIMAL_ACT;
  }
  
  
  public boolean isFixture() {
    return category == IS_FIXTURE || category == IS_STRUCTURAL;
  }
  
  
  public boolean isBuilding() {
    return category >= IS_BUILDING && category < IS_ACTOR;
  }
  
  
  public boolean isActor() {
    return category >= IS_ACTOR;
  }
  
  
  public boolean isPerson() {
    return category == IS_PERSON_ACT;
  }
  
  
  public boolean isAnimal() {
    //  TODO:  Create a separate category for non-organics...
    return category == IS_ANIMAL_ACT && ((ActorType) this).organic;
  }
  
  
  public boolean isConstruct() {
    //  TODO:  Create a separate category for non-organics...
    return category == IS_ANIMAL_ACT && ! ((ActorType) this).organic;
  }
  
  
  public boolean isVessel() {
    return category == IS_VESSEL_ACT;
  }
  
  
  public boolean isAirship() {
    return isVessel() && ((ActorType) this).moveMode == MOVE_AIR;
  }
  
  
  public boolean isHomeBuilding() {
    return category == IS_HOME_BLD;
  }
  
  
  public boolean isTradeBuilding() {
    return category == IS_TRADE_BLD;
  }
  
  
  public boolean isDockBuilding() {
    return category == IS_DOCK_BLD;
  }
  
  
  public boolean isMilitaryBuilding() {
    return
      category == IS_ARMY_BLD  ||
      category == IS_WALLS_BLD ||
      category == IS_GOVERN_BLD
    ;
  }
  
  
  public boolean isNestBuilding() {
    return category == IS_NEST_BLD;
  }
  
  
  public boolean hasFeature(Good feature) {
    return Visit.arrayIncludes(features, feature);
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    return name;
  }
  
  
  public Sprite makeSpriteFor(Element e) {
    if (model != null && model.stateLoaded()) {
      return model.makeSprite();
    }
    if (! Visit.empty(modelVariants)) {
      int index = e.varID() % modelVariants.length;
      ModelAsset pick = modelVariants[index];
      if (pick != null && pick.stateLoaded()) return pick.makeSprite();
    }
    return null;
  }
  
  
  public void prepareMedia(Sprite s, Element e) {
    if (s == null || e == null) return;
  }
  
  
  String composeName() {
    return name;
  }
  
}








