

package game;
import util.*;
import static game.Type.*;
import static game.CityMap.*;
import java.awt.Color;



public class GameConstants {
  
  
  /**  Colour prototyping-
    */
  private static int colour(int r, int g, int b) {
    return new Color(r / 10f, g / 10f, b / 10f).getRGB();
  }
  
  final static int
    BLANK_COLOR  = colour(5, 5, 5),
    PAVE_COLOR   = colour(8, 8, 8),
    WALKER_COLOR = colour(9, 9, 0),
    CITY_COLOR   = colour(7, 7, 1),
    NO_BLD_COLOR = colour(9, 0, 0),
    
    WHITE_COLOR  = colour(10, 10, 10),
    BLACK_COLOR  = colour(0 , 0 , 0 ),
    
    //  Brown, Grey, White, Red, Green, Blue, Orange, Purple, Yellow.
    //  Industrial and sanitation in brown.
    TINT_LITE_INDUSTRIAL  = colour(6, 3, 3),
    TINT_INDUSTRIAL       = colour(5, 2, 2),
    //  Military in red.
    TINT_MILITARY         = colour(7, 0, 0),
    //  Economic in blue.
    TINT_LITE_COMMERCIAL  = colour(3, 1, 8),
    TINT_COMMERCIAL       = colour(2, 0, 7),
    //  Residential in yellow.
    TINT_LITE_RESIDENTIAL = colour(8, 8, 1),
    TINT_RESIDENTIAL      = colour(7, 7, 0),
    //  Entertainment in purple.
    TINT_AMENITY          = colour(7, 0, 7),
    //  Health/education in white.
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
    //  Buildings and manufacture-
    AVG_UPDATE_GAP   = 50  ,  //  seconds between updates
    AVG_CRAFT_TIME   = 20  ,
    AVG_MAX_STOCK    = 10  ,
    MAX_TRADER_RANGE = 100 ,
    //
    //  Life cycle constants-
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
    //  Health and survival-
    STARVE_INTERVAL  = MONTH_LENGTH * 2,
    FATIGUE_INTERVAL = MONTH_LENGTH * 2,
    HUNGER_REGEN     = 5,
    FOOD_UNIT_PER_HP = 2,
    FATIGUE_REGEN    = MONTH_LENGTH / 4,
    HEALTH_REGEN     = MONTH_LENGTH / 2,
    AVG_MAX_HEALTH   = 5,
    //
    //  Commerce and amenities-
    MAX_WANDER_RANGE = 20  ,
    AVG_VISIT_TIME   = 20  ,
    MAX_SHOP_RANGE   = 50  ,
    AVG_CONSUME_TIME = 500 ,
    AVG_SERVICE_GIVE = 10  , //  value of education, diversion, etc.
    AVG_MAX_VISITORS = 4   ,
    //
    //  Military and combat-
    AVG_ARMY_SIZE    = 16  ,
    AVG_RANKS        = 4   ,
    AVG_FILE         = 4   ,
    AVG_ATTACK       = 2   ,
    AVG_DEFEND       = 2   ,
    AVG_SIGHT        = 6   ,
    AVG_RANGE        = 1   ,
    //
    //  Trade and migration-
    TRADE_DIST_TIME  = 50  ,
    MIGRANTS_PER_1KM = 10     //  per month per 1000 foreign citizens
  ;
  
  

  private static List <Terrain> TERRAINS_LIST = new List();
  static class Terrain extends Type {
    
    int terrainIndex = 0;
    Type  fixtures[] = new Type [0];
    Float weights [] = new Float[0];
    
    Terrain(String name, int index) {
      super("terrain_"+index, IS_TERRAIN);
      this.name         = name ;
      this.terrainIndex = index;
      this.blocks       = false;
      TERRAINS_LIST.add(this);
    }
    
