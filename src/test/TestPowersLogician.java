

package test;
import static game.GameConstants.*;
import game.*;
import static content.GameContent.*;
import static content.SchoolLogician.*;
import content.*;
import util.*;



public class TestPowersLogician {
  
  
  public static void main(String args[]) {
    testConcentrate(false);
    testIntegrity(false);
    testStrike(false);
  }
  
  
  static boolean testConcentrate(boolean graphics) {
    TestPowers.TestCondition test = new TestPowers.TestCondition(
      SCHOOL_LOG, ECOLOGIST, Tally.with(TRAIT_DILIGENCE, 1, SKILL_WRITE, 1)
    );
    return test.rulerPowerTest(graphics, "CONCENTRATION", CONCENTRATION);
  }
  
  
  static boolean testIntegrity(boolean graphics) {
    TestPowers.TestCondition test = new TestPowers.TestCondition(
      SCHOOL_LOG, ECOLOGIST, Tally.with(STAT_HEALTH, 1, STAT_ARMOUR, 1)
    ) {
      float initHP = -1;
      boolean boostHP = false;
      
      Target createSubject(AreaMap map, Building guild) {
        Actor subject = (Actor) super.createSubject(map, guild);
        initHP = subject.health.maxHealth();
        return subject;
      }
      
      boolean verifyEffect(Target subject, Actor caster) {
        Actor affects = (Actor) subject;
        if (! boostHP) {
          boostHP = affects.health.maxHealth() >= initHP + INTEG_HEALTH;
          if (! boostHP) return false;
        }
        if (super.verifyEffect(subject, caster)) {
          if (affects.health.maxHealth() > initHP) return false;
          return true;
        }
        return false;
      }
    };
    return test.rulerPowerTest(graphics, "INTEGRITY", INTEGRITY);
  }
  
  
  static boolean testStrike(boolean graphics) {
    TestPowers test = new TestPowers() {
      
      Building createGuild(AreaMap map, Base base) {
        map.world.settings.toggleFog     = false;
        map.world.settings.toggleRetreat = false;
        return createGuild(map, base, SCHOOL_LOG);
      }
      
      Target createSubject(AreaMap map, Building guild) {
        Actor subject = createSubject(map, guild, Trooper.TROOPER, true);
        //subject.health.takeDamage(subject.health.maxHealth() * 0.5f);
        //subject.health.incBleed(-1000);
        return subject;
      }
      
      boolean verifyEffect(Target subject, Actor caster) {
        Actor strikes = (Actor) subject;
        
        caster.traits.setClassLevel(MAX_CLASS_LEVEL);
        int minTire = NERVE_DAMAGE - 1;
        
        //  We are testing here to ensure that (A) the caster inflicts a certain
        //  level of nonlethal damage on the subject, and (B), casts a
        //  protective buff on themselves.
        
        if (strikes.health.active() && ! Task.inCombat(caster, strikes, false)) {
          Task combat = TaskCombat.configHunting(caster, strikes);
          caster.assignTask(combat, caster);
        }
        if (caster.health.injury() > 0) {
          caster.health.liftDamage(1000);
        }
        if (strikes.health.injury() > 0) {
          strikes.health.liftDamage(1000);
        }
        
        /*
        I.say("\nCaster in combat: "+Task.inCombat(caster));
        I.say("  Caster cooldown: "+caster.health.cooldown());
        I.say("  Strikes alive/active? "+strikes.health.alive()+"/"+strikes.health.active());
        I.say("  Caster  alive/active? "+caster .health.alive()+"/"+caster .health.active());
        
        if (strikes.map().time() > 70) {
          I.say("?");
        }
        //*/
        
        if (! caster.health.hasCondition(INTEGRITY_CONDITION)) {
          return false;
        }
        if (strikes.health.fatigue() < minTire) {
          return false;
        }
        return true;
      }
    };
    return test.actorPowerTest(graphics, "NERVE STRIKE", NERVE_STRIKE);
  }
  
}




