  

package game;
import util.*;
import graphics.common.*;
import graphics.sfx.*;
import java.awt.Color;



public class GameConstants {
  
  
  /**  Colour prototyping-
    */
  public static int colour(int r, int g, int b) {
    return new Color(r / 10f, g / 10f, b / 10f).getRGB();
  }
  
  public static int colour(int r, int g, int b, int a) {
    return new Color(r / 10f, g / 10f, b / 10f, a / 10f).getRGB();
  }
  
  final public static int
    
    //  Some default colours for diagnostic-display purposes:
    BLANK_COLOR  = colour(5, 5, 5),
    PAVE_COLOR   = colour(8, 8, 8),
    WALKER_COLOR = colour(9, 9, 0),
    CITY_COLOR   = colour(7, 7, 1),
    NO_BLD_COLOR = colour(9, 0, 0),
    MISSED_COLOR = colour(9, 0, 9),
    
    WHITE_COLOR  = colour(10, 10, 10),
    BLACK_COLOR  = colour(0 , 0 , 0 ),
    CLEAR_COLOR  = colour(0 , 0 , 0 , 0)
  ;
  
  
  
  /**  Various numeric gameplay constants-
    */
  final public static int
    //
    //  Time constants-
    DAY_LENGTH       = 120,
    DAYS_PER_YEAR    = 18,
    YEAR_LENGTH      = DAY_LENGTH * DAYS_PER_YEAR,
    //
    //  Health and survival-
    STARVE_INTERVAL  = DAY_LENGTH * 2,
    FATIGUE_INTERVAL = DAY_LENGTH * 2,
    HUNGER_REGEN     = 5   ,
    FOOD_UNIT_PER_HP = 2   ,
    FECES_UNIT_TIME  = DAY_LENGTH * 3,
    FATIGUE_REGEN    = DAY_LENGTH / 4,
    HEALTH_REGEN     = DAY_LENGTH / 2,
    AVG_MAX_HEALTH   = 10  ,
    AVG_MOVE_SPEED   = 100 ,  //  percent of 1 tile/second
    //
    //  Growth and crops-
    SCAN_PERIOD      = DAY_LENGTH * 1,
    RIPEN_PERIOD     = DAY_LENGTH * 6,
    CROP_YIELD       = 50  ,  //  percent of 1 full item
    AVG_GATHER_RANGE = 4   ,
    //
    //  Okay.  this means that an 10x10 area of crops will produce:
    //    100 x 0.5 = 50 units of food every 6 months (120 days.)
    //    That gives you ~8 units of food per month.
    //    Every citizen consumes 2 units of food per 2 months.  So that's just
    //    enough for 8 citizens.
    //
    TILES_PER_GRAZER = 100 ,
    TILES_PER_HUNTER = 400 ,
    AVG_ANIMAL_YIELD = 8   ,
    ANIMAL_MATURES   = DAY_LENGTH * 4,
    GRAZER_LIFESPAN  = YEAR_LENGTH  * 2,
    HUNTER_LIFESPAN  = YEAR_LENGTH  * 8,
    ANIMAL_PREG_TIME = ANIMAL_MATURES / 2,
    AVG_BUTCHER_TIME = DAY_LENGTH / (AVG_ANIMAL_YIELD * 2),
    //
    //  If 1 animal is worth 8 food, and 1 of them fit within 10x10 tiles, then
    //  if they 'ripen' within 1 month, that would be 8 units of food.
    //
    //  Slash that by a factor of 4, so it's 1/4 as land-efficient as farming.
    //  That gives a maturation period of 4 months (double that for lifespan),
    //  yielding 2 food per month.
    //
    //  That's enough to support 1 predator eating half of available prey (1
    //  food/month, just like humans.)
    //
    //  Life cycle constants-
    SEX_EITHER       = -1,
    SEX_MALE         =  1 << 0,
    SEX_FEMALE       =  1 << 1,
    AVG_INFANCY      = 4   ,
    AVG_PUBERTY      = 12  ,
    AVG_MARRIED      = 24  ,
    AVG_MENOPAUSE    = 48  ,
    AVG_RETIREMENT   = 60  ,
    MIN_PREG_CHANCE  = 35  ,
    MAX_PREG_CHANCE  = 65  ,
    PREGNANCY_LENGTH = 9 * DAY_LENGTH,
    AVG_CHILD_MORT   = 75  ,  //  child mortality percent
    AVG_SENIOR_MORT  = 10  ,  //  senior mortality percent
    LIFESPAN_LENGTH  = AVG_RETIREMENT * YEAR_LENGTH,
    //
    //  Skills and XP-
    SKILL_XP_MULTS[] = { 1 , 2 , 3 , 4 , 5 , 6 , 7 , 8 , 9 , 10, -1 },
    SKILL_XP_TOTAL[] = { 0 , 1 , 3 , 6 , 10, 15, 21, 28, 36, 45, 55 },
    MAX_SKILL_LEVEL  = 10,
    ALL_LEVELS_SUM   = SKILL_XP_TOTAL[10],
    MAX_TRAIN_TIME   = (YEAR_LENGTH * 10) / 3,
    BASE_LEVEL_XP    = MAX_TRAIN_TIME / ALL_LEVELS_SUM,
    MAX_SKILL_XP     = BASE_LEVEL_XP * ALL_LEVELS_SUM,
    CRAFT_XP_PERCENT = 100 ,
    FARM_XP_PERCENT  = 400 ,
    GATHR_XP_PERCENT = 200 ,
    BUILD_XP_PERCENT = 800 ,
    FIGHT_XP_PERCENT = 3200,
    TRAIN_XP_PERCENT = 50  ,
    //
    //  Basically, if you gain 1 XP per second while practicing 1/3rd of your
    //  free time, you can expect to master a skill within 10 years.  For some
    //  activities, this gets boosted/reduced based on the frequency of the
    //  behaviour.  (NOTE- this will need fine-tuning later...)
    //
    //  Building-update, commerce and manufacture-
    AVG_UPDATE_GAP   = 60  ,  //  seconds between updates
    AVG_CRAFT_TIME   = YEAR_LENGTH / 6,
    AVG_MAX_STOCK    = 10  ,
    MAX_TRADER_RANGE = 100 ,
    MAX_SHOP_RANGE   = 50  ,
    AVG_HOME_COMFORT = 10  ,
    HOME_USE_TIME    = YEAR_LENGTH * 2,
    AVG_SERVICE_GIVE = 10  ,  //  value of education, diversion, etc.
    AVG_MAX_VISITORS = 4   ,
    AVG_VISIT_TIME   = 20  ,
    AVG_PAVE_MARGIN  = 1   ,
    //
    //  Exploration and wandering-
    MAX_WANDER_RANGE = 20  ,
    AVG_EXPLORE_DIST = 8   ,
    MAX_EXPLORE_DIST = 200 ,
    //
    //  Military, and combat-
    AVG_ARMY_SIZE    = 9   ,
    AVG_RANKS        = 3   ,
    AVG_FILE         = 3   ,
    TOP_DAMAGE       = 10  ,
    TOP_ARMOUR       = 10  ,
    AVG_SIGHT        = 6   ,
    AVG_RANGE        = 3   ,
    MAX_RANGE        = 6   ,
    MAX_CASUALTIES   = 50  ,  //  percent of total force before retreat
    WALL_HIT_BONUS   = 40  ,  //  percent chance
    WALL_DEF_BONUS   = 25  ,  //  percent chance
    WALL_ARM_BONUS   = 2   ,
    WALL_DMG_BONUS   = 1   ,
    BUILD_TILE_HP    = 2   ,
    //
    //  Trade and migration-
    TRADE_DIST_TIME  = 50  ,
    MIGRANTS_PER_1KM = 10     //  per month per 1000 foreign citizens
  ;
  final public static int
    //
    //  Social-class constants-
    CLASS_COMMON   = 0,
    CLASS_SOLDIER  = 1,
    CLASS_NOBLE    = 2,
    ALL_CLASSES[]  = { 0, 1, 2 },
    TAX_VALUES []  = { 0, 25, 100 },
    AVG_HIRE_COST  = 400,
    AVG_TAX_VALUE  = 25,
    AVG_GOOD_VALUE = 25,
    TAX_INTERVAL   = YEAR_LENGTH,
    //
    //  Economic constants-
    MARKET_MARGIN   =  100,
    SCARCE_MARGIN   =  50,
    PLENTY_MARGIN   = -35,
    TRAVEL_MARGIN   =  35,
    //
    //  City constants-
    AVG_CITY_DIST   = 5,
    POP_PER_CITIZEN = 25,
    AVG_POPULATION  = 1000,
    AVG_HOUSE_POP   = 4 * POP_PER_CITIZEN,
    AVG_ARMY_POWER  = AVG_ARMY_SIZE * POP_PER_CITIZEN,
    //
    //  Inter-city constants-
    AVG_TRIBUTE_PERCENT = 25,
    AVG_TRIBUTE_YEARS   = 10,
    AVG_ALLIANCE_YEARS  = 15,
    MARRIAGE_VALUE_MULT = 12
  ;
  
  
  
