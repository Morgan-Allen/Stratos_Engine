


package game;
import util.*;
import static game.GameConstants.*;



public class TestEcology extends Test {
  
  
  public static void main(String args[]) {
    testAnimals(true);
  }
  
  
  static void testAnimals(boolean graphics) {
    
    CityMap map = Test.setupTestCity(100, JUNGLE, MEADOW);
    
    Type species[] = { QUAIL, JAGUAR };
    
    CityMapTerrain.populateAnimals(map, species);
    boolean popFailed = false;
    
    
    while(map.time < YEAR_LENGTH || graphics) {
      boolean popsOkay = true;
      
      for (Type s : species) {
        int pop = 0;
        for (Actor a : map.walkers) {
          if (a.type == s) pop++;
        }
        
        if (pop == 0) popsOkay = false;
      }
      
      if (! popsOkay) {
        popFailed = true;
        break;
      }
      
      Test.runGameLoop(map, 100, graphics, "saves/test_animals.tlt");
    }
    
    if (popFailed) {
      I.say("\nANIMALS TEST FAILED- Insufficient population!");
      return;
    }
    
    I.say("\nANIMALS TEST SUCCEEDED!");
  }
  
  
  
}



