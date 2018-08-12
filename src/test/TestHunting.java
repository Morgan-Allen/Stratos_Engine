


package test;
import game.*;
import static game.GameConstants.*;
import static content.GameContent.*;
import static content.GameWorld.*;
import util.*;



public class TestHunting extends LogicTest {
  
  
  public static void main(String args[]) {
    testHunting(true);
  }
  
  
  static boolean testHunting(boolean graphics) {
    LogicTest test = new TestHunting();
    
    Base base = setupTestBase(FACTION_SETTLERS_A, ALL_GOODS, 32, true, JUNGLE, MEADOW);
    Area map = base.activeMap();
    World world = map.world;
    
    world.settings.toggleFatigue   = false;
    world.settings.toggleHunger    = false;
    world.settings.toggleMigrate   = false;
    world.settings.toggleAutoBuild = false;
    world.settings.toggleFog       = false;
    
    Building lodge = (Building) KOMMANDO_REDOUBT.generate();
    lodge.enterMap(map, 4, 4, 1, base);
    ActorUtils.fillWorkVacancies(lodge);
    
    AreaTerrain.populateAnimals(map, QUDU);
    for (Actor a : map.actors()) if (a.type().isAnimal()) {
      a.health.setAgeYears(ANIMAL_MATURES * (1 + Rand.num()) / YEAR_LENGTH);
    }
    
    boolean huntingOkay = false;
    boolean testOkay    = false;
    
    while (map.time() < 1000 || graphics) {
      test.runLoop(base, 10, graphics, "saves/test_gathering.tlt");
      
      if (! huntingOkay) {
        huntingOkay = lodge.inventory(PROTEIN) > lodge.type().maxStock;
      }
      
      if (huntingOkay && ! testOkay) {
        I.say("\nHUNTING TEST CONCLUDED SUCCESSFULLY!");
        testOkay = true;
        if (! graphics) return true;
      }
    }
    
    I.say("\nHUNTING TEST FAILED!");
    return false;
  }
}