  /**  Specialise sub-type classes:
    */
  public static class Good extends Type {
    
    final public static int
      NONE     = 0,
      //
      //  These are properties of equipped weapons-
      MELEE    = 1 << 0,
      RANGED   = 1 << 1,
      ENERGY   = 1 << 2,
      KINETIC  = 1 << 3,
      STUN     = 1 << 4,
      NO_AMMO  = 1 << 5
    ;
    
    
    final public int price;
    public boolean isWeapon = false;
    public boolean isArmour = false;
    public boolean isUsable = false;
    public boolean isEdible = false;
    public int maxQuality = -1;
    public int maxCarried = -1;
    public int priceLevels[] = {};
    public Technique allows[] = {};
    
    public String groupName, animName;
    public PlaneFX.Model  slashModel ;
    public ShotFX.Model   shotModel  ;
    public PlaneFX.Model  burstModel ;
    public ShieldFX.Model shieldModel;
    
    
    public Good(String name, int price) {
      super(null, "good_"+name.toLowerCase().replace(' ', '_'), IS_GOOD);
      this.name    = name ;
      this.price   = price;
      this.pathing = PATH_HINDER;
    }
    
    public Good setAsWeapon(
      int damage, boolean ranged, int prices[],
      String groupName, String animName,
      ShotFX.Model shotModel, PlaneFX.Model burstModel
    ) {
      this.isWeapon    = true;
      this.meleeDamage = ranged ? 0 : damage;
      this.rangeDamage = ranged ? damage : 0;
      this.priceLevels = prices;
      this.maxQuality  = prices.length;
      
      this.groupName  = groupName ;
      this.animName   = animName  ;
      this.shotModel  = shotModel ;
      this.burstModel = burstModel;
      
      return this;
    }
    
