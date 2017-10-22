

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class TestGathering extends Test {
  
  
  public static void main(String args[]) {
    testGathering(true);
  }
  
  
  static void testGathering(boolean graphics) {
    
    CityMap map = setupTestCity(20, DESERT, MEADOW, JUNGLE);
    map.settings.toggleFog = false;
    
    BuildingForGather farm = (BuildingForGather) FARM_PLOT.generate();
    farm.enterMap(map, 9, 9, 1);
    fillWorkVacancies(farm);
    CityMap.applyPaving(map, 9, 8, 10, 1, true);
    
    Good needed[] = { MAIZE, RAW_COTTON };
    Tile plantTiles[] = BuildingForGather.applyPlanting(
      map, 6, 6, 10, 10, needed
    );
    
    CityMapFlagging forCrops = map.flagging.get(NEED_PLANT);
    if (plantTiles.length != forCrops.totalSum()) {
      I.say("\nGATHER TEST FAILED- NOT ALL PLANTED TILES WERE FLAGGED");
      I.say("  Flagged: "+forCrops.totalSum()+"/"+plantTiles.length);
      return;
    }
    
    boolean planted = false;
    boolean harvest = false;
    
    while (map.time < 1000 || graphics) {
      runGameLoop(map, 10, graphics, "saves/test_gathering.tlt");
      //
      //  Every surrounding tile needs to be either:
      //  (A) paved,
      //  (B) planted, or
      //  (C) impassible,
      //  ...for this to count as complete.
      if (! planted) {
        Batch <Element> crops = new Batch();
        int numT = 0;
        for (Tile t : plantTiles) {
          Type above = t.above == null ? null : t.above.type;
          if (above == null || above.growRate == 0) continue;
          numT += 1;
          if (Visit.arrayIncludes(needed, above) && t.above.buildLevel() >= 0) {
            crops.add(t.above);
          }
        }
        //
        //  If that's true, we bump up maturation to speed up harvest:
        if (crops.size() > (numT * 0.9f)) {
          planted = true;
          for (Element c : crops) c.setBuildLevel((3 + Rand.num()) / 4);
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
          I.say("\nGATHER TEST CONCLUDED SUCCESSFULLY!");
          if (! graphics) return;
        }
      }
    }
    
    I.say("\nGATHER TEST FAILED!");
    I.say("  Total gathered: "+farm.inventory);
  }
  
}






