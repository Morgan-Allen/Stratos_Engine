

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
    
    //  Buildings that allow assignment before construction must be capable of
    //  hiring recruits and assigning (certain) tasks in an unfinished state.
    
    //  They will also have different stock capacities and other traits (like
    //  a 'null upgrade' as it were.)
    
    //  This means the delivery, gather and building-tasks have to be updated
    //  to behave differently- you deliver to and pick up within the building-
    //  grounds instead of entering/exiting the structure.  (And some of the
    //  scripting for craft, gather and trade-buildings needs to be updated.)
    
    
    BuildingForTrade post = (BuildingForTrade) PORTER_POST.generate();
    post.enterMap(map, 2, 2, 0);
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




