


package test;
import game.*;
import static content.GameContent.*;
import static content.GameWorld.*;
import util.*;



public class TestForests extends LogicTest {
  
  
  public static void main(String args[]) {
    testForests(true);
  }
  
  
  static boolean testForests(boolean graphics) {
    LogicTest test = new TestForests();
    
    Base base = setupTestBase(BASE, FACTION_SETTLERS_A, ALL_GOODS, 32, true, JUNGLE, MEADOW);
    AreaMap map = base.activeMap();
    World world = map.world;
    world.settings.toggleFog = false;
    
    Building logs = (Building) HARVESTER.generate();
    logs.enterMap(map, 5, 5, 1, base);
    ActorUtils.fillWorkVacancies(logs);
    
    boolean loggingDone = false;
    
    while (map.time() < 1000 || graphics) {
      test.runLoop(base, 10, graphics, "saves/test_forests.tlt");
      
      if (! loggingDone) {
        loggingDone = logs.inventory(CARBONS) >= 10;
        
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