    public Good setAsArmour(int armour, boolean shields, int... prices) {
      this.isArmour    = true;
      this.armourClass = armour;
      this.priceLevels = prices;
      this.maxQuality  = prices.length;
      return this;
    }
    
    public Good setUsable(Technique... allows) {
      this.isUsable = true;
      this.allows = allows;
      return this;
    }
  }
  
  public static class Terrain extends Type {
    
    final int layerID;
    Type  fixtures[] = new Type [0];
    Float weights [] = new Float[0];
    
    ImageAsset animTex[], baseTex;
    
    public Terrain(String name, String ID, int layerID) {
      super(null, "terrain_"+ID, IS_TERRAIN);
      this.layerID = layerID;
      this.name    = name;
      this.pathing = PATH_FREE;
    }
    
    public void attachFixtures(Object... args) {
      Object split[][] = Visit.splitByModulus(args, 2);
      fixtures = (Type []) castArray(split[0], Type .class);
      weights  = (Float[]) castArray(split[1], Float.class);
    }
    
    public void attachGroundTex(
      Class baseClass, String basePath, String... groundTex
    ) {
      //I.say("\nGROUND TEXTURES:");
      this.animTex = new ImageAsset[groundTex.length];
      for (int i = animTex.length; i-- > 0;) {
        final String path = basePath+groundTex[i];
        animTex[i] = ImageAsset.fromImage(baseClass, "habitat_"+path, path);
        //I.say("  "+animTex[i]);
      }
      this.baseTex = animTex[0];
    }
  }
  
  public static class Trait extends Type {
    
    public Trait(String ID, String... names) {
      super(null, ID, IS_TRAIT);
      this.name = names[0];
      this.traitRangeNames = names;
    }
  }
  
  public static class Recipe {
    
    Good inputs[];
    Good made;
    float craftTime;
    Trait craftSkill;
    
    public Recipe(Good made, Trait skill, float time, Good... inputs) {
      this.inputs     = inputs;
      this.made       = made;
      this.craftTime  = time;
      this.craftSkill = skill;
    }
  }
  
  
  
  /**  Economic constants-
    */
  final public static Good
    
    //  TODO:  Use Traits for this instead.
    
    //  Note- 'void' is used to mark foundations for clearing during
    //  construction, and as a default build-material.
    VOID       = new Good("Void"        ,  0 ),
    NEED_BUILD = new Good("Need Build"  , -1 ),
    NEED_PLANT = new Good("Need Plant"  , -1 ),
        
    IS_CROP    = new Good("Is Crop"     , -1 ),
    IS_TREE    = new Good("Is Tree"     , -1 ),
    IS_WATER   = new Good("Is Water"    , -1 ),
    IS_STONE   = new Good("Is Stone"    , -1 ),
    
    IS_ADMIN   = new Good("Is Admin"    , -1 ),
    IS_VENDOR  = new Good("Is Vendor"   , -1 ),
    IS_TRADER  = new Good("Is Trader"   , -1 ),
    IS_HOUSING = new Good("Is Housing"  , -1 ),
    IS_TOWER   = new Good("Is Tower"    , -1 ),
    IS_TURRET  = new Good("Is Turret"   , -1 ),
    IS_GATE    = new Good("Is Gate"     , -1 ),
    IS_REFUGE  = new Good("Is Refuge"   , -1 ),
    
