

package test;
import static game.GameConstants.*;
import static game.RelationSet.*;
import game.*;
import static content.GameContent.*;
import static content.SchoolTekPriest.*;
import util.*;



public class TestPowersTekPriest {
  
  
  public static void main(String args[]) {
    testDrones(false);
    testStasis(false);
    testAssemble(false);
  }
  
  
  static boolean testDrones(boolean graphics) {
    TestPowers test = new TestPowers() {
      
      Actor toCharm = null;
      
      Building createGuild(AreaMap map, Base base) {
        map.world.settings.toggleInjury = false;
        map.world.settings.toggleFog    = false;
        return createGuild(map, base, SCHOOL_TEK);
      }
      
      Target createSubject(AreaMap map, Building guild) {
        Actor subject = (Actor) TRIPOD.generate();
        subject.enterMap(map, 20, 20, 1, map.locals);
        return toCharm = subject;
      }
      
      boolean verifyEffect(Target subject, Actor caster) {
        
        caster.traits.setClassLevel(MAX_DRONES_CL);
        
        if (toCharm.base() != caster.base()) return false;
        
        Actor master = (Actor) toCharm.bonds.bondedWith(ActorBonds.BOND_MASTER);
        if (master != caster) return false;
        
        int numDrones = 0;
        for (Focus a : caster.bonds.allBondedWith(ActorBonds.BOND_SERVANT)) {
          if (a.type().isConstruct()) numDrones += 1;
        }
        
        return numDrones == MAX_DRONES;
      }
    };
    return test.actorPowerTest(graphics, "DRONE UPLINK", DRONE_UPLINK);
  }
  
  
  static boolean testStasis(boolean graphics) {
    TestPowers.TestCondition test = new TestPowers.TestCondition(
      SCHOOL_TEK, ECOLOGIST, Tally.with(SKILL_EVADE, -1, STAT_SPEED, -1)
    );
    return test.rulerPowerTest(graphics, "STASIS FIELD", STASIS_FIELD);
  }
  
  
  static boolean testAssemble(boolean graphics) {
    TestPowers.TestCondition test = new TestPowers.TestCondition(
      SCHOOL_TEK, TRIPOD, Tally.with()
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
    return test.rulerPowerTest(graphics, "REASSEMBLY", REASSEMBLY);
  }
  
}





