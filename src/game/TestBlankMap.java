

package game;
import util.*;
import static game.GameConstants.*;



public class TestBlankMap extends Test {
  
  
  public static void main(String args[]) {
    String filename = "saves/blank_map.tlt";
    
    CityMap map = loadMap(null, filename);
    if (map == null) map = setupTestCity(32);
    
    while (true) {
      map = runGameLoop(map, 10, true, filename);
    }
  }
  
}