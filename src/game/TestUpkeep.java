

package game;
import util.*;
import static game.GameConstants.*;



public class TestUpkeep extends Test {
  
  
  public static void main(String args[]) {
    testUpkeep(false);
  }
  
  
  static boolean testUpkeep(boolean graphics) {
    
    CityMap map = setupTestCity(16);
    map.settings.toggleFog = false;
    
    //  TODO:  Now do this without the starting warehouse.
    
    //  If a building isn't done yet, then you cannot:
    //  Enter the building
    //  Use the building for services
    //  Have the building update regularly.
    
    //  You can only deliver goods to the building-site, up to it's current
    //  stock-limit.
    
    /*
    BuildingForTrade post = (BuildingForTrade) PORTER_HOUSE.generate();
    post.enterMap(map, 2, 2, 1);
    post.ID = "(Stock of Goods)";
    post.setTradeLevels(true,
      CLAY  , 40,
      ADOBE , 40,
      WOOD  , 60,
      COTTON, 20
    );
    //*/
    
    //  Wait a second.  That's not supposed to happen.  My test is broken!
    
    Building home   = (Building) HOUSE .generate();
    Building palace = (Building) PALACE.generate();
    Building mason  = (Building) MASON .generate();
    home  .enterMap(map, 6, 6, 0);
    palace.enterMap(map, 6, 0, 0);
    mason .enterMap(map, 9, 6, 0);
    CityMap.applyPaving(map, 2, 5, 15, 1, true);
    
    for (Building b : map.buildings) fillWorkVacancies(b);
    
    
    boolean upkeepOkay = false;
    
    while (map.time < 1000 || graphics) {
      runGameLoop(map, 10, graphics, "saves/test_upkeep.tlt");
      
      if (! upkeepOkay) {
        boolean allBuilt = true;
        for (Building b : map.buildings) {
          if (b.buildLevel() < 1) allBuilt = false;
        }
        
        upkeepOkay = allBuilt && map.buildings.size() > 0;
        
        if (upkeepOkay) {
          I.say("\nUPKEEP TEST CONCLUDED SUCCESSFULLY!");
          if (! graphics) return true;
        }
      }
    }
    
    I.say("\nUPKEEP TEST FAILED!");
    return false;
  }
  
}




