

import util.*;
import java.awt.Color;



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
    CITIZEN    = new ObjectType()
  ;
  static {
    CITIZEN.name = "Citizen";
    
    PALACE.name = "Palace";
    PALACE.wide = 5;
    PALACE.high = 5;
    PALACE.basicColor = colour(7, 3, 3);
    
    HOUSE.name = "House";
    HOUSE.wide = 2;
    HOUSE.high = 2;
    HOUSE.basicColor = colour(3, 7, 3);
    HOUSE.walkerType = CITIZEN;
    
    BALL_COURT.name = "Ball Court";
    BALL_COURT.wide = 3;
    BALL_COURT.high = 3;
    BALL_COURT.basicColor = colour(3, 3, 7);
  }
  
  
  
  public static void main(String args[]) {
    
    City map = new City();
    map.performSetup(20);
    
    Building palace = new Building(PALACE    );
    Building house1 = new Building(HOUSE     );
    Building house2 = new Building(HOUSE     );
    Building court  = new Building(BALL_COURT);
    
    palace.enterMap(map, 3 , 3);
    house1.enterMap(map, 9 , 6);
    house2.enterMap(map, 12, 6);
    court .enterMap(map, 9 , 9);
    
    Tile.applyPaving(map, 3, 8, 12, 1 , true);
    Tile.applyPaving(map, 8, 2, 1 , 16, true);
    
    
    int graphic[][] = new int[map.size][map.size];
    
    while (true) {
      for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
        int fill = BLANK_COLOR;
        Tile at = map.tileAt(c.x, c.y);
        if      (at.above != null) fill = at.above.type.basicColor;
        else if (at.paved        ) fill = PAVE_COLOR;
        graphic[c.x][c.y] = fill;
      }
      for (Walker w : map.walkers) if (w.inside == null) {
        int fill = WALKER_COLOR;
        if (w.home != null) fill = w.home.type.basicColor;
        graphic[w.x][w.y] = fill;
      }
      
      I.present(graphic, "City Map", 400, 400);
      
      map.update();
      
      try { Thread.sleep(500); }
      catch (Exception e) {}
    }
  }
  
  
}









