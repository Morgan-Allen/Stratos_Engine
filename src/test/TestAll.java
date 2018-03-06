

package test;
import util.*;


public class TestAll {
  
  static int numTests = 0;
  static int numPass  = 0;
  static int numFail  = 0;
  
  static void record(boolean result) {
    numTests += 1;
    numPass  += result ? 1 : 0;
    numFail  += result ? 0 : 1;
  }
  
  
  public static void main(String args[]) {
    
    long init = System.currentTimeMillis();
    
    record(TestDemands  .testDemands  (false));
    record(TestPathing  .testPathing  (false));
    record(TestPathCache.testPathCache(false));
    record(TestMilitary .testMilitary (false));
    record(TestSieging  .testSieging  (false));
    record(TestDiplomacy.testDiplomacy(false));
    record(TestSpawning .testSpawning (false));
    record(TestCity     .testCity     (false));
    record(TestPurchases.testPurchases(false));
    record(TestFarming  .testFarming  (false));
    record(TestForests  .testForests  (false));
    record(TestTrading  .testTrading  (false));
    record(TestBuilding .testBuilding (false));
    record(TestBridging .testBridging (false));
    record(TestAutoBuild.testAutoBuild(false));
    record(TestExploring.testExploring(false));
    record(TestWorld    .testWorld    (false));
    
    record(TestBounties.testAttackActorMission   (false));
    record(TestBounties.testAttackBuildingMission(false));
    record(TestBounties.testExploreAreaMission   (false));
    
    long taken = System.currentTimeMillis() - init;
    
    I.say("\nTOTAL TIME TO RUN TESTS: "+taken);
    I.say("  Pass/Fail: "+numPass+"/"+numFail);
    I.say("  Pass rate: "+I.percent(numPass * 1f / numTests));
  }
}


