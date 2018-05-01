

package test;
import static content.GameContent.*;
import static game.GameConstants.*;
import content.*;
import game.Actor;
import game.Area;
import game.Building;
import game.GameConstants.Target;
import util.*;



public class TestPowersLogician {
  
  
  public static void main(String args[]) {
    testConcentrate(false);
    testIntegrity(false);
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
        if (! boostHP) {
          boostHP = caster.health.maxHealth() >= initHP + Logician.INTEG_HEALTH;
          if (! boostHP) return false;
        }
        return super.verifyEffect(subject, caster);
      }
    };
    return test.rulerPowerTest(graphics, "INTEGRITY", Logician.INTEGRITY);
  }
  
}












