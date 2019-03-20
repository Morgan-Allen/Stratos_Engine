

package test;
import game.*;
import static game.GameConstants.*;
import static game.RelationSet.*;
import static content.GameContent.*;
import static content.GameWorld.*;
import util.*;




public class TestSubversion extends LogicTest {
  
  
  public static void main(String args[]) {
    testSubversion(false);
  }
  
  
  static boolean testSubversion(boolean graphics) {
    
    LogicTest test = new TestSubversion();
    
    Base base = setupTestBase(FACTION_SETTLERS_A, ALL_GOODS, 32, false);
    AreaMap map = base.activeMap();
    World world = map.world;
    world.settings.toggleFog     = false;
    world.settings.toggleHunger  = false;
    world.settings.toggleFatigue = false;
    map.area.locals.setName("Locals");
    
    
    Building mainHut = (Building) FOREST_HUT.generate();
    mainHut.enterMap(map, 27, 27, 1, map.area.locals);
    ActorUtils.fillWorkVacancies(mainHut);
    
    Building centre = (Building) BASTION.generate();
    centre.enterMap(map, 2, 2, 1, base);
    ActorUtils.fillWorkVacancies(centre);
    
    Good giftGoods[] = { GREENS, PARTS };
    MissionForContact contact = null;
    Batch <Actor> eval = new Batch();
    Visit.appendTo(eval, mainHut.workers());
    
    
    final int RUN_TIME = YEAR_LENGTH / 2;
    boolean madeContact = false;
    boolean gaveGifts   = false;
    boolean hasSympathy = false;
    boolean homeConvert = false;
    boolean testOkay    = false;
    
    //  TODO:  Okay.  So what do I need to implement?
    
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
        contact.beginMission();
      }
      
      if (! madeContact) {
        boolean allTalked = true, anyTalked = false;
        for (Actor a : mainHut.workers()) {
          boolean talked = false;
          for (Focus f : a.bonds.allBondedWith(ActorBonds.BOND_ANY)) {
            if (! f.type().isActor()) continue;
            if (((Actor) f).base() == base) talked = true;
          }
          if (! talked) allTalked = false;
          else anyTalked = true;
        }
        madeContact = allTalked && anyTalked;
      }
      
      if (! gaveGifts) {
        boolean anyGot = false;
        for (Good g : giftGoods) {
          if (mainHut.inventory(g) > 0) anyGot = true;
          for (Actor a : mainHut.residents()) {
            if (a.outfit.carried(g) > 0) anyGot = true;
          }
        }
        gaveGifts = anyGot;
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
        reportLoyalties(mainHut, eval);
        if (! graphics) return true;
      }
    }
    
    I.say("\nSUBVERSION TEST FAILED!");
    I.say("  Made contact: "+madeContact);
    I.say("  Gave gifts:   "+gaveGifts  );
    I.say("  Has sympathy: "+hasSympathy);
    I.say("  Home convert: "+homeConvert);
    
    reportLoyalties(mainHut, eval);
    
    return false;
  }
  
  
  private static void reportLoyalties(Building focus, Series <Actor> eval) {
    
    I.say("\nMain focus: "+focus+" ("+focus.base()+")");
    I.say("  Carries: "+focus.inventory());
    
    for (Actor a : eval) {
      I.say("\nBonds for "+a+" ("+a.bonds.baseLoyal()+")");
      for (Focus f : a.bonds.allBondedWith(0)) {
        if (! f.type().isActor()) continue;
        Actor o = (Actor) f;
        String name = I.padToLength(I.shorten(o.fullName(), 15), 15);
        String bond = I.shorten(a.bonds.bondLevel  (o), 2);
        String news = I.shorten(a.bonds.bondNovelty(o), 2);
        I.say("  "+name+": "+bond+" (N="+news+")");
      }
      I.say("  Carries: "+a.inventory());
      ///a.bonds.makeLoyaltyCheck();
    }
  }
  
}



