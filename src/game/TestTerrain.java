

package game;
import static game.GameConstants.*;
import util.*;



public class TestTerrain extends TestLoop {
  
  
  public static void main(String args[]) {
    
    CityMap map = CityMapGenerator.generateTerrain(20, DESERT, MEADOW, JUNGLE);
    
    Building farm = (Building) FARMER_HUT.generate();
    farm.enterMap(map, 9, 9);
    CityMap.applyPaving(map, 9, 8, 10, 1, true);
    
    for (Coord c : Visit.grid(6, 6, 8, 8, 1)) {
      if (map.blocked(c.x, c.y)) continue;
      if (map.paved  (c.x, c.y)) continue;
      
      Fixture crop;
      if (Rand.yes()) crop = new Fixture(MAIZE     );
      else            crop = new Fixture(RAW_COTTON);
      crop.enterMap(map, c.x, c.y);
      crop.buildLevel = 0.5f + Rand.num();
    }
    
    CityMapGenerator.populateFixtures(map);
    
    runGameLoop(map, -1);
  }
  
}