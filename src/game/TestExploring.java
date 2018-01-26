


package game;
import util.*;
import static game.GameContent.*;



public class TestExploring extends Test {
  
  
  public static void main(String args[]) {
    testExploring(true);
  }
  
  
  static boolean testExploring(boolean graphics) {
    Test test = new TestExploring();
    
    CityMap map = Test.setupTestCity(32, ALL_GOODS, true, JUNGLE, MEADOW);
    
    CityMapPlanning.markDemolish(map, true, 3, 3, 6, 6);
    Building lodge = (Building) HUNTER_LODGE.generate();
    lodge.enterMap(map, 4, 4, 1);
    Test.fillWorkVacancies(lodge);
    
    CityMapTerrain.populateAnimals(map, TAPIR);
    
    int tilesSeen = 0, tilesOpen = 0;
    boolean exploreOkay = false;
    boolean huntingOkay = false;
    boolean testOkay    = false;
    
    while (map.time < 1000 || graphics) {
      map = test.runLoop(map, 10, graphics, "saves/test_gathering.tlt");
      
      if (! exploreOkay) {
        tilesSeen = 0;
        tilesOpen = 0;
        
        for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
          if (map.blocked(c)) continue;
          tilesOpen += 1;
          if (map.fog.maxSightLevel(map.tileAt(c)) == 1) tilesSeen += 1;
        }
        
        exploreOkay = tilesSeen == tilesOpen;
      }
      
      if (! huntingOkay) {
        huntingOkay = lodge.inventory.valueFor(MEAT) > lodge.type.maxStock;
      }
      
      if (exploreOkay && huntingOkay && ! testOkay) {
        I.say("\nEXPLORING TEST CONCLUDED SUCCESSFULLY!");
        testOkay = true;
        if (! graphics) return true;
      }
    }
    
    I.say("\nEXPLORING TEST FAILED!");
    I.say("  Total tiles seen: "+tilesSeen+"/"+tilesOpen);
    I.say("  Explore Okay: "+exploreOkay);
    I.say("  Hunting Okay: "+huntingOkay);
    return false;
  }
  
}


