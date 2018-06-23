

package test;
import game.*;
import static game.GameConstants.*;
import static content.GameContent.*;
import util.*;




public class TestSubversion extends LogicTest {
  
  
  public static void main(String args[]) {
    testSubversion(false);
  }
  
  
  static boolean testSubversion(boolean graphics) {
    
    LogicTest test = new TestSubversion();
    
    Base base = setupTestBase(32, ALL_GOODS, false);
    Area map = base.activeMap();
    World world = map.world;
    world.settings.toggleFog     = false;
    world.settings.toggleHunger  = false;
    world.settings.toggleFatigue = false;
    
    
    Building mainHut = (Building) FOREST_HUT.generate();
    mainHut.enterMap(map, 27, 27, 1, map.locals);
    ActorUtils.fillWorkVacancies(mainHut);
    
    Building centre = (Building) BASTION.generate();
    centre.enterMap(map, 2, 2, 1, base);
    ActorUtils.fillWorkVacancies(centre);
    
    MissionForContact currentContact = null;
    
    
    final int RUN_TIME = DAY_LENGTH;
    boolean madeContact = false;
    boolean hasSympathy = false;
    boolean homeConvert = false;
    boolean testOkay    = false;
    
    
    //  TODO:  Okay.  So what do I need to implement?
    
    //  Joint-activities at the end of dialogue (scouting, hunting, repair/aid,
    //  intros, dining, gifting.)
    
    //  The possibility of becoming a sympathiser if your relations with
    //  another base are stronger than with your own.  (Or if the other side
    //  has leverage on you or just looks 'strong' enough.)
    
    //  The possibility of individuals or buildings converting to your base if
    //  outside 'sympathies' are strong enough.
    
    
    
    while (map.time() < RUN_TIME || graphics) {
      test.runLoop(base, 1, graphics, "saves/test_subversion.str");
      
      //  TODO:  Fill this in.
      /*
      while (currentContact == null || currentContact.complete()) {
        //  Initialise the contact mission...
      }
      //*/
      
      if (! madeContact) {
        boolean allTalked = true;
        for (Actor a : mainHut.workers()) {
          boolean talked = false;
          for (Actor w : a.bonds.allBondedWith(ActorBonds.BOND_ANY)) {
            if (w.base() == base) talked = true;
          }
          if (! talked) allTalked = false;
        }
        madeContact = allTalked;
      }
      
      if (madeContact && ! hasSympathy) {
        boolean anySympathy = false;
        for (Actor a : mainHut.workers()) {
          if (a.baseLoyal() == base) anySympathy = true;
        }
        hasSympathy = anySympathy;
      }
      
      if (hasSympathy && ! homeConvert) {
        homeConvert = mainHut.base() == base;
      }
      
      if (homeConvert && ! testOkay) {
        testOkay = true;
        I.say("\nSUBVERSION TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return true;
      }
    }
    
    I.say("\nSUBVERSION TEST FAILED!");
    I.say("  Made contact: "+madeContact);
    I.say("  Has sympathy: "+hasSympathy);
    I.say("  Home convert: "+homeConvert);
    
    return false;
  }
  
}



