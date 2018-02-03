

package test;
import game.*;
import game.GameConstants.Target;
import util.*;
import static content.GameContent.*;
import static game.GameConstants.*;



public class TestBounties extends Test {
  
  
  public static void main(String args[]) {
    testAttackBuildingMission(false);
  }
  
  
  //  What are the cases I need to cover here?
  
  //  Attack bounty on building.
  //  Attack bounty on actor.
  //  Defend bounty on building.
  //  Defend bounty on actor.
  //  Explore bounty on area.
  //  Explore bounty on actor.
  //  Contact bounty on actor.
  //  Contact bounty on building.
  
  //  So, 8 scenarios in total.  You'll have to customise the target-entry,
  //  and customise checking for success.
  
  //  But okay.  That is do-able.

  
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
      
      boolean checkVictory(CityMap map, City base, Mission mission) {
        Building nest = (Building) mission.focus();
        return nest.destroyed();
      }
    };
    return test.bountyTest(graphics);
  }
  
  
  
  boolean bountyTest(boolean graphics) {
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
        targetFinished = checkVictory(map, base, mission);
      }
      
      if (targetFinished && ! rewardSplit) {
        float actorFunds = 0;
        for (Actor a : fort.workers()) actorFunds += a.carried(CASH);
        float diff = Nums.abs(initActorFunds + reward - actorFunds);
        if (diff <= 1) rewardSplit = true;
      }
      
      if (rewardSplit && ! testOkay) {
        testOkay = true;
        I.say("\nBOUNTIES TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return true;
      }
    }
    
    I.say("\nBOUNTIES TEST FAILED!");
    I.say("  Funds taken:     "+fundsTaken    );
    I.say("  Mission taken:   "+missionTaken  );
    I.say("  Target finished: "+targetFinished);
    I.say("  Reward split:    "+rewardSplit   );
    
    return false;
  }
  
  
  Mission setupMission(CityMap map, City base) {
    return null;
  }
  
  
  boolean checkVictory(CityMap map, City base, Mission mission) {
    return false;
  }
  
}









