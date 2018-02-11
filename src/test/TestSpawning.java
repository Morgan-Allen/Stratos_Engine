

package test;
import game.*;
import util.*;
import static content.GameContent.*;
import static game.GameConstants.*;



public class TestSpawning extends Test {
  
  
  public static void main(String args[]) {
    testSpawning(false);
  }
  
  
  static boolean testSpawning(boolean graphics) {
    Test test = new TestSpawning();
    
    City base = setupTestCity(32, ALL_GOODS, true, JUNGLE, MEADOW);
    CityMap map = base.activeMap();
    
    
    ActorType species[] = { DRONE, TRIPOD };
    Tally <ActorType> popCounts = new Tally();
    
    BuildingForNest nest = (BuildingForNest) RUINS_LAIR.generate();
    nest.enterMap(map, 24, 24, 1, map.locals);
    
    City.setPosture(base, map.locals, City.POSTURE.ENEMY, true);
    
    
    boolean spawnDone = false;
    boolean testOkay  = false;
    final int RUN_TIME = YEAR_LENGTH;
    
    while(map.time() < RUN_TIME || graphics) {
      test.runLoop(base, 10, graphics, "saves/test_animals.tlt");
      
      popCounts.clear();
      for (Actor a : map.actors()) if (a.homeCity() == nest.homeCity()) {
        popCounts.add(1, a.type());
      }
      
      if (! spawnDone) {
        boolean done = true;
        for (ActorType s : species) if (popCounts.valueFor(s) < 2) done = false;
        spawnDone = done;
      }
      
      if (spawnDone && ! testOkay) {
        testOkay = true;
        I.say("\nSPAWNING TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return true;
      }
    }
    
    I.say("\nSPAWNING TEST FAILED!");
    I.say("  Spawn done: "+spawnDone);
    
    return false;
  }
  
}






