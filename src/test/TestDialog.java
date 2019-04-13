

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

    Building home = (Building) HOLDING.generate();
    Building work = (Building) TROOPER_LODGE.generate();
    home.enterMap(map, 7 , 7 , 1, base);
    work.enterMap(map, 11, 11, 1, base);
    home.setResident(sideA, true);
    work.setResident(sideA, true);
    
    sideA.enterMap(map, 2, 2, 1, base);
    sideB.enterMap(map, 5, 5, 1, base);
    
    boolean talkOkay    = false;
    boolean talkEnds    = false;
    boolean noveltyOkay = false;
    boolean joiningOkay = false;
    boolean testOkay    = false;
    
    final int RUN_TIME = DAY_LENGTH;
    
    
    //  TODO:  Test gift-giving too?
    
    //  TODO:  Some more comprehensive testing of different outcomes related to
    //  dialog would be nice as well...
    
    
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
      
      if (noveltyOkay && ! joiningOkay) {
        Task taskA = sideA.task(), taskB = sideB.task();
        if (taskA != null && taskB != null) {
          boolean sameType = taskA.type() == taskB.type();
          boolean joint = taskA.company() == sideB && taskB.company() == sideA;
          if (sameType && joint) {
            joiningOkay = true;
          }
        }
      }
      
      if (talkOkay && talkEnds && noveltyOkay && joiningOkay && ! testOkay) {
        testOkay = true;
        I.say("\nDIALOG TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return true;
      }
    }
    
    I.say("\nDIALOG TEST FAILED!");
    I.say("  Talk okay:    "+talkOkay   );
    I.say("  Talk ends:    "+talkEnds   );
    I.say("  Novelty okay: "+noveltyOkay);
    I.say("  Joining okay: "+joiningOkay);
    
    return false;
  }
  
}





