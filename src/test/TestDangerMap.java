

package test;
import game.*;
import static game.GameConstants.*;
import static game.AreaMap.*;
import static content.GameContent.*;
import static content.GameWorld.*;
import util.*;



public class TestDangerMap extends LogicTest {
  
  
  public static void main(String args[]) {
    testDangerMap(true);
  }
  
  
  static boolean testDangerMap(boolean graphics) {
    LogicTest test = new TestDangerMap();
    test.viewDangerMap = true;
    
    final int RUN_TIME = YEAR_LENGTH / 2;
    final int NUM_THREATS = 5;
    final int MAP_SIZE = 32;
    final int gridRes = MAP_SIZE / FLAG_RES;
    
    Base base = setupTestBase(FACTION_SETTLERS_A, ALL_GOODS, MAP_SIZE, false);
    AreaMap map = base.activeMap();
    World world = base.world;
    
    world.settings.toggleFog = false;
    boolean testFail = false;
    
    AreaTile centre = map.tileAt(map.size() / 2, map.size() / 2);
    Federation.setPosture(
      base.faction(), map.locals.faction(),
      RelationSet.BOND_ENEMY, world
    );
    
    for (int n = NUM_THREATS; n-- > 0;) {
      Type type = Rand.yes() ? TRIPOD : DRONE;
      Actor threat = (Actor) type.generate();
      AreaTile pos = ActorUtils.pickRandomTile(centre, 48, map);
      threat.enterMap(map, pos.x, pos.y, 1, map.locals);
    }
    
    
    while (map.time() < RUN_TIME || graphics) {
      test.runLoop(base, 10, graphics, "saves/test_danger_map.str");
      
      float expectSums[][] = new float[gridRes][gridRes];
      float actualSums[][] = new float[gridRes][gridRes];
      
      for (Actor a : map.actors()) {
        if (! base.isEnemyOf(a.base())) continue;
        
        float power = TaskCombat.attackPower(a);
        int gX = a.at().x / FLAG_RES, gY = a.at().y / FLAG_RES;
        expectSums[gX][gY] += power;
      }
      
      AreaDanger danger = map.dangerMap(base.faction(), false);
      boolean checkMapLevels = true;
      List <Coord> discrepancies = new List();
      
      for (Coord c : Visit.grid(0, 0, gridRes, gridRes, 1)) {
        float expected = expectSums[c.x][c.y];
        float actual = danger.baseLevel(c.x * FLAG_RES, c.y * FLAG_RES);
        
        actualSums[c.x][c.y] = actual;
        
        if (Nums.abs(expected - actual) > 1) {
          checkMapLevels = false;
          discrepancies.add(c);
        }
      }
      
      if ((! checkMapLevels) && ! testFail) {
        testFail = true;
        I.say("\nDanger levels did not match expected, time: "+map.time());
        printDangerGrid(expectSums, null, "Estimate");
        printDangerGrid(actualSums, null, "Actual");
        printDangerGrid(expectSums, actualSums, "Difference");
        I.say("\nDANGER MAP TEST FAILED!");
        
        if (! graphics) return false;
      }
    }
    
    I.say("\nDANGER MAP TESTING CONCLUDED SUCCESSFULLY!");
    return true;
  }
  
  
  static void printDangerGrid(
    float grid[][], float diffWith[][], String title
  ) {
    I.say("\n  "+title);
    int dim = grid.length;
    for (int y = 0; y < dim; y++) {
      I.say("\n    ");
      for (int x = 0; x < dim; x++) {
        float val = grid[x][y];
        if (diffWith != null) val -= diffWith[x][y];
        I.add(I.padToLength(I.shorten(val, 1), 4)+" ");
      }
    }
  }
  
}









