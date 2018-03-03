


package test;
import game.*;
import util.*;
import static content.GameContent.*;



public class TestForests extends Test {
  
  
  public static void main(String args[]) {
    testForests(true);
  }
  
  
  static boolean testForests(boolean graphics) {
    Test test = new TestForests();
    

    Base base = setupTestCity(32, ALL_GOODS, true, JUNGLE, MEADOW);
    AreaMap map = base.activeMap();
    World world = map.world;
    world.settings.toggleFog = false;
    
    Building logs = (Building) HARVESTER.generate();
    logs.enterMap(map, 5, 5, 1, base);
    clearMargins(logs, 2);
    fillWorkVacancies(logs);
    
    boolean loggingDone = false;
    
    while (map.time() < 1000 || graphics) {
      test.runLoop(base, 10, graphics, "saves/test_forests.tlt");
      
      if (! loggingDone) {
        loggingDone = logs.inventory(CARBONS) >= logs.type().maxStock;
        
        if (loggingDone) {
          I.say("\nFOREST TEST CONCLUDED SUCCESSFULLY!");
          if (! graphics) return true;
        }
      }
    }
    
    I.say("\nFORESTS TEST FAILED!");
    I.say("  Total gathered: "+logs.inventory());
    return false;
  }
  
}







