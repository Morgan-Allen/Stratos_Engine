


package test;
import util.*;
import game.*;
import static content.GameContent.*;
import static game.GameConstants.*;



public class TestAutoBuild extends LogicTest {
  
  
  public static void main(String args[]) {
    testAutoBuild(true);
  }
  
  
  static boolean testAutoBuild(boolean graphics) {
    LogicTest test = new TestAutoBuild();
    
    Base base = setupTestBase(16, ALL_GOODS, false);
    AreaMap map = base.activeMap();
    World world = map.world;
    world.settings.toggleFog         = false;
    world.settings.toggleFatigue     = false;
    world.settings.toggleHunger      = false;
    world.settings.toggleMigrate     = false;
    world.settings.toggleBuildEvolve = false;
    
    
    Building vault = (Building) BASTION.generate();
    vault.enterMap(map, 8, 8, 1, base);
    vault.inventory().setWith(
      PARTS   , 10,
      PLASTICS, 25,
      CARBS   , 20
    );
    ActorUtils.fillWorkVacancies(vault);
    
    Building vendor = (Building) STOCK_EXCHANGE.generate();
    vendor.enterMap(map, 1, 8, 1, base);
    ActorUtils.fillWorkVacancies(vendor);
    
    Building farm = (Building) NURSERY.generate();
    farm.enterMap(map, 1, 1, 1, base);
    ActorUtils.fillWorkVacancies(farm);
    
    boolean allHoused = false;
    boolean builtOkay = false;
    boolean testOkay  = false;
    
    
    final int RUN_TIME = YEAR_LENGTH;
    
    while (map.time() < RUN_TIME || graphics) {
      test.runLoop(base, 1, graphics, "saves/test_building.tlt");
      
      if (! builtOkay) {
        for (Building b : map.buildings()) {
          if (b.type() == HOLDING && b.complete()) {
            builtOkay = true;
            break;
          }
        }
      }
      
      if (! allHoused) {
        boolean housed = true;
        for (Actor a : map.actors()) {
          if (a.home() == null || ! a.home().complete()) housed = false;
        }
        allHoused = housed;
      }
      
      if (builtOkay && allHoused && ! testOkay) {
        I.say("\nAUTO-BUILD TEST CONCLUDED SUCCESSFULLY!");
        testOkay = true;
        if (! graphics) return true;
      }
    }
    
    I.say("\nAUTO-BUILD TEST FAILED!");
    I.say("  Built okay: "+builtOkay);
    I.say("  All housed: "+allHoused);
    
    return false;
  }
  
}

