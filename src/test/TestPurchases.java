


package test;
import game.*;
import static game.GameConstants.*;
import content.*;
import static content.GameContent.*;
import static content.GameWorld.*;
import util.*;



public class TestPurchases extends LogicTest {
  
  
  public static void main(String args[]) {
    testPurchases(false);
  }
  

  static boolean testPurchases(boolean graphics) {
    TestPurchases test = new TestPurchases();
    
    Base base = LogicTest.setupTestBase(BASE, FACTION_SETTLERS_A, ALL_GOODS, 16, false);
    AreaMap map = base.activeMap();
    World world = map.world;
    
    world.settings.toggleFog     = false;
    world.settings.toggleHunger  = false;
    world.settings.toggleFatigue = false;
    world.settings.toggleMigrate = false;
    
    Building fort = (Building) TROOPER_LODGE.generate();
    fort.enterMap(map, 2, 2, 1, base);
    ActorUtils.fillWorkVacancies(fort);
    
    BuildingForCrafts forge = (BuildingForCrafts) ENGINEER_STATION.generate();
    forge.enterMap(map, 10, 2, 1, base);
    ActorUtils.fillWorkVacancies(forge);
    
    BuildingForCrafts shop = (BuildingForCrafts) STOCK_EXCHANGE.generate();
    shop.enterMap(map, 2, 10, 1, base);
    ActorUtils.fillWorkVacancies(shop);
    
    
    Actor buys = fort.workers().first();
    Good weaponType = buys.type().weaponType;
    Good potionType = StockExGoods.MEDIKIT;
    int initQuality = (int) buys.outfit.carried(weaponType);
    buys.outfit.incCarried(CASH, 10000);
    
    forge.addInventory(20, ORES);
    forge.addInventory(20, CARBONS);
    shop.addInventory(20, MEDICINE);
    
    if (! forge.shopItems().includes(weaponType)) {
      I.say("\nForge did not allow shopping for "+weaponType);
      I.say("\nPURCHASES TEST FAILED!");
    }
    
    if (! shop.shopItems().includes(potionType)) {
      I.say("\nShop did not allow shopping for "+potionType);
      I.say("\nPURCHASES TEST FAILED!");
    }
    
    boolean orderPlaced = false;
    boolean itemMade    = false;
    boolean collectOkay = false;
    boolean potionOkay  = false;
    boolean usageOkay   = false;
    boolean testOkay    = false;
    int injureTime = -1;
    float initInjury = -1;
    
    //  TODO:  You should also test expiration dates for orders...
    
    
    while (map.time() < 1000 || graphics) {
      test.runLoop(base, 1, graphics, "saves/test_purchases.tlt");
      
      if (! orderPlaced) {
        orderPlaced = forge.hasItemOrder(weaponType, buys);
      }
      
      if (orderPlaced && ! itemMade) {
        itemMade = forge.orderComplete(weaponType, buys);
      }
      
      if (itemMade && ! collectOkay) {
        collectOkay = buys.outfit.carried(weaponType) >= initQuality + 1;
      }
      
      if (! potionOkay) {
        potionOkay = buys.outfit.carried(potionType) > 0;
        if (potionOkay) {
          injureTime = map.time();
          buys.takeDamage(buys.health.maxHealth() * 0.9f);
          initInjury = buys.health.injury();
        }
      }
      
      if (
        potionOkay && (! usageOkay) &&
        AreaMap.timeSince(map.time(), injureTime) < 10
      ) {
        float healAmount = StockExGoods.MEDIKIT_HEAL_AMOUNT;
        usageOkay = buys.health.injury() <= initInjury + 1 - healAmount;
      }
      
      if (collectOkay && usageOkay && ! testOkay) {
        I.say("\nPURCHASES TEST CONCLUDED SUCCESSFULLY!");
        I.say("  Time: "+map.time());
        testOkay = true;
        if (! graphics) return true;
      }
    }
    
    I.say("\nPURCHASES TEST FAILED!");
    I.say("  Order placed: "+orderPlaced);
    I.say("  Item made:    "+itemMade   );
    I.say("  Collect okay: "+collectOkay);
    I.say("  Potion okay:  "+potionOkay );
    I.say("  Usage okay:   "+usageOkay  );
    I.say("  Final inventory: "+buys.inventory());
    I.say("  Time: "+map.time());
    
    return false;
  }
  
  
}












