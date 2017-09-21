
package game;
import util.*;
import java.awt.Color;
import static game.Goods.*;



public class TestCity {
  
  //
  
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
    
    NOBLE.name = "Noble";

    WORKER.name = "Worker";
    
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
    MARKET.needed = new Good[] { POTTERY };
    
    WAREHOUSE.name = "Warehouse";
    WAREHOUSE.wide = 3;
    WAREHOUSE.high = 3;
    WAREHOUSE.tint = colour(7, 3, 7);
    WAREHOUSE.walkerType = WORKER;
  }
  
  
  
  public static void main(String args[]) {
    
    City map = new City();
    map.performSetup(20);
    
    Tile.applyPaving(map, 3, 8, 12, 1 , true);
    Tile.applyPaving(map, 8, 2, 1 , 16, true);

    //Building palace = new Building(PALACE    );
    Building house1 = new Building(HOUSE     );
    Building house2 = new Building(HOUSE     );
    //Building court  = new Building(BALL_COURT);
    
    //palace.enterMap(map, 3 , 3 );
    house1.enterMap(map, 9 , 6 );
    house2.enterMap(map, 12, 6 );
    //court .enterMap(map, 9 , 9 );
    
    Building quarry = new CraftBuilding(QUARRY_PIT);
    Building kiln   = new CraftBuilding(KILN      );
    Building market = new CraftBuilding(MARKET    );
    //Building wares  = new CraftBuilding(WAREHOUSE );
    
    quarry.enterMap(map, 4 , 15);
    kiln  .enterMap(map, 9 , 17);
    market.enterMap(map, 4 , 9 );
    //wares .enterMap(map, 9 , 13);
    
    quarry.inventory.add(2, CLAY);
    
    
    int graphic[][] = new int[map.size][map.size];
    
    while (true) {
      for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
        int fill = BLANK_COLOR;
        Tile at = map.tileAt(c.x, c.y);
        if      (at.above != null) fill = at.above.type.tint;
        else if (at.paved        ) fill = PAVE_COLOR;
        graphic[c.x][c.y] = fill;
      }
      for (Walker w : map.walkers) if (w.inside == null) {
        int fill = WALKER_COLOR;
        if (w.home != null) fill = w.home.type.tint;
        graphic[w.x][w.y] = fill;
      }
      
      I.present(graphic, "City Map", 400, 400);
      
      map.update();
      
      try { Thread.sleep(500); }
      catch (Exception e) {}
    }
  }
  
  
}









