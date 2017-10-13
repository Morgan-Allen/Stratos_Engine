

package game;
import static game.GameConstants.*;
import util.*;



public class TestCity extends Test {
  
  
  public static void main(String args[]) {
    testCity(true);
  }
  
  static void testCity(boolean graphics) {
    
    CityMap map = setupTestCity(32);
    
    CityMap.applyPaving(map, 3, 8, 25, 1 , true);
    CityMap.applyPaving(map, 8, 2, 1 , 25, true);
    
    Building palace  = (Building) PALACE    .generate();
    Building house1  = (Building) HOUSE     .generate();
    Building house2  = (Building) HOUSE     .generate();
    Building house3  = (Building) HOUSE     .generate();
    Building school  = (Building) SCHOOL    .generate();
    Building court   = (Building) BALL_COURT.generate();
    Building basin   = (Building) BASIN     .generate();
    Building latrine = (Building) LATRINE   .generate();
    Building admin   = (Building) COLLECTOR .generate();
    
    palace .enterMap(map, 3 , 3 , 1);
    house1 .enterMap(map, 9 , 6 , 1);
    house2 .enterMap(map, 12, 6 , 1);
    house3 .enterMap(map, 15, 6 , 1);
    court  .enterMap(map, 9 , 9 , 1);
    school .enterMap(map, 9 , 3 , 1);
    basin  .enterMap(map, 13, 9 , 1);
    latrine.enterMap(map, 16, 9 , 1);
    admin  .enterMap(map, 18, 9 , 1);
    
    Building quarry = (Building) QUARRY_PIT.generate();
    Building kiln1  = (Building) KILN      .generate();
    Building kiln2  = (Building) KILN      .generate();
    Building market = (Building) MARKET    .generate();
    
    quarry.enterMap(map, 4 , 15, 1);
    kiln1 .enterMap(map, 9 , 17, 1);
    kiln2 .enterMap(map, 9 , 14, 1);
    market.enterMap(map, 4 , 9 , 1);
    
    for (Building b : map.buildings) if (b.type == HOUSE) {
      b.inventory.set(NIGHTSOIL, 2 );
      b.inventory.set(CASH    , 20);
    }
    
    try {
      Session.saveSession("test_save.tlt", map);
      Session loaded = Session.loadSession("test_save.tlt", true);
      map = (CityMap) loaded.loaded()[0];
    }
    catch(Exception e) {
      I.report(e);
      return;
    }
    
    boolean housesOkay = false;
    
    while (map.time < 1000 || graphics) {
      runGameLoop(map, 10, graphics);
      
      if (! housesOkay) {
        boolean allNeeds = true;
        for (Building b : map.buildings) {
          if (b.type == MARKET && b.inventory.valueFor(COTTON) < 10) {
            b.inventory.add(10, COTTON);
          }
          if (b.type == HOUSE) {
            BuildingForHome home = (BuildingForHome) b;
            if (home.currentTier != HOUSE_T2             ) allNeeds = false;
            if (home.inventory.valueFor(NIGHTSOIL) > 0.5f) allNeeds = false;
            if (home.inventory.valueFor(CASH     ) > 5.0f) allNeeds = false;
          }
        }
        housesOkay = allNeeds;
        
        if (housesOkay) {
          I.say("\nCITY SERVICES TEST CONCLUDED SUCCESSFULLY!");
          if (! graphics) return;
        }
      }
    }

    I.say("\nCITY SERVICES TEST FAILED!");
  }
  
}







