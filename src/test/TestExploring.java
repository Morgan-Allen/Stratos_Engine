


package test;
import game.*;
import util.*;
import static content.GameContent.*;



public class TestExploring extends Test {
  
  
  public static void main(String args[]) {
    testExploring(true);
  }
  
  
  static boolean testExploring(boolean graphics) {
    Test test = new TestExploring();

    City base = setupTestCity(16, ALL_GOODS, true, JUNGLE, MEADOW);
    CityMap map = base.activeMap();
    World world = map.world;
    
    CityMapPlanning.markDemolish(map, true, 3, 3, 6, 6);
    Building lodge = (Building) ECOLOGIST_STATION.generate();
    lodge.enterMap(map, 4, 4, 1, base);
    Test.fillWorkVacancies(lodge);
    
    CityMapTerrain.populateAnimals(map, QUDU);
    
    int tilesSeen = 0, tilesOpen = 0;
    boolean exploreOkay = false;
    boolean huntingOkay = false;
    boolean testOkay    = false;
    
    while (map.time() < 1000 || graphics) {
      test.runLoop(base, 10, graphics, "saves/test_gathering.tlt");
      
      if (! exploreOkay) {
        tilesSeen = 0;
        tilesOpen = 0;
        
        for (Tile t : map.allTiles()) {
          if (map.blocked(t)) continue;
          tilesOpen += 1;
          if (map.fog.maxSightLevel(t) == 1) tilesSeen += 1;
        }
        
        exploreOkay = tilesSeen == tilesOpen;
      }
      
      if (! huntingOkay) {
        huntingOkay = lodge.inventory(PROTEIN) > lodge.type().maxStock;
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


