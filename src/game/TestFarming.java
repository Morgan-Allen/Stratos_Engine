

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class TestFarming extends Test {
  
  
  public static void main(String args[]) {
    testFarming(true);
  }
  
  
  static boolean testFarming(boolean graphics) {
    Test test = new TestFarming();
    
    CityMap map = setupTestCity(20, DESERT, MEADOW, JUNGLE);
    map.settings.toggleFog    = false;
    map.settings.toggleHunger = false;
    map.settings.toggleHunger = false;
    
    BuildingForGather farm = (BuildingForGather) FARM_PLOT.generate();
    farm.enterMap(map, 9, 9, 1);
    fillWorkVacancies(farm);
    CityMapPlanning.placeStructure(ROAD, map, true, 9, 8, 10, 1);
    
    Good needed[] = { MAIZE, RAW_COTTON };
    Tile plantTiles[] = BuildingForGather.applyPlanting(
      map, 6, 6, 10, 10, needed
    );
    
    CityMapFlagging forCrops = map.flagging.get(NEED_PLANT);
    if (plantTiles.length != forCrops.totalSum()) {
      I.say("\nFARMING TEST FAILED- NOT ALL PLANTED TILES WERE FLAGGED");
      I.say("  Flagged: "+forCrops.totalSum()+"/"+plantTiles.length);
      return false;
    }
    
    boolean planted  = false;
    boolean harvest  = false;
    boolean badFocus = false;
    
    while (map.time < 1000 || graphics) {
      map = test.runLoop(map, 10, graphics, "saves/test_farming.tlt");
      
      //
      //  Ensure that every actor has exactly one focus-target:
      for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
        
        Tile t = map.tileAt(c);
        
        int numF = 0;
        for (Actor a : t.focused()) {
          if (a.jobType() == Task.JOB.PLANTING) numF++;
          if (a.jobType() == Task.JOB.HARVEST ) numF++;
        }
        
        if (numF > 1) {
          I.say("\nFARMING TEST FAILED- MULTIPLE ACTORS FOCUSED ON POINT:");
          I.say("  "+t+" -> "+t.focused());
          badFocus = true;
          break;
        }
        
        Actor a = t.focused().first();
        if (a == null) continue;
        
        Target mainFocus = Task.focusTarget(a.task);
        if (mainFocus != t) {
          I.say("\nFARMING TEST FAILED- ACTOR-FOCUS WAS NOT REMOVED FROM:");
          I.say("  "+t+", focused by "+a+", now focused on "+mainFocus);
          badFocus = true;
          break;
        }
      }
      
      if (badFocus) {
        break;
      }
      
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
          if (Visit.arrayIncludes(needed, above) && t.above.growLevel() >= 0) {
            crops.add(t.above);
          }
        }
        //
        //  If that's true, we bump up maturation to speed up harvest:
        if (crops.size() > (numT * 0.9f)) {
          planted = true;
          for (Element c : crops) c.setGrowLevel((3 + Rand.num()) / 4);
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
          I.say("\nFARMING TEST CONCLUDED SUCCESSFULLY!");
          if (! graphics) return true;
        }
      }
    }
    
    I.say("\nFARMING TEST FAILED!");
    I.say("  Total gathered: "+farm.inventory);
    return false;
  }
  
}






