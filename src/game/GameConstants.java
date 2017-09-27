

package game;
import util.*;
import static game.ObjectType.*;
import java.awt.Color;

import game.GameConstants.Good;



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
    SCAN_PERIOD  = 200,
    RIPEN_PERIOD = 1000,
    CROP_YIELD   = 10  //  percent of 1 full item
  ;

  private static List <Terrain> TERRAINS_LIST = new List();
  static class Terrain extends ObjectType {
    
    int terrainIndex = 0;
    ObjectType fixtures[] = new ObjectType[0];
    float      weights [] = new float     [0];
    
    Terrain(String name, int index) {
      super("terrain_"+index, IS_TERRAIN);
      this.name         = name ;
      this.terrainIndex = index;
      this.blocks       = false;
      TERRAINS_LIST.add(this);
    }
    
    void attachFixtures(Object... args) {
      int hL = args.length / 2;
      fixtures = new ObjectType[hL];
      weights  = new float     [hL];
      int i = 0;
      while (i < hL) {
        fixtures[i] = (ObjectType) args[i * 2];
        weights [i] = (Float     ) args[(i * 2) + 1];
        i++;
      }
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
    RUBBER     = new Good("Rubber"      , 25, 2 ),
    
    CLAY       = new Good("Clay"        , 10, 4 ),
    POTTERY    = new Good("Pottery"     , 50, 5 ),
    COTTON     = new Good("Cotton"      , 75, 6 ),
    
    IS_MARKET  = new Good("Is Market"   , -1, 22),
    IS_AMENITY = new Good("Is Amenity"  , -1, 23),
    IS_TRADER  = new Good("Is Trader"   , -1, 24),
    
    CROP_TYPES[] = { MAIZE, RAW_COTTON, RUBBER },
    ALL_GOODS [] = (Good[]) GOODS_LIST.toArray(Good.class),
    NO_GOODS  [] = new Good[0];
  
  static {
    MAIZE     .tint = colour(9, 9, 1);
    RAW_COTTON.tint = colour(9, 8, 9);
  }
  
  
  
  /**  Infrastructure types-
    */
  final static ObjectType
    PALACE       = new ObjectType("type_palace"      , IS_HOME_BLD  ),
    HOUSE        = new ObjectType("type_house"       , IS_HOME_BLD  ),
    BALL_COURT   = new ObjectType("type_ball_court"  , IS_BUILDING  ),
    
    FARMER_HUT   = new ObjectType("type_farmer_hut"  , IS_GATHER_BLD),
    QUARRY_PIT   = new ObjectType("type_quarry_pit"  , IS_GATHER_BLD),
    KILN         = new ObjectType("type_kiln"        , IS_CRAFT_BLD ),
    WEAVER       = new ObjectType("type_weaver"      , IS_CRAFT_BLD ),
    MARKET       = new ObjectType("type_market"      , IS_CRAFT_BLD ),
    PORTER_HOUSE = new ObjectType("type_porter_house", IS_TRADE_BLD ),
    
    CITIZEN      = new ObjectType("type_citizen"     , IS_WALKER    ),
    NOBLE        = new ObjectType("type_noble"       , IS_WALKER    ),
    WORKER       = new ObjectType("type_worker"      , IS_WALKER    ),
    MERCHANT     = new ObjectType("type_merchant"    , IS_WALKER    ),
    PORTERS      = new ObjectType("type_porters"     , IS_TRADE_WLK ),
    
    NO_WALKERS[] = new ObjectType[0]
  ;
  static {
    CITIZEN .name = "Citizen" ;
    NOBLE   .name = "Noble"   ;
    WORKER  .name = "Worker"  ;
    MERCHANT.name = "Merchant";
    PORTERS .name = "Porters" ;
    
    PALACE.name = "Palace";
    PALACE.wide = 5;
    PALACE.high = 5;
    PALACE.tint = colour(7, 3, 3);
    PALACE.setWalkerTypes(NOBLE);
    
    HOUSE.name = "House";
    HOUSE.wide = 2;
    HOUSE.high = 2;
    HOUSE.tint = colour(3, 7, 3);
    HOUSE.setWalkerTypes(CITIZEN);
    HOUSE.consumed = new Good[] { POTTERY };
    HOUSE.maxStock = 2;
    
    BALL_COURT.name = "Ball Court";
    BALL_COURT.wide = 3;
    BALL_COURT.high = 3;
    BALL_COURT.tint = colour(3, 3, 7);
    BALL_COURT.features = new Good[] { IS_AMENITY };
    
    FARMER_HUT.name = "Farmer Hut";
    FARMER_HUT.wide = 4;
    FARMER_HUT.high = 4;
    FARMER_HUT.tint = colour(7, 7, 3);
    FARMER_HUT.setWalkerTypes(WORKER);
    FARMER_HUT.produced = CROP_TYPES;
    
    QUARRY_PIT.name = "Quarry Pit";
    QUARRY_PIT.wide = 4;
    QUARRY_PIT.high = 4;
    QUARRY_PIT.tint = colour(7, 7, 3);
    QUARRY_PIT.setWalkerTypes(WORKER);
    QUARRY_PIT.produced = new Good[] { CLAY };
    
    KILN.name = "Kiln";
    KILN.wide = 2;
    KILN.high = 2;
    KILN.tint = colour(7, 3, 7);
    KILN.setWalkerTypes(WORKER);
    KILN.needed   = new Good[] { CLAY };
    KILN.produced = new Good[] { POTTERY };
    KILN.craftTime *= 2;
    
    WEAVER.name = "Weaver";
    WEAVER.wide = 2;
    WEAVER.high = 2;
    WEAVER.tint = colour(7, 3, 7);
    WEAVER.setWalkerTypes(WORKER);
    WEAVER.needed   = new Good[] { RAW_COTTON };
    WEAVER.produced = new Good[] { COTTON };
    WEAVER.craftTime *= 2;
    
    MARKET.name = "Marketplace";
    MARKET.wide = 4;
    MARKET.high = 4;
    MARKET.tint = colour(4, 8, 4);
    MARKET.setWalkerTypes(MERCHANT);
    MARKET.needed   = new Good[] { POTTERY };
    MARKET.features = new Good[] { IS_MARKET };
    
    PORTER_HOUSE.name = "Warehouse";
    PORTER_HOUSE.wide = 3;
    PORTER_HOUSE.high = 3;
    PORTER_HOUSE.tint = colour(8, 4, 8);
    PORTER_HOUSE.setWalkerTypes(PORTERS, WORKER);
    PORTER_HOUSE.features = new Good[] { IS_TRADER };
  }
  
  
  
  /**  Commonly used interfaces-
    */
  static interface Trader {
    Tally <Good> tradeLevel();
    Tally <Good> inventory ();
    City tradeOrigin();
  }
}






