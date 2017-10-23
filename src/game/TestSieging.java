

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
    CityMap map   = CityMapTerrain.generateTerrain(
      cityA, 32, MEADOW, JUNGLE
    );
    map.settings.toggleFog = false;
    cityA.name = "Home City";
    cityB.name = "Away City";
    
    City.setupRoute(cityA, cityB, 1);
    City.setRelations(cityA, City.RELATION.ENEMY, cityB, City.RELATION.ENEMY);
    
    BuildingForArmy fort = (BuildingForArmy) GARRISON.generate();
    fort.enterMap(map, 10, 10, 1);
    CityMap.applyPaving(map, 10, 9, 40, 1, true);
    
    
    Formation enemies = new Formation();
    enemies.setupFormation(GARRISON, cityB);
    
    for (int n = 8; n-- > 0;) {
      Actor fights = (Actor) ((n < 3) ? SOLDIER : CITIZEN).generate();
      fights.assignHomeCity(cityB);
      enemies.toggleRecruit(fights, true);
    }
    enemies.beginSecuring(cityA);
    
    boolean victorious = false;
    
    while (map.time < 1000 || graphics) {
      map = runGameLoop(map, 10, graphics, "saves/test_sieging.tlt");
      
      if (cityA.hasLord(cityB) && (! enemies.away) && ! victorious) {
        victorious = true;
        I.say("\nSIEGING TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return;
      }
    }
    
    I.say("\nSIEGE TEST FAILED!");
  }
  
  
  
}



