

package game;
import static game.GameConstants.*;
import util.*;



public class TestSieging extends TestLoop {
  
  
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
    
    while (true) {
      map = runGameLoop(map, 10);
      
      if (cityA.hasLord(cityB) && (! enemies.away) && ! victorious) {
        victorious = true;
        I.say("\nTEST CONCLUDED SUCCESSFULLY!");
      }
    }
  }
  
  
  
}



