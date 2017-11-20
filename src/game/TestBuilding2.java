

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class TestBuilding2 extends Test {
  
  
  public static void main(String args[]) {
    testBuilding2(true);
  }
  
  
  static boolean testBuilding2(boolean graphics) {
    
    CityMap map = setupTestCity(16);
    map.settings.toggleFog = false;
    
    Building mason = (Building) MASON.generate();
    mason.enterMap(map, 9, 6, 1);
    fillWorkVacancies(mason);
    mason.inventory.set(STONE, 10);
    
    Batch <Tile> toPave = new Batch();
    float stoneLeft = mason.inventory.valueFor(STONE);
    for (Coord c : Visit.grid(2, 2, 10, 1, 1)) {
      Tile t = map.tileAt(c);
      map.planning.placeObject(ROAD, t);
      toPave.add(t);
      stoneLeft -= ROAD.materialNeed(STONE);
    }
    
    float matDiff = -1;
    boolean buildingOkay = false;
    boolean materialOkay = false;
    boolean testOkay     = false;
    
    
    while (map.time < 1000 || graphics) {
      runGameLoop(map, 10, graphics, "saves/test_building.tlt");
      
      if (! buildingOkay) {
        boolean allBuilt = true;
        for (Tile t : toPave) {
          if (t.above == null || t.above.type != ROAD) {
            allBuilt = false;
          }
        }
        buildingOkay = allBuilt;
      }
      
      if (! materialOkay) {
        matDiff = mason.inventory.valueFor(STONE) - stoneLeft;
        for (Actor a : mason.workers) matDiff += a.carried(STONE);
        if (Nums.abs(matDiff) < 0.1f) materialOkay = true;
      }
      
      if (buildingOkay && materialOkay && ! testOkay) {
        I.say("\nBUILDING TEST CONCLUDED SUCCESSFULLY!");
        testOkay = true;
        if (! graphics) return true;
      }
    }
    
    I.say("\nBUILDING TEST FAILED!");
    I.say("  Building okay: "+buildingOkay);
    I.say("  Material okay: "+materialOkay);
    I.say("  Material diff: "+matDiff);
    
    return false;
  }
  
}








