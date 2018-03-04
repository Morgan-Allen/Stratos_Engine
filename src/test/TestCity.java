


package test;
import util.*;
import game.*;
import static content.GameContent.*;
import static game.GameConstants.*;
import static game.Task.*;



public class TestCity extends Test {
  
  
  public static void main(String args[]) {
    testCity(true);
  }
  
  
  static boolean testCity(boolean graphics) {
    Test test = new TestCity();
    
    Base base = setupTestCity(32, ALL_GOODS, false);
    AreaMap map = base.activeMap();
    World world = map.world;
    world.settings.toggleFog = false;
    
    base.initFunds(5000);
    
    CityMapPlanning.placeStructure(WALKWAY, base, true, 3, 8, 25 , 1);
    CityMapPlanning.placeStructure(WALKWAY, base, true, 8, 2 , 1, 25);
    
    Building palace = (Building) BASTION          .generate();
    Building school = (Building) PHYSICIAN_STATION.generate();
    Building court  = (Building) CANTINA          .generate();
    Building admin  = (Building) ENFORCER_BLOC    .generate();
    
    palace.enterMap(map, 2 , 2 , 1, base);
    court .enterMap(map, 9 , 9 , 1, base);
    school.enterMap(map, 9 , 2 , 1, base);
    admin .enterMap(map, 18, 9 , 1, base);
    
    fillWorkVacancies(palace);
    
    for (int n = 4; n-- > 0;) {
      Building house = (Building) HOLDING.generate();
      house.enterMap(map, 9 + (n * 3), 6, 1f, base);
    }
    
    Building quarry = (Building) EXCAVATOR       .generate();
    Building kiln1  = (Building) ENGINEER_STATION.generate();
    Building kiln2  = (Building) ENGINEER_STATION.generate();
    Building market = (Building) STOCK_EXCHANGE  .generate();
    
    quarry.enterMap(map, 4 , 15, 1, base);
    kiln1 .enterMap(map, 9 , 18, 1, base);
    kiln2 .enterMap(map, 9 , 14, 1, base);
    market.enterMap(map, 4 , 9 , 1, base);
    
    for (int n = 4; n-- > 0;) {
      Element rock = new Element(DESERT_ROCK1);
      rock.enterMap(map, 1 + (n * 3), 28, 1, base);
    }
    
    CityMapFlagging forRock = map.flagMap(IS_STONE, true);
    if (forRock.totalSum() != 4) {
      I.say("NO ROCKS FLAGGED: "+forRock.totalSum());
      return false;
    }
    
    for (Building b : map.buildings()) {
      if (b.type() == HOLDING) {
        b.setInventory(CASH, 20);
      }
      if (b.type() == ENGINEER_STATION) {
        b.setInventory(ORES , 1);
        b.setInventory(PARTS, 1);
      }
      map.planning.placeObject(b);
    }
    
    try {
      Session.saveSession("saves/test_save.tlt", map);
      Session session = Session.loadSession("saves/test_save.tlt", true);
      AreaMap loaded = (AreaMap) session.loaded()[0];
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
    
    while (map.time() < RUN_TIME || graphics) {
      test.runLoop(base, 1, graphics, "saves/test_city.tlt");
      
      //
      //  Hire workers as and when necessary:
      for (Building b : map.buildings()) if (b.base() == base) {
        for (ActorType job : b.type().workerTypes.keys()) {
          int num = b.numWorkers(job), max = b.maxWorkers(job);
          boolean canHire = b.hireCost(job) <= base.funds();
          if (job.socialClass != CLASS_COMMON && num < max && canHire) {
            ActorUtils.generateMigrant(job, b, true);
          }
        }
      }
      
      //
      //  Check to ensure stocks are under control:
      if (goodsOkay) {
        for (Building b : map.buildings()) {
          if (b.type() == HOLDING) {
            BuildingForHome home = (BuildingForHome) b;
            for (Good g : home.usedBy(home.currentBuildingTier())) {
              float need = home.maxStock(g);
              float have = home.inventory(g);
              if (have > need + 2) {
                goodsOkay = false;
              }
            }
          }
        }
      }
      
      if (! housesOkay) {
        boolean allNeeds = true;
        for (Building b : map.buildings()) {
          if (b.type() == STOCK_EXCHANGE) {
            b.setInventory(MEDICINE, 10);
            b.setInventory(CARBS   , 10);
            b.setInventory(GREENS  , 10);
          }
          if (b.type() == BASTION) {
            for (Good g : b.type().homeUseGoods.keys()) {
              b.setInventory(g, 15);
            }
            b.setInventory(PLASTICS, 10);
          }
          if (b.type() == HOLDING) {
            BuildingForHome home = (BuildingForHome) b;
            if (home.currentBuildingTier() != HOUSE_T2) allNeeds = false;
            if (home.inventory(CASH) > 5.0f           ) allNeeds = false;
          }
          if (b.type() == ENGINEER_STATION) {
            boolean isCrafting = false;
            for (Actor a : b.workers()) {
              if (b.inventory(ORES) <= 0) break;
              if (a.inside() == b && a.jobType() == JOB.CRAFTING) {
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
        reportOnMap(map, base, true, PARTS, MEDICINE);
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
    
    reportOnMap(map, base, false, PARTS, MEDICINE);
    return false;
  }
  
  
  static void reportOnMap(AreaMap map, Base base, boolean okay, Good... goods) {
    I.say("  Current time: "+map.time());
    if (! okay) for (Building b : map.buildings()) {
      if (b.type().isHomeBuilding()) {
        BuildingForHome house = (BuildingForHome) b;
        I.say("  "+house);
        I.say("    Tier:      "+house.currentBuildingTier());
        I.say("    Inventory: "+house.inventory());
      }
    }
    I.say("\nTotal goods produced:");
    for (Good g : goods) {
      I.say("  "+g+": "+base.totalMade(g));
    }
    I.say("\nTotal goods consumed:");
    for (Good g : goods) {
      I.say("  "+g+": "+base.totalMade(g));
    }
  }
  
}







