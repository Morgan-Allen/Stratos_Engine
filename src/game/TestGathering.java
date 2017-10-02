

package game;
import static game.GameConstants.*;
import util.*;



public class TestGathering extends Test {
  
  
  public static void main(String args[]) {
    testGathering(true);
  }
  
  static void testGathering(boolean graphics) {
    
    World   world = new World();
    City    city  = new City(world);
    CityMap map   = CityMapGenerator.generateTerrain(
      city, 20, DESERT, MEADOW, JUNGLE
    );
    city.assignMap(map);
    
    Building farm = (Building) FARMER_HUT.generate();
    farm.enterMap(map, 9, 9, 1);
    fillWorkVacancies(farm);
    CityMap.applyPaving(map, 9, 8, 10, 1, true);
    
    for (Coord c : Visit.grid(6, 6, 10, 10, 1)) {
      if (map.blocked(c.x, c.y)) continue;
      if (map.paved  (c.x, c.y)) continue;
      
      Fixture crop;
      if (Rand.yes()) crop = new Fixture(MAIZE     );
      else            crop = new Fixture(RAW_COTTON);
      crop.enterMap(map, c.x, c.y, 1);
      crop.buildLevel = 0.5f + Rand.num();
    }
    
    CityMapGenerator.populateFixtures(map);
    
    Good needed[] = { MAIZE, RAW_COTTON };
    boolean harvest = false;
    
    while (map.time < 1000 || graphics) {
      runGameLoop(map, 10, graphics);
      
      if (! harvest) {
        boolean enough = true;
        for (Good n : needed) if (farm.inventory.valueFor(n) < 5) {
          enough = false;
        }
        harvest = enough;
        
        if (harvest) {
          I.say("\nTERRAIN TEST CONCLUDED SUCCESSFULLY!");
          if (! graphics) return;
        }
      }
    }

    I.say("\nTERRAIN TEST FAILED!");
  }
  
}






