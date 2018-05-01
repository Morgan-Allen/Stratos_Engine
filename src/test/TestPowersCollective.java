

package test;
import game.*;
import static game.GameConstants.*;
import static content.GameContent.*;
import content.*;
import util.*;



public class TestPowersCollective {
  
  
  public static void main(String args[]) {
    testHeal(false);
    testHarmonics(false);
    testSynergy(false);
  }
  
  
  static boolean testHeal(boolean graphics) {
    TestPowers test = new TestPowers() {
      
      Building createGuild(Area map, Base base) {
        return createGuild(map, base, SCHOOL_COL);
      }
      
      Target createSubject(Area map, Building guild) {
        Actor subject = (Actor) ECOLOGIST.generate();
        subject.enterMap(map, 20, 20, 1, guild.base());
        subject.takeDamage(subject.health.maxHealth() * 0.75f);
        subject.health.incBleed(-1000);
        return subject;
      }
      
      boolean verifyEffect(Target subject, Actor caster) {
        caster.traits.setClassLevel(Collective.PSY_HEAL.minLevel);
        Actor healed = (Actor) subject;
        return healed.health.injury() < (healed.health.maxHealth() * 0.25f);
      }
      
    };
    return test.actorPowerTest(graphics, "PSY HEAL", Collective.PSY_HEAL);
  }
  
  
  static boolean testHarmonics(boolean graphics) {
    TestPowers.TestCondition test = new TestPowers.TestCondition(
      SCHOOL_COL, ECOLOGIST, Tally.with(SKILL_EVADE, 1, STAT_SHIELD, 1)
    );
    return test.rulerPowerTest(graphics, "SHIELD HARMONICS", Collective.SHIELD_HARMONICS);
  }
  
  
  static boolean testSynergy(boolean graphics) {
    TestPowers test = new TestPowers() {
      
      float initSkill = -1;
      
      Building createGuild(Area map, Base base) {
        return createGuild(map, base, SCHOOL_COL);
      }
      
      Target createSubject(Area map, Building guild) {
        Actor subject = guild.workers().first();
        subject.traits.setClassLevel(Collective.SYNERGY.minLevel);
        initSkill = subject.traits.levelOf(SKILL_SPEAK);
        return subject;
      }
      
      boolean verifyEffect(Target subject, Actor caster) {
        return caster.traits.levelOf(SKILL_SPEAK) > initSkill;
      }
    };
    return test.actorPowerTest(graphics, "SYNERGY", Collective.SYNERGY);
  }
}










