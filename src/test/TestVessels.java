


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
    Base  baseC = new Base(world, world.addLocale(2, 2));
    Base  awayC = new Base(world, world.addLocale(3, 3));
    world.addBases(baseC, awayC);
    awayC.council.setTypeAI(BaseCouncil.AI_OFF);
    baseC.setName("(Home City)");
    awayC.setName("(Away City)");
    
    World.setupRoute(baseC.locale, awayC.locale, 1);
    Base.setPosture(baseC, awayC, Base.POSTURE.TRADING, true);
    
    
    Tally <Good> supplies = new Tally().setWith(GREENS, 10, SPYCE, 5);
    Base.setSuppliesDue(awayC, baseC, supplies);
    
    /*
    awayC.setTradeLevel(MEDICINE, 50, 0 );
    awayC.setTradeLevel(PARTS   , 50, 0 );
    awayC.setTradeLevel(GREENS  , 0 , 50);
    awayC.setTradeLevel(ORES    , 0 , 50);
    awayC.initInventory(
      GREENS    ,  35,
      ORES      ,  20,
      SPYCE     ,  10
    );
    //*/
    
    
    Area map = new Area(world, baseC.locale, baseC);
    map.performSetup(10, new Terrain[0]);
    world.settings.toggleFog       = false;
    world.settings.toggleHunger    = false;
    world.settings.toggleFatigue   = false;
    world.settings.toggleBuilding  = false;
    world.settings.togglePurchases = false;
    
    BuildingForTrade post = (BuildingForTrade) SUPPLY_DEPOT.generate();
    post.enterMap(map, 1, 6, 1, baseC);
    
    /*
    post.setID("(Does Trading)");
    post.setNeedLevels(false,
      GREENS    , 2,
      ORES      , 2
    );
    post.setProdLevels(false,
      MEDICINE  , 5,
      PARTS     , 5
    );
    //*/
    
    
    //  The away city will need to spawn a trader and assign it a Trading task,
    //  with orders to park in the nearest viable opening.  (If you can't find
    //  a depot or airfield, you just pick an open space nearby.)
    
    //  Base the chance of that on the build-level of something with trade-
    //  capabilities.
    
    //  The city has to choose who to trade with in a similar manner to an
    //  individual post.  And with the selection of ships stored at a given
    //  base.
    
    
    
    
    final int RUN_TIME = YEAR_LENGTH;
    
    boolean shipComing = false;
    boolean shipArrive = false;
    boolean shipTraded = false;
    boolean shipDone   = false;
    
    
    while (map.time() < RUN_TIME || graphics) {
      test.runLoop(baseC, 1, graphics, "saves/test_vessels.str");
      
      if (! shipComing) {
        for (Journey j : world.journeys()) {
          for (Journeys g : j.going()) {
            if (! g.isElement()) continue;
            if (j.goes() != baseC) continue;
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








