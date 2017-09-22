

package game;
import static game.Goods.*;

import java.awt.Color;



public class BuildingSet {
  
  
  private static int colour(int r, int g, int b) {
    return new Color(r / 10f, g / 10f, b / 10f).getRGB();
  }
  
  final static int
    BLANK_COLOR  = colour(5, 5, 5),
    PAVE_COLOR   = colour(8, 8, 8),
    FILL_COLOR   = colour(1, 1, 1),
    WALKER_COLOR = colour(9, 9, 0)
  ;
  
  
  final static ObjectType
    PALACE     = new ObjectType(),
    HOUSE      = new ObjectType(),
    BALL_COURT = new ObjectType(),
    
    QUARRY_PIT = new ObjectType(),
    KILN       = new ObjectType(),
    MARKET     = new ObjectType(),
    WAREHOUSE  = new ObjectType(),
    
    CITIZEN    = new ObjectType(),
    NOBLE      = new ObjectType(),
    WORKER     = new ObjectType()
  ;
  static {
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
    
    WAREHOUSE.name = "Warehouse";
    WAREHOUSE.wide = 3;
    WAREHOUSE.high = 3;
    WAREHOUSE.tint = colour(7, 3, 7);
    WAREHOUSE.walkerType = WORKER;
  }
}



