

package test;
import game.*;
import static game.GameConstants.*;
import static content.GameContent.*;
import static content.SchoolShaper.*;
import util.*;



public class TestPowersShaper {
  
  
  public static void main(String args[]) {
    testBonds(false);
    testCamo(false);
    testRegen(false);
  }
  
  
  static boolean testBonds(boolean graphics) {
    TestPowers test = new TestPowers() {
      
      Actor toCharm = null;
      
      Building createGuild(AreaMap map, Base base) {
        map.world.settings.toggleInjury = false;
        map.world.settings.toggleFog    = false;
        return createGuild(map, base, SCHOOL_SHA);
      }
      
      Target createSubject(AreaMap map, Building guild) {
        Actor subject = (Actor) MICOVORE.generate();
        subject.enterMap(map, 20, 20, 1, map.area.locals);
        return toCharm = subject;
      }
      
      boolean verifyEffect(Target subject, Actor caster) {
        caster.traits.setClassLevel(MAX_BEASTS_CL);
        
        Actor master = (Actor) toCharm.bonds.bondedWith(ActorBonds.BOND_MASTER);
        if (master != caster) return false;
        
        return true;
      }
    };
    return test.actorPowerTest(graphics, "PHEREMONE BOND", PHEREMONE_BOND);
  }
  
  
  static boolean testCamo(boolean graphics) {
    //  TODO:  You also need to test to ensure that other actors won't notice
    //  the subject while 'stealthed' like this...
    
    TestPowers.TestCondition test = new TestPowers.TestCondition(
      SCHOOL_SHA, ECOLOGIST, Tally.with(SKILL_EVADE, 1)
    );
    return test.rulerPowerTest(graphics, "CAMOUFLAGE", CAMOUFLAGE);
  }
  
  
  static boolean testRegen(boolean graphics) {
    TestPowers.TestCondition test = new TestPowers.TestCondition(
      SCHOOL_SHA, ECOLOGIST, Tally.with()
    ) {
      
      Target createSubject(AreaMap map, Building guild) {
        map.world.settings.toggleFog = false;
        Actor affects = (Actor) super.createSubject(map, guild);
        affects.health.takeDamage(1000);
        return affects;
      }
      
      boolean verifyEffect(Target subject, Actor caster) {
        Actor affects = (Actor) subject;
        float maxHP = affects.health.maxHealth();
        if (! affects.health.alive()) return false;
        if (affects.health.injury() > (maxHP / 2)) return false;
        return true;
      }
    };
    return test.rulerPowerTest(graphics, "REGENERATE", REGENERATE);
  }
  
}











