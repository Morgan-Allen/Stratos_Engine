

package test;
import game.*;
import static game.GameConstants.*;
import static game.BaseRelations.*;
import content.*;
import static content.GameContent.*;
import static content.GameWorld.*;
import util.*;




public class TestVesselMissions extends LogicTest {
  
  
  public static void main(String args[]) {
    
    testForeignToRaid(false);
    testRaidToForeign(false);
    
    //  TODO:  Now add tests for dialogue missions, and possibly for recon,
    //  security, etc.?
  }
  
  
  static boolean testForeignToRaid(boolean graphics) {
    
    //  TODO:  The troopers seem to be attacking the bastion in a strange
    //  formation?  Find out why.
    
    //  TODO:  Also, resident actors should maybe be defending more actively?
    
    TestVesselMissions test = new TestVesselMissions() {
      
      Mission generateMission(Base base, Base rival) {
        World world = base.world;
        Federation.setPosture(base.faction(), rival.faction(), BOND_ENEMY, world);
        
        Mission raid = new MissionForStrike(rival);
        raid.setWorldFocus(base);
        for (int n = 8; n-- > 0;) {
          Actor joins = (Actor) Trooper.TROOPER.generate();
          joins.assignBase(rival);
          raid.toggleRecruit(joins, true);
        }
        return raid;
      }
      
      boolean checkCompletion(Mission mission, Base base, Building centre) {
        return centre.destroyed();
      }
    };
    return test.vesselTest(false, "FOREIGN TO RAID", graphics);
  }
  
  static boolean testRaidToForeign(boolean graphics) {
    TestVesselMissions test = new TestVesselMissions() {
      
      Mission generateMission(Building centre, Building barracks, Base rival) {
        World world = centre.map().world;
        Base base = centre.base();
        Federation.setPosture(base.faction(), rival.faction(), BOND_ENEMY, world);
        
        Mission raid = new MissionForStrike(base);
        raid.setWorldFocus(rival);
        raid.terms.assignTerms(BOND_VASSAL, null, null, null);
        for (Actor a : barracks.workers()) raid.toggleRecruit(a, true);
        
        return raid;
      }
      
      boolean checkCompletion(Mission mission, Base base, Building centre) {
        Base away = mission.worldFocusBase();
        if (away.faction() != base.faction()) return false;
        //if (! away.relations.isVassalOf(base)) return false;
        return true;
      }
    };
    return test.vesselTest(true, "RAID TO FOREIGN", graphics);
  }
  
  
  
  
  boolean vesselTest(
    boolean fromLocal, String title, boolean graphics
  ) {
    
    Base  base  = LogicTest.setupTestBase(
      FACTION_SETTLERS_A, ALL_GOODS, 32, false, ALL_TERRAINS
    );
    World world = base.world;
    Area  map   = base.activeMap();
    
    world.settings.toggleFog     = false;
    world.settings.toggleMigrate = false;
    
    
    WorldLocale rivalAt = world.addLocale(4, 4);
    Base rival = new Base(world, rivalAt, FACTION_SETTLERS_B, "Rival Base");
    world.addBases(rival);
    World.setupRoute(rival.locale, base.locale, 1, Type.MOVE_AIR);
    
    
    
    Building centre = (Building) BASTION.generate();
    centre.enterMap(map, 10, 10, 1, base);
    ActorUtils.fillWorkVacancies(centre);
    
    Mission mission = null;
    ActorAsVessel ship = null;
    
    if (fromLocal) {
      Building barracks = (Building) TROOPER_LODGE.generate();
      barracks.enterMap(map, 4, 4, 1, base);
      ActorUtils.fillWorkVacancies(barracks);
      
      BuildingForDock airfield = (BuildingForDock) AIRFIELD.generate();
      airfield.enterMap(map, 10, 2, 1, base);
      ActorUtils.fillWorkVacancies(airfield);
      
      AreaTile dockPoint = airfield.nextFreeDockPoint();
      airfield.advanceShipConstruction(UPGRADE_DROPSHIP, dockPoint, 1);
      ship = airfield.docking().first();
      
      mission = generateMission(centre, barracks, rival);
      mission.assignTransport(ship);
      mission.beginMission();
    }
    else {
      mission = generateMission(base, rival);
      ship = (ActorAsVessel) Vassals.DROPSHIP.generate();
      ship.assignBase(rival);
      for (Actor a : mission.recruits()) a.setInside(ship, true);
      mission.assignTransport(ship);
      mission.beginMission();
    }
    
    
    final int RUN_TIME = YEAR_LENGTH;
    boolean shipArrive  = false;
    boolean shipDepart  = false;
    boolean missionDone = false;
    boolean crewReturn  = false;
    boolean testOkay    = false;
    
    while (map.time() < RUN_TIME || graphics) {
      runLoop(base, 1, graphics, "saves/test_vessel_missions.str");
      
      if (fromLocal) {
        
        if (! shipDepart) {
          if (! ship.onMap() && world.journeyFor(ship) != null) {
            shipDepart = true;
          }
          for (Actor a : mission.recruits()) {
            if (a.onMap()) shipDepart = false;
          }
        }
        
        if (shipDepart && ! missionDone) {
          if (checkCompletion(mission, base, centre)) {
            missionDone = true;
          }
        }
        
        if (missionDone && ! crewReturn) {
          boolean allBack = true;
          for (Actor a : mission.recruits()) {
            if (a.map() != map || a.inside() == ship) allBack = false;
          }
          if (ship.map() != map) allBack = false;
          if (allBack) {
            crewReturn = true;
          }
        }
      }
      else {
        
        if (! shipArrive) {
          if (ship.onMap() && ship.map() == map) {
            shipArrive = true;
          }
          for (Actor a : mission.recruits()) {
            if (a.offmap() != null) shipArrive = false;
          }
        }
        
        if (shipArrive && ! missionDone) {
          if (checkCompletion(mission, base, centre)) {
            missionDone = true;
          }
        }
        
        if (missionDone && ! crewReturn) {
          boolean allBack = true;
          for (Actor a : mission.recruits()) {
            if (a.offmap() != rival.locale) allBack = false;
          }
          if (ship.offmap() != rival.locale) allBack = false;
          if (allBack) {
            crewReturn = true;
          }
        }
      }
      
      if (crewReturn && ! testOkay) {
        testOkay = true;
        I.say("\n"+title+" TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return true;
      }
    }
    
    
    I.say("\n"+title+" TEST FAILED!");
    I.say("  Ship arrive: "+shipArrive );
    I.say("  Ship depart: "+shipDepart );
    I.say("  Raid done:   "+missionDone);
    I.say("  Crew return: "+crewReturn );
    
    return false;
  }
  
  
  Mission generateMission(Base base, Base rival) {
    return null;
  }
  
  Mission generateMission(Building centre, Building barracks, Base rival) {
    return null;
  }
  
  boolean checkCompletion(Mission mission, Base base, Building centre) {
    return false;
  }
  
  
}



