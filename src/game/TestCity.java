

package game;
import static game.GameConstants.*;
import util.*;



public class TestCity extends TestLoop {
  
  public static void main(String args[]) {
    
    CityMap map = new CityMap();
    map.performSetup(50);
    
    CityMap.applyPaving(map, 3, 8, 12, 1 , true);
    CityMap.applyPaving(map, 8, 2, 1 , 16, true);
    
    Building palace = (Building) PALACE    .generate();
    Building house1 = (Building) HOUSE     .generate();
    Building house2 = (Building) HOUSE     .generate();
    Building court  = (Building) BALL_COURT.generate();
    
    palace.enterMap(map, 3 , 3 , 1);
    house1.enterMap(map, 9 , 6 , 1);
    house2.enterMap(map, 12, 6 , 1);
    court .enterMap(map, 9 , 9 , 1);
    
    Building quarry = (Building) QUARRY_PIT.generate();
    Building kiln1  = (Building) KILN      .generate();
    Building kiln2  = (Building) KILN      .generate();
    Building market = (Building) MARKET    .generate();
    
    quarry.enterMap(map, 4 , 15, 1);
    kiln1 .enterMap(map, 9 , 17, 1);
    kiln2 .enterMap(map, 9 , 14, 1);
    market.enterMap(map, 4 , 9 , 1);
    
    quarry.inventory.add(2, CLAY   );
    market.inventory.add(3, POTTERY);
    
    try {
      Session.saveSession("test_save.tlt", map);
      Session loaded = Session.loadSession("test_save.tlt", true);
      map = (CityMap) loaded.loaded()[0];
    }
    catch(Exception e) {
      I.report(e);
      return;
    }
    
    runGameLoop(map, -1);
  }
  
}

