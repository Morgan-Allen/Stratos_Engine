

package game;
import util.*;
import static game.GameConstants.*;



public class TestBuilding extends Test {
  
  
  public static void main(String args[]) {
    testBuilding(true);
  }
  
  
  static boolean testBuilding(boolean graphics) {
    
    CityMap map = setupTestCity(16);
    map.settings.toggleFog = false;
    
    BuildingForTrade post = (BuildingForTrade) PORTER_POST.generate();
    post.enterMap(map, 2, 2, 0);
    post.ID = "(Stock of Goods)";
    post.setTradeLevels(true,
      CLAY  , 40,
      STONE , 40,
      WOOD  , 60,
      COTTON, 20
    );
    
    Building home   = (Building) HOUSE .generate();
    Building palace = (Building) PALACE.generate();
    Building mason  = (Building) MASON .generate();
    home  .enterMap(map, 6, 6, 0);
    palace.enterMap(map, 6, 0, 0);
    mason .enterMap(map, 9, 6, 0);
    CityMapPlanning.applyPaving(map, 2, 5, 15, 1, true);
    
    for (Building b : map.buildings) fillWorkVacancies(b);
    
    
    boolean buildingOkay = false;
    
    while (map.time < 1000 || graphics) {
      runGameLoop(map, 10, graphics, "saves/test_building.tlt");
      
      if (! buildingOkay) {
        boolean allBuilt = true;
        for (Building b : map.buildings) {
          if (b.buildLevel() < 1) allBuilt = false;
        }
        
        buildingOkay = allBuilt && map.buildings.size() > 0;
        
        if (buildingOkay) {
          I.say("\nBUILDING TEST CONCLUDED SUCCESSFULLY!");
          if (! graphics) return true;
        }
      }
    }
    
    I.say("\nBUILDING TEST FAILED!");
    for (Building b : map.buildings) {
      I.say("  "+b+": "+I.percent(b.buildLevel()));
    }
    
    return false;
  }
  
}




