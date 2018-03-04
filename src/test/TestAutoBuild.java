


package test;
import util.*;
import game.*;
import static content.GameContent.*;
import static game.GameConstants.*;



public class TestAutoBuild extends Test {
  
  
  public static void main(String args[]) {
    testAutoBuilding(true);
  }
  
  
  static boolean testAutoBuilding(boolean graphics) {
    Test test = new TestAutoBuild();
    
    Base base = setupTestCity(16, ALL_GOODS, false);
    AreaMap map = base.activeMap();
    World world = map.world;
    world.settings.toggleFog         = false;
    world.settings.toggleFatigue     = false;
    world.settings.toggleHunger      = false;
    world.settings.toggleMigrate     = false;
    world.settings.toggleBuildEvolve = false;
    
    
    Building farm = (Building) NURSERY.generate();
    farm.enterMap(map, 1, 1, 1, base);
    
    Building forge = (Building) ENGINEER_STATION.generate();
    forge.enterMap(map, 8, 8, 1, base);
    
    forge.setInventory(PARTS   , 10);
    forge.setInventory(PLASTICS, 25);
    
    Test.fillWorkVacancies(farm );
    Test.fillWorkVacancies(forge);
    
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

