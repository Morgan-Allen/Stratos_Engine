

package game;
import util.*;
import static game.GameConstants.*;



public class TestLifeCycle extends Test {
  
  
  public static void main(String args[]) {
    testLifeCycle(false);
  }
  
  
  static void testLifeCycle(boolean graphics) {
    Test test = new TestLifeCycle();
    
    CityMap map = setupTestCity(10);
    map.settings.toggleFog = false;
    
    for (int x = 7; x > 0; x -= 3) {
      for (int y = 7; y > 0; y -= 3) {
        Type type = y == 7 ? HOUSE : KILN;
        Building built = (Building) type.generate();
        built.enterMap(map, x, y, 1);
      }
      CityMapPlanning.applyStructure(ROAD, map, x - 1, 0, 1, 10, true);
    }
    CityMapPlanning.applyStructure(ROAD, map, 0, 0, 10, 1, true);
    
    final int RUN_TIME = LIFESPAN_LENGTH;
    boolean migrated = false;
    List <Actor> originalPop = null;
    List <Actor> births = new List();
    List <Actor> deaths = new List();
    boolean noBadJobs = true ;
    boolean cycled    = false;
    
    I.say("\nTOTAL LIFE CYCLE RUN TIME: "+RUN_TIME);
    
    while (map.time < RUN_TIME || graphics) {
      map = test.runLoop(map, 100, graphics, "saves/test_cycle.tlt");
      
      if (map.time % 1000 == 0) {
        I.say("  Time: "+map.time);
      }
      
      if (! migrated) {
        boolean allFilled = true;
        for (Building b : map.buildings) {
          for (Type t : b.type.workerTypes) {
            if (b.numWorkers(t) < b.maxWorkers(t)) allFilled = false;
          }
        }
        if (allFilled) {
          migrated = true;
          originalPop = map.actors.copy();
          int i = 0;
          for (Actor w : originalPop) {
            w.sexData = ((i++ % 2) == 0) ? Actor.SEX_FEMALE : Actor.SEX_MALE;
          }
        }
      }
      
      if (migrated) {
        
        for (Building b : map.buildings) {
          if (b.type == KILN) {
            b.inventory.set(CLAY   , 5);
            b.inventory.set(POTTERY, 5);
          }
          if (b.type == HOUSE) {
            b.inventory.set(MAIZE  , 5);
            b.inventory.set(FRUIT  , 5);
            b.inventory.set(POTTERY, 5);
            b.inventory.set(COTTON , 5);
          }
        }
        
        //  TODO:  Restore this later- it may take time to update employment
        //  at a given age!
        /*
        for (Walker w : map.walkers) {
          if (w.work != null && ! w.adult()) {
            noBadJobs = false;
            I.say("  "+w+" has bad job: "+w.work+", age: "+w.ageYears());
          }
        }
        //*/
        if (! noBadJobs) {
          break;
        }
        
        for (Actor w : map.actors) {
          if (w.child() && w.alive() && ! originalPop.includes(w)) {
            if (! births.includes(w)) I.say("  Born: "+w);
            births.include(w);
          }
        }
        for (Actor w : originalPop) {
          if (w.dead() && ! map.actors.includes(w)) {
            if (! deaths.includes(w)) I.say("  Died: "+w);
            deaths.include(w);
          }
        }
        
        if (births.size() >= 2 && deaths.size() >= 2 && noBadJobs) {
          cycled = true;
        }
        if (cycled) {
          I.say("\nLIFE CYCLE TEST CONCLUDED SUCCESSFULLY!");
          I.say("  Births: "+births);
          I.say("  Deaths: "+deaths);
          if (! graphics) return;
        }
      }
    }
    
    I.say("\nLIFE CYCLE TEST FAILED!");
    I.say("  Births:   "+births);
    I.say("  Deaths:   "+deaths);
    I.say("  Bad jobs? "+(! noBadJobs));
  }
  
}





