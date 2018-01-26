

package game;
import static game.GameContent.*;



public class TestBlankMap extends Test {
  
  
  public static void main(String args[]) {
    String filename = "saves/blank_map.tlt";
    Test test = new TestBlankMap();
    
    CityMap map = loadMap(null, filename);
    if (map == null) map = setupTestCity(32, ALL_GOODS, true, MEADOW, JUNGLE);
    
    while (true) {
      map = test.runLoop(map, 10, true, filename);
    }
  }
  
}