


package test;
import game.*;
import util.*;
import static content.GameContent.*;
import static content.GameWorld.*;
import static game.GameConstants.*;
import static game.BuildingForGather.*;




//  TODO:  Test other gathering tasks here as well!


public class TestFarming extends LogicTest {
  
  
  public static void main(String args[]) {
    testFarming(true);
  }
  
  
  static boolean testFarming(boolean graphics) {
    LogicTest test = new TestFarming();
    
    Base base = setupTestBase(BASE, FACTION_SETTLERS_A, ALL_GOODS, 20, true, DESERT, MEADOW, JUNGLE);
    AreaMap map = base.activeMap();
    World world = map.world;
    world.settings.toggleFog     = false;
    world.settings.toggleHunger  = false;
    world.settings.toggleFatigue = false;
    
    BuildingForGather farm = (BuildingForGather) NURSERY.generate();
    farm.enterMap(map, 9, 9, 1, base);
    ActorUtils.fillWorkVacancies(farm);
    AreaPlanning.placeStructure(WALKWAY, base, true, 9, 8, 10, 1);
    
    
    Batch <AreaTile> plantTiles = new Batch();
    for (Plot p : farm.plots()) for (AreaTile t : map.tilesUnder(p)) {
      if (t != null) plantTiles.add(t);
    }
    Good needed[] = farm.type().produced;
    
    
    boolean planted  = false;
    boolean harvest  = false;
    boolean badFocus = false;
    
    while (map.time() < 1000 || graphics) {
      test.runLoop(base, 10, graphics, "saves/test_farming.tlt");
      
      //
      //  Ensure that every actor has exactly one focus-target:
      for (AreaTile t : map.allTiles()) {
        
        int numF = 0;
        for (Active a : t.focused()) {
          if (a.jobType() == Task.JOB.PLANTING) numF++;
          if (a.jobType() == Task.JOB.HARVEST ) numF++;
        }
        
        if (numF > 1) {
          I.say("\nFARMING TEST FAILED- MULTIPLE ACTORS FOCUSED ON POINT:");
          I.say("  "+t+" -> "+t.focused());
          badFocus = true;
          break;
        }
        
        Active a = t.focused().first();
        if (a == null) continue;
        
        Target mainFocus = Task.mainTaskFocus((Element) a);
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
        for (AreaTile t : plantTiles) {
          Element aboveE = map.above(t);
          Type above = aboveE == null ? null : aboveE.type();
          if (above == null || above.growRate == 0) continue;
          numT += 1;
          if (Visit.arrayIncludes(needed, above) && aboveE.growLevel() >= 0) {
            crops.add(aboveE);
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
        for (Good n : needed) if (farm.inventory(n) < 2.5f) {
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
    I.say("  Total gathered: "+farm.inventory());
    return false;
  }
  
}






