


package test;
import game.*;
import util.*;
import static content.GameContent.*;
import static game.GameConstants.*;



public class TestEcology extends LogicTest {
  
  
  public static void main(String args[]) {
    testAnimals(false);
  }
  
  
  static void testAnimals(boolean graphics) {
    LogicTest test = new TestEcology();
    
    Base base = setupTestBase(64, ALL_GOODS, true, JUNGLE, MEADOW);
    Area map = base.activeMap();
    World world = map.world;
    
    world.settings.toggleFog = false;
    ActorAsAnimal.reportCycle = graphics;
    
    ActorType species[] = { VAREEN, MICOVORE };
    Tally <Type> realPops  = new Tally();
    Tally <Type> popLevels = new Tally();
    
    AreaTerrain.populateAnimals(map, species);
    
    final int RUN_TIME = HUNTER_LIFESPAN;
    boolean popFailed = false;
    
    I.say("\nTOTAL ECOLOGY RUN TIME: "+RUN_TIME);
    
    while(map.time() < RUN_TIME || graphics) {
      
      //  We want to be sure that populations remain relatively stable over
      //  time, so we check whether actual populations are within a certain
      //  range compared to their 'ideal' levels.
      
      boolean popsOkay = true;
      for (ActorType s : species) {
        float
          idealPop = AreaTerrain.idealPopulation(s, map),
          minPop   = (idealPop / 2.0f) - 2,
          maxPop   = (idealPop * 1.5f) + 2,
          realPop  = 0;
        
        for (Actor a : map.actors()) {
          if (a.type() == s) realPop++;
        }
        
        realPops .set(s, realPop);
        popLevels.set(s, realPop / idealPop);
        
        if (realPop == 0 || realPop > maxPop || realPop < minPop) {
          popsOkay = false;
        }
      }
      
      if (map.time() % 1000 == 0 || ! popsOkay) {
        I.say("\nTime: "+map.time());
        I.say("  Population levels:");
        for (Type s : species) {
          String percent = I.percent(popLevels.valueFor(s));
          I.say("    "+s+": "+realPops.valueFor(s)+" ("+percent+")");
        }
      }
      
      if (! popsOkay) {
        popFailed = true;
        break;
      }
      
      test.runLoop(base, 100, graphics, "saves/test_animals.tlt");
    }
    
    int grazeFail = (int) ActorAsAnimal.grazeFail;
    if (grazeFail > 0) {
      I.say("\nGraze nearby ratio: "+(ActorAsAnimal.grazeOkay / grazeFail));
    }
    
    if (popFailed) {
      I.say("\nANIMALS TEST FAILED!");
      return;
    }
    
    I.say("\nANIMALS TEST SUCCEEDED!");
  }
  
}






