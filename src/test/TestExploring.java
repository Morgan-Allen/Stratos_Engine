


package test;
import game.*;
import static game.GameConstants.*;
import static content.GameContent.*;
import static content.GameWorld.FACTION_SETTLERS;
import util.*;



public class TestExploring extends LogicTest {
  
  
  public static void main(String args[]) {
    testExploring(true);
  }
  
  
  static boolean testExploring(boolean graphics) {
    LogicTest test = new TestExploring();
    
    Base base = setupTestBase(FACTION_SETTLERS, ALL_GOODS, 32, true, JUNGLE, MEADOW);
    Area map = base.activeMap();
    World world = map.world;
    
    world.settings.toggleFatigue   = false;
    world.settings.toggleHunger    = false;
    world.settings.toggleMigrate   = false;
    world.settings.toggleAutoBuild = false;
    
    Building lodge = (Building) KOMMANDO_REDOUBT.generate();
    lodge.enterMap(map, 4, 4, 1, base);
    ActorUtils.fillWorkVacancies(lodge);
    
    AreaFog fog = map.fogMap(base.faction(), true);
    int tilesSeen = 0, tilesOpen = 0;
    boolean exploreOkay = false;
    boolean testOkay    = false;
    
    while (map.time() < 1000 || graphics) {
      test.runLoop(base, 10, graphics, "saves/test_gathering.tlt");
      
      if (! exploreOkay) {
        tilesSeen = 0;
        tilesOpen = 0;
        
        for (AreaTile t : map.allTiles()) {
          if (map.blocked(t)) continue;
          tilesOpen += 1;
          if (fog.maxSightLevel(t) > 0) tilesSeen += 1;
        }
        
        exploreOkay = tilesSeen == tilesOpen;
      }
      
      if (exploreOkay && ! testOkay) {
        I.say("\nEXPLORING TEST CONCLUDED SUCCESSFULLY!");
        testOkay = true;
        if (! graphics) return true;
      }
    }
    
    I.say("\nEXPLORING TEST FAILED!");
    I.say("  Total tiles seen: "+tilesSeen+"/"+tilesOpen);
    return false;
  }
  
}