    DIVERSION  = new Good("Diversion"   , -1 ),
    EDUCATION  = new Good("Education"   , -1 ),
    HEALTHCARE = new Good("Healthcare"  , -1 ),
    RELIGION   = new Good("Religion"    , -1 ),
    
    CASH       = new Good("Cash"        ,  1 ),
    
    EMPTY_MATERIAL[] = { VOID },
    NO_GOODS      [] = new Good[0],
    COMMERCE_TYPES[] = { IS_ADMIN, IS_TRADER, IS_VENDOR, IS_HOUSING },
    SERVICE_TYPES [] = { DIVERSION, EDUCATION, HEALTHCARE, RELIGION };
  
  final public static Terrain
    NO_HABITAT[] = {},
    EMPTY = new Terrain("Empty", "terr_empty", -1);
  
  final public static ActorType
    NO_WALKERS[] = new ActorType[0];
  
  
  
  /**  Walker types-
    */
  final public static Trait
    SKILL_MELEE = new Trait("skill_melee", "Melee"),
    SKILL_RANGE = new Trait("skill_range", "Range"),
    SKILL_EVADE = new Trait("skill_evade", "Evade"),
    SKILL_FARM  = new Trait("skill_farm" , "Farm" ),
    SKILL_BUILD = new Trait("skill_build", "Build"),
    SKILL_CRAFT = new Trait("skill_craft", "Craft"),
    SKILL_SPEAK = new Trait("skill_speak", "Speak"),
    SKILL_WRITE = new Trait("skill_write", "Write"),
    SKILL_PRAY  = new Trait("skill_pray" , "Pray" ),
    ALL_SKILLS[] = {
      SKILL_MELEE, SKILL_RANGE, SKILL_EVADE,
      SKILL_FARM , SKILL_BUILD, SKILL_CRAFT,
      SKILL_SPEAK, SKILL_WRITE, SKILL_PRAY ,
    },
    
    TRAIT_EMPATHY = new Trait("trait_empathy",
      "Empathic", null, "Cruel"
    ),
    TRAIT_DILIGENCE = new Trait("trait_diligence",
      "Diligent", null, "Fickle"
    ),
    TRAIT_BRAVERY = new Trait("trait_bravery",
      "Brave", null, "Nervous"
    ),
    TRAIT_CURIOSITY = new Trait("trait_curiosity",
      "Curious", null, "Dull"
    ),
    ALL_PERSONALITY[] =
    {
      TRAIT_EMPATHY, TRAIT_DILIGENCE, TRAIT_BRAVERY, TRAIT_CURIOSITY
    }
  ;
  
  
  /**  Infrastructure types-
    */
  final static int
    AMBIENCE_MIN = -10,
    AMBIENCE_AVG =  5 ,
    AMBIENCE_PAD =  2 ,
    AMBIENCE_MAX =  20
  ;
  final static BuildType
    NO_UPGRADES[] = new BuildType[0],
    NO_TIERS   [] = new BuildType[0],
    NO_NEEDS   [] = new BuildType[0]
  ;
  
  
  
  /**  Commonly used interfaces-
    */
  public static interface Active extends Target {
    
    AreaMap map();
    boolean isActor();
    
    Task.JOB jobType();
    Task task();
    Mission mission();
    
    void assignTask(Task task);
    void performAttack(Element other, boolean melee);
  }
  final static Series <Active> NONE_ACTIVE = new Batch();
  final static Series <Actor > NO_ACTORS   = new Batch();
  
  
  public static interface Target extends Flood.Fill {
    
    Tile at();
    float radius();
    float height();
    boolean onMap();
    boolean indoors();
    
    Type type();
    boolean isTile();
    
    void targetedBy(Active a);
    void setFocused(Active a, boolean is);
    Series <Active> focused();
    boolean hasFocus();
    
    Vec3D renderedPosition(Vec3D store);
  }
  
  
  public static interface Pathing extends Target {
    
    int pathType();
    Pathing[] adjacent(Pathing temp[], AreaMap map);
    boolean allowsEntryFrom(Pathing p);

    boolean allowsEntry(Actor a);
    void setInside(Actor a, boolean is);
    Series <Actor> inside();
  }
  
  
  public static interface Carrier {
    Tally <Good> inventory();
    Base base();
  }
  
  
  public static interface Trader extends Carrier {
    Tally <Good> needLevels();
    Tally <Good> prodLevels();
    float importPrice(Good g, Base sells);
    float exportPrice(Good g, Base buys );
  }
  
  
  public static interface Journeys {
    void onArrival(Base goes, World.Journey journey);
    Base base();
  }
  
  
  public static interface Employer {
    Task selectActorBehaviour(Actor actor);
    void actorUpdates(Actor actor);
    void actorPasses (Actor actor, Building other );
    void actorTargets(Actor actor, Target   other );
    void actorVisits (Actor actor, Building visits);
  }
  
}