    void attachFixtures(Object... args) {
      Object split[][] = Visit.splitByModulus(args, 2);
      fixtures = (Type []) castArray(split[0], Type .class);
      weights  = (Float[]) castArray(split[1], Float.class);
    }
    
  }
  final static Terrain
    MEADOW = new Terrain("Meadow", 0),
    JUNGLE = new Terrain("Jungle", 1),
    DESERT = new Terrain("Desert", 2),
    LAKE   = new Terrain("Lake"  , 3),
    ALL_TERRAINS[] = TERRAINS_LIST.toArray(Terrain.class),
    NO_HABITAT  [] = {}
  ;
  final static Type
    JUNGLE_TREE1 = new Type("fixture_j_tree1", IS_FIXTURE),
    DESERT_ROCK1 = new Type("fixture_d_rock1", IS_FIXTURE),
    DESERT_ROCK2 = new Type("fixture_d_rock2", IS_FIXTURE)
  ;
  final static Type
    TAPIR   = new Type("animal_tapir" , IS_ANIMAL_WLK),
    QUAIL   = new Type("animal_quail" , IS_ANIMAL_WLK),
    JAGUAR  = new Type("animal_jaguar", IS_ANIMAL_WLK),
    ALL_ANIMALS[] = { TAPIR, QUAIL, JAGUAR }
  ;
  static {
    JUNGLE.attachFixtures(JUNGLE_TREE1, 0.50f);
    MEADOW.attachFixtures(JUNGLE_TREE1, 0.05f);
    DESERT.attachFixtures(DESERT_ROCK1, 0.15f, DESERT_ROCK2, 0.20f);
    
    JUNGLE_TREE1.tint = colour(0, 3, 0);
    DESERT_ROCK1.tint = colour(6, 4, 4);
    DESERT_ROCK2.tint = colour(6, 4, 4);
    JUNGLE      .tint = colour(1, 4, 1);
    DESERT      .tint = colour(5, 4, 3);
    MEADOW      .tint = colour(2, 5, 2);
    LAKE        .tint = colour(0, 0, 3);
    
    //  TODO:  UNIFY WITH CROPS BELOW!
    JUNGLE_TREE1.growRate = 0.5f;
    DESERT_ROCK1.wide = DESERT_ROCK1.high = 2;
    LAKE.blocks = true;
    
    TAPIR.name     = "Tapir";
    TAPIR.habitats = new Terrain[] { JUNGLE, MEADOW };
    TAPIR.predator = false;
    
    QUAIL.name     = "Quail";
    QUAIL.habitats = new Terrain[] { MEADOW, DESERT };
    QUAIL.predator = false;
    
    JAGUAR.name     = "Jaguar";
    JAGUAR.habitats = new Terrain[] { JUNGLE };
    JAGUAR.predator = true;
    
    for (Type s : ALL_ANIMALS) {
      s.lifespan = s.predator ? HUNTER_LIFESPAN : GRAZER_LIFESPAN;
    }
  }
  
  
  
  /**  Economic constants-
    */
  private static List <Good> GOODS_LIST = new List();
  static class Good extends Type {
    
    int price;
    
    Good(String name, int price, int ID) {
      super("good_"+ID, IS_GOOD);
      
      GOODS_LIST.add(this);
      this.name   = name ;
      this.price  = price;
      this.blocks = false;
    }
  }
  final static Good
    WATER      = new Good("Water"       , 0 , 0 ),
    MAIZE      = new Good("Maize"       , 10, 1 ),
    FRUIT      = new Good("Fruit"       , 12, 2 ),
    RAW_COTTON = new Good("Raw Cotton"  , 15, 3 ),
    WOOD       = new Good("Wood"        , 10, 4 ),
    RUBBER     = new Good("Rubber"      , 25, 5 ),
    CLAY       = new Good("Clay"        , 10, 6 ),
    ADOBE      = new Good("Adobe"       , 20, 7 ),
    
    POTTERY    = new Good("Pottery"     , 50, 8 ),
    COTTON     = new Good("Cotton"      , 75, 9 ),
    
    CASH       = new Good("Cash"        , 1 , 10),
    SOIL       = new Good("Soil"        , 5 , 11),
    
    IS_ADMIN   = new Good("Is Admin"    , -1, 21),
    IS_MARKET  = new Good("Is Market"   , -1, 22),
    IS_TRADER  = new Good("Is Trader"   , -1, 24),
    IS_HOUSING = new Good("Is Housing"  , -1, 25),
    IS_WATER   = new Good("Is Water"    , -1, 26),
    
    DIVERSION  = new Good("Diversion"   , -1, 30),
    EDUCATION  = new Good("Education"   , -1, 31),
    HEALTHCARE = new Good("Healthcare"  , -1, 32),
    RELIGION   = new Good("Religion"    , -1, 33),
    
