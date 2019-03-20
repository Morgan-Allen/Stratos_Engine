

package test;
import game.*;
import util.*;
import static content.GameContent.*;
import static content.GameWorld.*;
import static game.GameConstants.*;



public class TestSpawning extends LogicTest {
  
  
  public static void main(String args[]) {
    testSpawning(true);
  }
  
  
  static boolean testSpawning(boolean graphics) {
    LogicTest test = new TestSpawning();
    
    Base base = setupTestBase(FACTION_SETTLERS_A, ALL_GOODS, 32, true, JUNGLE, MEADOW);
    AreaMap map = base.activeMap();
    World world = map.world;
    
    
    ActorType species[] = { DRONE, TRIPOD };
    Object spawnArgs[] = { TRIPOD, 0.50f, DRONE, 0.50f };
    int totalToSpawn = 4, minPop = 2;
    Tally <ActorType> popCounts = new Tally();
    
    BuildingForNest nest = (BuildingForNest) RUINS_LAIR.generate();
    nest.enterMap(map, 24, 24, 1, map.area.locals);
    //  TODO:  You need to arrange for some other system here!
    //  nest.assignSpawnParameters(DAY_LENGTH, totalToSpawn, true, spawnArgs);
    
    
    Building toRaze = (Building) ENFORCER_BLOC.generate();
    toRaze.enterMap(map, 6, 6, 1, base);
    
    Federation.setPosture(
      base.faction(), map.area.locals.faction(),
      RelationSet.BOND_ENEMY, world
    );
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
      for (Actor a : map.actors()) if (a.base() == nest.base()) {
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
        nestMission = map.area.locals.matchingMission(Mission.OBJECTIVE_STRIKE, toRaze);
        missionInit = nestMission != null;
      }
      
      if (missionInit && ! razingDone) {
        razingDone = TaskCombat.beaten(toRaze);
      }
      
      if (razingDone && ! missionDone) {
        missionDone = nestMission.complete();
      }
      
      if (missionDone && ! testOkay) {
        testOkay = true;
        I.say("\nSPAWNING TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return true;
      }
      
      /*
      if (map.time() > RUN_TIME - 100 && ! graphics) {
        graphics = true;
        world.settings.paused = true;
      }
      //*/
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


