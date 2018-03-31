

package test;
import game.*;
import static game.ActorTraits.*;
import static game.GameConstants.*;
import static game.BaseCouncil.*;
import static game.AreaPlanning.*;
import content.*;
import static content.GameContent.*;
import util.*;




public class TestLifeCycle extends LogicTest {
  
  
  public static void main(String args[]) {
    testLifeCycle(false);
  }
  
  
  static boolean testLifeCycle(boolean graphics) {
    LogicTest test = new TestLifeCycle();
    
    Base base = setupTestBase(16, ALL_GOODS, false);
    Area map = base.activeMap();
    World world = map.world;
    
    world.settings.toggleFog    = false;
    world.settings.toggleReacts = false;
    
    
    I.say("\nTesting XP gain...");
    ActorAsPerson single = (ActorAsPerson) Vassals.PYON.generate();
    for (int n = MAX_TRAIN_TIME; n-- > 0;) {
      single.traits.gainXP(SKILL_BUILD, 1);
      if (n % DAY_LENGTH == 0) I.say("  "+single.traits.levelOf(SKILL_BUILD));
    }
    
    if (single.traits.levelOf(SKILL_BUILD) < MAX_SKILL_LEVEL) {
      I.say("\nXP gain did not function correctly!");
      return false;
    }

    for (int x = 2; x-- > 0;) {
      for (int y = 2; y-- > 0;) {
        Building house = (Building) HOLDING.generate();
        house.enterMap(map, 1 + (x * 4), 10 + (y * 3), 1, base);
      }
    }
    for (int x = 3; x-- > 0;) {
      Building built = (Building) ENGINEER_STATION.generate();
      built.enterMap(map, 1 + (x * 4), 1, 1, base);
      placeStructure(WALKWAY, base, true, built.at().x - 1, 0, 1, 15);
    }
    
    placeStructure(WALKWAY, base, true, 0, 0, 10, 1);
    placeStructure(WALKWAY, base, true, 0, 9, 16, 1);
    
    Building palace = (Building) BASTION.generate();
    BaseCouncil council = base.council;
    
    ActorAsPerson oldKing = (ActorAsPerson) Nobles.NOBLE  .generate();
    ActorAsPerson consort = (ActorAsPerson) Nobles.CONSORT.generate();
    oldKing.type().initAsMigrant(oldKing);
    consort.type().initAsMigrant(consort);
    oldKing.health.setAgeYears(AVG_RETIREMENT / 3);
    consort.health.setAgeYears(AVG_RETIREMENT / 3);
    oldKing.health.setSexData(SEX_MALE  );
    consort.health.setSexData(SEX_FEMALE);
    ActorTraits.setBond(oldKing, consort, BOND_MARRIED, BOND_MARRIED, 0.5f);
    council.toggleMember(oldKing, Role.MONARCH, true);
    
    palace.setResident(oldKing, true);
    palace.setResident(consort, true);
    palace .enterMap(map, 9 , 10, 1, base);
    oldKing.enterMap(map, 12, 9 , 1, base);
    consort.enterMap(map, 12, 9 , 1, base);
    
    final int RUN_TIME  = LIFESPAN_LENGTH;
    final int MAX_DELAY = ((RUN_TIME * 1) / 3) - Rand.index(YEAR_LENGTH);
    final int MAX_REIGN = ((RUN_TIME * 2) / 3) + Rand.index(YEAR_LENGTH);
    List <Actor> originalPop = null;
    List <Actor> births = new List();
    List <Actor> deaths = new List();
    
    boolean migrated   = false;
    boolean noBadJobs  = true ;
    boolean popCycled  = false;
    boolean heirBorn   = false;
    boolean recognised = false;
    boolean kingDied   = false;
    boolean succession = false;
    boolean testOkay   = false;
    
    ActorUtils.fillAllWorkVacancies(map);
    for (Actor a : map.actors()) a.health.setHungerLevel(0.75f);
    
    
    I.say("\nTOTAL LIFE CYCLE RUN TIME: "+RUN_TIME);
    
    while (map.time() < RUN_TIME || graphics) {
      test.runLoop(base, 1, graphics, "saves/test_cycle.tlt");
      
      if (map.time() % 1000 == 0) {
        I.say("  Time: "+map.time());
      }
      
      if (! migrated) {
        boolean allFilled = true;
        for (Building b : map.buildings()) {
          for (ActorType t : b.type().workerTypes.keys()) {
            if (b.numWorkers(t) < b.maxWorkers(t)) allFilled = false;
          }
        }
        if (allFilled) {
          migrated = true;
          originalPop = new List();
          Visit.appendTo(originalPop, map.actors());
          int i = 0;
          for (Actor a : originalPop) {
            if (a.type().socialClass == CLASS_NOBLE) continue;
            a.health.setSexData(((i++ % 2) == 0) ? SEX_FEMALE : SEX_MALE);
          }
        }
      }
      
      if (migrated) {
        
        for (Building b : map.buildings()) {
          if (b.type() == ENGINEER_STATION) {
            b.setInventory(ORES , 5);
            b.setInventory(PARTS, 5);
          }
          if (b.type() == HOLDING || b.type() == BASTION) {
            b.setInventory(CARBS   , 5);
            b.setInventory(GREENS  , 5);
            b.setInventory(PARTS   , 5);
            b.setInventory(MEDICINE, 5);
          }
        }
        //  TODO:  Restore this later- it may take time to update employment
        //  at a given age!
        /*
        for (Walker w : map.walkers) {
          if (w.work != null && ! w.adult()) {
            noBadJobs = false;
            I.say("  "+w+" has bad job: "+w.work+", age: "+w.ageYears());
          }
        }
        //*/
        if (! noBadJobs) {
          break;
        }
        
        for (Actor a : map.actors()) {
          if (a.health.child() && a.health.alive() && ! originalPop.includes(a)) {
            if (! births.includes(a)) {
              Series <Actor> parents = a.traits.allBondedWith(BOND_PARENT);
              I.say("  Born: "+a+", parents: "+parents);
              
              if (parents.includes(oldKing)) {
                heirBorn = true;
                I.say("  Was heir!");
                if (! parents.includes(consort)) {
                  I.say("  Was bastard!");
                }
              }
            }
            births.include(a);
          }
        }
        for (Actor w : originalPop) {
          if (w.health.dead() && ! map.actors().includes(w)) {
            if (! deaths.includes(w)) I.say("  Died: "+w);
            deaths.include(w);
          }
        }
        
        if (births.size() >= 2 && deaths.size() >= 2 && noBadJobs) {
          popCycled = true;
        }
      }
      
      //
      //  We need to speed things up a little to avoid infant mortality and
      //  variable lifespans, if succession is going to be checked-
      if (map.time() > MAX_DELAY && (! consort.health.pregnant()) && ! heirBorn) {
        consort.health.beginPregnancy();
      }
      if (consort.health.pregnant() && consort.inside() == palace && ! heirBorn) {
        consort.health.completePregnancy(palace, true);
      }
      if (! recognised) {
        recognised = council.memberWithRole(Role.HEIR) != null;
      }
      if (map.time() > MAX_REIGN && recognised && ! oldKing.health.dead()) {
        oldKing.health.setAsKilled("Died to allow heir to succeed");
      }
      if (oldKing.health.dead() && ! kingDied) {
        kingDied = true;
      }
      if (kingDied && ! succession) {
        Actor newKing = council.memberWithRole(Role.MONARCH);
        boolean isHeir = true;
        isHeir &= newKing != null;
        isHeir &= newKing != oldKing;
        isHeir &= oldKing.traits.hasBondType(newKing, BOND_CHILD);
        succession = isHeir;
      }
      
      //
      //  If all the boxes are checked, return a success-
      if (popCycled && succession && ! testOkay) {
        I.say("\nLIFE CYCLE TEST CONCLUDED SUCCESSFULLY!");
        I.say("  Births: "+births);
        I.say("  Deaths: "+deaths);
        testOkay = true;
        if (! graphics) break;
      }
    }
    
    if (! testOkay) {
      I.say("\nLIFE CYCLE TEST FAILED!");
      I.say("  Births:       "+births);
      I.say("  Deaths:       "+deaths);
    }
    
    I.say("  Pop. cycled:  "+popCycled    );
    I.say("  Succession:   "+succession   );
    I.say("  Bad jobs?     "+(! noBadJobs));
    I.say("  Old king:     "+oldKing);
    I.say("  Heir born:    "+heirBorn);
    I.say("  Alive:        "+oldKing.health.alive());
    I.say("  Children:     "+oldKing.traits.allBondedWith(BOND_CHILD));
    I.say("  Current king: "+council.memberWithRole(Role.MONARCH));
    
    return testOkay;
  }
  
}