    CROP_TYPES  [] = { MAIZE, FRUIT, RAW_COTTON },
    FOOD_TYPES  [] = { MAIZE, FRUIT },
    TREE_TYPES  [] = { WOOD, RUBBER },
    STONE_TYPES [] = { CLAY, ADOBE  },
    BUILD_GOODS [] = { WOOD, CLAY, ADOBE },
    HOME_GOODS  [] = { POTTERY, COTTON },
    MARKET_GOODS[] = (Good[]) Visit.compose(Good.class, FOOD_TYPES, HOME_GOODS),
    ALL_GOODS   [] = (Good[]) GOODS_LIST.toArray(Good.class),
    
    COMMERCE_TYPES[] = { IS_ADMIN, IS_TRADER, IS_MARKET, IS_HOUSING },
    SERVICE_TYPES [] = { DIVERSION, EDUCATION, HEALTHCARE, RELIGION },
    NO_GOODS      [] = new Good[0];
  
  static {
    int i = 0;
    for (Good g : CROP_TYPES) {
      g.tint = TINT_CROPS[i++ % 3];
      g.growRate = 1f;
    }
    for (Good g : TREE_TYPES) {
      g.growRate = 0.5f;
    }
  }
  
  
  /**  Walker types-
    */
  static class WalkerType extends Type {
    WalkerType(String ID, int category, int socialClass) {
      super(ID, category);
      this.socialClass = socialClass;
    }
  }
  final static int
    CLASS_SLAVE   = 0,
    CLASS_COMMON  = 1,
    CLASS_TRADER  = 2,
    CLASS_NOBLE   = 3,
    ALL_CLASSES[] = { 0, 1, 2, 3 },
    TAX_VALUES [] = { 0, 10, 25, 100 }
  ;
  final static WalkerType
    NO_WALKERS[] = new WalkerType[0],
    
    VAGRANT  = new WalkerType("type_vagrant" , IS_PERSON_WLK, CLASS_COMMON),
    CHILD    = new WalkerType("type_child"   , IS_PERSON_WLK, CLASS_COMMON),
    CITIZEN  = new WalkerType("type_citizen" , IS_PERSON_WLK, CLASS_COMMON),
    SERVANT  = new WalkerType("type_servant" , IS_PERSON_WLK, CLASS_SLAVE ),
    NOBLE    = new WalkerType("type_noble"   , IS_PERSON_WLK, CLASS_NOBLE ),
    WORKER   = new WalkerType("type_worker"  , IS_PERSON_WLK, CLASS_COMMON),
    MERCHANT = new WalkerType("type_merchant", IS_PERSON_WLK, CLASS_TRADER),
    PORTERS  = new WalkerType("type_porters" , IS_PERSON_WLK, CLASS_SLAVE ),
    SOLDIER  = new WalkerType("type_soldier" , IS_PERSON_WLK, CLASS_NOBLE ),
    PRIEST   = new WalkerType("type_priest"  , IS_PERSON_WLK, CLASS_NOBLE )
  ;
  static {
    VAGRANT .name = "Vagrant" ;
    CHILD   .name = "Child"   ;
    CITIZEN .name = "Citizen" ;
    SERVANT .name = "Servant" ;
    NOBLE   .name = "Noble"   ;
    WORKER  .name = "Worker"  ;
    MERCHANT.name = "Merchant";
    PORTERS .name = "Porters" ;
    SOLDIER .name = "Soldier" ;
    PRIEST  .name = "Priest"  ;
    
    SOLDIER.attackScore = 5;
    SOLDIER.defendScore = 4;
  }
  
  
  /**  Infrastructure types-
    */
  static class BuildType extends Type {
    BuildType(String ID, int category) {
      super(ID, category);
    }
  }
  final static int
    AMBIENCE_MIN = -10,
    AMBIENCE_AVG =  5 ,
    AMBIENCE_PAD =  2 ,
    AMBIENCE_MAX =  20
  ;
  final static BuildType  
    
    NO_TIERS[] = new BuildType[0],
    NO_NEEDS[] = new BuildType[0],
    
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
    
