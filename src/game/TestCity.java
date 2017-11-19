

package game;
import util.*;
import static game.GameConstants.*;



public class TestCity extends Test {
  
  
  public static void main(String args[]) {
    testCity(true);
  }
  
  
  static boolean testCity(boolean graphics) {
    
    CityMap map = setupTestCity(32);
    
    CityMapPlanning.applyPaving(map, 3, 8, 25, 1 , true);
    CityMapPlanning.applyPaving(map, 8, 2, 1 , 25, true);
    
    Building palace  = (Building) PALACE    .generate();
    Building school  = (Building) SCHOOL    .generate();
    Building court   = (Building) BALL_COURT.generate();
    Building basin   = (Building) BASIN     .generate();
    Building sweeper = (Building) SWEEPER   .generate();
    Building admin   = (Building) COLLECTOR .generate();
    
    palace .enterMap(map, 3 , 3 , 1);
    court  .enterMap(map, 9 , 9 , 1);
    school .enterMap(map, 9 , 3 , 1);
    basin  .enterMap(map, 13, 9 , 1);
    sweeper.enterMap(map, 16, 9 , 1);
    admin  .enterMap(map, 18, 9 , 1);
    
    for (int n = 4; n-- > 0;) {
      Building house = (Building) HOUSE.generate();
      house.enterMap(map, 9 + (n * 3), 6, 1f);
    }
    
    Building quarry = (Building) QUARRY_PIT.generate();
    Building kiln1  = (Building) KILN      .generate();
    Building kiln2  = (Building) KILN      .generate();
    Building market = (Building) MARKET    .generate();
    
    quarry.enterMap(map, 4 , 15, 1);
    kiln1 .enterMap(map, 9 , 17, 1);
    kiln2 .enterMap(map, 9 , 14, 1);
    market.enterMap(map, 4 , 9 , 1);
    
    for (int n = 4; n-- > 0;) {
      Element rock = new Element(CLAY_BANK1);
      rock.enterMap(map, 1 + (n * 3), 28, 1);
    }
    
    CityMapFlagging forRock = map.flagging.get(IS_STONE);
    if (forRock.totalSum() != 4) {
      I.say("NO ROCKS FLAGGED: "+forRock.totalSum());
      return false;
    }
    
    for (Building b : map.buildings) {
      if (b.type == HOUSE) {
        b.inventory.set(SOIL, 2 );
        b.inventory.set(CASH, 20);
      }
      if (b.type == KILN) {
        b.inventory.set(CLAY   , 1);
        b.inventory.set(POTTERY, 1);
      }
    }
    
    try {
      Session.saveSession("saves/test_save.tlt", map);
      Session loaded = Session.loadSession("saves/test_save.tlt", true);
      map = (CityMap) loaded.loaded()[0];
    }
    catch(Exception e) {
      I.report(e);
      return false;
    }
    
    final int RUN_TIME = YEAR_LENGTH;
    boolean housesOkay = false;
    
    while (map.time < RUN_TIME || graphics) {
      runGameLoop(map, 10, graphics, "saves/test_city.tlt");
      
      if (! housesOkay) {
        boolean allNeeds = true;
        for (Building b : map.buildings) {
          if (b.type == MARKET) {
            b.inventory.set(COTTON, 10);
            b.inventory.set(MAIZE , 10);
          }
          if (b.type == HOUSE) {
            BuildingForHome home = (BuildingForHome) b;
            if (home.currentTier != HOUSE_T2        ) allNeeds = false;
            if (home.inventory.valueFor(SOIL) > 1.0f) allNeeds = false;
            if (home.inventory.valueFor(CASH) > 5.0f) allNeeds = false;
          }
        }
        housesOkay = allNeeds;
        
        if (housesOkay) {
          I.say("\nCITY SERVICES TEST CONCLUDED SUCCESSFULLY!");
          reportOnMap(map, true);
          if (! graphics) return true;
        }
      }
    }

    I.say("\nCITY SERVICES TEST FAILED!");
    reportOnMap(map, false);
    return false;
  }
  
  
  static void reportOnMap(CityMap map, boolean okay) {
    if (! okay) for (Building b : map.buildings) if (b.type == HOUSE) {
      BuildingForHome house = (BuildingForHome) b;
      I.say("  "+house);
      I.say("    Tier:      "+house.currentTier);
      I.say("    Inventory: "+house.inventory);
    }
    I.say("\nTotal goods produced:");
    for (Good g : HOUSE_T2.homeUsed) {
      I.say("  "+g+": "+map.city.makeTotals.valueFor(g));
    }
    I.say("\nTotal goods consumed:");
    for (Good g : HOUSE_T2.homeUsed) {
      I.say("  "+g+": "+map.city.usedTotals.valueFor(g));
    }
  }
  
}







