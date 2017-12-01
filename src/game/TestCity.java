

package game;
import util.*;
import static game.GameConstants.*;



public class TestCity extends Test {
  
  
  public static void main(String args[]) {
    testCity(false);
  }
  
  
  static boolean testCity(boolean graphics) {
    Test test = new TestCity();
    
    CityMap map = setupTestCity(32);
    map.settings.toggleFog = false;
    
    CityMapPlanning.placeStructure(ROAD, map, true, 3, 8, 25 , 1);
    CityMapPlanning.placeStructure(ROAD, map, true, 8, 2 , 1, 25);
    
    Building school  = (Building) SCHOOL    .generate();
    Building court   = (Building) BALL_COURT.generate();
    Building basin   = (Building) BASIN     .generate();
    Building sweeper = (Building) SWEEPER   .generate();
    Building admin   = (Building) COLLECTOR .generate();
    Building mason   = (Building) MASON     .generate();
    
    court  .enterMap(map, 9 , 9 , 1);
    school .enterMap(map, 9 , 3 , 1);
    basin  .enterMap(map, 13, 9 , 1);
    sweeper.enterMap(map, 16, 9 , 1);
    admin  .enterMap(map, 18, 9 , 1);
    mason  .enterMap(map, 5 , 5 , 1);
    
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
      map.planning.placeObject(b);
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
    boolean goodsOkay  = true ;
    boolean testOkay   = false;
    
    while (map.time < RUN_TIME || graphics) {
      map = test.runLoop(map, 10, graphics, "saves/test_city.tlt");
      
      if (goodsOkay) {
        for (Building b : map.buildings) {
          if (b.type == HOUSE) {
            BuildingForHome home = (BuildingForHome) b;
            for (Good g : home.usedBy(home.currentTier)) {
              float need = home.maxStock(g) + 1;
              float have = home.inventory.valueFor(g);
              if (have > need + 1) goodsOkay = false;
            }
          }
        }
      }
      
      if (! housesOkay) {
        boolean allNeeds = true;
        for (Building b : map.buildings) {
          if (b.type == BASIN) {
            b.inventory.set(WATER, 10);
          }
          if (b.type == MARKET) {
            b.inventory.set(COTTON, 10);
            b.inventory.set(MAIZE , 10);
          }
          if (b.type == MASON) {
            b.inventory.set(STONE, 10);
            b.inventory.set(WOOD , 10);
          }
          if (b.type == HOUSE) {
            BuildingForHome home = (BuildingForHome) b;
            if (home.currentTier != HOUSE_T2        ) allNeeds = false;
            if (home.inventory.valueFor(SOIL) > 1.0f) allNeeds = false;
            if (home.inventory.valueFor(CASH) > 5.0f) allNeeds = false;
          }
        }
        housesOkay = allNeeds;
      }
      
      if (housesOkay && goodsOkay && ! testOkay) {
        I.say("\nCITY SERVICES TEST CONCLUDED SUCCESSFULLY!");
        testOkay = true;
        reportOnMap(map, true);
        if (! graphics) return true;
      }
    }
    
    I.say("\nCITY SERVICES TEST FAILED!");
    reportOnMap(map, false);
    return false;
  }
  
  
  static void reportOnMap(CityMap map, boolean okay) {
    if (! okay) for (Building b : map.buildings) {
      if (b.type.isHomeBuilding()) {
        BuildingForHome house = (BuildingForHome) b;
        I.say("  "+house);
        I.say("    Tier:      "+house.currentTier);
        I.say("    Inventory: "+house.inventory);
      }
    }
    I.say("\nTotal goods produced:");
    for (Good g : HOUSE_T2.homeUseGoods) {
      I.say("  "+g+": "+map.city.makeTotals.valueFor(g));
    }
    I.say("\nTotal goods consumed:");
    for (Good g : HOUSE_T2.homeUseGoods) {
      I.say("  "+g+": "+map.city.usedTotals.valueFor(g));
    }
  }
  
}







