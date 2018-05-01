

package test;
import static game.GameConstants.*;
import game.*;
import static content.GameContent.*;
import content.*;
import util.*;



public class TestPowersLogician {
  
  
  public static void main(String args[]) {
    testConcentrate(false);
    testIntegrity(false);
    testCombat(false);
  }
  
  
  static boolean testConcentrate(boolean graphics) {
    TestPowers.TestCondition test = new TestPowers.TestCondition(
      SCHOOL_LOG, ECOLOGIST, Tally.with(TRAIT_DILIGENCE, 1, SKILL_WRITE, 1)
    );
    return test.rulerPowerTest(graphics, "CONCENTRATION", Logician.CONCENTRATION);
  }
  
  
  static boolean testIntegrity(boolean graphics) {
    TestPowers.TestCondition test = new TestPowers.TestCondition(
      SCHOOL_LOG, ECOLOGIST, Tally.with(STAT_HEALTH, 1, STAT_ARMOUR, 1)
    ) {
      float initHP = -1;
      boolean boostHP = false;
      
      Target createSubject(Area map, Building guild) {
        Actor subject = (Actor) super.createSubject(map, guild);
        initHP = subject.health.maxHealth();
        return subject;
      }
      
      boolean verifyEffect(Target subject, Actor caster) {
        Actor affects = (Actor) subject;
        if (! boostHP) {
          boostHP = affects.health.maxHealth() >= initHP + Logician.INTEG_HEALTH;
          if (! boostHP) return false;
        }
        if (super.verifyEffect(subject, caster)) {
          if (affects.health.maxHealth() > initHP) return false;
          return true;
        }
        return false;
      }
    };
    return test.rulerPowerTest(graphics, "INTEGRITY", Logician.INTEGRITY);
  }
  
  
  static boolean testCombat(boolean graphics) {
    TestPowers test = new TestPowers() {
      
      Building createGuild(Area map, Base base) {
        return createGuild(map, base, SCHOOL_LOG);
      }
      
      Target createSubject(Area map, Building guild) {
        Actor subject = createSubject(map, guild, Trooper.TROOPER, true);
        subject.health.takeDamage(subject.health.maxHealth() * 0.7f);
        subject.health.incBleed(-1000);
        return subject;
      }
      
      boolean verifyEffect(Target subject, Actor caster) {
        Actor strikes = (Actor) subject;
        
        caster.traits.setClassLevel(MAX_CLASS_LEVEL);
        
        if (! Task.inCombat(caster)) {
          Task combat = TaskCombat.configHunting(caster, strikes);
          caster.assignTask(combat, caster);
        }
        
        int minArmour = caster.type().armourClass + Logician.INTEG_ARMOUR;
        int minTire   = Logician.NERVE_DAMAGE - 1;
        
        if (caster.armourClass()     < minArmour) return false;
        if (strikes.health.fatigue() < minTire  ) return false;
        return true;
      }
    };
    return test.actorPowerTest(graphics, "NERVE STRIKE", Logician.NERVE_STRIKE);
  }
  
}




