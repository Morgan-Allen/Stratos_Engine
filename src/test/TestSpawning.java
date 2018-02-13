

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
    World world = map.world;
    
    
    ActorType species[] = { DRONE, TRIPOD };
    Object spawnArgs[] = { TRIPOD, 0.50f, DRONE, 0.50f };
    int totalToSpawn = 4, minPop = 2;
    Tally <ActorType> popCounts = new Tally();
    
    BuildingForNest nest = (BuildingForNest) RUINS_LAIR.generate();
    nest.enterMap(map, 24, 24, 1, map.locals);
    nest.assignSpawnParameters(MONTH_LENGTH, totalToSpawn, spawnArgs);
    
    Building toRaze = (Building) ENFORCER_BLOC.generate();
    toRaze.enterMap(map, 6, 6, 1, base);
    
    City.setPosture(base, map.locals, City.POSTURE.ENEMY, true);
    world.settings.toggleFog = false;
    
    boolean spawnDone   = false;
    boolean missionInit = false;
    boolean razingDone  = false;
    boolean missionDone = false;
    boolean testOkay    = false;
    Mission nestMission = null;
    
    final int RUN_TIME = YEAR_LENGTH;
    
    while(map.time() < RUN_TIME || graphics) {
      test.runLoop(base, 10, graphics, "saves/test_animals.tlt");
      
      popCounts.clear();
      for (Actor a : map.actors()) if (a.homeCity() == nest.homeCity()) {
        popCounts.add(1, a.type());
      }
      
      if (! spawnDone) {
        boolean done = true;
        for (ActorType s : species) {
          if (popCounts.valueFor(s) < minPop) done = false;
        }
        spawnDone = done;
      }
      
      if (spawnDone && ! missionInit) {
        boolean missionMatch = false;
        for (Mission m : map.locals.missions()) {
          if (m.focus() == toRaze && m.objective == Mission.OBJECTIVE_CONQUER) {
            nestMission = m;
            missionMatch = true;
          }
        }
        missionInit = missionMatch;
      }
      
      if (missionInit && ! razingDone) {
        razingDone = toRaze.destroyed();
      }
      
      if (razingDone && ! missionDone) {
        missionDone = nestMission.complete();
      }
      
      if (missionDone && ! testOkay) {
        testOkay = true;
        I.say("\nSPAWNING TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return true;
      }
    }
    
    I.say("\nSPAWNING TEST FAILED!");
    I.say("  Spawn done:   "+spawnDone  );
    I.say("  Mission init: "+missionInit);
    I.say("  Razing done:  "+razingDone );
    I.say("  Mission done: "+missionDone);
    I.say("  Population counts: "+popCounts);
    
    return false;
  }
  
}

