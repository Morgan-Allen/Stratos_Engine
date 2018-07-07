

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
    
    MissionForContact contact = null;
    
    
    final int RUN_TIME = DAY_LENGTH;
    boolean madeContact = false;
    boolean hasSympathy = false;
    boolean homeConvert = false;
    boolean testOkay    = false;
    
    
    //  TODO:  Okay.  So what do I need to implement?
    
    //  Gifting is complicated, especially if you allow for on-to-off-map cases.
    
    /*
    Giving a gift is easy enough, even if it's from yourself.  Taking one on is
    harder.  But I think it can be done.
    
    Put the tests in place first.  You already have a TestDiplomacy class, so
    just add the appropriate checks there.  For TestSubversion, you just need
    to do the same thing- check that gifts are delivered and clap your hands
    accordingly.
    
    Do a TaskDialog assessment per usual, and segue into Gifting if a target is
    found and you can find a suitable present.
    
    In the case of a MissionContact, you can extract a Gifting behaviour
    directly, targeted at the settlement- that will end immediately once the
    actor has picked up gift-materials.  Keep gifting as a to-do item and end
    once you arrive off-world and/or the mission completes.
    
    //*/
    
    
    
    //  Joint-activities at the end of dialogue (scouting, hunting, repair/aid,
    //  intros, dining, gifting.)
    
    
    //  Base the probability of conversion on local rather than global factors.
    
    //  Loyalty to one's leader, vs. loyalty to one's people?  (And if you have
    //  no leader, that's zero by default.)  Hmm.  Maybe.
    
    
    
    
    while (map.time() < RUN_TIME || graphics) {
      test.runLoop(base, 1, graphics, "saves/test_subversion.str");
      
      while (contact == null || contact.complete()) {
        contact = new MissionForContact(base);
        for (Actor a : centre.workers()) {
          if (a.type().isCommoner()) continue;
          contact.toggleRecruit(a, true);
          contact.toggleEnvoy(a, true);
        }
        contact.setLocalFocus(mainHut);
        contact.beginMission(base);
      }
      
      if (centre.inventory(GREENS) < 10) {
        centre.addInventory(1, GREENS);
      }
      
      if (! madeContact) {
        boolean allTalked = true, anyTalked = false;
        for (Actor a : mainHut.workers()) {
          boolean talked = false;
          for (Actor w : a.bonds.allBondedWith(ActorBonds.BOND_ANY)) {
            if (w.base() == base) talked = true;
          }
          if (! talked) allTalked = false;
          else anyTalked = true;
        }
        madeContact = allTalked && anyTalked;
      }
      
      if (madeContact && ! hasSympathy) {
        boolean anySympathy = false;
        for (Actor a : mainHut.workers()) {
          if (a.bonds.baseLoyal() == base) anySympathy = true;
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

    for (Actor a : map.actors()) if (a.base() == map.locals) {
      I.say("\nBonds for "+a+" ("+a.base()+")");
      for (Actor o : a.bonds.allBondedWith(0)) {
        I.say("\n  "+o+": "+a.bonds.bondLevel(o));
      }
      a.bonds.makeLoyaltyCheck();
    }
    
    return false;
  }
  
}



