


package game;
import util.*;
import static game.GameConstants.*;
import static game.GameContent.*;
import static game.Task.*;



public class TestCity extends Test {
  
  
  public static void main(String args[]) {
    testCity(true);
  }
  
  
  static boolean testCity(boolean graphics) {
    Test test = new TestCity();
    
    CityMap map = setupTestCity(32, ALL_GOODS, false);
    World world = map.city.world;
    world.settings.toggleFog = false;
    
    CityMapPlanning.placeStructure(ROAD, map, true, 3, 8, 25 , 1);
    CityMapPlanning.placeStructure(ROAD, map, true, 8, 2 , 1, 25);
    
    Building palace = (Building) PALACE.generate();
    Building school = (Building) PHYSICIAN_STATION.generate();
    Building court  = (Building) BALL_COURT.generate();
    Building admin  = (Building) COLLECTOR .generate();
    
    palace.enterMap(map, 3 , 3 , 1);
    court .enterMap(map, 9 , 9 , 1);
    school.enterMap(map, 9 , 3 , 1);
    admin .enterMap(map, 18, 9 , 1);
    
    for (int n = 4; n-- > 0;) {
      Building house = (Building) HOUSE.generate();
      house.enterMap(map, 9 + (n * 3), 6, 1f);
    }
    
    Building quarry = (Building) QUARRY_PIT.generate();
    Building kiln1  = (Building) ENGINEER_STATION      .generate();
    Building kiln2  = (Building) ENGINEER_STATION      .generate();
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
        b.inventory.set(CASH, 20);
      }
      if (b.type == ENGINEER_STATION) {
        b.inventory.set(CLAY   , 1);
        b.inventory.set(POTTERY, 1);
      }
      map.planning.placeObject(b);
    }
    
    try {
      Session.saveSession("saves/test_save.tlt", map);
      Session session = Session.loadSession("saves/test_save.tlt", true);
      CityMap loaded = (CityMap) session.loaded()[0];
      I.say("\nSuccessfully loaded map: "+loaded);
    }
    catch(Exception e) {
      I.report(e);
      return false;
    }
    
    int timeCrafting = 0, timeIdle = 0;
    
    final int RUN_TIME = YEAR_LENGTH;
    boolean housesOkay = false;
    boolean goodsOkay  = true ;
    boolean testOkay   = false;
    
    while (map.time < RUN_TIME || graphics) {
      map = test.runLoop(map, 1, graphics, "saves/test_city.tlt");
      
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
          if (b.type == MARKET) {
            b.inventory.set(COTTON, 10);
            b.inventory.set(MAIZE , 10);
          }
          if (b.type == PALACE) {
            for (Good g : b.type.homeUseGoods) {
              b.inventory.set(g, 15);
            }
            b.inventory.set(STONE, 10);
            b.inventory.set(WOOD , 10);
            b.inventory.set(CLAY , 10);
          }
          if (b.type == HOUSE) {
            BuildingForHome home = (BuildingForHome) b;
            if (home.currentTier != HOUSE_T2        ) allNeeds = false;
            if (home.inventory.valueFor(CASH) > 5.0f) allNeeds = false;
          }
          if (b.type == ENGINEER_STATION) {
            boolean isCrafting = false;
            for (Actor a : b.workers) {
              if (b.inventory.valueFor(CLAY) <= 0) break;
              if (a.inside == b && a.jobType() == JOB.CRAFTING) {
                isCrafting = true;
              }
            }
            if (isCrafting) {
              timeCrafting += 1;
            }
            else {
              timeIdle += 1;
            }
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
    I.say("  Houses okay: "+housesOkay);
    I.say("  Goods okay:  "+goodsOkay );
    I.say("  Time idle/crafting: "+timeIdle+" / "+timeCrafting);
    
    I.say("  RUN TIME: "+RUN_TIME+", YEAR LENGTH: "+YEAR_LENGTH);
    I.say("  TOTAL CRAFT TIME: "+TaskCrafting.totalCraftTime);
    I.say("  TOTAL PROGRESS:   "+TaskCrafting.totalProgInc  );
    I.say("");
    
    reportOnMap(map, false);
    return false;
  }
  
  
  static void reportOnMap(CityMap map, boolean okay) {
    I.say("  Current time: "+map.time);
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







