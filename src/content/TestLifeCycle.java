

package content;
import game.*;
import util.*;
import static content.GameContent.*;
import static game.ActorAsPerson.*;
import static game.GameConstants.*;
import static game.CityCouncil.*;
import static game.CityMapPlanning.*;




public class TestLifeCycle extends Test {
  
  
  public static void main(String args[]) {
    testLifeCycle(false);
  }
  
  
  static boolean testLifeCycle(boolean graphics) {
    Test test = new TestLifeCycle();
    
    I.say("\nTesting XP gain...");
    ActorAsPerson single = (ActorAsPerson) CITIZEN.generate();
    for (int n = MAX_TRAIN_TIME; n-- > 0;) {
      single.gainXP(SKILL_BUILD, 1);
      if (n % MONTH_LENGTH == 0) I.say("  "+single.levelOf(SKILL_BUILD));
    }
    
    if (single.levelOf(SKILL_BUILD) < MAX_SKILL_LEVEL) {
      I.say("\nXP gain did not function correctly!");
      return false;
    }
    
    CityMap map = setupTestCity(16, ALL_GOODS, false);
    World world = map.city.world;
    world.settings.toggleFog = false;
    
    for (int x = 7; x > 0; x -= 3) {
      for (int y = 7; y > 0; y -= 3) {
        Type type = y == 7 ? HOUSE : ENGINEER_STATION;
        Building built = (Building) type.generate();
        built.enterMap(map, x, y, 1);
      }
      placeStructure(ROAD, map, true, x - 1, 0, 1, 10);
    }
    placeStructure(ROAD, map, true, 0, 0, 10, 1);
    placeStructure(ROAD, map, true, 0, 9, 16, 1);
    
    Building palace = (Building) PALACE.generate();
    CityCouncil council = map.city.council;
    
    ActorAsPerson oldKing = (ActorAsPerson) NOBLE  .generate();
    ActorAsPerson consort = (ActorAsPerson) CONSORT.generate();
    oldKing.type().initAsMigrant(oldKing);
    consort.type().initAsMigrant(consort);
    oldKing.setAgeYears(AVG_RETIREMENT / 3);
    consort.setAgeYears(AVG_RETIREMENT / 3);
    oldKing.setSexData(SEX_MALE  );
    consort.setSexData(SEX_FEMALE);
    ActorAsPerson.setBond(oldKing, consort, BOND_MARRIED, BOND_MARRIED, 0.5f);
    council.toggleMember(oldKing, Role.MONARCH, true);
    
    palace.setResident(oldKing, true);
    palace.setResident(consort, true);
    palace .enterMap(map, 10, 10, 1);
    oldKing.enterMap(map, 12, 9 , 1);
    consort.enterMap(map, 12, 9 , 1);
    
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
    
    I.say("\nTOTAL LIFE CYCLE RUN TIME: "+RUN_TIME);
    
    while (map.time() < RUN_TIME || graphics) {
      map = test.runLoop(map, 1, graphics, "saves/test_cycle.tlt");
      
      if (map.time() % 1000 == 0) {
        I.say("  Time: "+map.time());
      }
      
      if (! migrated) {
        boolean allFilled = true;
        for (Building b : map.buildings()) {
          for (Type t : b.type().workerTypes) {
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
            a.setSexData(((i++ % 2) == 0) ? SEX_FEMALE : SEX_MALE);
          }
        }
      }
      
      if (migrated) {
        
        for (Building b : map.buildings()) {
          if (b.type() == ENGINEER_STATION) {
            b.setInventory(CLAY   , 5);
            b.setInventory(POTTERY, 5);
          }
          if (b.type() == HOUSE || b.type() == PALACE) {
            b.setInventory(MAIZE  , 5);
            b.setInventory(FRUIT  , 5);
            b.setInventory(POTTERY, 5);
            b.setInventory(COTTON , 5);
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
          if (a.child() && a.alive() && ! originalPop.includes(a)) {
            if (! births.includes(a)) {
              Series <Actor> parents = a.allBondedWith(BOND_PARENT);
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
          if (w.dead() && ! map.actors().includes(w)) {
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
      if (map.time() > MAX_DELAY && (! consort.pregnant()) && ! heirBorn) {
        consort.beginPregnancy();
      }
      if (consort.pregnant() && consort.inside() == palace && ! heirBorn) {
        consort.completePregnancy(palace, true);
      }
      if (! recognised) {
        recognised = council.memberWithRole(Role.HEIR) != null;
      }
      if (map.time() > MAX_REIGN && recognised && ! oldKing.dead()) {
        oldKing.setAsKilled("Died to allow heir to succeed");
      }
      if (oldKing.dead() && ! kingDied) {
        kingDied = true;
      }
      if (kingDied && ! succession) {
        Actor newKing = council.memberWithRole(Role.MONARCH);
        boolean isHeir = true;
        isHeir &= newKing != null;
        isHeir &= newKing != oldKing;
        isHeir &= oldKing.hasBondType(newKing, BOND_CHILD);
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
    I.say("  Alive:        "+oldKing.alive());
    I.say("  Children:     "+oldKing.allBondedWith(BOND_CHILD));
    I.say("  Current king: "+council.memberWithRole(Role.MONARCH));
    
    return testOkay;
  }
  
}





