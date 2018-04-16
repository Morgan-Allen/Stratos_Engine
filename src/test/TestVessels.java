


package test;
import game.*;
import static game.GameConstants.*;
import static game.World.*;
import static content.GameContent.*;
import content.*;
import util.*;



public class TestVessels extends LogicTest {
  
  
  public static void main(String args[]) {
    testForeignToLand(false);
    testForeignToDock(false);
    testDockToForeign(false);
  }
  
  
  //  TODO:  Test to ensure that goods are picked up as well as deposited.
  
  //  TODO:  Limit the cargo that can be delivered by the supply corps
  //  workers- 40 or 50 units in one go is a little too much!
  
  //  TODO:  You also need to test to ensure that traders will be auto-
  //  generated correctly by off-map bases, and configure their cargo based
  //  on 'fuzzy' supply/demand nearby.
  
  //  TODO:  Finally, you should ideally create a separate Task for ship-
  //  visits, rather than piggyback off task-trading.  And you can re-use
  //  that for missions later.
  
  
  static boolean testForeignToLand(boolean graphics) {
    TestVessels test = new TestVessels();
    return test.vesselTest(false, false, true, "FOREIGN TO LAND VESSEL", graphics);
  }
  
  
  static boolean testForeignToDock(boolean graphics) {
    TestVessels test = new TestVessels();
    return test.vesselTest(false, true, true, "FOREIGN TO DOCK VESSEL", graphics);
  }
  
  
  static boolean testDockToForeign(boolean graphics) {
    TestVessels test = new TestVessels();
    return test.vesselTest(true, false, true, "DOCK TO FOREIGN VESSEL", graphics);
  }
  
  
  
