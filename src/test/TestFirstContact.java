

package test;
import static content.GameContent.*;
import static content.GameWorld.*;
import static game.GameConstants.*;
import game.*;
import util.*;



public class TestFirstContact extends LogicTest {
  
  
  public static void main(String args[]) {
    testFirstContact(false);
  }
  
  static boolean testFirstContact(boolean graphics) {
    LogicTest test = new TestFirstContact();
    
    Base base = setupTestBase(BASE, FACTION_SETTLERS_A, ALL_GOODS, 16, false);
    AreaMap map = base.activeMap();
    World world = map.world;
    world.settings.toggleFog     = false;
    world.settings.toggleHunger  = false;
    world.settings.toggleFatigue = false;
    
    
    
    //  Now create a tribe of angsty natives who could tip toward either
    //  peace or hostility (i.e, are moderately hostile by default.)
    
    //  Then set up a contact mission (by physicians, let's say), with the
    //  intent of securing good relations.
    
    //  Then... well, that's the problem, isn't it.  To the extent that it's
    //  tense and uncertain and requires player-management, you can't really
    //  test for it automatically.  You'd need to do a dozen tests, let's say,
    //  and ensure that you get at least one success and one failure (resulting
    //  in either retreat/death OR ongoing talking/sympathisers.)
    
    
    final int RUN_TIME = DAY_LENGTH;

    
    
    while (map.time() < RUN_TIME || graphics) {
      test.runLoop(base, 1, graphics, "saves/test_dialog.str");
    }
    
    return true;
  }
  
}






