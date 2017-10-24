

package game;
import util.*;




public class TestDemands {
  
  public static void main(String args[]) {
    testDemands(true);
  }
  
  static void testDemands(boolean graphics) {
    CityMap map = Test.setupTestCity(32);
    CityMapDemands demands = new CityMapDemands(map, "AAA");
    
    //
    //  Test insertion-
    demands.setAmount(5 , "<source 1>", 3 , 3 );
    demands.setAmount(2 , "<source 2>", 11, 1 );
    demands.setAmount(12, "<source 3>", 14, 7 );
    demands.setAmount(6 , "<source 4>", 2 , 16);
    
    boolean allOkay = true;
    float amountOut = 0;
    
    amountOut = demands.amountAt(3, 3);
    if (amountOut != 5) {
      I.say("\nWRONG AT 3|3, IS "+amountOut+", SHOULD BE 5");
      allOkay = false;
    }
    
    amountOut = demands.totalAmount();
    if (amountOut != 25) {
      I.say("\nWRONG TOTAL, IS "+amountOut+", SHOULD BE 25");
      allOkay = false;
    }
    
    //
    //  Test basic and proximity queries-
    I.say("Listing in order of proximity to 5/15...");
    for (CityMapDemands.Entry e : demands.nearbyEntries(5, 15)) {
      float dist = CityMap.distance(5, 15, e.x, e.y);
      I.say("  "+e.source+" -> "+e.x+"|"+e.y+", distance: "+dist);
    }
    
    //
    //  Test deletion-
    demands.setAmount(0, null, 11, 1);
    amountOut = demands.amountAt(11, 1);
    if (amountOut != 0) {
      I.say("\nWRONG AT 11|1, IS "+amountOut+", SHOULD BE 0");
      allOkay = false;
    }
    
    amountOut = demands.totalAmount();
    if (amountOut != 23) {
      I.say("\nWRONG TOTAL, IS "+amountOut+", SHOULD BE 23");
      allOkay = false;
    }
    
    //
    //  If that works out, return okay-
    if (allOkay) {
      I.say("\nALL DEMAND-MAP TESTS SUCCESSFUL!");
    }
  }
}






