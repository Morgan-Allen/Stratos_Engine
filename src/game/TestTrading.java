

package game;
import util.*;
import static game.GameConstants.*;



public class TestTrading extends Test {
  

  public static void main(String args[]) {
    testTrading(true);
  }
  
  static void testTrading(boolean graphics) {
    
    World   world = GameConstants.setupDefaultWorld();
    City    cityA = world.cities.atIndex(0);
    City    cityB = world.cities.atIndex(1);
    CityMap map   = new CityMap(cityA);
    cityA.name = "(Home City)";
    cityB.name = "(Away City)";
    
    map.performSetup(10);
    City.setupRoute(cityA, cityB, 1);
    
    cityB.tradeLevel.set(COTTON    ,  50);
    cityB.tradeLevel.set(POTTERY   ,  50);
    cityB.tradeLevel.set(RAW_COTTON, -50);
    cityB.tradeLevel.set(CLAY      , -50);
    cityB.inventory .set(RAW_COTTON,  50);
    cityB.inventory .set(CLAY      ,  50);
    
    BuildingForTrade post1 = (BuildingForTrade) PORTER_HOUSE.generate();
    post1.enterMap(map, 1, 6, 1);
    post1.ID = "(Does Trading)";
    post1.tradeLevel.set(RAW_COTTON,  10);
    post1.tradeLevel.set(CLAY      ,  10);
    post1.tradeLevel.set(COTTON    , -20);
    post1.tradeLevel.set(POTTERY   , -20);
    post1.tradePartner = cityB;
    
    CityMap.applyPaving(map, 1, 5, 8, 1, true);
    
    //  TODO:  Find something useful to do with this yoke.
    /*
    BuildingForTrade post2 = (BuildingForTrade) PORTER_HOUSE.generate();
    post2.enterMap(map, 5, 6, 1);
    post2.ID = "(Does Storage)";
    post2.tradeLevel.set(COTTON , 35);
    post2.tradeLevel.set(POTTERY, 35);
    //*/
    
    Building kiln = (Building) KILN.generate();
    kiln.enterMap(map, 2, 3, 1);
    
    Building weaver = (Building) WEAVER.generate();
    weaver.enterMap(map, 5, 3, 1);
    
    Test.fillAllVacancies(map);
    
    
    boolean tradeOkay = false;
    
    while (map.time < 1000 || graphics) {
      runGameLoop(map, 10, graphics);
      
      if (! tradeOkay) {
        boolean check = true;
        check &= cityB.inventory.valueFor(POTTERY) > 10;
        check &= cityB.inventory.valueFor(COTTON ) > 10;
        check &= cityA.currentFunds > 500;
        tradeOkay = check;
        
        if (tradeOkay) {
          I.say("\nTRADING TEST CONCLUDED SUCCESSFULLY!");
          if (! graphics) return;
        }
      }
    }
    
    I.say("Pottery sold:  "+cityB.inventory.valueFor(POTTERY));
    I.say("Cotton  sold:  "+cityB.inventory.valueFor(COTTON ));
    I.say("Current funds: "+cityA.currentFunds);
    I.say("\nTRADING TEST FAILED!");
  }

}











