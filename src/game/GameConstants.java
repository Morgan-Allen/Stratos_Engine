  

package game;
import util.*;
import static game.Type.*;
import static game.CityMap.*;
import java.awt.Color;




//  Trooper.  Runner.  Enforcer.
//  


public class GameConstants {
  
  
  /**  Colour prototyping-
    */
  static int colour(int r, int g, int b) {
    return new Color(r / 10f, g / 10f, b / 10f).getRGB();
  }
  
  static int colour(int r, int g, int b, int a) {
    return new Color(r / 10f, g / 10f, b / 10f, a / 10f).getRGB();
  }
  
  final static int
    
    //  Some default colours for diagnostic-display purposes:
    BLANK_COLOR  = colour(5, 5, 5),
    PAVE_COLOR   = colour(8, 8, 8),
    WALKER_COLOR = colour(9, 9, 0),
    CITY_COLOR   = colour(7, 7, 1),
    NO_BLD_COLOR = colour(9, 0, 0),
    MISSED_COLOR = colour(9, 0, 9),
    
    WHITE_COLOR  = colour(10, 10, 10),
    BLACK_COLOR  = colour(0 , 0 , 0 ),
    CLEAR_COLOR  = colour(0 , 0 , 0 , 0),
    
    //  Industrial in brown.
    TINT_LITE_INDUSTRIAL  = colour(6, 3, 3),
    TINT_INDUSTRIAL       = colour(5, 2, 2),
    //  Aquatic in cyan.
    TINT_LITE_AQUATIC     = colour(7, 8, 9),
    TINT_AQUATIC          = colour(0, 5, 7),
    //  Military in red.
    TINT_LITE_MILITARY    = colour(8, 1, 1),
    TINT_MILITARY         = colour(7, 0, 0),
    //  Economic in blue.
    TINT_LITE_COMMERCIAL  = colour(3, 1, 8),
    TINT_COMMERCIAL       = colour(2, 0, 7),
    //  Residential in yellow.
    TINT_LITE_RESIDENTIAL = colour(8, 8, 1),
    TINT_RESIDENTIAL      = colour(7, 7, 0),
    //  Entertainment in purple.
    TINT_LITE_AMENITY     = colour(8, 1, 8),
    TINT_AMENITY          = colour(7, 0, 7),
    //  Health/education in white.
    TINT_LITE_HEALTH_ED   = colour(8, 8, 8),
    TINT_HEALTH_ED        = colour(7, 7, 7),
    //  Religious in orange.
    TINT_LITE_RELIGIOUS   = colour(8, 4, 1),
    TINT_RELIGIOUS        = colour(7, 3, 0),
    
    //  Crops in shades of green:
    TINT_CROPS[] = {
      colour(1, 8, 3),
      colour(0, 7, 2),
      colour(2, 8, 0),
    }
  ;
  
  
  
