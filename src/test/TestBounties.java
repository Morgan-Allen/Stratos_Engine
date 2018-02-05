

package test;
import game.*;
import util.*;
import static content.GameContent.*;
import static game.GameConstants.*;



public class TestBounties extends Test {
  
  
  public static void main(String args[]) {
    testAttackBuildingMission(false);
    testAttackActorMission   (false);
    testExploreAreaMission   (false);
  }
  
  
  //  What are the cases I need to cover here?
  
  //  Defend bounty on building.
  //  Defend bounty on actor.
  //  Explore bounty on actor.
  //  Contact bounty on actor.
  //  Contact bounty on building.

  
  static boolean testAttackBuildingMission(boolean graphics) {
    TestBounties test = new TestBounties() {
      
      Mission setupMission(CityMap map, City base) {
        BuildingForNest nest = (BuildingForNest) RUINS_LAIR.generate();
        nest.enterMap(map, 20, 20, 1, map.locals);
        
        Mission mission;
        mission = new Mission(Mission.OBJECTIVE_CONQUER, base, false);
        mission.setFocus(nest, 0, map);
        return mission;
      }
      
      boolean checkVictory(CityMap map, City base, Object focus) {
        Building nest = (Building) focus;
        return nest.destroyed();
      }
    };
    return test.bountyTest(graphics, "ATTACK BUILDING MISSION");
  }

  
  static boolean testAttackActorMission(boolean graphics) {
    TestBounties test = new TestBounties() {
      
      Mission setupMission(CityMap map, City base) {
        Actor creature = (Actor) MICOVORE.generate();
        creature.enterMap(map, 20, 20, 1, map.locals);
        creature.takeDamage(creature.maxHealth() * 0.7f);
        
        Mission mission;
        mission = new Mission(Mission.OBJECTIVE_CONQUER, base, false);
        mission.setFocus(creature, 0, map);
        return mission;
      }
      
      boolean checkVictory(CityMap map, City base, Object focus) {
        Actor creature = (Actor) focus;
        return creature.destroyed();
      }
    };
    return test.bountyTest(graphics, "ATTACK ACTOR MISSION");
  }
  
  
  static boolean testExploreAreaMission(boolean graphics) {
    TestBounties test = new TestBounties() {
      
      int RANGE = 8;
      
      Mission setupMission(CityMap map, City base) {
        Tile looks = map.tileAt(20, 20);
        
        Mission mission;
        mission = new Mission(Mission.OBJECTIVE_RECON, base, false);
        mission.setFocus(looks, 0, map);
        mission.setExploreRange(RANGE);
        return mission;
      }
      
      boolean checkVictory(CityMap map, City base, Object focus) {
        Tile looks = (Tile) focus;
        int r = RANGE;
        boolean allSeen = true;
        
        for (Tile t : map.tilesUnder(looks.x - r, looks.y - r, r * 2, r * 2)) {
          float dist = CityMap.distance(looks, t);
          if (dist > r) continue;
          if (map.fog.maxSightLevel(t) == 0) allSeen = false;
        }
        return allSeen;
      }
    };
    return test.bountyTest(graphics, "EXPLORE AREA MISSION");
  }
  
  
  
  boolean bountyTest(boolean graphics, String title) {
    
    City base = Test.setupTestCity(32, ALL_GOODS, false);
    CityMap map = base.activeMap();
    
    int initFunds = 1000, reward = 500;
    base.initFunds(initFunds);
    
    BuildingForArmy fort = (BuildingForArmy) TROOPER_LODGE.generate();
    fort.enterMap(map, 2, 2, 1, base);
    fillWorkVacancies(fort);
    
    float initActorFunds = 0;
    for (Actor a : fort.workers()) initActorFunds += a.carried(CASH);
    
    Mission mission = setupMission(map, base);
    mission.setAsBounty(reward);
    Object focus = mission.focus();
    
    
    map.update();
    Actor sample = fort.workers().first();
    Task given = mission.selectActorBehaviour(sample);
    
    if (given == null) {
      I.say("\n"+title+" TEST FAILED!");
      I.say("  No task provided for actor: "+sample);
      return false;
    }
    
    
    boolean fundsTaken     = false;
    boolean missionTaken   = false;
    boolean targetFinished = false;
    boolean rewardSplit    = false;
    boolean testOkay       = false;
    
    while (map.time() < 1000 || graphics) {
      runLoop(base, 10, graphics, "saves/test_military.tlt");
      
      if (! fundsTaken) {
        fundsTaken = mission.cashReward() == initFunds - base.funds();
      }
      
      if (fundsTaken && ! missionTaken) {
        boolean allOn = true, someOn = false;
        for (Actor a : fort.workers()) {
          if (a.formation() == mission) someOn = true;
          else allOn = false;
        }
        missionTaken = allOn && someOn;
      }
      
      if (missionTaken && ! targetFinished) {
        targetFinished = checkVictory(map, base, focus);
      }
      
      if (targetFinished && ! rewardSplit) {
        float actorFunds = 0;
        for (Actor a : fort.workers()) actorFunds += a.carried(CASH);
        float diff = Nums.abs(initActorFunds + reward - actorFunds);
        if (diff <= 1) rewardSplit = true;
      }
      
      if (rewardSplit && ! testOkay) {
        testOkay = true;
        I.say("\n"+title+" TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return true;
      }
    }
    
    I.say("\n"+title+" TEST FAILED!");
    I.say("  Funds taken:     "+fundsTaken    );
    I.say("  Mission taken:   "+missionTaken  );
    I.say("  Target finished: "+targetFinished);
    I.say("  Reward split:    "+rewardSplit   );
    
    return false;
  }
  
  
  Mission setupMission(CityMap map, City base) {
    return null;
  }
  

  boolean checkVictory(CityMap map, City base, Object focus) {
    return false;
  }
  
}