    FARMER_HUT    = new BuildType("type_farmer_hut"   , IS_GATHER_BLD ),
    QUARRY_PIT    = new BuildType("type_quarry_pit"   , IS_CRAFTS_BLD ),
    KILN          = new BuildType("type_kiln"         , IS_CRAFTS_BLD ),
    WEAVER        = new BuildType("type_weaver"       , IS_CRAFTS_BLD ),
    MASON         = new BuildType("type_mason"        , IS_CRAFTS_BLD ),
    INDUSTRIAL_BUILDINGS[] = { FARMER_HUT, QUARRY_PIT, KILN, WEAVER, MASON },
    
    MARKET        = new BuildType("type_market"       , IS_CRAFTS_BLD ),
    PORTER_HOUSE  = new BuildType("type_porter_house" , IS_TRADE_BLD  ),
    COLLECTOR     = new BuildType("type_collector"    , IS_COLLECT_BLD),
    ECONOMIC_BUILDINGS[] = { MARKET, PORTER_HOUSE, COLLECTOR },
    
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
    //  Palace structures:
    PALACE.name = "Palace";
    PALACE.tint = TINT_RESIDENTIAL;
    PALACE.setDimensions(5, 5, 2);
    PALACE.setBuildMaterials(WOOD, 15, ADOBE, 25, COTTON, 10, POTTERY, 5);
    PALACE.setWorkerTypes(NOBLE, SERVANT);
    PALACE.homeSocialClass = CLASS_NOBLE;
    PALACE.maxResidents = 2;
    PALACE.maxWorkers = 2;
    PALACE.maxHealth = 300;
    PALACE.features = new Good[] { IS_HOUSING };
    
    //
    //  Residential structures:
    HOUSE.name = "House";
    HOUSE.tint = TINT_LITE_RESIDENTIAL;
    HOUSE.setDimensions(2, 2, 1);
    HOUSE.setBuildMaterials(WOOD, 2, CLAY, 1);
    HOUSE.setWorkerTypes(CITIZEN);
    HOUSE.maxResidents = 4;
    HOUSE.maxStock = 2;
    HOUSE.buildsWith = new Good[] { WOOD, CLAY };
    HOUSE.features = new Good[] { IS_HOUSING };
    HOUSE.setUpgradeTiers(HOUSE, HOUSE_T1, HOUSE_T2);
    
    HOUSE_T1.name = "Improved House";
    HOUSE_T1.setBuildMaterials(WOOD, 4, CLAY, 2);
    HOUSE_T1.consumed = new Good[] { POTTERY };
    HOUSE_T1.maxStock = 2;
    HOUSE_T1.setUpgradeNeeds(DIVERSION, 10);
    
    HOUSE_T2.name = "Fancy House";
    HOUSE_T2.setBuildMaterials(WOOD, 6, CLAY, 3);
    HOUSE_T2.consumed = new Good[] { POTTERY, COTTON };
    HOUSE_T2.maxStock = 2;
    HOUSE_T2.setUpgradeNeeds(DIVERSION, 15, SCHOOL, 1);
    
    BASIN.name = "Basin";
    BASIN.tint = TINT_HEALTH_ED;
    BASIN.setDimensions(2, 2, 0);
    BASIN.setBuildMaterials(ADOBE, 2, CLAY, 2);
    BASIN.features = new Good[] { IS_WATER, IS_MARKET };
    
    SWEEPER.name = "Sweeper";
    SWEEPER.tint = TINT_LITE_INDUSTRIAL;
    SWEEPER.setDimensions(1, 1, 1);
    SWEEPER.setBuildMaterials(WOOD, 2, CLAY, 1);
    SWEEPER.setWorkerTypes(WORKER);
    SWEEPER.produced = new Good[] { SOIL };
    
    SCHOOL.name = "Public School";
    SCHOOL.tint = TINT_HEALTH_ED;
    SCHOOL.setDimensions(2, 2, 1);
    SCHOOL.setBuildMaterials(WOOD, 5, CLAY, 2, ADOBE, 3);
    SCHOOL.setWorkerTypes(CITIZEN);
    SCHOOL.features = new Good[] { EDUCATION };
    