  /**  Various numeric gameplay constants-
    */
  final public static int
    //
    //  Time and distance-
    DAY_LENGTH       = 6   ,
    DAYS_PER_MONTH   = 20  ,
    MONTHS_PER_YEAR  = 18  ,
    DAYS_PER_YEAR    = 365 ,
    MONTH_LENGTH     = DAY_LENGTH * DAYS_PER_MONTH,
    YEAR_LENGTH      = DAY_LENGTH * DAYS_PER_YEAR ,
    //
    //  Growth and crops-
    SCAN_PERIOD      = MONTH_LENGTH * 1,
    RIPEN_PERIOD     = MONTH_LENGTH * 6,
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
    ANIMAL_MATURES   = MONTH_LENGTH * 4,
    GRAZER_LIFESPAN  = YEAR_LENGTH  * 2,
    HUNTER_LIFESPAN  = YEAR_LENGTH  * 8,
    ANIMAL_PREG_TIME = ANIMAL_MATURES / 2,
    AVG_BUTCHER_TIME = MONTH_LENGTH / (AVG_ANIMAL_YIELD * 2),
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
    PREGNANCY_LENGTH = 9 * MONTH_LENGTH,
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
    //  Health and survival-
    STARVE_INTERVAL  = MONTH_LENGTH * 2,
    FATIGUE_INTERVAL = MONTH_LENGTH * 2,
    HUNGER_REGEN     = 5   ,
    FOOD_UNIT_PER_HP = 2   ,
    FECES_UNIT_TIME  = MONTH_LENGTH * 3,
    FATIGUE_REGEN    = MONTH_LENGTH / 4,
    HEALTH_REGEN     = MONTH_LENGTH / 2,
    AVG_MAX_HEALTH   = 5   ,
    //
    //  Building-update, commerce and manufacture-
    AVG_UPDATE_GAP   = 60  ,  //  seconds between updates
    AVG_CRAFT_TIME   = YEAR_LENGTH / 6,
    AVG_MAX_STOCK    = 10  ,
    MAX_TRADER_RANGE = 100 ,
    MAX_WANDER_RANGE = 20  ,
    AVG_VISIT_TIME   = 20  ,
    MAX_SHOP_RANGE   = 50  ,
    HOME_USE_TIME    = YEAR_LENGTH * 2,
    AVG_SERVICE_GIVE = 10  ,  //  value of education, diversion, etc.
    AVG_MAX_VISITORS = 4   ,
    //
    //  Military, recon and combat-
    AVG_ARMY_SIZE    = 16  ,
    AVG_RANKS        = 4   ,
    AVG_FILE         = 4   ,
    AVG_MELEE        = 2   ,
    AVG_MISSILE      = 1   ,
    AVG_DEFEND       = 2   ,
    AVG_SIGHT        = 6   ,
    AVG_RANGE        = 3   ,
    MAX_RANGE        = 6   ,
    MAX_CASUALTIES   = 50  ,  //  percent of total force before retreat
    MAX_EXPLORE_DIST = 200 ,
    WALL_HIT_BONUS   = 40  ,  //  percent chance
    WALL_DEF_BONUS   = 25  ,  //  percent chance
    WALL_ARM_BONUS   = 2   ,
    WALL_DMG_BONUS   = 1   ,
    //
    //  Trade and migration-
    TRADE_DIST_TIME  = 50  ,
    MIGRANTS_PER_1KM = 10     //  per month per 1000 foreign citizens
  ;
  final static int
    CLASS_SLAVE    = 0,
    CLASS_COMMON   = 1,
    CLASS_TRADER   = 2,
    CLASS_NOBLE    = 3,
    ALL_CLASSES[]  = { 0, 1, 2, 3 },
    TAX_VALUES []  = { 0, 25, 75, 250 },
    AVG_TAX_VALUE  = 25,
    AVG_GOOD_VALUE = 25,
    TAX_INTERVAL   = YEAR_LENGTH,
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
  
  
  
  //private static List <Good> GOODS_LIST = new List();
  //private static List <Terrain> TERRAINS_LIST = new List();
  
  /**  Specialise sub-type classes:
    */
  static class Good extends Type {
    
    int price;
    
    Good(String name, int price) {
      super(null, "good_"+name.toLowerCase().replace(' ', '_'), IS_GOOD);
      //if (price != -1) GOODS_LIST.add(this);
      this.name    = name ;
      this.price   = price;
      this.pathing = PATH_HINDER;
    }
  }
  
  static class Terrain extends Type {
    
    int terrainID = 0;
    Type  fixtures[] = new Type [0];
    Float weights [] = new Float[0];
    
    Terrain(String name, int index) {
      super(null, "terrain_"+index, IS_TERRAIN);
      this.name      = name ;
      this.terrainID = index;
      this.pathing   = PATH_FREE;
      //TERRAINS_LIST.add(this);
    }
    
    void attachFixtures(Object... args) {
      Object split[][] = Visit.splitByModulus(args, 2);
      fixtures = (Type []) castArray(split[0], Type .class);
      weights  = (Float[]) castArray(split[1], Float.class);
    }
  }
  
  static class Trait extends Type {
    
    Trait(String ID, String... names) {
      super(null, ID, IS_TRAIT);
      this.name = names[0];
      this.namesRange = names;
    }
  }
  
