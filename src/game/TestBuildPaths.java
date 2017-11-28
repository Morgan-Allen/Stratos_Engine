
package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class TestBuildPaths extends Test {
  
  

  public static void main(String args[]) {
    testBuildPaths(true);
  }
  
  
  static boolean testBuildPaths(boolean graphics) {
    Test test = new TestBuildPaths();
    
    //  Okay.  I need terrain-elevation.  Deep water.  Shallow water.
    //  Bridge-building to an island.  Aqueducts.
    
    Terrain terrTypes[] = { LAKE, MEADOW, JUNGLE };
    byte terrIDs[] = {
      1, 1, 1, 1,   2, 2, 0, 0,   0, 0, 2, 2,   2, 2, 2, 2
    };
    byte elevation[] = {
      1, 1, 1, 1,   0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0
    };
    CityMap map = setupTestCity(16);
    map.settings.toggleFog = false;
    
    for (Tile t : map.allTiles()) {
      Terrain ter = terrTypes[terrIDs[t.y]];
      int high = elevation[t.y];
      map.setTerrain(t, ter, high);
    }
    for (Tile t : map.tilesUnder(4, 1, 2, 2)) {
      map.setTerrain(t, LAKE, t.elevation);
    }
    
    //
    //  Test to ensure that water can flow along connected aqueducts,
    //  but not uphill:
    Building cistern1 = (Building) CISTERN.generate();
    cistern1.enterMap(map, 1, 0, 1);
    
    Building cistern2 = (Building) CISTERN.generate();
    cistern2.enterMap(map, 12, 10, 1);
    
    Building basin1 = (Building) BASIN.generate();
    basin1.enterMap(map, 1, 13, 1);

    Building basin2 = (Building) BASIN.generate();
    basin2.enterMap(map, 14, 0, 1);
    
    CityMapPlanning.applyStructure(AQUEDUCT, map, 2 , 3, 1, 10, true);
    CityMapPlanning.applyStructure(AQUEDUCT, map, 15, 2, 1, 10, true);
    
    //
    //  Test to ensure that water does not collect in cisterns away
    //  from a water-source:
    Building cistern0 = (Building) CISTERN.generate();
    cistern0.enterMap(map, 8, 0, 1);
    
    //
    //  Run simulation:
    boolean waterOkay = false;
    
    while (map.time < 1000 || graphics) {
      map = test.runLoop(map, 1, graphics, "saves/test_build_path.tlt");
      
      if (! waterOkay) {
        boolean fillsOkay = true;
        fillsOkay &= basin1  .inventory.valueFor(WATER) >= 5;
        fillsOkay &= basin2  .inventory.valueFor(WATER) == 0;
        fillsOkay &= cistern0.inventory.valueFor(WATER) == 0;
        waterOkay = fillsOkay;
        
        if (waterOkay) {
          I.say("\nBuild-Path Test Successful!");
          if (! graphics) return true;
        }
      }
    }
    
    I.say("\nBuild-Path Test Failed!");
    
    return false;
  }
  
}











