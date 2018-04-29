

package game;
import graphics.common.*;
import util.*;
import static game.GameConstants.*;
import java.lang.reflect.*;

import game.GameConstants.Good;




public class Type extends Index.Entry implements Session.Saveable {
  
  
  /**  Indexing, categorisation, spawning and save/load methods-
    */
  final public static int
    IS_MEDIA       = -200,
    IS_TRAIT       = -100,
    IS_TERRAIN     =  0,
    IS_FIXTURE     =  1,
    IS_STRUCTURAL  =  2,
    IS_GOOD        =  3,
    IS_BUILDING    =  4,
    IS_UPGRADE     =  5,
    IS_CRAFTS_BLD  =  6,
    IS_GATHER_BLD  =  7,
    IS_MARKET_BLD  =  8,
    IS_TRADE_BLD   =  9,
    IS_DOCK_BLD    =  10,
    IS_HOME_BLD    =  11,
    IS_AMENITY_BLD =  12,
    IS_GOVERN_BLD  =  13,
    IS_HUNTS_BLD   =  14,
    IS_ARMY_BLD    =  15,
    IS_WALLS_BLD   =  16,
    IS_FAITH_BLD   =  17,
    IS_NEST_BLD    =  18,
    IS_ACTOR       =  19,
    IS_PERSON_ACT  =  20,
    IS_ANIMAL_ACT  =  21,
    IS_VESSEL_ACT  =  22
  ;
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
  
  
  final static Index <Type> INDEX = new Index();
  
  
  public Type(Class baseClass, String ID, int category) {
    super(INDEX, ID);
    this.baseClass = baseClass;
    this.category  = category ;
  }
  
  
  public static Type loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  public Object generate() {
    if (baseClass == null) {
      return null;
    }
    try {
      if (! Element.class.isAssignableFrom(baseClass)) return null;
      Constructor c = null;
      for (Constructor n : baseClass.getConstructors()) {
        Class params[] = n.getParameterTypes();
        if (params.length == 1 && Type.class.isAssignableFrom(params[0])) {
          c = n;
          break;
        }
      }
      return c.newInstance(this);
    }
    catch (NullPointerException e) {
      I.say(
        "\n  WARNING: NO TYPE CONSTRUCTOR FOR: "+baseClass.getName()+
        "\n  All Elements should implement a public constructor taking a Type "+
        "\n  as the sole argument, or else their Type should override the "+
        "\n  generate() method.  Thank you.\n"
      );
      return null;
    }
    catch (Exception e) {
      I.say("ERROR INSTANCING "+baseClass.getSimpleName()+": "+e);
      e.printStackTrace();
      return null;
    }
  }
  
  
  Object[] castArray(Object arr[], Class c) {
    Object n[] = (Object[]) Array.newInstance(c, arr.length);
    for (int i = arr.length; i-- > 0;) n[i] = arr[i];
    return n;
  }
  
  
  
  /**  Common data fields and setup functions-
    */
  public String name = "";
  public String traitRangeNames[];
  public int tint = BLACK_COLOR;
  
  public ImageAsset icon = null;
  public ModelAsset model = null;
  public ModelAsset modelVariants[] = {};
  
  public Class baseClass;
  public int category;
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
  public boolean rulerCanBuild(Base ruler, Area map) {
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
    return category == IS_ANIMAL_ACT;
  }
  
  
  public boolean isConstruct() {
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
  
  
  public boolean isArmyOrWallsBuilding() {
    return category == IS_ARMY_BLD || category == IS_WALLS_BLD;
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








