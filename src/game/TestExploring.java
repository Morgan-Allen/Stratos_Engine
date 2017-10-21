

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class TestExploring extends Test {
  
  
  public static void main(String args[]) {
    testExploring(true);
  }
  
  
  static void testExploring(boolean graphics) {
    
    CityMap map = Test.setupTestCity(40, JUNGLE, MEADOW);
    
    Building lodge = (Building) HUNTER_LODGE.generate();
    lodge.enterMap(map, 4, 4, 1);
    Test.fillWorkVacancies(lodge);
    
    boolean explored = false;
    int tilesSeen = 0, tilesOpen = 0;
    
    while (map.time < 1000 || graphics) {
      runGameLoop(map, 10, graphics, "saves/test_gathering.tlt");
      
      if (! explored) {
        
        tilesSeen = 0;
        tilesOpen = 0;
        for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
          if (map.blocked(c)) continue;
          tilesOpen += 1;
          if (map.fog.maxSightLevel(map.tileAt(c)) == 1) tilesSeen += 1;
        }
        
        explored = tilesSeen == tilesOpen;
        
        if (explored) {
          I.say("\nEXPLORING TEST CONCLUDED SUCCESSFULLY!");
          if (! graphics) return;
        }
      }
    }
    
    I.say("\nEXPLORING TEST FAILED!");
    I.say("  Total tiles seen: "+tilesSeen+"/"+tilesOpen);
  }
  
}






