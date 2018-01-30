

package game;
import graphics.common.*;
import util.*;
import static game.GameConstants.*;
import java.lang.reflect.*;




public class Type extends Index.Entry implements Session.Saveable {
  
  
  /**  Indexing, categorisation, spawning and save/load methods-
    */
  final public static int
    IS_TRAIT       = -100,
    IS_TERRAIN     = 0,
    IS_FIXTURE     = 1,
    IS_STRUCTURAL  = 2,
    IS_GOOD        = 3,
    IS_BUILDING    = 4,
    IS_UPGRADE     = 5,
    IS_CRAFTS_BLD  = 6,
    IS_GATHER_BLD  = 7,
    IS_WATER_BLD   = 8,
    IS_TRADE_BLD   = 9,
    IS_HOME_BLD    = 10,
    IS_AMENITY_BLD = 11,
    IS_COLLECT_BLD = 12,
    IS_HUNTS_BLD   = 13,
    IS_ARMY_BLD    = 14,
    IS_WALLS_BLD   = 15,
    IS_FAITH_BLD   = 16,
    IS_ACTOR       = 17,
    IS_PERSON_ACT  = 18,
    IS_ANIMAL_ACT  = 19
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
      final Constructor c = baseClass.getConstructor(Type.class);
      return c.newInstance(this);
    }
    catch (NoSuchMethodException e) {
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
  public String name, namesRange[];
  public int tint = BLACK_COLOR;
  public ModelAsset model = null;
  public ModelAsset modelVariants[] = {};
  
  public Class baseClass;
  public int category;
  public Type flagKey = null;
  public int wide = 1, high = 1, deep = 1;
  
  public Good    yields      = null;
  public float   yieldAmount = 0;
  public Good    builtFrom  [] = EMPTY_MATERIAL;
  public Integer builtAmount[] = { 1 };
  
  public int     pathing  = CityMap.PATH_BLOCK;
  public boolean mobile   = false;
  public float   growRate = 0;
  public int     ambience = 0;
  public boolean isCrop   = false;
  public boolean isWater  = false;
  public boolean isWall   = false;
  
  
  public void setDimensions(int w, int h, int d) {
    this.wide = w;
    this.high = h;
    this.deep = d;
    this.maxHealth = (int) (10 * w * h * (1 + ((d - 1) / 2f)));
  }
  
  
  public void setBuildMaterials(Object... args) {
    //
    //  Note:  1 unit of 'nothing' is always included in the list of
    //  build-materials so that a foundation can be laid and allow
    //  other materials to arrive.
    Object ground[] = { VOID, 1 };
    args = Visit.compose(Object.class, ground, args);
    Object split[][] = Visit.splitByModulus(args, 2);
    builtFrom   = (Good   []) castArray(split[0], Good   .class);
    builtAmount = (Integer[]) castArray(split[1], Integer.class);
  }
  
  
  
  /**  Building-specific data fields and setup methods-
    */
  public Type    upgradeTiers[] = NO_TIERS;
  public Type    upgradeNeeds[] = NO_NEEDS;
  public Integer upgradeUsage[] = {};
  public Good    homeUseGoods[] = NO_GOODS;
  public Integer homeUsage   [] = {};
  public Good    homeFoods   [] = {};
  public Good    buildsWith  [] = NO_GOODS;
  public boolean worksBeforeBuilt = false;
  
  public int homeSocialClass  = CLASS_COMMON;
  public int homeAmbienceNeed = AMBIENCE_MIN;
  
  public Good needed  [] = NO_GOODS;
  public Good produced[] = NO_GOODS;
  public Good features[] = NO_GOODS;
  public Type gatherFlag = null;
  public Trait craftSkill = null;
  
  public int updateTime      = AVG_UPDATE_GAP  ;
  public int craftTime       = AVG_CRAFT_TIME  ;
  public int gatherRange     = AVG_GATHER_RANGE;
  public int maxDeliverRange = MAX_TRADER_RANGE;
  public int maxStock        = AVG_MAX_STOCK   ;
  public int homeUseTime     = HOME_USE_TIME   ;
  public int featureAmount   = AVG_SERVICE_GIVE;
  
  public Type workerTypes[] = NO_WALKERS;
  public int maxWorkers   = 1;
  public int maxResidents = 0;
  public int maxVisitors  = AVG_MAX_VISITORS;
  public int maxRecruits  = AVG_ARMY_SIZE;
  
  
  public void setUpgradeTiers(Type... tiers) {
    this.upgradeTiers = tiers;
  }
  
  
  public void setUpgradeNeeds(Object... args) {
    Object split[][] = Visit.splitByModulus(args, 2);
    upgradeNeeds = (Type   []) castArray(split[0], Type   .class);
    upgradeUsage = (Integer[]) castArray(split[1], Integer.class);
  }
  
  
  public void setHomeUsage(Object... args) {
    Object split[][] = Visit.splitByModulus(args, 2);
    homeUseGoods = (Good   []) castArray(split[0], Good   .class);
    homeUsage    = (Integer[]) castArray(split[1], Integer.class);
  }
  
  
  public void setWorkerTypes(Type... types) {
    this.workerTypes = types;
  }
  
  
  public void setFeatures(Good... features) {
    this.features = features;
  }
  
  
  public boolean hasFeature(Good feature) {
    return Visit.arrayIncludes(features, feature);
  }
  
  
  public boolean isNatural() {
    return
      category == IS_FIXTURE ||
      category == IS_TERRAIN ||
      category == IS_ANIMAL_ACT
    ;
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
  
  
  public boolean isHomeBuilding() {
    return category == IS_HOME_BLD;
  }
  
  
  public boolean isTradeBuilding() {
    return category == IS_TRADE_BLD;
  }
  
  
  public boolean isArmyOrWallsBuilding() {
    return category == IS_ARMY_BLD || category == IS_WALLS_BLD;
  }
  
  
  
  /**  Walker-specific stats and setup methods-
    */
  public int  socialClass  = CLASS_COMMON;
  public int  genderRole   = SEX_EITHER;
  public  Type patronGods[] = null;
  public boolean isPorter  = false;
  
  public int maxHealth   = AVG_MAX_HEALTH;
  public int meleeDamage = AVG_MELEE;
  public int rangeDamage = AVG_MISSILE;
  public int rangeDist   = AVG_RANGE;
  public int armourClass = AVG_DEFEND;
  public int sightRange  = AVG_SIGHT;
  
  public Trait   initTraits [] = {};
  public Integer traitLevels[] = {};
  
  public Terrain habitats[]   = NO_HABITAT;
  public boolean predator     = false;
  public int     lifespan     = LIFESPAN_LENGTH;
  public Good[]  foodsAllowed = null;
  public Good    meatType     = null;
  
  
  public void setInitTraits(Object... args) {
    Object split[][] = Visit.splitByModulus(args, 2);
    initTraits  = (Trait  []) castArray(split[0], Trait  .class);
    traitLevels = (Integer[]) castArray(split[1], Integer.class);
  }
  
  
  public float initTraitLevel(Trait t) {
    int index = Visit.indexOf(t, initTraits);
    return index == -1 ? null : traitLevels[index];
  }
  
  
  public void initAsMigrant(ActorAsPerson a) {
    
    float age = Rand.range(AVG_MARRIED, AVG_MENOPAUSE);
    age += Rand.num() - 0.5f;
    
    int sex = genderRole;
    if (sex == SEX_EITHER) {
      sex = Rand.yes() ? SEX_FEMALE : SEX_MALE;
    }
    
    a.ageSeconds = (int) (age * YEAR_LENGTH);
    a.sexData    = sex;
    a.hunger     = Rand.num() - 0.5f;
    
    for (int i = 0; i < initTraits.length; i++) {
      a.setLevel(initTraits[i], traitLevels[i]);
    }
    
    for (Trait t : ALL_PERSONALITY) {
      a.setLevel(t, Rand.range(-1, 1));
    }
  }
  
  
  public void initAsAnimal(ActorAsAnimal a) {
    float age = Rand.num() * a.type().lifespan;
    a.ageSeconds = (int) age;
    a.hunger     = Rand.num() - 0.5f;
    return;
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    return name;
  }
  
  
  public Sprite makeSpriteFor(Element e) {
    if (model != null) {
      return model.makeSprite();
    }
    if (! Visit.empty(modelVariants)) {
      int index = e.varID() % modelVariants.length;
      ModelAsset pick = modelVariants[index];
      return pick.makeSprite();
    }
    return null;
  }
  
  
  public void prepareMedia(Sprite s, Element e) {
    return;
  }
  
}