    BALL_COURT.name = "Ball Court";
    BALL_COURT.tint = TINT_AMENITY;
    BALL_COURT.setDimensions(3, 3, 1);
    BALL_COURT.setBuildMaterials(ADOBE, 10, RUBBER, 5);
    BALL_COURT.features = new Good[] { DIVERSION };
    BALL_COURT.featureAmount = 15;

    //
    //  Industrial structures:
    MASON.name = "Mason";
    MASON.tint = TINT_LITE_INDUSTRIAL;
    MASON.setDimensions(2, 2, 1);
    MASON.setBuildMaterials(ADOBE, 2, WOOD, 2, CLAY, 2);
    MASON.setWorkerTypes(WORKER);
    MASON.craftTime *= 2;
    MASON.maxWorkers = 2;
    MASON.buildsWith = new Good[] { WOOD, CLAY, ADOBE };
    
    FARMER_HUT.name = "Farmer Hut";
    FARMER_HUT.tint = TINT_LITE_INDUSTRIAL;
    FARMER_HUT.setDimensions(3, 3, 1);
    FARMER_HUT.setBuildMaterials(WOOD, 5, CLAY, 2);
    FARMER_HUT.setWorkerTypes(WORKER);
    FARMER_HUT.produced = CROP_TYPES;
    FARMER_HUT.maxWorkers = 2;
    
    QUARRY_PIT.name = "Quarry Pit";
    QUARRY_PIT.tint = TINT_LITE_INDUSTRIAL;
    QUARRY_PIT.setDimensions(4, 4, 0);
    QUARRY_PIT.setBuildMaterials(WOOD, 5, CLAY, 2);
    QUARRY_PIT.setWorkerTypes(WORKER);
    QUARRY_PIT.produced = new Good[] { CLAY };
    
    KILN.name = "Kiln";
    KILN.tint = TINT_INDUSTRIAL;
    KILN.setDimensions(2, 2, 1);
    KILN.setBuildMaterials(ADOBE, 2, WOOD, 2, CLAY, 1);
    KILN.setWorkerTypes(WORKER);
    KILN.needed   = new Good[] { CLAY };
    KILN.produced = new Good[] { POTTERY };
    KILN.craftTime *= 2;
    
    WEAVER.name = "Weaver";
    WEAVER.tint = TINT_INDUSTRIAL;
    WEAVER.setDimensions(2, 2, 1);
    WEAVER.setBuildMaterials(WOOD, 2, RAW_COTTON, 2, CLAY, 1);
    WEAVER.setWorkerTypes(WORKER);
    WEAVER.needed   = new Good[] { RAW_COTTON };
    WEAVER.produced = new Good[] { COTTON };
    WEAVER.craftTime *= 2;
    
    //
    //  Commercial structures:
    MARKET.name = "Marketplace";
    MARKET.tint = TINT_COMMERCIAL;
    MARKET.setDimensions(4, 4, 1);
    MARKET.setBuildMaterials(WOOD, 4, COTTON, 2, ADOBE, 2);
    MARKET.setWorkerTypes(MERCHANT);
    MARKET.needed   = MARKET_GOODS;
    MARKET.features = new Good[] { IS_MARKET };
    
    PORTER_HOUSE.name = "Porter Post";
    PORTER_HOUSE.tint = TINT_COMMERCIAL;
    PORTER_HOUSE.setDimensions(3, 3, 1);
    PORTER_HOUSE.setBuildMaterials(WOOD, 4, ADOBE, 2, POTTERY, 2);
    PORTER_HOUSE.setWorkerTypes(PORTERS, WORKER);
    PORTER_HOUSE.features = new Good[] { IS_TRADER };
    
    COLLECTOR.name = "Collector";
    COLLECTOR.tint = TINT_COMMERCIAL;
    COLLECTOR.setDimensions(2, 2, 1);
    COLLECTOR.setBuildMaterials(ADOBE, 2, WOOD, 2, CLAY, 2);
    COLLECTOR.setWorkerTypes(MERCHANT);
    COLLECTOR.produced = new Good[] { CASH };
    COLLECTOR.features = new Good[] { IS_ADMIN };
    
