

package game;
import util.*;
import static game.ObjectType.*;
import java.awt.Color;



public class BuildingSet {
  
  
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
  
  
  final static ObjectType
    PALACE       = new ObjectType("type_palace"      , IS_HOME_BLD  ),
    HOUSE        = new ObjectType("type_house"       , IS_HOME_BLD  ),
    BALL_COURT   = new ObjectType("type_ball_court"  , IS_BUILDING  ),
    
    FARMER_HUT   = new ObjectType("type_farmer_hut"  , IS_GATHER_BLD),
    QUARRY_PIT   = new ObjectType("type_quarry_pit"  , IS_GATHER_BLD),
    KILN         = new ObjectType("type_kiln"        , IS_CRAFT_BLD ),
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
    MAIZE .tint = colour(9, 9, 1);
    RAW_COTTON.tint = colour(9, 8, 9);
    
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
    PORTER_HOUSE.tint = colour(7, 3, 7);
    PORTER_HOUSE.setWalkerTypes(WORKER, PORTERS);
    PORTER_HOUSE.features = new Good[] { IS_TRADER };
  }
}



