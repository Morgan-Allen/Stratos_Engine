

package game;
import static game.BuildingSet.*;
import util.*;



public class TestCity extends TestLoop {
  
  
  public static void main(String args[]) {
    
    CityMap map = new CityMap();
    map.performSetup(50);
    
    Tile.applyPaving(map, 3, 8, 12, 1 , true);
    Tile.applyPaving(map, 8, 2, 1 , 16, true);

    Building palace = new HomeBuilding(PALACE    );
    Building house1 = new HomeBuilding(HOUSE     );
    Building house2 = new HomeBuilding(HOUSE     );
    Building court  = new Building    (BALL_COURT);
    
    palace.enterMap(map, 3 , 3 );
    house1.enterMap(map, 9 , 6 );
    house2.enterMap(map, 12, 6 );
    court .enterMap(map, 9 , 9 );

    Building quarry = new CraftBuilding(QUARRY_PIT);
    Building kiln1  = new CraftBuilding(KILN      );
    Building kiln2  = new CraftBuilding(KILN      );
    Building market = new CraftBuilding(MARKET    );
    
    quarry.enterMap(map, 4 , 15);
    kiln1 .enterMap(map, 9 , 17);
    kiln2 .enterMap(map, 9 , 14);
    market.enterMap(map, 4 , 9 );
    
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
    
    runGameLoop(map);
  }
  
  
}









