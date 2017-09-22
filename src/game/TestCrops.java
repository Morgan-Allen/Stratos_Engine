


package game;
import static game.Goods.*;
import static game.BuildingSet.*;
import util.*;



public class TestCrops extends TestLoop {
  
  
  
  
  public static void main(String args[]) {
    
    City map = new City();
    map.performSetup(48);
    
    for (Coord c : Visit.grid(12, 12, 32, 32, 1)) {
      GatherBuilding.Crop crop;
      if (Rand.yes()) crop = new GatherBuilding.Crop(MAIZE );
      else            crop = new GatherBuilding.Crop(COTTON);
      crop.enterMap(map, c.x, c.y);
    }
    
    runGameLoop(map);
  }
  
}
