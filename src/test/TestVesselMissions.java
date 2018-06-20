

package test;
import content.*;
import static content.GameContent.*;
import game.*;
import static game.GameConstants.*;
import util.*;




public class TestVesselMissions extends LogicTest {
  
  
  public static void main(String args[]) {
    testForeignToRaid(true);
    testRaidToForeign(true);
  }
  
  
  static boolean testForeignToRaid(boolean graphics) {
    
    //  TODO:  The troopers seem to be attacking the bastion in a strange
    //  formation?  Find out why.
    
    //  TODO:  Also, resident actors should maybe be defending more actively?
    
    TestVesselMissions test = new TestVesselMissions() {
      
      Mission generateMission(Base base, Base rival) {
        Mission raid = new MissionStrike(rival);
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
    return test.vesselTest("FOREIGN TO RAID", graphics);
  }
  
  static boolean testRaidToForeign(boolean graphics) {
    //TestVesselMissions test = new TestVesselMissions();
    //return test.vesselTest("RAID TO FOREIGN", graphics);
    return false;
  }
  
  
  
  
  boolean vesselTest(
    String title, boolean graphics
  ) {
    
    Base  base  = LogicTest.setupTestBase(32, ALL_GOODS, false, ALL_TERRAINS);
    World world = base.world;
    Area  map   = base.activeMap();
    
    world.settings.toggleFog     = false;
    world.settings.toggleMigrate = false;
    
    Building centre = (Building) BASTION.generate();
    centre.enterMap(map, 10, 10, 1, base);
    ActorUtils.fillWorkVacancies(centre);
    
    
    WorldLocale rivalAt = world.addLocale(4, 4);
    Base rival = new Base(world, rivalAt, "Rival Base");
    world.addBases(rival);
    
    World.setupRoute(rival.locale, base.locale, 1, Type.MOVE_AIR);
    Base.setPosture(base, rival, Base.POSTURE.ENEMY, true);
    
    
    Mission mission = generateMission(base, rival);
    ActorAsVessel ship = (ActorAsVessel) Vassals.DROPSHIP.generate();
    ship.assignBase(rival);
    for (Actor a : mission.recruits()) a.setInside(ship, true);
    mission.assignTransport(ship);
    mission.beginMission(rival);
    
    
    
    final int RUN_TIME = YEAR_LENGTH;
    boolean shipArrive  = false;
    boolean missionDone = false;
    boolean crewReturn  = false;
    boolean testOkay    = false;
    
    while (map.time() < RUN_TIME || graphics) {
      runLoop(base, 1, graphics, "saves/test_vessel_missions.str");
      
      if (! shipArrive) {
        if (ship.onMap() && ship.map() == map) {
          shipArrive = true;
        }
        for (Actor a : mission.recruits()) {
          if (a.offmapBase() != null) shipArrive = false;
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
          if (a.offmapBase() != rival) allBack = false;
        }
        if (ship.offmapBase() != rival) allBack = false;
        if (allBack) {
          crewReturn = true;
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
    I.say("  Raid done:   "+missionDone);
    I.say("  Crew return: "+crewReturn );
    
    return false;
  }
  
  
  Mission generateMission(Base base, Base rival) {
    return null;
  }
  
  boolean checkCompletion(Mission mission, Base base, Building centre) {
    return false;
  }
  
  
}



