

package game;
import static game.GameConstants.*;
import util.*;



public class TestMilitary extends TestLoop {
  
  
  public static void main(String args[]) {

    World   world = GameConstants.setupDefaultWorld();
    City    cityA = world.cities.atIndex(0);
    City    cityB = world.cities.atIndex(1);
    CityMap map   = CityMapGenerator.generateTerrain(
      50, MEADOW, JUNGLE
    );
    map.attachCity(cityA);
    cityA.name = "Home City";
    cityB.name = "Away City";
    cityB.setArmyPower(2);
    
    City.setupRoute(cityA, cityB, 1);
    City.setRelations(cityA, City.RELATION.ENEMY, cityB, City.RELATION.ENEMY);
    
    
    BuildingForMilitary fort = (BuildingForMilitary) GARRISON.generate();
    fort.enterMap(map, 20, 20);
    
    Formation troops = new Formation();
    troops.setupFormation(GARRISON, cityA);
    fort.assignFormation(troops);
    
    for (int n = 8; n-- > 0;) {
      Building house = (Building) HOUSE.generate();
      house.enterMap(map, 10 + (n * 3), 17);
    }
    CityMap.applyPaving(map, 10, 19, 40, 1, true);
    
    
    Formation enemies = new Formation();
    enemies.setupFormation(GARRISON, cityB);
    
    for (int n = 4; n-- > 0;) {
      Walker fights = (Walker) ((n == 0) ? SOLDIER : CITIZEN).generate();
      fights.assignHomeCity(cityB);
      enemies.toggleRecruit(fights, true);
    }
    
    boolean recruited = false;
    boolean invaded   = false;
    boolean homeWin   = false;
    boolean invading  = false;
    boolean awayWin   = false;
    boolean backHome  = false;
    
    while (true) {
      map = runGameLoop(map, 10);
      
      if (troops.recruits.size() >= 8 && ! recruited) {
        troops.beginSecuring(map.tileAt(30, 40), TileConstants.E);
        recruited = true;
      }
      
      if (troops.formationReady() && ! invaded) {
        World.Journey j = world.beginJourney(cityB, cityA, enemies);
        world.completeJourney(j);
        enemies.beginSecuring(troops.securedPoint, TileConstants.W);
        invaded = true;
      }
      
      if (recruited && ! homeWin) {
        boolean survivors = false;
        for (Walker w : enemies.recruits) {
          if (w.state < Walker.STATE_DEAD) survivors = true;
        }
        homeWin = ! survivors;
        if (homeWin) fort.formation.stopSecuringPoint();
      }
      
      if (homeWin && troops.recruits.size() >= 8 && ! invading) {
        troops.beginSecuring(cityB);
        invading = true;
      }
      
      if (invading && cityA.isVassal(cityB)) {
        awayWin = true;
      }
      
      if (awayWin && ! backHome) {
        boolean someAway = false;
        for (Walker w : troops.recruits) {
          if (w.map != map) someAway = true;
        }
        backHome = ! someAway;
        
        if (backHome) {
          I.say("\nTEST CONCLUDED SUCCESSFULLY!");
        }
      }
    }
  }
  
  
  
}




