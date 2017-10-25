

package game;
import util.*;
import static game.GameConstants.*;

import game.GameConstants.Good;



public class TestTrading extends Test {
  

  public static void main(String args[]) {
    testTrading(false);
  }
  
  static void testTrading(boolean graphics) {
    
    World   world = GameConstants.setupDefaultWorld();
    City    cityA = world.cities.atIndex(0);
    City    cityB = world.cities.atIndex(1);
    CityMap map   = new CityMap(cityA);
    map.settings.toggleFog     = false;
    map.settings.toggleHunger  = false;
    map.settings.toggleFatigue = false;
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
    post1.tradeLevel.set(RAW_COTTON,  2);
    post1.tradeLevel.set(CLAY      ,  2);
    post1.tradeLevel.set(COTTON    , -5);
    post1.tradeLevel.set(POTTERY   , -5);
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
    
    final int RUN_TIME = YEAR_LENGTH;
    boolean tradeOkay = false;
    
    while (map.time < RUN_TIME || graphics) {
      runGameLoop(map, 100, graphics, "saves/test_trading.tlt");
      
      if (! tradeOkay) {
        boolean check = true;
        check &= cityB.inventory.valueFor(POTTERY) > 1;
        check &= cityB.inventory.valueFor(COTTON ) > 1;
        check &= cityA.currentFunds > 100;
        tradeOkay = check;
        
        if (tradeOkay) {
          I.say("\nTRADING TEST CONCLUDED SUCCESSFULLY!");
          reportOnMap(map, true);
          if (! graphics) return;
        }
      }
    }
    
    I.say("\nTRADING TEST FAILED!");
    I.say("  Pottery sold:  "+cityB.inventory.valueFor(POTTERY));
    I.say("  Cotton  sold:  "+cityB.inventory.valueFor(COTTON ));
    reportOnMap(map, false);
  }
  
  
  static void reportOnMap(CityMap map, boolean okay) {
    final Good GOODS[] = { CLAY, RAW_COTTON, POTTERY, COTTON };
    
    I.say("\nTotal goods produced:");
    for (Good g : GOODS) {
      I.say("  "+g+": "+map.city.makeTotals.valueFor(g));
    }
    I.say("  Current funds: "+map.city.currentFunds);
    I.say("  Current time:  "+map.time);
  }

}











