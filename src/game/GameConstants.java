

package game;
import util.*;
import static game.ObjectType.*;
import java.awt.Color;

import game.CityMap.Tile;
import game.World.Journey;



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
    
    WHITE_COLOR  = colour(10, 10, 10),
    BLACK_COLOR  = colour(0 , 0 , 0 )
  ;
  
  
  
  /**  Terrain-related constants-
    */
  final public static int
    HOUR_LENGTH      = 4   ,
    DAY_LENGTH       = 48  ,
    SCAN_PERIOD      = 200 ,
    RIPEN_PERIOD     = 1000,
    CROP_YIELD       = 25  ,  //  percent of 1 full item
    MIGRANTS_PER_1KD = 10     //  per day per 1000 foreign citizens
  ;

  private static List <Terrain> TERRAINS_LIST = new List();
  static class Terrain extends ObjectType {
    
    int terrainIndex = 0;
    ObjectType fixtures[] = new ObjectType[0];
    Float      weights [] = new Float     [0];
    
    Terrain(String name, int index) {
      super("terrain_"+index, IS_TERRAIN);
      this.name         = name ;
      this.terrainIndex = index;
      this.blocks       = false;
      TERRAINS_LIST.add(this);
    }
    
    void attachFixtures(Object... args) {
      Object split[][] = Visit.splitByModulus(args, 2);
      fixtures = (ObjectType[]) castArray(split[0], ObjectType.class);
      weights  = (Float     []) castArray(split[1], Float     .class);
    }
    
  }
  final static Terrain
    MEADOW = new Terrain("Meadow", 0),
    JUNGLE = new Terrain("Jungle", 1),
    DESERT = new Terrain("Desert", 2),
    LAKE   = new Terrain("Lake"  , 3),
    ALL_TERRAINS[] = TERRAINS_LIST.toArray(Terrain.class)
  ;
  final static ObjectType
    JUNGLE_TREE1 = new ObjectType("fixture_j_tree1", IS_FIXTURE),
    DESERT_ROCK1 = new ObjectType("fixture_d_rock1", IS_FIXTURE),
    DESERT_ROCK2 = new ObjectType("fixture_d_rock2", IS_FIXTURE)
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
    
    DESERT_ROCK1.wide = DESERT_ROCK1.high = 2;
    LAKE.blocks = true;
  }
  
  
  
  /**  Economic constants-
    */
  private static List <Good> GOODS_LIST = new List();
  static class Good extends ObjectType {
    
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
    MAIZE      = new Good("Maize"       , 10, 0 ),
    RAW_COTTON = new Good("Raw Cotton"  , 15, 1 ),
    WOOD       = new Good("Wood"        , 10, 2 ),
    RUBBER     = new Good("Rubber"      , 25, 3 ),
    CLAY       = new Good("Clay"        , 10, 4 ),
    ADOBE      = new Good("Adobe"       , 20, 5 ),
    
    POTTERY    = new Good("Pottery"     , 50, 6 ),
    COTTON     = new Good("Cotton"      , 75, 7 ),
    
    IS_MARKET  = new Good("Is Market"   , -1, 22),
    IS_AMENITY = new Good("Is Amenity"  , -1, 23),
    IS_TRADER  = new Good("Is Trader"   , -1, 24),
    IS_HOUSING = new Good("Is Housing"  , -1, 25),
    
    CROP_TYPES [] = { MAIZE, RAW_COTTON },
    TREE_TYPES [] = { WOOD, RUBBER },
    STONE_TYPES[] = { CLAY, ADOBE },
    BUILD_GOODS[] = { WOOD, CLAY, ADOBE },
    ALL_GOODS  [] = (Good[]) GOODS_LIST.toArray(Good.class),
    NO_GOODS   [] = new Good[0];
  
  static {
    MAIZE     .tint = colour(9, 9, 1);
    RAW_COTTON.tint = colour(9, 8, 9);
    RUBBER    .tint = colour(2, 2, 2);
    
    for (Good g : CROP_TYPES) g.growRate = 1f;
  }
  
  
  /**  Walker types-
    */
  static class WalkerType extends ObjectType {
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
    ALL_CLASSES[] = { 0, 1, 2, 3 }
  ;
  final static WalkerType
    NO_WALKERS[] = new WalkerType[0],
    
    VAGRANT  = new WalkerType("type_vagrant" , IS_WALKER   , CLASS_COMMON),
    CITIZEN  = new WalkerType("type_citizen" , IS_WALKER   , CLASS_COMMON),
    SERVANT  = new WalkerType("type_servant" , IS_WALKER   , CLASS_SLAVE ),
    NOBLE    = new WalkerType("type_noble"   , IS_WALKER   , CLASS_NOBLE ),
    WORKER   = new WalkerType("type_worker"  , IS_WALKER   , CLASS_COMMON),
    MERCHANT = new WalkerType("type_merchant", IS_WALKER   , CLASS_TRADER),
    PORTERS  = new WalkerType("type_porters" , IS_TRADE_WLK, CLASS_SLAVE ),
    SOLDIER  = new WalkerType("type_soldier" , IS_WALKER   , CLASS_NOBLE )
  ;
  static {
    VAGRANT .name = "Vagrant" ;
    CITIZEN .name = "Citizen" ;
    SERVANT .name = "Servant" ;
    NOBLE   .name = "Noble"   ;
    WORKER  .name = "Worker"  ;
    MERCHANT.name = "Merchant";
    PORTERS .name = "Porters" ;
    SOLDIER .name = "Soldier" ;
    
    SOLDIER.attackScore = 5;
    SOLDIER.defendScore = 4;
  }
  
  
  /**  Infrastructure types-
    */
  static class BuildType extends ObjectType {
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
    PALACE       = new BuildType("type_palace"      , IS_HOME_BLD   ),
    HOUSE        = new BuildType("type_house"       , IS_HOME_BLD   ),
    MASON        = new BuildType("type_mason"       , IS_CRAFTS_BLD ),
    BALL_COURT   = new BuildType("type_ball_court"  , IS_BUILDING   ),
    FARMER_HUT   = new BuildType("type_farmer_hut"  , IS_GATHER_BLD ),
    QUARRY_PIT   = new BuildType("type_quarry_pit"  , IS_CRAFTS_BLD ),
    KILN         = new BuildType("type_kiln"        , IS_CRAFTS_BLD ),
    WEAVER       = new BuildType("type_weaver"      , IS_CRAFTS_BLD ),
    MARKET       = new BuildType("type_market"      , IS_CRAFTS_BLD ),
    PORTER_HOUSE = new BuildType("type_porter_house", IS_TRADE_BLD  ),
    GARRISON     = new BuildType("type_garrison"    , IS_ARMY_BLD   ),
    
    NO_AMENITIES[] = new BuildType[0]
  ;
  static {
    PALACE.name = "Palace";
    PALACE.wide = 5;
    PALACE.high = 5;
    PALACE.tint = colour(7, 3, 3);
    PALACE.homeSocialClass = CLASS_NOBLE;
    PALACE.maxResidents = 2;
    PALACE.setWorkerTypes(NOBLE);
    PALACE.maxWorkers = 2;
    PALACE.maxHealth = 300;
    PALACE.setBuildMaterials(WOOD, 15, ADOBE, 25, COTTON, 10, POTTERY, 5);
    
    MASON.name = "Mason";
    MASON.wide = 2;
    MASON.high = 2;
    MASON.tint = colour(6, 2, 6);
    MASON.setWorkerTypes(WORKER);
    MASON.craftTime *= 2;
    MASON.maxWorkers = 2;
    MASON.setBuildMaterials(ADOBE, 2, WOOD, 2, CLAY, 2);
    MASON.buildsWith = new Good[] { WOOD, CLAY, ADOBE };
    
    HOUSE.name = "House";
    HOUSE.wide = 2;
    HOUSE.high = 2;
    HOUSE.tint = colour(3, 7, 3);
    HOUSE.maxResidents = 2;
    HOUSE.consumed = new Good[] { POTTERY };
    HOUSE.maxStock = 2;
    HOUSE.setBuildMaterials(WOOD, 2, CLAY, 1);
    HOUSE.buildsWith = new Good[] { WOOD, CLAY };
    
    BALL_COURT.name = "Ball Court";
    BALL_COURT.wide = 3;
    BALL_COURT.high = 3;
    BALL_COURT.tint = colour(3, 3, 7);
    BALL_COURT.features = new Good[] { IS_AMENITY };
    BALL_COURT.setBuildMaterials(ADOBE, 10, RUBBER, 5);
    
    FARMER_HUT.name = "Farmer Hut";
    FARMER_HUT.wide = 3;
    FARMER_HUT.high = 3;
    FARMER_HUT.tint = colour(7, 7, 3);
    FARMER_HUT.setWorkerTypes(WORKER);
    FARMER_HUT.produced = CROP_TYPES;
    FARMER_HUT.maxWorkers = 2;
    FARMER_HUT.setBuildMaterials(WOOD, 5, CLAY, 2);
    
    QUARRY_PIT.name = "Quarry Pit";
    QUARRY_PIT.wide = 4;
    QUARRY_PIT.high = 4;
    QUARRY_PIT.tint = colour(7, 7, 3);
    QUARRY_PIT.setWorkerTypes(WORKER);
    QUARRY_PIT.produced = new Good[] { CLAY };
    QUARRY_PIT.setBuildMaterials(WOOD, 5, CLAY, 2);
    
    KILN.name = "Kiln";
    KILN.wide = 2;
    KILN.high = 2;
    KILN.tint = colour(7, 3, 7);
    KILN.setWorkerTypes(WORKER);
    KILN.needed   = new Good[] { CLAY };
    KILN.produced = new Good[] { POTTERY };
    KILN.craftTime *= 2;
    KILN.setBuildMaterials(ADOBE, 2, WOOD, 2, CLAY, 1);
    
    WEAVER.name = "Weaver";
    WEAVER.wide = 2;
    WEAVER.high = 2;
    WEAVER.tint = colour(7, 3, 7);
    WEAVER.setWorkerTypes(WORKER);
    WEAVER.needed   = new Good[] { RAW_COTTON };
    WEAVER.produced = new Good[] { COTTON };
    WEAVER.craftTime *= 2;
    WEAVER.setBuildMaterials(WOOD, 2, RAW_COTTON, 2, CLAY, 1);
    
    MARKET.name = "Marketplace";
    MARKET.wide = 4;
    MARKET.high = 4;
    MARKET.tint = colour(4, 8, 4);
    MARKET.setWorkerTypes(MERCHANT);
    MARKET.needed   = new Good[] { POTTERY };
    MARKET.features = new Good[] { IS_MARKET };
    MARKET.setBuildMaterials(WOOD, 4, COTTON, 2, ADOBE, 2);
    
    PORTER_HOUSE.name = "Porter Post";
    PORTER_HOUSE.wide = 3;
    PORTER_HOUSE.high = 3;
    PORTER_HOUSE.tint = colour(8, 4, 8);
    PORTER_HOUSE.setWorkerTypes(PORTERS, WORKER);
    PORTER_HOUSE.features = new Good[] { IS_TRADER };
    PORTER_HOUSE.setBuildMaterials(WOOD, 4, ADOBE, 2, POTTERY, 2);
    
    GARRISON.name = "Garrison";
    GARRISON.wide = 6;
    GARRISON.high = 6;
    GARRISON.tint = colour(8, 8, 8);
    GARRISON.setWorkerTypes(SOLDIER);
    GARRISON.maxWorkers = 2;
    GARRISON.maxHealth  = 250;
    GARRISON.setBuildMaterials(ADOBE, 10, WOOD, 5);
  }
  
  
  
  /**  Commonly used interfaces-
    */
  static interface Target {
    Tile at();
    void targetedBy(Walker w);
    void setFocused(Walker w, boolean is);
  }
  
  static interface Trader {
    Tally <Good> tradeLevel();
    Tally <Good> inventory ();
    City tradeOrigin();
  }
  
  static interface Journeys {
    void onArrival(City goes, Journey journey);
  }
  
  static interface Employer {
    void selectWalkerBehaviour(Walker walker);
    void walkerUpdates(Walker walker);
    void walkerPasses (Walker walker, Building other );
    void walkerTargets(Walker walker, Target   other );
    void walkerEnters (Walker walker, Building enters);
    void walkerVisits (Walker walker, Building visits);
    void walkerExits  (Walker walker, Building enters);
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
    return world;
  }
}