    //
    //  Military structures:
    GARRISON.name = "Garrison";
    GARRISON.tint = TINT_MILITARY;
    GARRISON.setDimensions(6, 6, 2);
    GARRISON.setBuildMaterials(ADOBE, 10, WOOD, 5);
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
    TEMPLE_HU .name = "Temple to Huitzilipochtli";
    TEMPLE_TL .name = "Temple to Tlaloc";
    //
    //  Life and Death: Om / Mi
    TEMPLE_MI .name = "Temple to Mictecacehuatl";
    SHRINE_OMC.name = "Shrine to Omecihuatl";
    SHRINE_OMT.name = "Shrine to Ometicuhtli";
    //
    //  Neutrality/balance: Xt
    TEMPLE_XT .name = "Temple to Xipe Totec";
    
    for (Type t : ALL_TEMPLES) {
      t.tint = TINT_RELIGIOUS;
      t.setDimensions(6, 6, 3);
      t.setBuildMaterials(ADOBE, 15, POTTERY, 5);
      t.setWorkerTypes(PRIEST);
      t.maxWorkers      = 1;
      t.maxHealth       = 100;
      t.maxResidents    = 1;
      t.homeSocialClass = CLASS_NOBLE;
      t.features        = new Good[] { RELIGION };
    }
    //
    //  TODO:  Possibly, later-
    //  Note:  I'm doing a certain amount of shoe-horning here for the sake of
    //         balance/concision.  Bear with me.
    //         These might also work as 'upgrades' for a particular temple...
    //
    //  Associates of Quetzalcoatl:
    //    Patecatl, god of medicine and healing
    //    Yacetecuhtli, god of merchants and travel
    //    Ehecatl, god of winds
    //
    //  Associates of Tezcatlipoca:
    //    Itztli, god of obsidian
    //    Huehuecoyotl, festive trickster
    //    Tlazolteotl, sin and confession
    //
    //  Associates of Huitzilipochtli:
    //    Tonatiuh, god of the sun
    //    Mixcoatl, war and hunting
    //    Xiuhtecuhtli/Chantico, hearth, lordship and calendar
    //
    //  Associates of Tlaloc:
    //    Chalchiutlicue, lakes and rivers
    //    Xochiquetzal, nature and beauty
    //    Cipactli/Tlaltecuhtli, caves and aquatic monsters
    //
    //  Associates of Mictecacehuatl/Mictlantecuhtli:
    //    Xolotl, guide to the dead
    //    Coyolxauhqui/Itzapapalotl, moon, stars and eclipses
    //    Toci-Quilaztli, childbirth and vengeance
    //
    //  Associates of Xipe Totec:
    //    Centeotl/Xilonen, god/dess of maize
    //    Balance between other Gods counts as bonus for XT.
    
    for (Type t : ALL_SHRINES) {
      t.tint = TINT_RELIGIOUS;
      if (t == SHRINE_OMC || t == SHRINE_OMT) {
        t.setDimensions(2, 2, 1);
        t.setBuildMaterials(ADOBE, 8);
      }
      else {
        t.setDimensions(1, 1, 1);
        t.setBuildMaterials(ADOBE, 2);
      }
    }
  }
  
  
  
  /**  Commonly used interfaces-
    */
  static interface Target {
    Tile at();
    void targetedBy(Actor w);
    //void setFocused(Walker w, boolean is);
  }
  
  static interface Trader {
    Tally <Good> tradeLevel();
    Tally <Good> inventory ();
    City tradeOrigin();
  }
  
  static interface Journeys {
    void onArrival(City goes, World.Journey journey);
  }
  
  static interface Employer {
    void selectActorBehaviour(Actor actor);
    void actorUpdates(Actor actor);
    void actorPasses (Actor actor, Building other );
    void actorTargets(Actor actor, Target   other );
    void actorEnters (Actor actor, Building enters);
    void actorVisits (Actor actor, Building visits);
    void actorExits  (Actor actor, Building enters);
  }
  
  
  
  /**  Default geography:
    */
  static World setupDefaultWorld() {
    World world = new World();
    City  cityA = new City(world);
    City  cityB = new City(world);
    
    cityA.name = "Xochimilco";
    cityB.name = "Tlacopan"  ;
    cityA.setWorldCoords(1, 1);
    cityB.setWorldCoords(3, 3);
    City.setupRoute(cityA, cityB, 2);
    
    world.addCity(cityA);
    world.addCity(cityB);
    world.mapWide = world.mapHigh = 10;
    
    return world;
  }
}












