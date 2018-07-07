

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
    
    Good giftGoods[] = { GREENS, PARTS };
    MissionForContact contact = null;
    
    
    final int RUN_TIME = DAY_LENGTH;
    boolean madeContact = false;
    boolean gaveGifts   = false;
    boolean hasSympathy = false;
    boolean homeConvert = false;
    boolean testOkay    = false;
    
    
    //  TODO:  Okay.  So what do I need to implement?
    
    //  Make sure gifting works, first of all.
    
    //  Secondly, revise how much favour with the other tribe you need to gain
    //  in order to get them to switch sides.  (Loyalty to leader + threshold,
    //  which for bases without a leader is zero, say?  Might even be easier
    //  if they actively hate their leader.)
    
    //  Relative danger-levels count as well.  Settlements close to yours will
    //  be more likely to switch.
    
    
    while (map.time() < RUN_TIME || graphics) {
      test.runLoop(base, 1, graphics, "saves/test_subversion.str");
      
      for (Good g : giftGoods) if (centre.inventory(g) < 10) {
        centre.addInventory(5, g);
      }
      
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
      
      if (! gaveGifts) {
        gaveGifts = mainHut.inventory(PARTS) > 0;
      }
      
      if (madeContact && gaveGifts && ! hasSympathy) {
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
    I.say("  Gave gifts:   "+gaveGifts  );
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



