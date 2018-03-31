

package test;
import game.*;
import util.I;

import static game.GameConstants.*;
import static content.GameContent.*;




public class TestFirstAid extends LogicTest {
  
  
  public static void main(String args[]) {
    testFirstAid(true);
  }
  
  static boolean testFirstAid(boolean graphics) {
    LogicTest test = new TestAutoBuild();
    
    Base base = setupTestBase(16, ALL_GOODS, false);
    Area map = base.activeMap();
    World world = map.world;
    world.settings.toggleFog    = false;
    world.settings.toggleHunger = false;
    
    
    //  TODO:  Ensure that random actors will perform a similar service...
    
    //  TODO:  You also need to ensure that the bandage actually speeds the
    //  healing process.
    
    Building sickbay = (Building) PHYSICIAN_STATION.generate();
    sickbay.enterMap(map, 2, 2, 1, base);
    ActorUtils.spawnActor(sickbay, PHYSICIAN, false);
    //ActorUtils.fillWorkVacancies(sickbay);
    
    boolean aidBegun   = false;
    boolean stabilised = false;
    boolean carryBegun = false;
    boolean atSickbay  = false;
    boolean healFinish = false;
    boolean testOkay   = false;
    
    ActorAsPerson patient = (ActorAsPerson) ECOLOGIST.generate();
    ECOLOGIST.initAsMigrant(patient);
    patient.enterMap(map, 11, 11, 1, base);
    patient.takeDamage (patient.maxHealth() * 0.66f);
    patient.takeFatigue(patient.maxHealth() * 0.66f);
    
    
    final int RUN_TIME = DAY_LENGTH * 2;
    
    while (map.time() < RUN_TIME || graphics) {
      test.runLoop(base, 1, graphics, "saves/test_first_aid.str");
      
      if (! aidBegun) {
        for (Actor a : sickbay.workers()) if (a.jobType() == Task.JOB.HEALING) {
          aidBegun = true;
        }
      }
      
      if (aidBegun && ! stabilised) {
        stabilised = patient.bleed() <= 0;
      }
      
      if (stabilised && ! carryBegun) {
        carryBegun = patient.isPassenger();
      }
      
      if (carryBegun && ! atSickbay) {
        atSickbay = patient.inside() == sickbay;
      }
      
      if (atSickbay && ! healFinish) {
        healFinish = patient.injury() < patient.maxHealth() * 0.33f;
      }
      
      if (healFinish && ! testOkay) {
        testOkay = true;
        I.say("\nFIRST AID TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return true;
      }
    }

    I.say("\n FIRST AID TEST FAILED!");
    I.say("  Aid begun:   "+aidBegun  );
    I.say("  Stabilised:  "+stabilised);
    I.say("  Carry begun: "+carryBegun);
    I.say("  At sickbay:  "+atSickbay );
    I.say("  Heal finish: "+healFinish);
    return false;
  }
  
}










