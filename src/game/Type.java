

package game;
import static game.GameConstants.*;
import util.*;
import java.lang.reflect.*;




public class Type extends Index.Entry implements Session.Saveable {
  
  
  /**  Indexing, categorisation, spawning and save/load methods-
    */
  final static int
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
  
  
  Type(Class baseClass, String ID, int category) {
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
  
  
  Object generate() {
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
  String name, namesRange[];
  int tint = BLACK_COLOR;
  
  
  Class baseClass;
  int category;
  Type flagKey = null;
  int wide = 1, high = 1, deep = 1;
  
  Good    yields      = null;
  float   yieldAmount = 0;
  Good    builtFrom  [] = EMPTY_MATERIAL;
  Integer builtAmount[] = { 1 };
  
  int     pathing  = CityMap.PATH_BLOCK;
  boolean mobile   = false;
  float   growRate = 0;
  int     ambience = 0;
  boolean isCrop   = false;
  boolean isWater  = false;
  boolean isWall   = false;
  
  
  void setDimensions(int w, int h, int d) {
    this.wide = w;
    this.high = h;
    this.deep = d;
    this.maxHealth = (int) (10 * w * h * (1 + ((d - 1) / 2f)));
  }
  
  
  void setBuildMaterials(Object... args) {
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
  Good    buildsWith  [] = NO_GOODS;
  Type    upgradeTiers[] = NO_TIERS;
  Type    upgradeNeeds[] = NO_NEEDS;
  Integer upgradeUsage[] = {};
  Good    homeUseGoods[] = NO_GOODS;
  Integer homeUsage   [] = {};
  boolean worksBeforeBuilt = false;
  
  int homeSocialClass  = CLASS_COMMON;
  int homeAmbienceNeed = AMBIENCE_MIN;
  
  Good needed  [] = NO_GOODS;
  Good produced[] = NO_GOODS;
  Good features[] = NO_GOODS;
  Type gatherFlag = null;
  Trait craftSkill = null;
  
  int updateTime      = AVG_UPDATE_GAP  ;
  int craftTime       = AVG_CRAFT_TIME  ;
  int gatherRange     = AVG_GATHER_RANGE;
  int maxDeliverRange = MAX_TRADER_RANGE;
  int maxStock        = AVG_MAX_STOCK   ;
  int homeUseTime     = HOME_USE_TIME   ;
  int featureAmount   = AVG_SERVICE_GIVE;
  
  Type workerTypes[] = NO_WALKERS;
  int maxWorkers   = 1;
  int maxResidents = 0;
  int maxVisitors  = AVG_MAX_VISITORS;
  int maxRecruits  = AVG_ARMY_SIZE;
  
  
  void setUpgradeTiers(Type... tiers) {
    this.upgradeTiers = tiers;
  }
  
  
  void setUpgradeNeeds(Object... args) {
    Object split[][] = Visit.splitByModulus(args, 2);
    upgradeNeeds = (Type   []) castArray(split[0], Type   .class);
    upgradeUsage = (Integer[]) castArray(split[1], Integer.class);
  }
  
  
  void setHomeUsage(Object... args) {
    Object split[][] = Visit.splitByModulus(args, 2);
    homeUseGoods = (Good   []) castArray(split[0], Good   .class);
    homeUsage    = (Integer[]) castArray(split[1], Integer.class);
  }
  
  
  void setWorkerTypes(Type... types) {
    this.workerTypes = types;
  }
  
  
  void setFeatures(Good... features) {
    this.features = features;
  }
  
  
  boolean hasFeature(Good feature) {
    return Visit.arrayIncludes(features, feature);
  }
  
  
  boolean isNatural() {
    return
      category == IS_FIXTURE ||
      category == IS_TERRAIN ||
      category == IS_ANIMAL_ACT
    ;
  }
  
  
  boolean isTerrain() {
    return category == IS_TERRAIN;
  }
  
  
  boolean isFlora() {
    return category == IS_FIXTURE && growRate > 0;
  }
  
  
  boolean isFauna() {
    return category == IS_ANIMAL_ACT;
  }
  
  
  boolean isFixture() {
    return category == IS_FIXTURE || category == IS_STRUCTURAL;
  }
  
  
  boolean isBuilding() {
    return category >= IS_BUILDING && category < IS_ACTOR;
  }
  
  
  boolean isActor() {
    return category >= IS_ACTOR;
  }
  
  
  boolean isPerson() {
    return category == IS_PERSON_ACT;
  }
  
  
  boolean isAnimal() {
    return category == IS_ANIMAL_ACT;
  }
  
  
  boolean isHomeBuilding() {
    return category == IS_HOME_BLD;
  }
  
  
  boolean isTradeBuilding() {
    return category == IS_TRADE_BLD;
  }
  
  
  boolean isArmyOrWallsBuilding() {
    return category == IS_ARMY_BLD || category == IS_WALLS_BLD;
  }
  
  
  
  /**  Walker-specific stats and setup methods-
    */
  int  socialClass  = CLASS_COMMON;
  int  genderRole   = SEX_EITHER;
  Type patronGods[] = null;
  
  int maxHealth   = AVG_MAX_HEALTH;
  int meleeDamage = AVG_MELEE;
  int rangeDamage = AVG_MISSILE;
  int rangeDist   = AVG_RANGE;
  int armourClass = AVG_DEFEND;
  int sightRange  = AVG_SIGHT;
  
  Trait   initTraits [] = {};
  Integer traitLevels[] = {};
  
  Terrain habitats[] = NO_HABITAT;
  boolean predator   = false;
  int     lifespan   = LIFESPAN_LENGTH;
  
  
  void setInitTraits(Object... args) {
    Object split[][] = Visit.splitByModulus(args, 2);
    initTraits  = (Trait  []) castArray(split[0], Trait  .class);
    traitLevels = (Integer[]) castArray(split[1], Integer.class);
  }
  
  
  float initTraitLevel(Trait t) {
    int index = Visit.indexOf(t, initTraits);
    return index == -1 ? null : traitLevels[index];
  }
  
  
  void initAsMigrant(ActorAsPerson a) {
    
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
  
  
  void initAsAnimal(ActorAsAnimal a) {
    float age = Rand.num() * a.type.lifespan;
    a.ageSeconds = (int) age;
    a.hunger     = Rand.num() - 0.5f;
    return;
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    return name;
  }
}