  boolean vesselTest(
    boolean fromLocal, boolean goesDock, boolean createShip,
    String title, boolean graphics
  ) {
    
    World world = new World(ALL_GOODS);
    Base  homeC = new Base(world, world.addLocale(2, 2));
    Base  awayC = new Base(world, world.addLocale(3, 3));
    world.addBases(homeC, awayC);
    awayC.council.setTypeAI(BaseCouncil.AI_OFF);
    homeC.setName("(Home City)");
    awayC.setName("(Away City)");
    
    World.setupRoute(homeC.locale, awayC.locale, 1, Type.MOVE_AIR);
    Base.setPosture(homeC, awayC, Base.POSTURE.TRADING, true);
    
    
    Tally <Good> supplies = new Tally().setWith(GREENS, 10, SPYCE, 5);
    Base.setSuppliesDue(awayC, homeC, supplies);
    
    awayC.setTradeLevel(GREENS  ,  0, 50);
    awayC.setTradeLevel(MEDICINE,  0, 20);
    awayC.setTradeLevel(ORES    , 30,  0);
    awayC.initInventory(GREENS, 100, MEDICINE, 40);
    
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
    post.setProdLevels(true, ORES, 10);
    post.setNeedLevels(false, GREENS, 30, MEDICINE, 10);
    
    BuildingForDock dock = null; if (goesDock || fromLocal) {
      dock = (BuildingForDock) AIRFIELD.generate();
      dock.enterMap(map, 6, 2, 1, homeC);
    }
    
    if (fromLocal) {
      ActorAsVessel ship = (ActorAsVessel) Vassals.DROPSHIP.generate();
      ship.assignBase(homeC);
      AreaTile point = dock.nextFreeDockPoint();
      dock.setWorker(ship, true);
      ship.enterMap(map, point.x, point.y, 1, homeC);
      ship.doLanding(point);
      
      TaskTrading trading = BuildingForTrade.selectTraderBehaviour(
        post, ship, homeC, map
      );
      ship.assignTask(trading);
    }
    else if (createShip) {
      ActorAsVessel ship = (ActorAsVessel) Vassals.DROPSHIP.generate();
      ship.assignBase(awayC);
      
      for (int n = 2; n-- > 0;) {
        Actor porter = (Actor) Vassals.SUPPLY_CORPS.generate();
        porter.type().initAsMigrant((ActorAsPerson) porter);
        porter.assignBase(awayC);
        porter.setInside(ship, true);
        ship.setWorker(porter, true);
      }
      
      I.say("\n\nShip's crew is: "+ship.crew());
      for (Actor a : ship.crew()) {
        I.say(a+" is inside: "+a.inside());
      }
      
      TaskTrading trading = BuildingForTrade.selectTraderBehaviour(
        awayC, ship, homeC, map
      );
      if (trading == null) {
        I.say("\nCould not generate trade-task for ship!");
        return false;
      }
      
      ship.assignTask(trading);
      trading.beginFromOffmap(awayC);
    }
    
    
    if (fromLocal) {
      final int RUN_TIME = YEAR_LENGTH;
      
      boolean shipExport = false;
      boolean shipGoing  = false;
      boolean shipTraded = false;
      boolean shipReturn = false;
      boolean shipLanded = false;
      boolean shipImport = false;
      boolean testOkay   = false;
      ActorAsVessel ship = dock.docking().first();
      
      while (map.time() < RUN_TIME || graphics) {
        runLoop(homeC, 1, graphics, "saves/test_vessels.str");
        
        if (! shipExport) {
          shipExport = post.inventory(ORES) == 0;
          shipExport &= ship.carried(ORES) >= 20;
        }
        
        if (shipExport && ! shipGoing) {
          shipGoing = ! ship.landed();
        }
        
        if (shipGoing && ! shipTraded) {
          boolean goodsMoved = true;
          goodsMoved &= ship.carried(ORES) == 0;
          shipTraded = goodsMoved;
        }
        
        if (shipTraded && ! shipReturn) {
          World.Journey j = world.journeyFor(ship);
          if (j != null && j.goes() == homeC) shipReturn = true;
        }
        
        if (shipReturn && ! shipLanded) {
          shipLanded = ship.landed();
        }
        
        if (shipLanded && ! shipImport) {
          boolean goodsMoved = true;
          goodsMoved &= post.inventory(GREENS  ) >= 30;
          goodsMoved &= post.inventory(MEDICINE) >= 10;
          shipImport = goodsMoved;
        }
        
        if (shipImport && ! testOkay) {
          testOkay = true;
          I.say("\n"+title+" TEST CONCLUDED SUCCESSFULLY!");
          if (! graphics) return true;
        }
      }
    }
    
    else {
      final int RUN_TIME = YEAR_LENGTH;
      
      boolean shipComing = false;
      boolean shipArrive = false;
      boolean shipLanded = false;
      boolean shipTraded = false;
      boolean shipDone   = false;
      boolean testOkay   = false;
      ActorAsVessel ship = null;
      
      while (map.time() < RUN_TIME || graphics) {
        runLoop(homeC, 1, graphics, "saves/test_vessels.str");
        
        if (! shipComing) {
          for (Journey j : world.journeys()) {
            for (Journeys g : j.going()) {
              if (! g.isElement()) continue;
              if (j.goes() != homeC) continue;
              Element e = (Element) g;
              if (e.type().isAirship()) {
                ship = (ActorAsVessel) e;
                shipComing = true;
              }
            }
          }
        }
        
        if (shipComing && ! shipArrive) {
          shipArrive = ship.onMap();
        }
        
        if (shipArrive && ! shipLanded) {
          shipLanded = ship.landed();
          Type t = ship.type();
          AreaTile o = ship.at();
          
          if (goesDock && o != null) {
            for (Coord c : Visit.grid(0, 0, t.wide, t.high, 1)) {
              AreaTile u = map.tileAt(o.x + c.x, o.y + c.y);
              if (map.above(u) != dock) shipLanded = false;
            }
            if (! dock.isDocked(ship)) shipLanded = false;
            if (ship.dockedAt() != dock) shipLanded = false;
          }
          else if (o != null) {
            for (Coord c : Visit.grid(0, 0, t.wide, t.high, 1)) {
              AreaTile u = map.tileAt(o.x + c.x, o.y + c.y);
              if (map.above(u) != ship) shipLanded = false;
            }
          }
          if (o != null && shipLanded) {
            ActorPathSearch forward = new ActorPathSearch(map, ship, post, -1);
            forward.doSearch();
            if (! forward.success()) {
              shipLanded = false;
            }
            ActorPathSearch reverse = new ActorPathSearch(map, post, ship, -1);
            reverse.doSearch();
            if (! reverse.success()) {
              shipLanded = false;
            }
          }
        }
        
        if (shipLanded && ! shipTraded) {
          boolean goodsMoved = true;
          goodsMoved &= post.inventory(GREENS  ) >= 30;
          goodsMoved &= post.inventory(MEDICINE) >= 10;
          goodsMoved &= post.inventory(ORES    ) ==  0;
          shipTraded = goodsMoved;
        }
        
        if (shipTraded && ! shipDone) {
          shipDone = ! ship.onMap();
        }
        
        if (shipDone && ! testOkay) {
          testOkay = true;
          I.say("\n"+title+" TEST CONCLUDED SUCCESSFULLY!");
          if (! graphics) return true;
        }
      }
      
      I.say("\n"+title+" TEST FAILED!");
      I.say("  Ship coming: "+shipComing);
      I.say("  Ship arrive: "+shipArrive);
      I.say("  Ship landed: "+shipLanded);
      I.say("  Ship traded: "+shipTraded);
      I.say("  Ship done:   "+shipDone  );
    }
    
    return false;
  }
  

}










