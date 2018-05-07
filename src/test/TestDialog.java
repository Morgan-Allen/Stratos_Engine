

package test;
import game.*;
import static game.GameConstants.*;
import static game.Task.*;
import content.*;
import static content.GameContent.*;
import util.*;



public class TestDialog extends LogicTest {
  

  public static void main(String args[]) {
    testDialog(true);
  }
  
  
  static boolean testDialog(boolean graphics) {
    LogicTest test = new TestDialog();
    
    Base base = setupTestBase(16, ALL_GOODS, false);
    Area map = base.activeMap();
    World world = map.world;
    world.settings.toggleFog     = false;
    world.settings.toggleHunger  = false;
    world.settings.toggleFatigue = false;
    
    
    Actor sideA = (Actor) Trooper.TROOPER.generate();
    Actor sideB = (Actor) SchoolCollective.COLLECTIVE.generate();
    
    sideA.enterMap(map, 2, 2, 1, base);
    sideB.enterMap(map, 5, 5, 1, base);
    
    
    boolean talkOkay    = false;
    boolean talkEnds    = false;
    boolean noveltyOkay = false;
    boolean testOkay    = false;
    
    final int RUN_TIME = DAY_LENGTH;
    
    
    while (map.time() < RUN_TIME || graphics) {
      test.runLoop(base, 1, graphics, "saves/test_dialog.str");
      
      if (! talkOkay) {
        talkOkay = true;
        talkOkay &= sideA.jobFocus() == sideB && sideA.jobType() == JOB.DIALOG;
        talkOkay &= sideB.jobFocus() == sideA && sideB.jobType() == JOB.DIALOG;
      }
      
      if (talkOkay && ! talkEnds) {
        talkEnds = true;
        talkEnds &= sideA.jobFocus() != sideB || sideA.jobType() != JOB.DIALOG;
        talkEnds &= sideB.jobFocus() != sideA || sideB.jobType() != JOB.DIALOG;
      }
      
      if (talkOkay && ! noveltyOkay) {
        noveltyOkay = false;
        noveltyOkay |= sideA.traits.bondNovelty(sideB) <= 0;
        noveltyOkay |= sideB.traits.bondNovelty(sideA) <= 0;
      }
      
      if (talkOkay && talkEnds && noveltyOkay && ! testOkay) {
        testOkay = true;
        I.say("\nDIALOG TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return true;
      }
    }
    
    I.say("\nDIALOG TEST FAILED!");
    I.say("  Talk okay:    "+talkOkay   );
    I.say("  Talk ends:    "+talkEnds   );
    I.say("  Novelty okay: "+noveltyOkay);
    
    return false;
  }
  
}









