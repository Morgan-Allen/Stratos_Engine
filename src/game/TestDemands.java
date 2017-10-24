

package game;
import util.*;
import static game.GameConstants.*;



public class TestDemands {
  
  public static void main(String args[]) {
    testDemands(true);
  }
  
  static void testDemands(boolean graphics) {
    CityMap map = Test.setupTestCity(32);
    CityMapDemands demands = new CityMapDemands(map, "AAA");
    
    
    class TestItem {
      int x, y, amount;
      public String toString() { return x+"|"+y; }
    }
    
    List <TestItem> items = new List();
    int trueSum = 0;
    
    for (int n = 16; n-- > 0;) {
      TestItem i = new TestItem();
      i.x = Rand.index(32);
      i.y = Rand.index(32);
      
      if (map.above(i.x, i.y) != null) continue;
      Element e = new Element(JUNGLE_TREE1);
      e.enterMap(map, i.x, i.y, 1);
      
      i.amount = 1 + Rand.index(4);
      trueSum += i.amount;
      items.add(i);
      
    }
    
    //
    //  Test insertion-
    for (TestItem i : items) {
      demands.setAmount(i.amount, i, i.x, i.y);
    }
    
    boolean allOkay = true;
    
    for (TestItem i : items) {
      float amount = demands.amountAt(i.x, i.y);
      if (amount != i.amount) {
        I.say("\nWRONG AT "+i+" IS "+amount+", SHOULD BE "+i.amount);
        allOkay = false;
      }
    }
    
    float demandSum = demands.totalAmount();
    if (demandSum != trueSum) {
      I.say("\nWRONG TOTAL, IS "+demandSum+", SHOULD BE "+trueSum);
      allOkay = false;
    }
    
    //
    //  Test deletion-
    for (int n = 4; n-- > 0;) {
      if (items.empty()) break;
      TestItem i = (TestItem) Rand.pickFrom(items);
      items.remove(i);
      
      demands.setAmount(0, null, i.x, i.y);
      float amount = demands.amountAt(i.x, i.y);
      
      if (amount != 0) {
        I.say("\nWRONG AT "+i+" IS "+amount+", SHOULD BE 0");
        allOkay = false;
      }
      
      trueSum -= i.amount;
    }
    
    demandSum = demands.totalAmount();
    if (demandSum != trueSum) {
      I.say("\nWRONG TOTAL, IS "+demandSum+", SHOULD BE "+trueSum);
      allOkay = false;
    }
    
    //
    //  Test basic and proximity queries-
    int fromX = Rand.index(32), fromY = Rand.index(32);
    float lastDist = 0;
    int numIters = 0;
    
    for (CityMapDemands.Entry e : demands.nearbyEntries(fromX, fromY)) {
      float dist = CityMap.distance(fromX, fromY, e.x, e.y);
      
      if (dist < lastDist) {
        I.say("\nDID NOT SORT ENTRIES BY DISTANCE:");
        I.say("  "+e.source+" -> "+e.x+"|"+e.y+", distance: "+dist);
        allOkay = false;
      }
      lastDist = dist;
      numIters += 1;
    }
    
    if (numIters != items.size()) {
      I.say("\nDID NOT ITERATE OVER ALL MEMBERS");
      I.say("  Expected: "+items.size()+", covered: "+numIters);
      allOkay = false;
    }
    
    //
    //  If that works out, return okay-
    if (allOkay) {
      I.say("\nALL DEMAND-MAP TESTS SUCCESSFUL!");
    }
  }
}






