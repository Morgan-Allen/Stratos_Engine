

package game;
import util.*;
import static game.GameConstants.*;



public class TestMilitary extends Test {
  
  
  public static void main(String args[]) {
    testMilitary(true);
  }
  
  static void testMilitary(boolean graphics) {
    
    World   world = GameConstants.setupDefaultWorld();
    City    cityA = world.cities.atIndex(0);
    City    cityB = world.cities.atIndex(1);
    CityMap map   = CityMapTerrain.generateTerrain(
      cityA, 32, MEADOW, JUNGLE
    );
    cityA.name = "Home City";
    cityB.name = "Away City";
    map.settings.toggleFog = false;
    
    City.setupRoute(cityA, cityB, 1);
    City.setPosture(cityA, cityB, City.POSTURE.ENEMY, true);
    
    
    BuildingForArmy fort = (BuildingForArmy) GARRISON.generate();
    fort.enterMap(map, 10, 10, 1);
    fillWorkVacancies(fort);
    CityMap.applyPaving(map, 2, 9, 30, 1, true);
    
    Formation troops = fort.formation;
    
    for (int n = 8; n-- > 0;) {
      Building house = (Building) HOUSE.generate();
      house.enterMap(map, 2 + (n * 3), 7, 1);
      fillHomeVacancies(house, CITIZEN);
    }
    
    
    Formation enemies = new Formation();
    enemies.setupFormation(GARRISON, cityB);
    for (int n = 4; n-- > 0;) {
      Actor fights = (Actor) ((n == 0) ? SOLDIER : CITIZEN).generate();
      fights.assignHomeCity(cityB);
      enemies.toggleRecruit(fights, true);
    }
    cityB.armyPower = AVG_ARMY_POWER / 4;
    
    boolean recruited = false;
    boolean invaded   = false;
    boolean homeWin   = false;
    boolean invading  = false;
    boolean awayWin   = false;
    boolean backHome  = false;
    
    while (map.time < 1000 || graphics) {
      map = runGameLoop(map, 10, graphics, "saves/test_military.tlt");
      
      if (troops.recruits.size() >= 8 && ! recruited) {
        troops.beginSecuring(map.tileAt(25, 25), TileConstants.E, null);
        recruited = true;
      }
      
      if (troops.formationReady() && ! invaded) {
        enemies.beginSecuring(cityA);
        World.Journey j = world.beginJourney(cityB, cityA, enemies);
        world.completeJourney(j);
        enemies.beginSecuring(troops.securedPoint, TileConstants.W, troops);
        invaded = true;
      }
      
      if (recruited && ! homeWin) {
        boolean survivors = false;
        for (Actor w : enemies.recruits) {
          if (w.state < Actor.STATE_DEAD) survivors = true;
        }
        homeWin = ! survivors;
        if (homeWin) fort.formation.stopSecuringPoint();
      }
      
      if (homeWin && troops.recruits.size() >= 12 && ! invading) {
        troops.beginSecuring(cityB);
        troops.assignDemands(City.POSTURE.VASSAL, null, null);
        invading = true;
      }
      
      if (invading && cityA.isLordOf(cityB)) {
        awayWin = true;
      }
      
      if (awayWin && ! backHome) {
        boolean someAway = false;
        for (Actor w : troops.recruits) {
          if (w.map != map) someAway = true;
        }
        backHome = troops.recruits.size() > 8 && ! someAway;
        
        if (backHome) {
          I.say("\nMILITARY TEST CONCLUDED SUCCESSFULLY!");
          if (! graphics) return;
        }
      }
    }
    
    I.say("\nMILITARY TEST FAILED!");
  }
  
  
  
}




