


package test;
import game.*;
import static game.GameConstants.*;
import static game.World.*;
import static content.GameContent.*;
import content.*;
import util.*;


//  TODO:  Add the crew check down further in the suite?


public class TestVessels extends LogicTest {
  
  
  public static void main(String args[]) {
    testForeignToLand(false);
    testForeignToDock(false);
    testDockToForeign(false);
    testForeignSpawn (false);
    testLocalSpawn   (false);
  }
  
  
  
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
  
  
  static boolean testForeignSpawn(boolean graphics) {
    TestVessels test = new TestVessels();
    return test.vesselTest(false, false, false, "FOREIGN VESSEL SPAWN", graphics);
  }
  
  
  static boolean testLocalSpawn(boolean graphics) {
    TestVessels test = new TestVessels();
    return test.vesselTest(true, false, false, "LOCAL VESSEL SPAWN", graphics);
  }
  
  
  
  boolean vesselTest(
    boolean fromLocal, boolean goesDock, boolean createShip,
    String title, boolean graphics
  ) {
    
    World world = new World(ALL_GOODS);
    world.assignTypes(
      ALL_BUILDINGS, ALL_SHIPS(), ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES()
    );
    
    Base  homeC = new Base(world, world.addLocale(2, 2));
    Base  awayC = new Base(world, world.addLocale(3, 3));
    world.addBases(homeC, awayC);
    awayC.council.setTypeAI(BaseCouncil.AI_OFF);
    homeC.setName("(Home City)");
    awayC.setName("(Away City)");
    
    World.setupRoute(homeC.locale, awayC.locale, 10, Type.MOVE_AIR);
    Base.setPosture(homeC, awayC, Base.POSTURE.TRADING, true);
    homeC.setHomeland(awayC);
    
    Tally <Good> supplies = new Tally().setWith(GREENS, 10, PSALT, 5);
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
    world.settings.toggleShipping  = ! createShip;
    
    
    BuildingForTrade post = (BuildingForTrade) SUPPLY_DEPOT.generate();
    post.enterMap(map, 1, 6, 1, homeC);
    post.setProdLevels(true, ORES, 10);
    post.setNeedLevels(false, GREENS, 30, MEDICINE, 10);
    ActorUtils.fillWorkVacancies(post);
    
    int cashEstimate = 0, totalCash = -1;
    for (Good g : post.prodLevels().keys()) {
      float prod = post.prodLevels().valueFor(g);
      cashEstimate += prod * homeC.exportPrice(g, awayC);
    }
    for (Good g : post.needLevels().keys()) {
      float need = post.needLevels().valueFor(g);
      cashEstimate -= need * homeC.importPrice(g, awayC);
    }
    
    
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
    
    
    if (createShip) {
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
        ship.assignTask(trading, ship);
      }
      
      else {
        awayC.toggleVisitor(ship, true);
        for (Actor a : ship.crew()) {
          awayC.toggleVisitor(a, true);
          a.health.setHungerLevel(0.5f);
        }
        for (int n = HUNGER_REGEN; n-- > 0;) {
          map.update(1);
        }
        for (Actor a : ship.crew()) {
          if (a.health.hungerLevel() > 0) {
            I.say("WARNING: CREW WERE HUNGRY!");
          }
        }
        
        TaskTrading trading = BuildingForTrade.selectTraderBehaviour(
          awayC, ship, homeC, false, map
        );
        ship.assignTask(trading, ship);
        trading.beginFromOffmap(awayC);
      }
    }
    else if (fromLocal) {
      dock.beginUpgrade(UPGRADE_DROPSHIP);
      dock.setBuildLevel(1);
    }
    
    
    final int RUN_TIME = YEAR_LENGTH;
    boolean spawnDone   = false;
    boolean shipComing  = false;
    boolean shipArrive  = false;
    boolean shipLanded  = false;
    boolean shipTraded  = false;
    boolean migrateDone = false;
    boolean cashOkay    = false;
    boolean testOkay    = false;
    ActorAsVessel ship = null;
    
    while (map.time() < RUN_TIME || graphics) {
      runLoop(homeC, 1, graphics, "saves/test_vessels.str");
      
      if (createShip) {
        spawnDone = true;
      }
      else if (! spawnDone) {
        boolean hasShip = false;
        ActorAsVessel spawned = null;
        if (fromLocal) {
          for (Actor a : dock.workers()) if (a.type().isVessel()) {
            hasShip = true;
            spawned = (ActorAsVessel) a;
          }
        }
        else {
          for (ActorAsVessel v : awayC.traders()) if (v.guestBase() == homeC) {
            hasShip = true;
            spawned = v;
          }
        }
        
        /*
        if (hasShip) {
          int numCrew = 0;
          for (Actor a : spawned.crew()) {
            if (fromLocal && ! a.onMap()) continue;
            if (a.type() == Vassals.SUPPLY_CORPS) numCrew += 1;
          }
          hasShip &= numCrew >= 2;
        }
        //*/
        
        spawnDone = hasShip;
      }
      
      if (spawnDone && ! shipComing) {
        for (Journey j : world.journeys()) {
          for (Journeys g : j.going()) {
            if (j.goes() != homeC || ! g.isActor()) continue;
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
      
      if (shipTraded && ! cashOkay) {
        totalCash = 0;
        if (post != null) totalCash += post.inventory(CASH);
        if (dock != null) totalCash += dock.inventory(CASH);
        cashOkay = Nums.abs(totalCash - cashEstimate) < 1;
      }
      
      if (shipTraded && migrateDone && cashOkay && ! testOkay) {
        testOkay = true;
        I.say("\n"+title+" TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return true;
      }
    }
    
    I.say("\n"+title+" TEST FAILED!");
    I.say("  Spawn done:   "+spawnDone  );
    I.say("  Ship coming:  "+shipComing );
    I.say("  Ship arrive:  "+shipArrive );
    I.say("  Ship landed:  "+shipLanded );
    I.say("  Ship traded:  "+shipTraded );
    I.say("  Migrate done: "+migrateDone);
    I.say("  Cash okay:    "+cashOkay   );
    I.say("  Cash/estimate: "+totalCash+"/"+cashEstimate);
    
    return false;
  }
  
}



