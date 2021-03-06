

package test;
import game.*;
import static game.GameConstants.*;
import static game.Task.*;
import content.*;
import static content.GameContent.*;
import static content.GameWorld.*;
import util.*;



public class TestDialog extends LogicTest {
  

  public static void main(String args[]) {
    testDialog(true);
  }
  
  
  static boolean testDialog(boolean graphics) {
    LogicTest test = new TestDialog();
    
    Base base = setupTestBase(BASE, FACTION_SETTLERS_A, ALL_GOODS, 16, false);
    AreaMap map = base.activeMap();
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
    
    //  TODO:  Test gift-giving too?  And/or joint activities?
    
    
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
        noveltyOkay |= sideA.bonds.bondNovelty(sideB) <= 0;
        noveltyOkay |= sideB.bonds.bondNovelty(sideA) <= 0;
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









