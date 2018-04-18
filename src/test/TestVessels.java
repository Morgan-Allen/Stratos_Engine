


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
  
  //  TODO:  You need to auto-generate traders at bases that have 'Trader'
  //  posture or better, and dispatch them on trading missions at regular
  //  intervals.
  
  //  TODO:  You need to ensure that airships are constructed/spawned at local
  //  docks, and are given a crew, and dispatch them on trading missions at
  //  regular intervals.
  
  //  TODO:  Ensure that migrants are spawned off-map in the first place after
  //  hiring.  (And disallow if you have no trading partners.)
  
  //  Okay.  I can hack those into place relatively easily.
  

  
  //  TODO:  See if you can at least streamline the TaskTrading class.  And
  //  just use TaskDelivery to do the local cargo-transport.
  
  //  TODO:  Ideally, traders should use 'fuzzy' calibration of supply/demand
  //  when selecting a dock-point and cargo.
  
  //  TODO:  Ensure that Dropships dump their earnings at the dock-site!
  
  //  TODO:  And then attach TaskTrading and/or TaskMissionDropoff to the
  //  Vessel class.  (Create the latter as a separate task-class.)
  
  
  
  //  What about regular old human traders, though?
  //  Tlatoani would properly require a MissionTrade for the purpose, auto-
  //  created by the post.  And then all the recruits would come from the post
  //  and do pickups/deliveries as part of the mission.  Simple enough.
  
  //  Okay.  Do that when the time comes.  As a stop-gap hack, you could create
  //  a 'vehicle' that looks just like a person and has porters trailing after
  //  like ghosts.
  
  //  Okay.  Do that.
  
  
  
  
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
    
    Batch <Actor> migrants = new Batch();
    for (int n = 2; n-- > 0;) {
      Actor migrant = (Actor) ECOLOGIST.generate();
      migrant.type().initAsMigrant((ActorAsPerson) migrant);
      migrant.assignBase(homeC);
      awayC.addMigrant(migrant);
      migrants.add(migrant);
    }
    
    {
      ActorAsVessel ship = (ActorAsVessel) Vassals.DROPSHIP.generate();
      ship.assignBase(fromLocal ? homeC : awayC);
      
      for (int n = 2; n-- > 0;) {
        Actor porter = (Actor) Vassals.SUPPLY_CORPS.generate();
        porter.type().initAsMigrant((ActorAsPerson) porter);
        porter.assignBase(ship.base());
        porter.setInside(ship, true);
        ship.setWorker(porter, true);
      }
      
      if (fromLocal) {
        AreaTile point = dock.nextFreeDockPoint();
        dock.setWorker(ship, true);
        ship.enterMap(map, point.x, point.y, 1, homeC);
        ship.doLanding(point);
        
        AreaTile at = ship.centre();
        for (Actor a : ship.crew()) if (! a.onMap()) {
          a.enterMap(map, at.x, at.y, 1, a.base());
        }
        
        TaskTrading trading = BuildingForTrade.selectTraderBehaviour(
          post, ship, homeC, false, map
        );
        ship.assignTask(trading);
      }
      
      else if (createShip) {
        TaskTrading trading = BuildingForTrade.selectTraderBehaviour(
          awayC, ship, homeC, false, map
        );
        ship.assignTask(trading);
        trading.beginAsVessel(awayC);
      }
    }
    
    
    final int RUN_TIME = YEAR_LENGTH;
    boolean shipComing  = false;
    boolean shipArrive  = false;
    boolean shipLanded  = false;
    boolean shipTraded  = false;
    boolean migrateDone = false;
    boolean testOkay    = false;
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
        Type     t = ship.type();
        AreaTile o = ship.at();
        
        if (ship.dockedAt() != null && o != null) {
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
      
      if (! migrateDone) {
        boolean allHere = true;
        for (Actor a : migrants) {
          if (! a.onMap()) allHere = false;
          if (a.inside() == ship) allHere = false;
        }
        migrateDone = allHere;
      }
      
      if (shipTraded && migrateDone && ! testOkay) {
        testOkay = true;
        I.say("\n"+title+" TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return true;
      }
    }
    
    I.say("\n"+title+" TEST FAILED!");
    I.say("  Ship coming:  "+shipComing );
    I.say("  Ship arrive:  "+shipArrive );
    I.say("  Ship landed:  "+shipLanded );
    I.say("  Ship traded:  "+shipTraded );
    I.say("  Migrate done: "+migrateDone);
    
    return false;
  }
  

}



