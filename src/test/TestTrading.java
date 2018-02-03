


package test;
import game.*;
import util.*;
import static content.GameContent.*;
import static game.GameConstants.*;



public class TestTrading extends Test {
  
  
  public static void main(String args[]) {
    testTrading(true);
  }
  
  
  static boolean testTrading(boolean graphics) {
    Test test = new TestTrading();
    
    World world = new World(ALL_GOODS);
    City  baseC = new City(world, world.addLocale(2, 2));
    City  awayC = new City(world, world.addLocale(3, 3));
    world.addCities(baseC, awayC);
    awayC.council.setTypeAI(CityCouncil.AI_OFF);
    baseC.setName("(Home City)");
    awayC.setName("(Away City)");
    
    World.setupRoute(baseC.locale, awayC.locale, 1);
    City.setPosture(baseC, awayC, City.POSTURE.VASSAL, true);
    
    Tally <Good> supplies = new Tally().setWith(GREENS, 10, SPYCE, 5);
    City.setSuppliesDue(awayC, baseC, supplies);
    
    awayC.initTradeLevels(
      MEDICINE  ,  50,
      PARTS     ,  50,
      GREENS    , -50,
      ORES      , -50
    );
    awayC.initInventory(
      GREENS    ,  35,
      ORES      ,  20,
      SPYCE     ,  5 
    );
    
    
    CityMap map = new CityMap(world, baseC.locale, baseC);
    map.performSetup(10, new Terrain[0]);
    world.settings.toggleFog     = false;
    world.settings.toggleHunger  = false;
    world.settings.toggleFatigue = false;
    
    BuildingForTrade post1 = (BuildingForTrade) SUPPLY_DEPOT.generate();
    post1.enterMap(map, 1, 6, 1, baseC);
    post1.setID("(Does Trading)");
    post1.setTradeLevels(false,
      GREENS    ,  2 ,
      ORES      ,  2 ,
      MEDICINE  , -5 ,
      PARTS     , -5 
    );
    
    BuildingForTrade post2 = (BuildingForTrade) SUPPLY_DEPOT.generate();
    post2.enterMap(map, 5, 6, 1, baseC);
    post2.setID("(Gets Supplies)");
    post2.setTradeLevels(false,
      GREENS    , 15,
      SPYCE     , 5 
    );
    
    Building kiln = (Building) ENGINEER_STATION.generate();
    kiln.enterMap(map, 2, 3, 1, baseC);
    
    Building weaver = (Building) PHYSICIAN_STATION.generate();
    weaver.enterMap(map, 5, 3, 1, baseC);
    
    CityMapPlanning.placeStructure(WALKWAY, baseC, true, 1, 5, 8, 1);
    
    fillAllVacancies(map, PYON);
    int initFunds = 100;
    baseC.initFunds(initFunds);
    
    
    final int RUN_TIME = YEAR_LENGTH;
    boolean tradeOkay  = false;
    boolean supplyOkay = false;
    boolean tradeStop  = false;
    boolean moneyOkay  = false;
    boolean testOkay   = false;
    
    while (map.time() < RUN_TIME || graphics) {
      test.runLoop(baseC, 1, graphics, "saves/test_trading.tlt");
      
      if (! tradeOkay) {
        boolean check = true;
        check &= City.goodsSent(baseC, awayC, PARTS   ) > 1;
        check &= City.goodsSent(baseC, awayC, MEDICINE) > 1;
        check &= baseC.funds() > 0;
        tradeOkay = check;
      }
      
      if (! supplyOkay) {
        boolean check = true;
        check &= post2.inventory(SPYCE ) >= 5 ;
        check &= post2.inventory(GREENS) >= 15;
        supplyOkay = check;
      }
      
      if (tradeOkay && supplyOkay && ! tradeStop) {
        post1.toggleTrading(false);
        post2.toggleTrading(false);
        tradeStop = true;
      }
      
      boolean allHome = true;
      for (Building b : map.buildings()) for (Actor a : b.workers()) {
        if (a.jobType() == Task.JOB.TRADING) allHome = false;
      }
      if (tradeStop && allHome && ! moneyOkay) {
        float funds = projectedEarnings(baseC, awayC, initFunds, false);
        if (Nums.abs(funds - baseC.funds()) > 1) {
          I.say("\nTrade-earnings do not match projections!");
          I.say("  Expected: "+funds        );
          I.say("  Actual:   "+baseC.funds());
          projectedEarnings(baseC, awayC, initFunds, true);
          return false;
        }
        moneyOkay = true;
      }
      
      if (tradeOkay && supplyOkay && moneyOkay && ! testOkay) {
        I.say("\nTRADING TEST CONCLUDED SUCCESSFULLY!");
        testOkay = true;
        reportOnMap(baseC, awayC, true);
        if (! graphics) return true;
      }
    }
    
    I.say("\nTRADING TEST FAILED!");
    I.say("  Trade okay:  "+tradeOkay );
    I.say("  Supply okay: "+supplyOkay);
    I.say("  Money okay:  "+moneyOkay );
    reportOnMap(baseC, awayC, false);
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
    final Good GOODS[] = { ORES, PARTS, MEDICINE, GREENS, SPYCE };
    
    I.say("\nGoods report:");
    for (Good g : GOODS) {
      I.say("  Made "+g+": "+a.totalMade(g));
      I.add("  Sent "+City.goodsSent(a, b, g));
      I.add("  Got " +City.goodsSent(b, a, g));
    }
    I.say("  Current funds: "+a.funds());
    I.say("  Current time:  "+a.world.time());
  }

}
