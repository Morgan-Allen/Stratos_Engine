
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
    
    CityMap map = setupTestCity(16);
    map.settings.toggleFog     = false;
    map.settings.toggleFatigue = false;
    map.settings.toggleHunger  = false;
    
    //
    //  Configure some artificially partitioned terrain:
    Terrain terrTypes[] = { LAKE, MEADOW, JUNGLE };
    byte terrIDs[] = {
      1, 1, 1, 1,   2, 2, 0, 0,   0, 0, 2, 2,   2, 2, 2, 2
    };
    byte elevation[] = {
      1, 1, 1, 1,   0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0
    };
    for (Tile t : map.allTiles()) {
      Terrain ter = terrTypes[terrIDs[t.y]];
      int high = elevation[t.y];
      map.setTerrain(t, ter, high);
    }
    for (Tile t : map.tilesUnder(4, 1, 2, 2)) {
      map.setTerrain(t, LAKE, t.elevation);
    }
    
    //
    //  Set up some essential construction facilities:
    Building mason = (Building) MASON.generate();
    mason.enterMap(map, 8, 12, 1);
    fillWorkVacancies(mason);
    
    //
    //  Test to ensure that water does not collect in cisterns away
    //  from a water-source:
    Building cistern0 = (Building) CISTERN.generate();
    map.planning.placeObject(cistern0, 8, 0);
    
    //
    //  Test to ensure that water can flow along connected aqueducts,
    //  but not uphill:
    Building cistern1 = (Building) CISTERN.generate();
    map.planning.placeObject(cistern1, 1, 0);
    
    Building cistern2 = (Building) CISTERN.generate();
    map.planning.placeObject(cistern2, 12, 10);
    
    Building basin1 = (Building) BASIN.generate();
    map.planning.placeObject(basin1, 1, 13);
    
    Building basin2 = (Building) BASIN.generate();
    map.planning.placeObject(basin2, 14, 0);
    
    CityMapPlanning.placeStructure(AQUEDUCT, map, false , 2, 3, 1, 10);
    CityMapPlanning.placeStructure(AQUEDUCT, map, false, 15, 2, 1, 10);
    
    //
    //  Run simulation:
    boolean buildOkay = false;
    boolean waterOkay = false;
    
    while (map.time < 1000 || graphics) {
      map = test.runLoop(map, 1, graphics, "saves/test_build_path.tlt");
      
      for (Good g : mason.type.buildsWith) {
        mason.inventory.set(g, 10);
      }
      
      if (! buildOkay) {
        boolean buildDone = true;
        for (Tile t : map.allTiles()) {
          if (t.above == null || t.above.type.isNatural()) continue;
          if (t.above.buildLevel() < 1) buildDone = false;
        }
        buildOkay = buildDone;
      }
      
      if (buildOkay && ! waterOkay) {
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











