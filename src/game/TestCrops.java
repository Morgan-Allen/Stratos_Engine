

package game;
import static game.BuildingSet.*;
import util.*;



public class TestCrops extends TestLoop {
  
  
  public static void main(String args[]) {
    
    CityMap map = new CityMap();
    map.performSetup(20);
    
    Building farm = (Building) FARMER_HUT.generate();
    farm.enterMap(map, 9, 9);
    Tile.applyPaving(map, 9, 8, 10, 1, true);
    
    
    for (Coord c : Visit.grid(6, 6, 8, 8, 1)) {
      if (map.blocked(c.x, c.y)) continue;
      if (map.paved  (c.x, c.y)) continue;
      
      GatherBuilding.Crop crop;
      if (Rand.yes()) crop = new GatherBuilding.Crop(MAIZE     );
      else            crop = new GatherBuilding.Crop(RAW_COTTON);
      crop.enterMap(map, c.x, c.y);
      crop.buildLevel = 0.5f + Rand.num();
    }
    
    runGameLoop(map);
  }
  
}







