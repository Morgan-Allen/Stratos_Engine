

package test;
import game.*;
import static game.GameConstants.*;
import static content.GameContent.*;
import static content.GameWorld.FACTION_SETTLERS;

import util.*;




public class TestFirstAid extends LogicTest {
  
  
  public static void main(String args[]) {
    testFirstAid(true);
  }
  
  
  static boolean testFirstAid(boolean graphics) {
    LogicTest test = new TestFirstAid();
    
    Base base = setupTestBase(FACTION_SETTLERS, ALL_GOODS, 32, false);
    Area map = base.activeMap();
    World world = map.world;
    world.settings.toggleFog    = false;
    world.settings.toggleHunger = false;
    
    Building sickbay = (Building) PHYSICIAN_STATION.generate();
    sickbay.enterMap(map, 2, 2, 1, base);
    ActorUtils.spawnActor(sickbay, PHYSICIAN, false);
    
    ActorAsPerson patient = (ActorAsPerson) ECOLOGIST.generate();
    ECOLOGIST.initAsMigrant(patient);
    patient.enterMap(map, 11, 11, 1, base);
    patient.health.takeDamage (patient.health.maxHealth() * 0.66f);
    patient.health.takeFatigue(patient.health.maxHealth() * 0.66f);
    
    boolean aidBegun   = false;
    boolean stabilised = false;
    boolean carryBegun = false;
    boolean atSickbay  = false;
    boolean healFinish = false;
    boolean testOkay   = false;
    
    float initHurt = -1, targetHurt = -1;
    final int RUN_TIME = AVG_BANDAGE_TIME * 2;
    
    while (map.time() < RUN_TIME || graphics) {
      test.runLoop(base, 1, graphics, "saves/test_first_aid.str");
      
      if (! aidBegun) {
        for (Actor a : sickbay.workers()) if (a.jobType() == Task.JOB.HEALING) {
          aidBegun = true;
        }
      }
      
      if (aidBegun && ! stabilised) {
        stabilised = patient.health.bleed() <= 0;
        initHurt   = patient.health.injury();
        targetHurt = initHurt - INJURY_HEAL_AMOUNT;
      }
      
      if (stabilised && ! carryBegun) {
        carryBegun = patient.isPassenger();
      }
      
      if (carryBegun && ! atSickbay) {
        atSickbay = patient.inside() == sickbay;
      }
      
      if (atSickbay && ! healFinish) {
        //I.say("\nPatient injury: "+patient.health.injury()+"/"+targetHurt);
        //I.say("  Total healed: "+TaskFirstAid.totalHealed);
        healFinish = patient.health.injury() < targetHurt;
      }
      
      if (healFinish && ! testOkay) {
        testOkay = true;
        I.say("\nFIRST AID TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return true;
      }
    }
    
    I.say("\nFIRST AID TEST FAILED!");
    I.say("  Aid begun:   "+aidBegun  );
    I.say("  Stabilised:  "+stabilised);
    I.say("  Carry begun: "+carryBegun);
    I.say("  At sickbay:  "+atSickbay );
    I.say("  Heal finish: "+healFinish);
    return false;
  }
  
}










