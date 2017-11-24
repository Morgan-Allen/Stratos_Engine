

package game;
import util.*;
import static game.GameConstants.*;
import java.lang.reflect.Array;




public class Type extends Index.Entry implements Session.Saveable {
  
  
  /**  Indexing, categorisation, spawning and save/load methods-
    */
  final static int
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
    IS_FAITH_BLD   = 15,
    IS_ACTOR       = 16,
    IS_PERSON_ACT  = 17,
    IS_ANIMAL_ACT  = 18
  ;
  
  final static Index <Type> INDEX = new Index();
  
  
  Type(String ID, int category) {
    super(INDEX, ID);
    this.category = category;
  }
  
  
  public static Type loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  Object generate() {
    switch (category) {
      case(IS_FIXTURE    ): return new Element(this);
      case(IS_STRUCTURAL ): return new Element(this);
      case(IS_BUILDING   ): return new Building          (this);
      case(IS_CRAFTS_BLD ): return new BuildingForCrafts (this);
      case(IS_GATHER_BLD ): return new BuildingForGather (this);
      case(IS_WATER_BLD  ): return new BuildingForWater  (this);
      case(IS_TRADE_BLD  ): return new BuildingForTrade  (this);
      case(IS_HOME_BLD   ): return new BuildingForHome   (this);
      case(IS_AMENITY_BLD): return new BuildingForAmenity(this);
      case(IS_COLLECT_BLD): return new BuildingForCollect(this);
      case(IS_HUNTS_BLD  ): return new BuildingForHunt   (this);
      case(IS_ARMY_BLD   ): return new BuildingForArmy   (this);
      case(IS_FAITH_BLD  ): return new BuildingForFaith  (this);
      case(IS_ACTOR      ): return new Actor        (this);
      case(IS_PERSON_ACT ): return new ActorAsPerson(this);
      case(IS_ANIMAL_ACT ): return new ActorAsAnimal(this);
    }
    return null;
  }
  
  
  Object[] castArray(Object arr[], Class c) {
    Object n[] = (Object[]) Array.newInstance(c, arr.length);
    for (int i = arr.length; i-- > 0;) n[i] = arr[i];
    return n;
  }
  
  
  
  /**  Common data fields and setup functions-
    */
  String name;
  int tint = BLACK_COLOR;
  
  int category;
  Type flagKey = null;
  int wide = 1, high = 1, deep = 1;
  
  Good    yields      = null;
  float   yieldAmount = 0;
  Good    builtFrom  [] = EMPTY_MATERIAL;
  Integer builtAmount[] = { 1 };
  
  boolean blocks   = true ;
  boolean paved    = false;
  boolean mobile   = false;
  float   growRate = 0;
  int     ambience = 0;
  boolean isCrop   = false;
  boolean aqueduct = false;
  
  
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
  int numRanks     = AVG_RANKS;
  int numFile      = AVG_FILE ;
  
  
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
  
  
  boolean isHomeBuilding() {
    return category == IS_HOME_BLD;
  }
  
  
  boolean isTradeBuilding() {
    return category == IS_TRADE_BLD;
  }
  
  
  
  /**  Walker-specific stats and setup methods-
    */
  int  socialClass  = CLASS_COMMON;
  Type patronGods[] = null;
  String names[] = {};
  
  int maxHealth   = AVG_MAX_HEALTH;
  int attackScore = AVG_ATTACK;
  int defendScore = AVG_DEFEND;
  int sightRange  = AVG_SIGHT;
  int attackRange = AVG_RANGE;
  
  Terrain habitats[] = NO_HABITAT;
  boolean predator   = false;
  int     lifespan   = LIFESPAN_LENGTH;
  
  
  void initAsMigrant(Actor w) {
    float age = Rand.range(AVG_MARRIED, AVG_MENOPAUSE) + Rand.num() - 0.5f;
    int   sex = Rand.yes() ? Actor.SEX_FEMALE : Actor.SEX_MALE;
    w.ageSeconds = (int) (age * YEAR_LENGTH);
    w.sexData    = sex;
    w.hunger     = Rand.num() - 0.5f;
  }
  
  
  void initAsAnimal(Actor w) {
    float age = Rand.num() * w.type.lifespan;
    w.ageSeconds = (int) age;
    w.hunger     = Rand.num() - 0.5f;
    return;
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    return name;
  }
}



