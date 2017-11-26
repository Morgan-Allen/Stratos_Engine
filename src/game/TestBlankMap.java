

package game;



public class TestBlankMap extends Test {
  
  
  public static void main(String args[]) {
    String filename = "saves/blank_map.tlt";
    Test test = new TestBlankMap();
    
    CityMap map = loadMap(null, filename);
    if (map == null) map = setupTestCity(32);
    
    while (true) {
      map = test.runLoop(map, 10, true, filename);
    }
  }
  
}