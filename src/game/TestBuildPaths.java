
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
    
    byte layout[][] = {
      { 1, 1, 1, 1,   1, 0, 1, 1,   1, 1, 1, 2,   0, 0, 0, 0 },
      { 1, 1, 1, 1,   1, 0, 0, 1,   2, 1, 2, 2,   0, 0, 0, 0 },
      { 1, 1, 1, 1,   1, 2, 0, 1,   1, 2, 2, 0,   0, 0, 0, 0 },
      { 1, 1, 1, 1,   1, 1, 0, 1,   1, 2, 2, 0,   0, 0, 0, 0 },

      { 1, 1, 1, 1,   1, 1, 0, 1,   1, 2, 2, 0,   0, 0, 0, 0 },
      { 2, 1, 2, 2,   2, 2, 0, 0,   2, 2, 0, 0,   0, 0, 0, 0 },
      { 2, 2, 2, 2,   2, 2, 2, 0,   2, 0, 0, 0,   0, 0, 0, 0 },
      { 2, 2, 0, 0,   0, 2, 2, 0,   0, 0, 0, 0,   0, 0, 0, 0 },
      
      { 0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0 },
      { 0, 0, 0, 0,   0, 0, 0, 0,   2, 2, 0, 0,   0, 0, 0, 0 },
      { 2, 0, 0, 2,   2, 2, 0, 2,   2, 2, 2, 0,   0, 0, 0, 0 },
      { 2, 2, 2, 2,   2, 2, 2, 2,   2, 2, 2, 2,   0, 0, 0, 0 },

      { 2, 2, 2, 2,   2, 2, 2, 2,   2, 2, 2, 2,   2, 0, 0, 0 },
      { 2, 1, 1, 2,   2, 2, 2, 2,   2, 2, 2, 2,   2, 2, 0, 0 },
      { 1, 2, 2, 2,   2, 1, 1, 1,   2, 1, 2, 2,   2, 2, 0, 0 },
      { 1, 1, 1, 1,   2, 1, 1, 2,   2, 2, 2, 2,   2, 0, 0, 0 },
    };
    byte elevation[][] = {
      { 1, 1, 1, 1,   1, 1, 1, 1,   1, 1, 0, 0,   0, 0, 0, 0 },
      { 1, 1, 1, 1,   1, 1, 1, 1,   1, 1, 0, 0,   0, 0, 0, 0 },
      { 1, 1, 1, 1,   1, 1, 1, 1,   1, 0, 0, 0,   0, 0, 0, 0 },
      { 1, 1, 1, 1,   1, 1, 1, 1,   1, 1, 0, 0,   0, 0, 0, 0 },

      { 1, 1, 1, 1,   1, 0, 1, 1,   1, 0, 0, 0,   0, 0, 0, 0 },
      { 0, 1, 0, 1,   1, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0 },
      { 1, 0, 1, 0,   0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0 },
      { 0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0 },

      { 0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0 },
      { 0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0 },
      { 0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0 },
      { 0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0 },

      { 0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0 },
      { 0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0 },
      { 0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0 },
      { 0, 1, 0, 0,   0, 0, 0, 0,   0, 0, 1, 1,   0, 0, 0, 0 },
    };
    
    
    //  Okay.  I need terrain-elevation.  Deep water.  Shallow water.
    //  Bridge-building to an island.  Aqueducts
    
    CityMap map = setupTestCity(layout, elevation, LAKE, MEADOW, JUNGLE);
    map.settings.toggleFog = false;
    
    
    Building cistern = (Building) CISTERN.generate();
    cistern.enterMap(map, 1, 2, 1);
    CityMapPlanning.applyStructure(AQUEDUCT, map, 4, 3, 9, 1, true);
    
    Building basin = (Building) BASIN.generate();
    basin.enterMap(map, 13, 3, 1);
    
    boolean waterOkay = false;
    
    
    while (map.time < 1000 || graphics) {
      map = test.runLoop(map, 1, graphics, "saves/test_build_path.tlt");
      
      if (! waterOkay) {
        waterOkay = basin.inventory.valueFor(WATER) > 5;
        
        if (waterOkay) {
          I.say("\nBuild-Path Test Successful!");
          if (! graphics) return true;
        }
      }
    }
    
    
    //  TODO:  Find a way to indicate higher elevations!
    
    I.say("\nBuild-Path Test Failed!");
    
    return false;
  }
  
}











