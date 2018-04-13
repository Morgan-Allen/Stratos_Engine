


package test;
import game.*;
import static game.GameConstants.*;
import static game.World.*;
import static content.GameContent.*;
import content.*;
import util.*;



public class TestVessels extends LogicTest {
  
  
  public static void main(String args[]) {
    testVessels(false);
  }
  
  
  static boolean testVessels(boolean graphics) {
    LogicTest test = new TestTrading();
    
    World world = new World(ALL_GOODS);
    Base  homeC = new Base(world, world.addLocale(2, 2));
    Base  awayC = new Base(world, world.addLocale(3, 3));
    world.addBases(homeC, awayC);
    awayC.council.setTypeAI(BaseCouncil.AI_OFF);
    homeC.setName("(Home City)");
    awayC.setName("(Away City)");
    
    World.setupRoute(homeC.locale, awayC.locale, 1);
    Base.setPosture(homeC, awayC, Base.POSTURE.TRADING, true);
    
    
    Tally <Good> supplies = new Tally().setWith(GREENS, 10, SPYCE, 5);
    Base.setSuppliesDue(awayC, homeC, supplies);
    
    awayC.setTradeLevel(GREENS  , 0, 50);
    awayC.setTradeLevel(MEDICINE, 0, 20);
    awayC.initInventory(GREENS, 100);
    
    Area map = new Area(world, homeC.locale, homeC);
    map.performSetup(10, new Terrain[0]);
    world.settings.toggleFog       = false;
    world.settings.toggleHunger    = false;
    world.settings.toggleFatigue   = false;
    world.settings.toggleBuilding  = false;
    world.settings.togglePurchases = false;
    world.settings.toggleShipping  = false;
    
    BuildingForTrade post = (BuildingForTrade) SUPPLY_DEPOT.generate();
    post.enterMap(map, 1, 6, 1, homeC);
    post.setNeedLevels(false, GREENS, 30, MEDICINE, 10);
    
    //while (homeC.needLevels().empty()) map.update(1);
    
    
    
    
    ActorAsVessel testShip = (ActorAsVessel) Vassals.DROPSHIP.generate();
    testShip.assignBase(awayC);
    
    for (int n = 2; n-- > 0;) {
      Actor porter = (Actor) Vassals.SUPPLY_CORPS.generate();
      porter.assignBase(awayC);
      testShip.setInside(porter, true);
      testShip.setWorker(porter, true);
    }
    
    TaskTrading trading = BuildingForTrade.selectTraderBehaviour(
      awayC, testShip, homeC, map
    );
    
    if (trading == null) {
      I.say("\nCould not generate trade-task for ship!");
      return false;
    }
    
    AreaTile lands = testShip.findLandingPoint(map, trading);
    
    I.say("Lands at: "+lands);
    //testShip.assignTask(trading);
    
    
    
    //  TODO:  You can simply calibrate the supply/demand profiles of bound vessels
    //  *as if* they were already present.  No need for proxy-profiles or local
    //  inventory, just plumb the map/base like you were already there.
    
    /*
    testShip.calibrateTrading(map, homeC, lands);
    I.say("\nUpdated trade proxy:");
    I.say("  Supply: "+testShip.proxy.prodLevels());
    I.say("  Demand: "+testShip.proxy.needLevels());
    //*/
    
    
    /*
    for (int n = DAY_LENGTH; n-- > 0;) map.update(1);
    
    if (awayC.traders().size() != 1) {
      I.say("\nWrong number of trade vessels!...");
      return false;
    }
    //*/
    
    
    
    
    final int RUN_TIME = YEAR_LENGTH;
    
    boolean shipComing = false;
    boolean shipArrive = false;
    boolean shipTraded = false;
    boolean shipDone   = false;
    
    
    while (map.time() < RUN_TIME || graphics) {
      test.runLoop(homeC, 1, graphics, "saves/test_vessels.str");
      
      if (! shipComing) {
        for (Journey j : world.journeys()) {
          for (Journeys g : j.going()) {
            if (! g.isElement()) continue;
            if (j.goes() != homeC) continue;
            Element e = (Element) g;
            if (e.type().isVessel()) {
              shipComing = true;
            }
          }
        }
      }
      
    }
    
    I.say("\nVESSELS TEST FAILED!");
    
    return false;
  }
  

}