  static class WalkerType extends Type {
    
    WalkerType(Class baseClass, String ID, int category, int socialClass) {
      super(baseClass, ID, category);
      this.socialClass = socialClass;
      this.mobile      = true;
    }
    
    WalkerType(Class baseClass, String ID, int category) {
      this(baseClass, ID, category, CLASS_COMMON);
    }
  }
  
  static class BuildType extends Type {
    
    BuildType(Class baseClass, String ID, int category) {
      super(baseClass, ID, category);
    }
  }

  
  
  /**  Economic constants-
    */
  final public static Good
    
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
    IS_VENDOR  = new Good("Is Market"   , -1 ),
    IS_TRADER  = new Good("Is Trader"   , -1 ),
    IS_HOUSING = new Good("Is Housing"  , -1 ),
    IS_TOWER   = new Good("Is Tower"    , -1 ),
    IS_GATE    = new Good("Is Gate"     , -1 ),
    IS_CISTERN = new Good("Is Cistern"  , -1 ),
    
    DIVERSION  = new Good("Diversion"   , -1 ),
    EDUCATION  = new Good("Education"   , -1 ),
    HEALTHCARE = new Good("Healthcare"  , -1 ),
    RELIGION   = new Good("Religion"    , -1 ),
    
    CASH       = new Good("Cash"        , 1  ),
    
    EMPTY_MATERIAL[] = { VOID },
    NO_GOODS      [] = new Good[0],
    COMMERCE_TYPES[] = { IS_ADMIN, IS_TRADER, IS_VENDOR, IS_HOUSING },
    SERVICE_TYPES [] = { DIVERSION, EDUCATION, HEALTHCARE, RELIGION };
  
  final static Terrain
    NO_HABITAT[] = {},
    EMPTY = new Terrain("Empty", 0);
  
  final static WalkerType
    NO_WALKERS[] = new WalkerType[0],
    CHILD = new WalkerType(ActorAsPerson.class, "type_child", IS_PERSON_ACT, CLASS_COMMON);
  
  static {
    CHILD.name = "Child";
    CHILD.setInitTraits();
  }
  
  
  
  /**  Walker types-
    */
  final static Trait
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
    
    TRAIT_COMPASSION = new Trait("trait_compassion",
      "Compassionate", null, "Cruel"
    ),
    TRAIT_DILIGENCE  = new Trait("trait_diligence",
      "Diligent", null, "Fickle"
    ),
    TRAIT_BRAVERY    = new Trait("trait_bravery",
      "Brave", null, "Nervous"
    ),
    ALL_PERSONALITY[] = { TRAIT_COMPASSION, TRAIT_DILIGENCE, TRAIT_BRAVERY }
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
    NO_TIERS[] = new BuildType[0],
    NO_NEEDS[] = new BuildType[0]
  ;
  
  
  
  /**  Commonly used interfaces-
    */
  final static Series <Actor> NO_ACTORS = new Batch();
  
  static interface Target extends Flood.Fill {
    
    Type type();
    Tile at();
    boolean isTile();
    boolean onMap();
    
    void targetedBy(Actor a);
    void setFocused(Actor a, boolean is);
    Series <Actor> focused();
    boolean hasFocus();
  }
  
  
  static interface Pathing extends Target {
    
    int pathType();
    Pathing[] adjacent(Pathing temp[], CityMap map);
    boolean allowsEntryFrom(Pathing p);

    boolean allowsEntry(Actor a);
    void setInside(Actor a, boolean is);
    Series <Actor> inside();
  }
  
  
  static interface Trader {
    Tally <Good> tradeLevel();
    Tally <Good> inventory ();
    City homeCity();
  }
  
  
  static interface Journeys {
    void onArrival(City goes, World.Journey journey);
    City homeCity();
  }
  
  
  static interface Employer {
    void selectActorBehaviour(Actor actor);
    void actorUpdates(Actor actor);
    void actorPasses (Actor actor, Building other );
    void actorTargets(Actor actor, Target   other );
    void actorVisits (Actor actor, Building visits);
  }
  
}









