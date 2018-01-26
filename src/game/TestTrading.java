


package game;
import util.*;
import static game.GameConstants.*;
import static game.GameContent.*;



public class TestTrading extends Test {
  

  public static void main(String args[]) {
    testTrading(false);
  }
  
  
  static boolean testTrading(boolean graphics) {
    Test test = new TestTrading();

    World world = new World(ALL_GOODS);
    City  homeC = new City(world);
    City  awayC = new City(world);
    world.addCities(homeC, awayC);
    awayC.council.typeAI = CityCouncil.AI_OFF;
    homeC.name = "(Home City)";
    awayC.name = "(Away City)";
    
    City.setupRoute(homeC, awayC, 1);
    City.setPosture(homeC, awayC, City.POSTURE.VASSAL, true);
    
    Tally <Good> supplies = new Tally().setWith(FRUIT, 10, STONE, 5);
    City.setSuppliesDue(awayC, homeC, supplies);
    
    awayC.tradeLevel.setWith(
      COTTON    ,  50,
      POTTERY   ,  50,
      RAW_COTTON, -50,
      CLAY      , -50
    );
    awayC.inventory.setWith(
      RAW_COTTON,  20,
      CLAY      ,  20,
      FRUIT     ,  15,
      STONE     ,  5 
    );
    
    
    CityMap map = new CityMap(homeC);
    map.performSetup(10, new Terrain[0]);
    world.settings.toggleFog     = false;
    world.settings.toggleHunger  = false;
    world.settings.toggleFatigue = false;
    
    BuildingForTrade post1 = (BuildingForTrade) PORTER_POST.generate();
    post1.enterMap(map, 1, 6, 1);
    post1.ID = "(Does Trading)";
    post1.tradeLevel.setWith(
      RAW_COTTON,  2 ,
      CLAY      ,  2 ,
      COTTON    , -5 ,
      POTTERY   , -5 
    );
    
    BuildingForTrade post2 = (BuildingForTrade) PORTER_POST.generate();
    post2.enterMap(map, 5, 6, 1);
    post2.ID = "(Gets Supplies)";
    post2.tradeLevel.setWith(
      FRUIT     , 15,
      STONE     , 5 
    );
    
    Building kiln = (Building) ENGINEER_STATION.generate();
    kiln.enterMap(map, 2, 3, 1);
    
    Building weaver = (Building) PHYSICIAN_STATION.generate();
    weaver.enterMap(map, 5, 3, 1);
    
    CityMapPlanning.placeStructure(ROAD, map, true, 1, 5, 8, 1);
    
    fillAllVacancies(map);
    int initFunds = 100;
    homeC.currentFunds = initFunds;
    
    
    final int RUN_TIME = YEAR_LENGTH;
    boolean tradeOkay  = false;
    boolean supplyOkay = false;
    boolean moneyOkay  = false;
    
    while (map.time < RUN_TIME || graphics) {
      map = test.runLoop(map, 100, graphics, "saves/test_trading.tlt");
      
      boolean allHome = true;
      for (Building b : map.buildings) for (Actor a : b.workers) {
        if (a.jobType() == Task.JOB.TRADING) allHome = false;
      }
      if (allHome) {
        float funds = projectedEarnings(homeC, awayC, initFunds, false);
        if (Nums.abs(funds - homeC.currentFunds) > 1) {
          I.say("\nTrade-earnings do not match projections!");
          I.say("  Expected: "+funds    );
          I.say("  Actual:   "+homeC.currentFunds);
          projectedEarnings(homeC, awayC, initFunds, true);
          return false;
        }
        moneyOkay = true;
      }
      
      if (! tradeOkay) {
        boolean check = true;
        check &= City.goodsSent(homeC, awayC, POTTERY) > 1;
        check &= City.goodsSent(homeC, awayC, COTTON ) > 1;
        check &= homeC.currentFunds > 0;
        tradeOkay = check;
      }
      
      if (! supplyOkay) {
        boolean check = true;
        check &= post2.inventory.valueFor(STONE) >= 5;
        check &= post2.inventory.valueFor(FRUIT) >= 15;
        supplyOkay = check;
      }
      
      if (tradeOkay && supplyOkay && moneyOkay) {
        I.say("\nTRADING TEST CONCLUDED SUCCESSFULLY!");
        reportOnMap(homeC, awayC, true);
        if (! graphics) return true;
      }
    }
    
    I.say("\nTRADING TEST FAILED!");
    reportOnMap(homeC, awayC, false);
    return false;
  }
  
  
  static float projectedEarnings(
    City cityA, City cityB, int initFunds, boolean report
  ) {
    if (report) I.say("\nProjected earnings:");
    
    float projectedFunds = initFunds;
    for (Good g : ALL_GOODS) {
      float sent = City.goodsSent  (cityA, cityB, g);
      float got  = City.goodsSent  (cityB, cityA, g);
      float free = City.suppliesDue(cityB, cityA, g);
      if (sent == 0 && got == 0 && free == 0) continue;
      
      float pays = sent * g.price;
      pays -= Nums.max(0, got - free) * g.price;
      projectedFunds += pays;
      
      if (report) {
        I.say("  "+g+": +"+sent+" -"+got);
        if (free > 0) I.add(" ("+free+" free)");
        I.add(", price: "+g.price+", total: "+pays);
      }
    }
    return projectedFunds;
  }
  
  
  static void reportOnMap(City a, City b, boolean okay) {
    final Good GOODS[] = { CLAY, RAW_COTTON, POTTERY, COTTON, FRUIT, STONE };
    
    I.say("\nGoods report:");
    for (Good g : GOODS) {
      I.say("  Made "+g+": "+a.makeTotals.valueFor(g));
      I.add("  Sent "+City.goodsSent(a, b, g));
      I.add("  Got " +City.goodsSent(b, a, g));
    }
    I.say("  Current funds: "+a.currentFunds);
    I.say("  Current time:  "+a.world.time);
  }

}
