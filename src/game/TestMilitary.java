

package game;
import static game.GameConstants.*;
import util.*;



public class TestMilitary extends TestLoop {
  
  
  public static void main(String args[]) {

    World   world = GameConstants.setupDefaultWorld();
    City    cityA = world.cityNamed("Tlacopan"  );
    City    cityB = world.cityNamed("Xochimilco");
    CityMap map   = CityMapGenerator.generateTerrain(
      50, MEADOW, JUNGLE
    );
    map.attachCity(cityA);
    
    BuildingForMilitary fort = (BuildingForMilitary) GARRISON.generate();
    fort.enterMap(map, 20, 20);
    fort.formation.beginSecuring(map.tileAt(30, 40), TileConstants.E);
    
    for (int n = 8; n-- > 0;) {
      Building house = (Building) HOUSE.generate();
      house.enterMap(map, 10 + (n * 3), 17);
    }
    CityMap.applyPaving(map, 10, 19, 40, 1, true);
    
    
    Formation enemies = new Formation(GARRISON);
    
    for (int n = 4; n-- > 0;) {
      Walker fights = (Walker) ((n == 0) ? SOLDIER : CITIZEN).generate();
      enemies.toggleRecruit(fights, true);
    }
    
    while (true) {
      runGameLoop(map, 10);
      
      if (fort.formation.recruits.size() >= 8) {
        World.Journey j = world.beginJourney(cityB, cityA, enemies);
        world.completeJourney(j);
        enemies.beginSecuring(fort.formation.securedPoint, TileConstants.W);
      }
    }
  }
  
  
  
}



