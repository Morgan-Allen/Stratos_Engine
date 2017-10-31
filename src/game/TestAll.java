

package game;
import util.*;


public class TestAll {
  
  
  public static void main(String args[]) {
    
    long init = System.currentTimeMillis();
    
    TestDemands   .testDemands   (false);
    TestPathing   .testPathing   (false);
    TestMilitary  .testMilitary  (false);
    TestSieging   .testSieging   (false);
    TestCity      .testCity      (false);
    TestGathering .testGathering (false);
    TestTrading   .testTrading   (false);
    TestUpkeep    .testUpkeep    (false);
    TestExploring .testExploring (false);
    TestCityEvents.testCityEvents(false);
    
    long taken = System.currentTimeMillis() - init;
    
    I.say("\nTOTAL TIME TO RUN TESTS: "+taken);
  }
}
