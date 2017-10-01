

package game;
import util.*;


public class TestAllSuite {
  
  
  public static void main(String args[]) {
    
    long init = System.currentTimeMillis();
    
    TestMilitary.testMilitary(false);
    TestSieging .testSieging (false);
    TestCity    .testCity    (false);
    TestTerrain .testTerrain (false);
    TestTrading .testTrading (false);
    
    long taken = System.currentTimeMillis() - init;
    
    I.say("\nTOTAL TIME TO RUN TESTS: "+taken);
  }
}
