

package game;
import util.*;
import static game.GameConstants.*;



public class TestUpkeep extends TestLoop {
  
  
  public static void main(String args[]) {
    testUpkeep(true);
  }
  
  static void testUpkeep(boolean graphics) {
    
    CityMap map = new CityMap();
    map.performSetup(25);

    BuildingForTrade post = (BuildingForTrade) PORTER_HOUSE.generate();
    post.enterMap(map, 2, 2, 1);
    post.ID = "(Stock of Goods)";
    post.setTradeLevels(true,
      CLAY  , 20,
      ADOBE , 20,
      WOOD  , 30,
      COTTON, 10
    );
    
    BuildingForHome home = (BuildingForHome) HOUSE.generate();
    home.enterMap(map, 6, 6, 0);
    
    
    
    
    boolean upkeepOkay = false;
    
    while (map.time < 1000 || graphics) {
      runGameLoop(map, 10, graphics);
      
      if (! upkeepOkay) {
        boolean allBuilt = true;
        for (Building b : map.buildings) {
          if (b.buildLevel < 1) allBuilt = false;
        }
        
        
        if (upkeepOkay) {
          I.say("\nUPKEEP TEST CONCLUDED SUCCESSFULLY!");
          if (! graphics) return;
        }
      }
    }
    
    I.say("\nUPKEEP TEST FAILED!");
  }
  
}




