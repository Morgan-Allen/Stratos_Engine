

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



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
    CityMapGenerator.populateFixtures(map);
    
    
    BuildingForGather farm = (BuildingForGather) FARMER_HUT.generate();
    farm.enterMap(map, 9, 9, 1);
    fillWorkVacancies(farm);
    CityMap.applyPaving(map, 9, 8, 10, 1, true);
    
    
    Good needed[] = { MAIZE, RAW_COTTON };
    farm.cropLevels.set(MAIZE, 0.5f);
    farm.cropLevels.set(RAW_COTTON, 0.5f);
    
    boolean planted = false;
    boolean harvest = false;
    
    while (map.time < 1000 || graphics) {
      runGameLoop(map, 10, graphics);
      //
      //  Every surrounding tile needs to be either:
      //  (A) paved,
      //  (B) planted, or
      //  (C) impassible,
      //  ...for this to count as complete.
      if (! planted) {
        Batch <Fixture> crops = new Batch();
        int numT = 0;
        for (Coord c : Visit.grid(farm.fullArea())) {
          Tile t = map.tileAt(c.x, c.y);
          
          if (t == null || t.paved) continue;
          ObjectType above = t.above == null ? null : t.above.type;
          if (above != null && above.growRate == 0) continue;
          
          numT += 1;
          if (Visit.arrayIncludes(needed, above)) crops.add(t.above);
        }
        //
        //  If that's true, we bump up maturation to speed up harvest:
        if (crops.size() > (numT * 0.9f)) {
          planted = true;
          for (Fixture c : crops) c.buildLevel = (3 + Rand.num()) / 4;
        }
      }
      //
      //  Then actual harvesting needs to proceed correctly:
      if (planted && ! harvest) {
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
    I.say("  Total gathered: "+farm.inventory);
  }
  
}






