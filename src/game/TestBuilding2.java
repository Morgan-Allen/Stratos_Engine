

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
    
    
    Building farm = (Building) FARM_PLOT.generate();
    farm.enterMap(map, 1, 1, 1);
    Element tree = (Element) JUNGLE_TREE1.generate();
    tree.enterMap(map, 6, 2, 1);
    Element toRaze[] = { farm, tree };
    
    
    BuildingForTrade post = (BuildingForTrade) map.planning.placeObject(
      PORTER_POST, 2, 2
    );
    post.ID = "(Stock of Goods)";
    post.setTradeLevels(true,
      CLAY  , 40,
      STONE , 40,
      WOOD  , 60,
      COTTON, 20
    );
    Building home   = (Building) map.planning.placeObject(HOUSE , 6, 6);
    Building palace = (Building) map.planning.placeObject(PALACE, 6, 0);
    //Building mason  = (Building) map.planning.placeObject(MASON , 9, 6);
    
    Building mason = (Building) MASON.generate();
    mason.enterMap(map, 9, 6, 1);
    
    Building toBuild[] = { post, home, palace, mason };
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
    
    
    //  TODO:  You need to ensure that any structures in the way of
    //  construction are flagged for razing and salvaged beforehand.
    
    //  TODO:  You also need to test for more complex structures, such
    //  as elite housing, with non-standard materials.  Both building
    //  and salvage.
    
    //  TODO:  You should also test to ensure that wear-and-tear is
    //  repaired over time.
    
    //  TODO:  And finally, you need to ensure that building can take
    //  place even if the store itself is unfinished.
    
    float matDiff = -1;
    boolean razingOkay   = false;
    boolean buildingOkay = false;
    boolean pavingOkay   = false;
    boolean materialOkay = false;
    boolean testOkay     = false;
    
    
    while (map.time < 1000 || graphics) {
      runGameLoop(map, 10, graphics, "saves/test_building.tlt");
      
      if (! razingOkay) {
        boolean allRazed = true;
        for (Element e : toRaze) {
          if (e.onMap()) allRazed = false;
        }
        razingOkay = allRazed;
      }
      
      if (! buildingOkay) {
        boolean allBuilt = true;
        for (Building b : toBuild) {
          if (b.buildLevel() < 1) allBuilt = false;
        }
        buildingOkay = allBuilt;
      }
      
      if (! pavingOkay) {
        boolean allPaved = true;
        for (Tile t : toPave) {
          if (t.above == null || t.above.type != ROAD) {
            allPaved = false;
          }
        }
        pavingOkay = allPaved;
      }
      
      if (! materialOkay) {
        matDiff = mason.inventory.valueFor(STONE) - stoneLeft;
        for (Actor a : mason.workers) matDiff += a.carried(STONE);
        if (Nums.abs(matDiff) < 0.1f) materialOkay = true;
      }
      
      if (pavingOkay && materialOkay && ! testOkay) {
        I.say("\nBUILDING TEST CONCLUDED SUCCESSFULLY!");
        testOkay = true;
        if (! graphics) return true;
      }
    }
    
    I.say("\nBUILDING TEST FAILED!");
    I.say("  Razing okay:   "+razingOkay  );
    I.say("  Building okay: "+buildingOkay);
    I.say("  Paving okay:   "+pavingOkay  );
    I.say("  Material okay: "+materialOkay);
    I.say("  Material diff: "+matDiff);
    
    return false;
  }
  
}








