


package test;
import game.*;
import util.*;
import static game.GameConstants.*;
import static content.GameContent.*;



public class TestPurchases extends Test {
  
  
  public static void main(String args[]) {
    testPurchases(true);
  }
  

  static boolean testPurchases(boolean graphics) {
    TestPurchases test = new TestPurchases();
    
    Base base = Test.setupTestCity(16, ALL_GOODS, false);
    AreaMap map = base.activeMap();
    World world = map.world;
    
    world.settings.toggleFog     = false;
    world.settings.toggleHunger  = false;
    world.settings.toggleFatigue = false;
    world.settings.toggleMigrate = false;
    
    Building fort = (Building) TROOPER_LODGE.generate();
    fort.enterMap(map, 2, 2, 1, base);
    fillWorkVacancies(fort);
    
    BuildingForCrafts forge = (BuildingForCrafts) ENGINEER_STATION.generate();
    forge.enterMap(map, 10, 2, 1, base);
    fillWorkVacancies(forge);
    
    Actor buys = fort.workers().first();
    Good itemType = buys.type().weaponType;
    int initQuality = (int) buys.carried(itemType);
    buys.incCarried(CASH, 10000);
    
    forge.addInventory(20, ORES);
    forge.addInventory(20, CARBONS);
    
    
    boolean orderPlaced = false;
    boolean itemMade    = false;
    boolean collectOkay = false;
    boolean testOkay    = false;
    
    //  TODO:  You should also test expiration dates for orders, and purchases
    //  of usable items like potions.
    
    
    while (map.time() < 1000 || graphics) {
      test.runLoop(base, 1, graphics, "saves/test_purchases.tlt");
      
      if (! orderPlaced) {
        orderPlaced = forge.hasItemOrder(itemType, buys);
      }
      
      if (orderPlaced && ! itemMade) {
        itemMade = forge.orderComplete(itemType, buys);
      }
      
      if (itemMade && ! collectOkay) {
        collectOkay = buys.carried(itemType) >= initQuality + 1;
      }
      
      if (collectOkay && ! testOkay) {
        I.say("\nPURCHASES TEST CONCLUDED SUCCESSFULLY!");
        testOkay = true;
        if (! graphics) return true;
      }
    }
    
    I.say("\nPURCHASES TEST FAILED!");
    I.say("  Order placed: "+orderPlaced);
    I.say("  Item made:    "+itemMade   );
    I.say("  Collect okay: "+collectOkay);
    
    return false;
  }
  
  
}












