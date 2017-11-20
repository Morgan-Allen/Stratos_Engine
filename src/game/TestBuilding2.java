

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class TestBuilding2 extends Test {
  
  
  public static void main(String args[]) {
    testBuilding2(true);
  }
  
  
  static boolean testBuilding2(boolean graphics) {
    
    CityMap map = setupTestCity(16);
    map.settings.toggleFog = false;
    
    Building mason = (Building) MASON.generate();
    mason.enterMap(map, 9, 6, 1);
    fillWorkVacancies(mason);
    
    Batch <Tile> toPave = new Batch();
    for (Coord c : Visit.grid(2, 2, 10, 1, 1)) {
      Tile t = map.tileAt(c);
      map.planning.placeObject(ROAD, t);
      toPave.add(t);
    }
    boolean buildingOkay = false;
    
    
    while (map.time < 1000 || graphics) {
      runGameLoop(map, 10, graphics, "saves/test_building.tlt");
      
      if (! buildingOkay) {
        boolean allBuilt = true;
        for (Tile t : toPave) {
          if (t.above == null || t.above.type != ROAD) {
            allBuilt = false;
          }
        }
        
        if (allBuilt) {
          I.say("\nBUILDING TEST CONCLUDED SUCCESSFULLY!");
          buildingOkay = true;
          if (! graphics) return true;
        }
      }
    }
    
    I.say("\nBUILDING TEST FAILED!");
    return false;
  }
  
}




