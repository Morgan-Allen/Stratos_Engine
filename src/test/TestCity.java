


package test;
import util.*;
import game.*;
import static content.GameContent.*;
import static content.GameWorld.*;
import static game.GameConstants.*;
import static game.Task.*;



public class TestCity extends LogicTest {
  
  
  public static void main(String args[]) {
    testCity(true);
  }
  
  
  static boolean testCity(boolean graphics) {
    LogicTest test = new TestCity();
    
    Base base = setupTestBase(BASE, FACTION_SETTLERS_A, ALL_GOODS, 32, false);
    AreaMap map = base.activeMap();
    World world = map.world;
    world.assignTypes(
      ALL_BUILDINGS, ALL_SHIPS(), ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES()
    );
    
    Base homeland = new Base(world, world.addArea(DISTANT), FACTION_SETTLERS_A);
    homeland.setName("Homeland");
    world.addBases(homeland);
    base.federation().assignHomeland(homeland);
    
    AreaType.setupRoute(base.area.type, homeland.area.type, 3, Type.MOVE_AIR);
    
    world.settings.toggleFog = false;
    
    base.initFunds(5000);
    
    AreaPlanning.placeStructure(WALKWAY, base, true, 4, 9, 25, 1);
    AreaPlanning.placeStructure(WALKWAY, base, true, 9, 3, 1, 25);
    AreaPlanning.placeStructure(WALKWAY, base, true, 1, 1, 9, 9 );
    
    Building palace = (Building) BASTION          .generate();
    Building school = (Building) PHYSICIAN_STATION.generate();
    Building court  = (Building) CANTINA          .generate();
    Building admin  = (Building) ENFORCER_BLOC    .generate();
    
    palace.enterMap(map, 2 , 2 , 1, base);
    court .enterMap(map, 10, 10, 1, base);
    school.enterMap(map, 10, 3 , 1, base);
    admin .enterMap(map, 19, 10, 1, base);
    
    ActorUtils.fillWorkVacancies(palace);
    
    for (int n = 4; n-- > 0;) {
      Building house = (Building) HOLDING.generate();
      house.enterMap(map, 10 + (n * 3), 7, 1f, base);
    }
    
    Building quarry = (Building) EXCAVATOR       .generate();
    Building kiln1  = (Building) ENGINEER_STATION.generate();
    Building kiln2  = (Building) ENGINEER_STATION.generate();
    Building market = (Building) STOCK_EXCHANGE  .generate();
    
    quarry.enterMap(map, 5 , 16, 1, base);
    kiln1 .enterMap(map, 10, 19, 1, base);
    kiln2 .enterMap(map, 10, 15, 1, base);
    market.enterMap(map, 5 , 10, 1, base);
    
    for (int n = 4; n-- > 0;) {
      Element rock = new Element(DESERT_ROCK1);
      rock.enterMap(map, 1 + (n * 3), 28, 1, base);
    }
    
    AreaFlagging forRock = map.flagMap(IS_STONE, true);
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
    final int maxCash = (TAX_VALUES[1] * HOLDING.maxResidents) / 2;
    
    final int RUN_TIME = YEAR_LENGTH * 2;
    boolean housesOkay = false;
    boolean taxesOkay  = true ;
    boolean goodsOkay  = true ;
    boolean testOkay   = false;
    float   goodExcess = -1;
    float   taxExcess  = -1;
    Good    excessGood = null;
    
    while (map.time() < RUN_TIME || graphics) {
      test.runLoop(base, 1, graphics, "saves/test_city.tlt");
      
      //
      //  Hire workers as and when necessary:
      for (Building b : map.buildings()) if (b.base() == base) {
        for (ActorType job : b.type().workerTypes.keys()) {
          int num = b.numWorkers(job), max = b.maxWorkers(job);
          boolean canHire = base.trading.hireCost(job) <= base.funds();
          if (job.socialClass != CLASS_COMMON && num < max && canHire) {
            ActorUtils.generateMigrant(job, b, true);
          }
        }
      }
      //
      //  Check to ensure stocks are under control:
      if (goodsOkay) {
        search: for (Building b : map.buildings()) {
          if (b.type() == HOLDING) {
            BuildingForHome home = (BuildingForHome) b;
            for (Good g : home.usedBy(home.currentBuildingTier())) {
              float need = home.maxStock(g);
              float have = home.inventory(g);
              if (have > need + 2) {
                goodsOkay = false;
                goodExcess = have - need;
                excessGood = g;
                break search;
              }
            }
          }
        }
      }
      //
      //  And do the same for taxes-
      if (taxesOkay) {
        boolean allTaxed = true;
        for (Building b : map.buildings()) {
          if (b.type() == HOLDING) {
            if (b.inventory(CASH) > maxCash) {
              allTaxed = false;
              taxExcess = b.inventory(CASH);
            }
          }
        }
        taxesOkay = allTaxed;
      }
      //
      //  And check for housing evolution-
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
            b.setInventory(CARBS   , 10);
          }
          if (b.type() == HOLDING) {
            BuildingForHome home = (BuildingForHome) b;
            if (home.currentBuildingTier() != HOUSE_T2) allNeeds = false;
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
      //
      //  Then check for successful conclusion-
      if (housesOkay && goodsOkay && taxesOkay && ! testOkay) {
        I.say("\nCITY TEST CONCLUDED SUCCESSFULLY!");
        testOkay = true;
        reportOnMap(map, base, true, PARTS, MEDICINE);
        if (! graphics) return true;
      }
    }
    
    I.say("\nCITY TEST FAILED!");
    I.say("  Houses okay: "+housesOkay);
    I.say("  Goods okay:  "+goodsOkay );
    I.say("  Taxes okay:  "+taxesOkay );
    I.say("  Good excess: "+goodExcess+" "+excessGood);
    I.say("  Tax excess:  "+taxExcess+"/"+maxCash);
    
    I.say("\n  Time idle/crafting: "+timeIdle+" / "+timeCrafting);
    I.say("  RUN TIME: "+RUN_TIME+", YEAR LENGTH: "+YEAR_LENGTH);
    I.say("  TOTAL CRAFT TIME: "+TaskCrafting.totalCraftTime);
    I.say("  TOTAL PROGRESS:   "+TaskCrafting.totalProgInc  );
    
    int mapCitizens = 0, totalCitizens = 0;
    for (Building b : map.buildings()) for (Actor a : b.workers()) {
      totalCitizens += 1;
      if (a.onMap()) mapCitizens += 1;
    }
    I.say("\n  CITIZENS ON MAP: "+mapCitizens+"/"+totalCitizens);
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
      I.say("  "+g+": "+base.trading.totalMade(g));
    }
    I.say("\nTotal goods consumed:");
    for (Good g : goods) {
      I.say("  "+g+": "+base.trading.totalUsed(g));
    }
  }
  
}







