

package game;
import static game.GameConstants.*;
import util.*;



public class TestSieging extends Test {
  
  
  public static void main(String args[]) {
    testSieging(true);
  }
  
  
  static void testSieging(boolean graphics) {

    World   world = GameConstants.setupDefaultWorld();
    City    cityA = world.cities.atIndex(0);
    City    cityB = world.cities.atIndex(1);
    CityMap map   = CityMapGenerator.generateTerrain(
      cityA, 50, MEADOW, JUNGLE
    );
    cityA.attachMap(map);
    cityA.name = "Home City";
    cityB.name = "Away City";
    
    City.setupRoute(cityA, cityB, 1);
    City.setRelations(cityA, City.RELATION.ENEMY, cityB, City.RELATION.ENEMY);
    
    BuildingForMilitary fort = (BuildingForMilitary) GARRISON.generate();
    fort.enterMap(map, 20, 20, 1);
    CityMap.applyPaving(map, 10, 19, 40, 1, true);
    
    
    
    Formation enemies = new Formation();
    enemies.setupFormation(GARRISON, cityB);
    
    for (int n = 8; n-- > 0;) {
      Walker fights = (Walker) ((n < 3) ? SOLDIER : CITIZEN).generate();
      fights.assignHomeCity(cityB);
      enemies.toggleRecruit(fights, true);
    }
    enemies.beginSecuring(cityA);
    
    boolean victorious = false;
    
    while (map.time < 1000 || graphics) {
      map = runGameLoop(map, 10, graphics);
      
      if (cityA.hasLord(cityB) && (! enemies.away) && ! victorious) {
        victorious = true;
        I.say("\nSIEGING TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return;
      }
    }
    
    I.say("\nSIEGE TEST FAILED!");
  }
  
  
  
}



