


package game;
import static game.GameConstants.*;
import util.*;



public class TestForests extends Test {
  
  
  public static void main(String args[]) {
    testForests(true);
  }
  
  
  static boolean testForests(boolean graphics) {
    
    CityMap map = setupTestCity(32, JUNGLE, MEADOW);
    map.settings.toggleFog = false;
    
    Building logs = (Building) LOGGER.generate();
    logs.enterMap(map, 5, 5, 1);
    fillWorkVacancies(logs);
    
    boolean loggingDone = false;
    
    while (map.time < 1000 || graphics) {
      runGameLoop(map, 10, graphics, "saves/test_forests.tlt");
      
      if (! loggingDone) {
        loggingDone = logs.inventory.valueFor(WOOD) >= logs.type.maxStock;
      }
      
      if (loggingDone) {
        I.say("\nFOREST TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return true;
      }
    }
    
    I.say("\nFORESTS TEST FAILED!");
    I.say("  Total gathered: "+logs.inventory);
    return false;
  }
  
}







