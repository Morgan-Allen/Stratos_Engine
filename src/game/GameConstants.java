  

package game;
import util.*;
import static game.Type.*;
import static game.CityMap.*;
import java.awt.Color;



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
  
  
  /**  Specialise sub-type classes:
    */
  static class Good extends Type {
    
    int price;
    
    Good(String name, int price) {
      super("good_"+name.toLowerCase().replace(' ', '_'), IS_GOOD);
      if (price != -1) GOODS_LIST.add(this);
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
      super("terrain_"+index, IS_TERRAIN);
      this.name      = name ;
      this.terrainID = index;
      this.pathing   = PATH_FREE;
      TERRAINS_LIST.add(this);
    }
    
    void attachFixtures(Object... args) {
      Object split[][] = Visit.splitByModulus(args, 2);
      fixtures = (Type []) castArray(split[0], Type .class);
      weights  = (Float[]) castArray(split[1], Float.class);
    }
  }
  
  static class Trait extends Type {
    
    Trait(String ID, String... names) {
      super(ID, IS_TRAIT);
      this.name = names[0];
      this.namesRange = names;
    }
  }
  
  static class WalkerType extends Type {
    
    WalkerType(String ID, int category, int socialClass) {
      super(ID, category);
      this.socialClass = socialClass;
      this.mobile      = true;
    }
    
    WalkerType(String ID, int category) {
      this(ID, category, CLASS_COMMON);
    }
  }

  static class BuildType extends Type {
    
    BuildType(String ID, int category) {
      super(ID, category);
    }
  }
  
  
  
  /**  Economic constants-
    */
  private static List <Good> GOODS_LIST = new List();
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
    
    EMPTY_MATERIAL[] = { VOID },
    NO_GOODS      [] = new Good[0],
    COMMERCE_TYPES[] = { IS_ADMIN, IS_TRADER, IS_VENDOR, IS_HOUSING },
    SERVICE_TYPES [] = { DIVERSION, EDUCATION, HEALTHCARE, RELIGION },
    
    WATER      = new Good("Water"       , 0  ),
    MAIZE      = new Good("Maize"       , 10 ),
    FRUIT      = new Good("Fruit"       , 12 ),
    MEAT       = new Good("Meat"        , 35 ),
    RAW_COTTON = new Good("Raw Cotton"  , 15 ),
    WOOD       = new Good("Wood"        , 10 ),
    CLAY       = new Good("Clay"        , 10 ),
    STONE      = new Good("Stone"       , 20 ),
    
    POTTERY    = new Good("Pottery"     , 50 ),
    COTTON     = new Good("Cotton"      , 75 ),
    
    CASH       = new Good("Cash"        , 1  ),
    SOIL       = new Good("Soil"        , 5  ),
    
    CROP_TYPES  [] = { MAIZE, FRUIT, RAW_COTTON },
    FOOD_TYPES  [] = { MAIZE, FRUIT, MEAT },
    STONE_TYPES [] = { CLAY, STONE  },
    BUILD_GOODS [] = { CLAY, WOOD, STONE },
    HOME_GOODS  [] = { POTTERY, COTTON },
    MARKET_GOODS[] = (Good[]) Visit.compose(Good.class, FOOD_TYPES, HOME_GOODS),
    ALL_GOODS   [] = (Good[]) GOODS_LIST.toArray(Good.class);
  
  

  private static List <Terrain> TERRAINS_LIST = new List();
  final static Terrain
    EMPTY  = new Terrain("Empty" , 0),
    MEADOW = new Terrain("Meadow", 1),
    JUNGLE = new Terrain("Jungle", 2),
    DESERT = new Terrain("Desert", 3),
    LAKE   = new Terrain("Lake"  , 4),
    OCEAN  = new Terrain("Ocean" , 5),
    ALL_TERRAINS[] = TERRAINS_LIST.toArray(Terrain.class),
    NO_HABITAT  [] = {}
  ;
  final static Type
    JUNGLE_TREE1 = new Type("fixture_j_tree1", IS_FIXTURE),
    DESERT_ROCK1 = new Type("fixture_d_rock1", IS_FIXTURE),
    DESERT_ROCK2 = new Type("fixture_d_rock2", IS_FIXTURE),
    CLAY_BANK1   = new Type("fixture_b_clay1", IS_FIXTURE),
    
    ALL_TREES[] = { JUNGLE_TREE1 },
    ALL_ROCKS[] = { DESERT_ROCK1, DESERT_ROCK2 },
    ALL_CLAYS[] = { CLAY_BANK1 }
  ;
  final static WalkerType
    TAPIR   = new WalkerType("animal_tapir" , IS_ANIMAL_ACT),
    QUAIL   = new WalkerType("animal_quail" , IS_ANIMAL_ACT),
    JAGUAR  = new WalkerType("animal_jaguar", IS_ANIMAL_ACT),
    ALL_ANIMALS[] = { TAPIR, QUAIL, JAGUAR }
  ;
  static {
    JUNGLE.attachFixtures(JUNGLE_TREE1, 0.50f);
    MEADOW.attachFixtures(JUNGLE_TREE1, 0.05f);
    DESERT.attachFixtures(DESERT_ROCK1, 0.15f, DESERT_ROCK2, 0.20f);
    
    JUNGLE_TREE1.tint = colour(0, 3, 0);
    DESERT_ROCK1.tint = colour(6, 4, 4);
    DESERT_ROCK2.tint = colour(6, 4, 4);
    CLAY_BANK1  .tint = colour(5, 3, 3);
    JUNGLE      .tint = colour(1, 4, 1);
    DESERT      .tint = colour(5, 4, 3);
    MEADOW      .tint = colour(2, 5, 2);
    LAKE        .tint = colour(0, 0, 3);
    
    //  TODO:  UNIFY WITH CROPS BELOW!
    JUNGLE_TREE1.growRate = 0.5f;
    DESERT_ROCK1.setDimensions(2, 2, 1);
    CLAY_BANK1  .setDimensions(2, 2, 0);
    LAKE.pathing = PATH_BLOCK;
    LAKE.isWater = true;
    
    //  TODO:  UNIFY WITH WALKER-TYPES BELOW!
    TAPIR .name        = "Tapir";
    TAPIR .habitats    = new Terrain[] { JUNGLE, MEADOW };
    TAPIR .predator    = false;
    TAPIR .meleeDamage = 1;
    TAPIR .armourClass = 3;
    TAPIR .maxHealth   = 9;
    
    QUAIL .name        = "Quail";
    QUAIL .habitats    = new Terrain[] { MEADOW, DESERT };
    QUAIL .predator    = false;
    QUAIL .meleeDamage = 0;
    QUAIL .armourClass = 1;
    QUAIL .maxHealth   = 2;
    
    JAGUAR.name        = "Jaguar";
    JAGUAR.habitats    = new Terrain[] { JUNGLE };
    JAGUAR.predator    = true;
    JAGUAR.meleeDamage = 5;
    JAGUAR.armourClass = 3;
    JAGUAR.maxHealth   = 8;
    
    for (Type s : ALL_ANIMALS) {
      s.rangeDamage = -1;
      s.lifespan = s.predator ? HUNTER_LIFESPAN : GRAZER_LIFESPAN;
    }
    
    int i = 0;
    for (Good c : CROP_TYPES) {
      c.tint = TINT_CROPS[i++ % 3];
      c.growRate    = 1f;
      c.isCrop      = true;
      c.flagKey     = IS_CROP;
      c.yields      = c;
      c.yieldAmount = CROP_YIELD / 100f;
    }
    for (Type t : ALL_TREES) {
      t.name = "Forest";
      t.growRate    = CROP_YIELD * 0.5f / 100f;
      t.flagKey     = IS_TREE;
      t.yields      = WOOD;
      t.yieldAmount = 1f;
    }
    for (Type r : ALL_ROCKS) {
      r.name = "Rocks";
      r.flagKey     = IS_STONE;
      r.yields      = STONE;
      r.yieldAmount = 1f;
    }
    for (Type c : ALL_CLAYS) {
      c.name = "Clay Bank";
      c.flagKey     = IS_STONE;
      c.yields      = CLAY;
      c.yieldAmount = 1f;
    }
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
  final static WalkerType
    NO_WALKERS[] = new WalkerType[0],
    
    VAGRANT  = new WalkerType("type_vagrant" , IS_PERSON_ACT, CLASS_COMMON),
    CHILD    = new WalkerType("type_child"   , IS_PERSON_ACT, CLASS_COMMON),
    CITIZEN  = new WalkerType("type_citizen" , IS_PERSON_ACT, CLASS_COMMON),
    SERVANT  = new WalkerType("type_servant" , IS_PERSON_ACT, CLASS_SLAVE ),
    NOBLE    = new WalkerType("type_noble"   , IS_PERSON_ACT, CLASS_NOBLE ),
    CONSORT  = new WalkerType("type_consort" , IS_PERSON_ACT, CLASS_NOBLE ),
    WORKER   = new WalkerType("type_worker"  , IS_PERSON_ACT, CLASS_COMMON),
    MERCHANT = new WalkerType("type_merchant", IS_PERSON_ACT, CLASS_TRADER),
    PORTER   = new WalkerType("type_porter"  , IS_PERSON_ACT, CLASS_SLAVE ),
    HUNTER   = new WalkerType("type_hunter"  , IS_PERSON_ACT, CLASS_NOBLE ),
    SOLDIER  = new WalkerType("type_soldier" , IS_PERSON_ACT, CLASS_NOBLE ),
    PRIEST   = new WalkerType("type_priest"  , IS_PERSON_ACT, CLASS_NOBLE )
  ;
  static {
    //  TODO:  Get rid of this background as a starting option.
    VAGRANT .name = "Vagrant";
    
    CHILD   .name = "Child";
    CHILD.setInitTraits();
    
    CITIZEN .name = "Citizen";
    CITIZEN.setInitTraits(SKILL_FARM, 1, SKILL_BUILD, 1, SKILL_CRAFT, 1);
    
    SERVANT .name = "Servant";
    SERVANT.setInitTraits(SKILL_SPEAK, 1, SKILL_WRITE, 1);
    
    NOBLE   .name = "Noble";
    NOBLE.setInitTraits(SKILL_MELEE, 1, SKILL_SPEAK, 2, SKILL_WRITE, 2);
    
    CONSORT .name = "Consort";
    CONSORT.setInitTraits(SKILL_SPEAK, 2, SKILL_WRITE, 1, SKILL_EVADE, 2);
    
    WORKER  .name = "Worker";
    WORKER.setInitTraits(SKILL_FARM, 1, SKILL_BUILD, 1, SKILL_CRAFT, 1);
    
    MERCHANT.name = "Merchant";
    MERCHANT.setInitTraits(SKILL_SPEAK, 2, SKILL_WRITE, 2);
    
    PORTER  .name = "Porter";
    PORTER.setInitTraits();
    
    HUNTER  .name = "Hunter";
    HUNTER .rangeDamage = 4;
    HUNTER .armourClass = 3;
    HUNTER .rangeDist   = 6;
    HUNTER .genderRole  = SEX_MALE;
    HUNTER.setInitTraits(SKILL_RANGE, 2, SKILL_EVADE, 2);

    SOLDIER .name = "Soldier";
    SOLDIER.meleeDamage = 5;
    SOLDIER.rangeDamage = 2;
    SOLDIER.rangeDist   = 4;
    SOLDIER.armourClass = 4;
    SOLDIER.maxHealth   = 6;
    SOLDIER.genderRole  = SEX_MALE;
    SOLDIER.setInitTraits(SKILL_MELEE, 2, SKILL_RANGE, 1, SKILL_EVADE, 1);
    
    PRIEST  .name = "Priest";
    PRIEST.setInitTraits(SKILL_PRAY, 2, SKILL_WRITE, 2, SKILL_SPEAK, 2);
  }
  
  
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
    NO_NEEDS[] = new BuildType[0],
    
    ROAD          = new BuildType("type_road"    , IS_STRUCTURAL),
    WALL          = new BuildType("type_wall"    , IS_STRUCTURAL),
    GATE          = new BuildType("type_gate"    , IS_WALLS_BLD ),
    TOWER         = new BuildType("type_tower"   , IS_WALLS_BLD ),
    AQUEDUCT      = new BuildType("type_aqueduct", IS_STRUCTURAL),
    CISTERN       = new BuildType("type_cistern" , IS_WATER_BLD ),
    
    INFRASTRUCTURE_BUILDINGS[] = {
      ROAD, WALL, GATE, TOWER, AQUEDUCT, CISTERN
    },
    
    PALACE        = new BuildType("type_palace"       , IS_HOME_BLD   ),
    PALACE_BUILDINGS[] = { PALACE },
    
    HOUSE         = new BuildType("type_house"        , IS_HOME_BLD   ),
    HOUSE_T1      = new BuildType("type_house_tier1"  , IS_UPGRADE    ),
    HOUSE_T2      = new BuildType("type_house_tier2"  , IS_UPGRADE    ),
    SWEEPER       = new BuildType("type_sweeper"      , IS_COLLECT_BLD),
    BASIN         = new BuildType("type_basin"        , IS_WATER_BLD  ),
    SCHOOL        = new BuildType("type_public_school", IS_AMENITY_BLD),
    BALL_COURT    = new BuildType("type_ball_court"   , IS_AMENITY_BLD),
    RESIDENTIAL_BUILDINGS[] = { HOUSE, SWEEPER, BASIN, SCHOOL, BALL_COURT },
    
    FARM_PLOT     = new BuildType("type_farm_plot"    , IS_GATHER_BLD ),
    SAWYER        = new BuildType("type_sawyer"       , IS_GATHER_BLD ),
    QUARRY_PIT    = new BuildType("type_quarry_pit"   , IS_GATHER_BLD ),
    KILN          = new BuildType("type_kiln"         , IS_CRAFTS_BLD ),
    WEAVER        = new BuildType("type_weaver"       , IS_CRAFTS_BLD ),
    MASON         = new BuildType("type_mason"        , IS_CRAFTS_BLD ),
    INDUSTRIAL_BUILDINGS[] = { FARM_PLOT, QUARRY_PIT, KILN, WEAVER, MASON },
    
    MARKET        = new BuildType("type_market"       , IS_CRAFTS_BLD ),
    PORTER_POST   = new BuildType("type_porter_post"  , IS_TRADE_BLD  ),
    COLLECTOR     = new BuildType("type_collector"    , IS_COLLECT_BLD),
    ECONOMIC_BUILDINGS[] = { MARKET, PORTER_POST, COLLECTOR },
    
    HUNTER_LODGE  = new BuildType("type_hunter_lodge" , IS_HUNTS_BLD  ),
    GARRISON      = new BuildType("type_garrison"     , IS_ARMY_BLD   ),
    MILITARY_BUILDINGS[] = { GARRISON },
    
    TEMPLE_QZ     = new BuildType("type_temple_qz"    , IS_FAITH_BLD  ),
    TEMPLE_TZ     = new BuildType("type_temple_tz"    , IS_FAITH_BLD  ),
    TEMPLE_HU     = new BuildType("type_temple_hu"    , IS_FAITH_BLD  ),
    TEMPLE_TL     = new BuildType("type_temple_tl"    , IS_FAITH_BLD  ),
    TEMPLE_MI     = new BuildType("type_temple_mi"    , IS_FAITH_BLD  ),
    TEMPLE_XT     = new BuildType("type_temple_xt"    , IS_FAITH_BLD  ),
    
    SHRINE_OMC    = new BuildType("type_shrine_omc"   , IS_FAITH_BLD  ),
    SHRINE_OMT    = new BuildType("type_shrine_omt"   , IS_FAITH_BLD  ),
    
    ALL_TEMPLES[] = {
      TEMPLE_QZ, TEMPLE_TZ, TEMPLE_HU, TEMPLE_TL, TEMPLE_MI, TEMPLE_XT
    },
    ALL_SHRINES[] = {
      SHRINE_OMC, SHRINE_OMT
    },
    RELIGIOUS_BUILDINGS[] = (BuildType[]) Visit.compose(
      BuildType.class, ALL_TEMPLES, ALL_SHRINES
    )
  ;
  static {
    
    //
    //  1x1 infrastructure:
    ROAD.name = "Road";
    ROAD.tint = PAVE_COLOR;
    ROAD.pathing = PATH_PAVE;
    ROAD.setDimensions(1, 1, 0);
    ROAD.setBuildMaterials(STONE, 1);
    
    WALL.name = "Wall";
    WALL.tint = TINT_LITE_MILITARY;
    WALL.pathing = PATH_WALLS;
    WALL.isWall  = true;
    WALL.setDimensions(1, 1, 2);
    WALL.setBuildMaterials(STONE, 2);
    
    GATE.name = "Gate";
    GATE.tint = TINT_MILITARY;
    GATE.pathing = PATH_WALLS;
    GATE.isWall  = true;
    GATE.setDimensions(2, 2, 2);
    GATE.setBuildMaterials(STONE, 10);
    GATE.setFeatures(IS_GATE);
    
    TOWER.name = "Tower";
    TOWER.tint = TINT_MILITARY;
    TOWER.pathing = PATH_BLOCK;
    TOWER.isWall  = true;
    TOWER.setDimensions(2, 2, 4);
    TOWER.setBuildMaterials(STONE, 10);
    TOWER.setFeatures(IS_TOWER);
    
    AQUEDUCT.name = "Aqueduct";
    AQUEDUCT.tint = TINT_LITE_AQUATIC;
    AQUEDUCT.isWater = true;
    AQUEDUCT.pathing = PATH_WALLS;
    AQUEDUCT.setDimensions(1, 1, 1);
    AQUEDUCT.setBuildMaterials(CLAY, 1, STONE, 2);
    
    //  TODO:  Fractional material-costs should be possible here!
    
    CISTERN.name = "Cistern";
    CISTERN.tint = TINT_AQUATIC;
    CISTERN.pathing = PATH_WALLS;
    CISTERN.isWater = true;
    CISTERN.setDimensions(3, 3, 1);
    CISTERN.setBuildMaterials(CLAY, 4, STONE, 10);
    
    //
    //  Palace structures:
    PALACE.name = "Palace";
    PALACE.tint = TINT_RESIDENTIAL;
    PALACE.setDimensions(5, 5, 2);
    PALACE.setBuildMaterials(CLAY, 5, WOOD, 15, STONE, 25);
    PALACE.setHomeUsage(POTTERY, 5, COTTON, 10);
    PALACE.setWorkerTypes(NOBLE, SERVANT);
    PALACE.homeSocialClass = CLASS_NOBLE;
    PALACE.maxResidents = 2;
    PALACE.maxWorkers = 2;
    PALACE.maxHealth = 300;
    PALACE.setFeatures(IS_HOUSING);
    
    //
    //  Residential structures:
    HOUSE.name = "House";
    HOUSE.tint = TINT_LITE_RESIDENTIAL;
    HOUSE.setDimensions(2, 2, 1);
    HOUSE.setBuildMaterials(WOOD, 2, CLAY, 1);
    HOUSE.setWorkerTypes(CITIZEN);
    HOUSE.maxResidents = 4;
    HOUSE.maxStock     = 1;
    HOUSE.buildsWith   = new Good[] { WOOD, CLAY };
    HOUSE.features     = new Good[] { IS_HOUSING };
    HOUSE.setUpgradeTiers(HOUSE, HOUSE_T1, HOUSE_T2);
    
    HOUSE_T1.name = "Improved House";
    HOUSE_T1.setBuildMaterials(WOOD, 4, CLAY, 2);
    HOUSE_T1.setHomeUsage(POTTERY, 1);
    HOUSE_T1.maxStock = 2;
    HOUSE_T1.setUpgradeNeeds(DIVERSION, 10);
    
    HOUSE_T2.name = "Fancy House";
    HOUSE_T2.setBuildMaterials(WOOD, 6, CLAY, 3);
    HOUSE_T2.setHomeUsage(POTTERY, 1, COTTON, 1);
    HOUSE_T2.maxStock = 2;
    HOUSE_T2.setUpgradeNeeds(DIVERSION, 15, SCHOOL, 1);
    
    BASIN.name = "Basin";
    BASIN.tint = TINT_AQUATIC;
    BASIN.isWater = true;
    BASIN.pathing = PATH_WALLS;
    BASIN.setDimensions(1, 1, 1);
    BASIN.setBuildMaterials(STONE, 1, CLAY, 1);
    BASIN.setFeatures(IS_WATER, IS_VENDOR);
    
    SWEEPER.name = "Sweeper";
    SWEEPER.tint = TINT_LITE_INDUSTRIAL;
    SWEEPER.setDimensions(1, 1, 1);
    SWEEPER.setBuildMaterials(WOOD, 2, CLAY, 1);
    SWEEPER.setWorkerTypes(WORKER);
    SWEEPER.produced = new Good[] { SOIL };
    
    SCHOOL.name = "Public School";
    SCHOOL.tint = TINT_HEALTH_ED;
    SCHOOL.setDimensions(2, 2, 1);
    SCHOOL.setBuildMaterials(WOOD, 5, CLAY, 2, STONE, 3);
    SCHOOL.setWorkerTypes(CITIZEN);
    SCHOOL.setFeatures(EDUCATION);
    
    BALL_COURT.name = "Ball Court";
    BALL_COURT.tint = TINT_AMENITY;
    BALL_COURT.setDimensions(3, 3, 1);
    BALL_COURT.setBuildMaterials(STONE, 10);
    BALL_COURT.setFeatures(DIVERSION);
    BALL_COURT.featureAmount = 15;

    //
    //  Industrial structures:
    MASON.name = "Mason";
    MASON.tint = TINT_LITE_INDUSTRIAL;
    MASON.setDimensions(2, 2, 1);
    MASON.setBuildMaterials(STONE, 2, WOOD, 2, CLAY, 2);
    MASON.setWorkerTypes(WORKER);
    MASON.worksBeforeBuilt = true;
    MASON.craftTime *= 2;
    MASON.maxWorkers = 2;
    MASON.buildsWith = new Good[] { WOOD, CLAY, STONE };
    MASON.needed     = MASON.buildsWith;
    MASON.maxStock   = 10;
    
    FARM_PLOT.name = "Farm Plot";
    FARM_PLOT.tint = TINT_LITE_INDUSTRIAL;
    FARM_PLOT.setDimensions(2, 2, 1);
    FARM_PLOT.setBuildMaterials(WOOD, 5, CLAY, 2);
    FARM_PLOT.setWorkerTypes(WORKER);
    FARM_PLOT.worksBeforeBuilt = true;
    FARM_PLOT.gatherFlag = IS_CROP;
    FARM_PLOT.produced   = CROP_TYPES;
    FARM_PLOT.maxStock   = 25;
    FARM_PLOT.maxWorkers = 2;
    FARM_PLOT.craftSkill = SKILL_FARM;
    
    SAWYER.name = "Sawyer";
    SAWYER.tint = TINT_LITE_INDUSTRIAL;
    SAWYER.setDimensions(2, 2, 1);
    SAWYER.setBuildMaterials(WOOD, 5, CLAY, 2);
    SAWYER.setWorkerTypes(WORKER);
    SAWYER.worksBeforeBuilt = true;
    SAWYER.gatherFlag = IS_TREE;
    SAWYER.maxStock   = 25;
    SAWYER.produced   = new Good[] { WOOD };
    SAWYER.maxWorkers = 2;
    SAWYER.craftSkill = SKILL_CRAFT;
    
    QUARRY_PIT.name = "Quarry Pit";
    QUARRY_PIT.tint = TINT_LITE_INDUSTRIAL;
    QUARRY_PIT.setDimensions(2, 2, 1);
    QUARRY_PIT.setBuildMaterials(WOOD, 5, CLAY, 2);
    QUARRY_PIT.setWorkerTypes(WORKER);
    QUARRY_PIT.worksBeforeBuilt = true;
    QUARRY_PIT.gatherFlag = IS_STONE;
    QUARRY_PIT.maxStock   = 25;
    QUARRY_PIT.produced   = new Good[] { CLAY, STONE };
    QUARRY_PIT.maxWorkers = 2;
    QUARRY_PIT.craftSkill = SKILL_CRAFT;
    
    KILN.name = "Kiln";
    KILN.tint = TINT_INDUSTRIAL;
    KILN.setDimensions(2, 2, 1);
    KILN.setBuildMaterials(STONE, 2, WOOD, 2, CLAY, 1);
    KILN.setWorkerTypes(WORKER);
    KILN.needed   = new Good[] { CLAY };
    KILN.produced = new Good[] { POTTERY };
    KILN.maxStock = 3;
    KILN.craftSkill = SKILL_CRAFT;
    
    WEAVER.name = "Weaver";
    WEAVER.tint = TINT_INDUSTRIAL;
    WEAVER.setDimensions(2, 2, 1);
    WEAVER.setBuildMaterials(WOOD, 2, CLAY, 1);
    WEAVER.setWorkerTypes(WORKER);
    WEAVER.needed   = new Good[] { RAW_COTTON };
    WEAVER.produced = new Good[] { COTTON };
    WEAVER.maxStock = 3;
    WEAVER.craftSkill = SKILL_CRAFT;
    
    //
    //  Commercial structures:
    MARKET.name = "Marketplace";
    MARKET.tint = TINT_COMMERCIAL;
    MARKET.setDimensions(4, 4, 1);
    MARKET.setBuildMaterials(WOOD, 4, STONE, 2);
    MARKET.setWorkerTypes(MERCHANT);
    MARKET.needed   = MARKET_GOODS;
    MARKET.features = new Good[] { IS_VENDOR };
    
    PORTER_POST.name = "Porter Post";
    PORTER_POST.tint = TINT_COMMERCIAL;
    PORTER_POST.setDimensions(3, 3, 1);
    PORTER_POST.setBuildMaterials(WOOD, 4, STONE, 2);
    PORTER_POST.setWorkerTypes(PORTER, WORKER);
    PORTER_POST.worksBeforeBuilt = true;
    PORTER_POST.features = new Good[] { IS_TRADER };
    
    //  TODO:  You could make this into more of a 'governor's post', since the
    //  tax-collectors basically functioned that way.
    COLLECTOR.name = "Collector";
    COLLECTOR.tint = TINT_COMMERCIAL;
    COLLECTOR.setDimensions(2, 2, 1);
    COLLECTOR.setBuildMaterials(STONE, 2, WOOD, 2, CLAY, 2);
    COLLECTOR.setWorkerTypes(MERCHANT);
    COLLECTOR.produced = new Good[] { CASH };
    COLLECTOR.features = new Good[] { IS_ADMIN };
    
    //
    //  Military structures:
    HUNTER_LODGE.name = "Hunter Lodge";
    HUNTER_LODGE.tint = TINT_MILITARY;
    HUNTER_LODGE.setDimensions(4, 4, 1);
    HUNTER_LODGE.setBuildMaterials(WOOD, 10);
    HUNTER_LODGE.setWorkerTypes(HUNTER);
    HUNTER_LODGE.worksBeforeBuilt = true;
    HUNTER_LODGE.maxWorkers = 2;
    HUNTER_LODGE.maxHealth  = 100;
    HUNTER_LODGE.produced   = new Good[] { MEAT };
    
    GARRISON.name = "Garrison";
    GARRISON.tint = TINT_MILITARY;
    GARRISON.setDimensions(6, 6, 2);
    GARRISON.setBuildMaterials(STONE, 10, WOOD, 5);
    GARRISON.setWorkerTypes(SOLDIER);
    GARRISON.maxWorkers = 2;
    GARRISON.maxHealth  = 250;
    
    //
    //  Religious structures:
    //
    //  Day/Order and Night/Freedom : Qz / Tz
    TEMPLE_QZ .name = "Temple to Quetzalcoatl";
    TEMPLE_TZ .name = "Temple to Tezcatlipoca";
    //
    //  Fire and Water: Hu / Tl
    TEMPLE_HU .name = "Temple to Huitzilopochtli";
    TEMPLE_TL .name = "Temple to Tlaloc";
    //
    //  Life and Death: Om / Mi
    TEMPLE_MI .name = "Temple to Mictecacehuatl";
    SHRINE_OMC.name = "Shrine to Omecihuatl";
    SHRINE_OMT.name = "Shrine to Ometicuhtli";
    //
    //  Balancing/tension: Xt
    TEMPLE_XT .name = "Temple to Xipe Totec";
    
    for (Type t : ALL_TEMPLES) {
      t.tint = TINT_RELIGIOUS;
      t.setDimensions(6, 6, 3);
      t.setBuildMaterials(STONE, 15);
      t.setWorkerTypes(PRIEST);
      t.maxWorkers      = 1;
      t.maxHealth       = 100;
      t.maxResidents    = 1;
      t.homeSocialClass = CLASS_NOBLE;
      t.features        = new Good[] { RELIGION };
    }
    
    //
    //  NOTE- there's a bunch of other shrines/upgrades I might add
    //  later, but I want to work those out in due time.  Later.
    
    //
    //  Quetzalcoatl:
    //    Aspects for healing & knowledge, travel & wind
    //    Patecatl/Piltzintecuhtli, Yacetecuhtli/Zacatzontli, Ehecatl
    //  Tezcatlipoca:
    //    Aspects for discord, beauty, sin & confession
    //    Itztli/Itztlacoliuhqui, Huehuecoyotl/Mayahuel, Tlazolteotl
    //  Tlaloc:
    //    Aspects for rivers & oceans, fertility & monstrosity
    //    Chalchiutlicue/Huixtocihuatl, Xochiquetzal/Xochipilli,
    //    Cipactli/Coatlicue/Tlaltecuhtli
    //  Huitzilopochtli:
    //    Aspects for warfare & lordship, fire & hearth
    //    Tonatiuh, Mixcoatl, Xiuhtecuhtli/Chantico
    //  Mictecacehuatl:
    //    Aspects for transmigration, ancestry, consumption & eclipse
    //    Xolotl/Nanahuatzin, Coyolxauhqui/Itzapapalotl/Oxomoco, Quilaztli
    //  Xipe Totec:
    //    Aspects of decay & abundance, factional tension
    //    Centeotl/Xilonen
    //  
    //  Ometeotl:     Synergy bonus based on factional balance
    //  The Toci:     Synergy bonus for earth-goddesses
    //  400 Rabbits:  Synergy bonus for regular festivals
    
    for (Type t : ALL_SHRINES) {
      t.tint = TINT_RELIGIOUS;
      if (t == SHRINE_OMC || t == SHRINE_OMT) {
        t.setDimensions(2, 2, 1);
        t.setBuildMaterials(STONE, 8);
      }
      else {
        t.setDimensions(1, 1, 1);
        t.setBuildMaterials(STONE, 2);
      }
    }
  }
  
  
  
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
  
  
  
  /**  Default geography:
    */
  static World setupDefaultWorld() {
    World world = new World();
    City  cityA = new City(world);
    City  cityB = new City(world);
    
    cityA.name = "Xochimilco";
    cityA.setWorldCoords(1, 1);
    cityA.tradeLevel.setWith(
      POTTERY, 5f ,
      COTTON , 10f
    );
    cityA.initBuildLevels(
      GARRISON, 2f ,
      HOUSE   , 10f
    );
    world.addCities(cityA);
    
    cityB.name = "Tlacopan";
    cityB.setWorldCoords(3, 3);
    cityB.tradeLevel.setWith(
      MAIZE, 5f ,
      CLAY , 10f
    );
    cityA.initBuildLevels(
      GARRISON, 0.75f,
      HOUSE   , 5f
    );
    world.addCities(cityB);
    
    City.setupRoute(cityA, cityB, AVG_CITY_DIST / 2);
    world.mapWide = world.mapHigh = 10;
    
    return world;
  }
}









