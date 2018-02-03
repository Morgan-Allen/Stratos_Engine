

package test;
import game.*;
import util.*;
import static content.GameContent.*;
import static game.GameConstants.*;



public class TestBounties extends Test {
  
  
  public static void main(String args[]) {
    testBounties(false);
  }
  
  
  static boolean testBounties(boolean graphics) {
    TestBounties test = new TestBounties();
    
    City base = Test.setupTestCity(32, ALL_GOODS, false);
    CityMap map = base.activeMap();
    
    int initFunds = 1000, reward = 500;
    base.initFunds(initFunds);
    
    BuildingForArmy fort = (BuildingForArmy) TROOPER_LODGE.generate();
    fort.enterMap(map, 2, 2, 1, base);
    fillWorkVacancies(fort);
    
    float initActorFunds = 0;
    for (Actor a : fort.workers()) initActorFunds += a.carried(CASH);
    
    
    BuildingForNest nest = (BuildingForNest) RUINS_LAIR.generate();
    nest.enterMap(map, 20, 20, 1, map.locals);
    
    Formation mission;
    mission = new Formation(Formation.OBJECTIVE_CONQUER, base, false);
    mission.beginSecuring(nest, 0, map);
    mission.setAsBounty(reward);
    
    
    boolean fundsTaken     = false;
    boolean missionTaken   = false;
    boolean targetFinished = false;
    boolean rewardSplit    = false;
    boolean testOkay       = false;
    
    while (map.time() < 1000 || graphics) {
      test.runLoop(base, 10, graphics, "saves/test_military.tlt");
      
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
        targetFinished = nest.destroyed();
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
  
}









