

package game;
import util.*;
import static game.GameConstants.*;



public class TestUpkeep extends Test {
  
  
  public static void main(String args[]) {
    testUpkeep(true);
  }
  
  static void testUpkeep(boolean graphics) {
    GameSettings.toggleFog = false;
    
    CityMap map = setupTestCity(25);
    
    BuildingForTrade post = (BuildingForTrade) PORTER_HOUSE.generate();
    post.enterMap(map, 2, 2, 1);
    post.ID = "(Stock of Goods)";
    post.setTradeLevels(true,
      CLAY  , 40,
      ADOBE , 40,
      WOOD  , 60,
      COTTON, 20
    );
    
    Building home   = (Building) HOUSE .generate();
    Building palace = (Building) PALACE.generate();
    Building mason  = (Building) MASON .generate();
    home  .enterMap(map, 6, 6, 0);
    palace.enterMap(map, 6, 0, 0);
    mason .enterMap(map, 9, 6, 1);
    CityMap.applyPaving(map, 2, 5, 20, 1, true);
    
    
    boolean upkeepOkay = false;
    
    while (map.time < 1000 || graphics) {
      runGameLoop(map, 10, graphics, "saves/test_upkeep.tlt");
      
      if (! upkeepOkay) {
        boolean allBuilt = true;
        for (Building b : map.buildings) {
          if (b.buildLevel < 1) allBuilt = false;
        }
        
        upkeepOkay = allBuilt;
        
        if (upkeepOkay) {
          I.say("\nUPKEEP TEST CONCLUDED SUCCESSFULLY!");
          if (! graphics) return;
        }
      }
    }
    
    I.say("\nUPKEEP TEST FAILED!");
  }
  
}




