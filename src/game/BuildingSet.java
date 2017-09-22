

package game;
import util.*;
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
    
    Good(String name, int ID) {
      super("good_"+ID);
      
      GOODS_LIST.add(this);
      this.name   = name ;
      this.blocks = false;
    }
  }
  final static Good
    MAIZE      = new Good("Maize"     , 0 ),
    COTTON     = new Good("Cotton"    , 1 ),
    
    CLAY       = new Good("Clay"      , 4 ),
    POTTERY    = new Good("Pottery"   , 5 ),
    
    IS_MARKET  = new Good("Is Market" , 22),
    IS_AMENITY = new Good("Is Amenity", 23),
    
    CROP_TYPES[] = { MAIZE, COTTON },
    ALL_GOODS [] = (Good[]) GOODS_LIST.toArray(Good.class),
    NO_GOODS  [] = new Good[0];
  
  
  final static ObjectType
    PALACE       = new ObjectType("type_palace"      ),
    HOUSE        = new ObjectType("type_house"       ),
    BALL_COURT   = new ObjectType("type_ball_court"  ),
    
    FARMER_HUT   = new ObjectType("type_farmer_hut"  ),
    QUARRY_PIT   = new ObjectType("type_quarry_pit"  ),
    KILN         = new ObjectType("type_kiln"        ),
    MARKET       = new ObjectType("type_market"      ),
    PORTER_HOUSE = new ObjectType("type_porter_house"),
    
    CITIZEN      = new ObjectType("type_citizen"     ),
    NOBLE        = new ObjectType("type_noble"       ),
    WORKER       = new ObjectType("type_worker"      )
  ;
  
  
  static {
    MAIZE .tint = colour(9, 9, 1);
    COTTON.tint = colour(9, 8, 9);
    
    CITIZEN.name = "Citizen";
    NOBLE  .name = "Noble"  ;
    WORKER .name = "Worker" ;
    
    PALACE.name = "Palace";
    PALACE.wide = 5;
    PALACE.high = 5;
    PALACE.tint = colour(7, 3, 3);
    PALACE.walkerType = NOBLE;
    
    HOUSE.name = "House";
    HOUSE.wide = 2;
    HOUSE.high = 2;
    HOUSE.tint = colour(3, 7, 3);
    HOUSE.walkerType = CITIZEN;
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
    FARMER_HUT.walkerType = WORKER;
    FARMER_HUT.produced = CROP_TYPES;
    
    QUARRY_PIT.name = "Quarry Pit";
    QUARRY_PIT.wide = 4;
    QUARRY_PIT.high = 4;
    QUARRY_PIT.tint = colour(7, 7, 3);
    QUARRY_PIT.walkerType = WORKER;
    QUARRY_PIT.produced = new Good[] { CLAY };
    
    KILN.name = "Kiln";
    KILN.wide = 2;
    KILN.high = 2;
    KILN.tint = colour(7, 3, 7);
    KILN.walkerType = WORKER;
    KILN.needed   = new Good[] { CLAY };
    KILN.produced = new Good[] { POTTERY };
    KILN.craftTime *= 2;
    
    MARKET.name = "Marketplace";
    MARKET.wide = 4;
    MARKET.high = 4;
    MARKET.tint = colour(4, 8, 4);
    MARKET.walkerType = CITIZEN;
    MARKET.needed   = new Good[] { POTTERY };
    MARKET.features = new Good[] { IS_MARKET };
    
    PORTER_HOUSE.name = "Warehouse";
    PORTER_HOUSE.wide = 3;
    PORTER_HOUSE.high = 3;
    PORTER_HOUSE.tint = colour(7, 3, 7);
    PORTER_HOUSE.walkerType = WORKER;
  }
}



