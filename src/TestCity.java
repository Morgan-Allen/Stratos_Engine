

import util.*;
import java.awt.Color;



public class TestCity {

  
  final static int
    BLANK_COLOR  = new Color(0.5f, 0.5f, 0.5f).getRGB(),
    PAVE_COLOR   = new Color(0.8f, 0.8f, 0.8f).getRGB(),
    FILL_COLOR   = new Color(0.1f, 0.1f, 0.1f).getRGB(),
    WALKER_COLOR = new Color(1.0f, 1.0f, 0.0f).getRGB()
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
    
    HOUSE.name = "House";
    HOUSE.wide = 2;
    HOUSE.high = 2;
    HOUSE.walkerType = CITIZEN;
    
    BALL_COURT.name = "Ball Court";
    BALL_COURT.wide = 3;
    BALL_COURT.high = 3;
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
        if (map.blocked(c.x, c.y)) fill = FILL_COLOR;
        if (map.paved  (c.x, c.y)) fill = PAVE_COLOR;
        graphic[c.x][c.y] = fill;
      }
      for (Walker w : map.walkers) if (w.inside == null) {
        graphic[w.x][w.y] = WALKER_COLOR;
      }
      
      I.present(graphic, "City Map", 400, 400);
      
      map.update();
      
      try { Thread.sleep(500); }
      catch (Exception e) {}
    }
  }
  
  
}









