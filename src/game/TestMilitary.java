

package game;
import static game.GameConstants.*;
import util.*;



public class TestMilitary extends TestLoop {
  
  
  public static void main(String args[]) {
    
    
    CityMap map = CityMapGenerator.generateTerrain(
      50, MEADOW, JUNGLE
    );
    
    BuildingForMilitary fort = (BuildingForMilitary) GARRISON.generate();
    fort.enterMap(map, 20, 20);
    fort.formation.beginSecuring(map.tileAt(30, 40), TileConstants.E);
    
    for (int n = 8; n-- > 0;) {
      Building house = (Building) HOUSE.generate();
      house.enterMap(map, 10 + (n * 3), 17);
    }
    CityMap.applyPaving(map, 10, 19, 40, 1, true);
    
    runGameLoop(map);
  }
  
}