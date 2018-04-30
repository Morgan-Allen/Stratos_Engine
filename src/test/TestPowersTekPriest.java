

package test;
import game.*;
import static game.GameConstants.*;
import static content.GameContent.*;
import content.*;
import util.*;



public class TestPowersTekPriest {
  
  
  public static void main(String args[]) {
    testDrones(false);
    testStasis(false);
  }
  
  
  static boolean testDrones(boolean graphics) {
    TestPowers test = new TestPowers() {
      
      Actor toCharm = null;
      
      Building createGuild(Area map, Base base) {
        map.world.settings.toggleInjury = false;
        map.world.settings.toggleFog    = false;
        return createGuild(map, base, SCHOOL_TEK);
      }
      
      Target createSubject(Area map, Building guild) {
        Actor subject = (Actor) TRIPOD.generate();
        subject.enterMap(map, 20, 20, 1, map.locals);
        return toCharm = subject;
      }
      
      boolean verifyEffect(Target subject, Actor caster) {
        
        caster.traits.setClassLevel(TekPriest.MAX_DRONES_CL);
        
        if (toCharm.base() != caster.base()) return false;
        
        Actor master = toCharm.traits.bondedWith(ActorTraits.BOND_MASTER);
        if (master != caster) return false;
        
        int numDrones = 0;
        for (Actor a : caster.traits.allBondedWith(ActorTraits.BOND_SERVANT)) {
          if (a.type().isConstruct()) numDrones += 1;
        }
        
        return numDrones == TekPriest.MAX_DRONES;
      }
    };
    return test.actorPowerTest(graphics, "DRONE UPLINK", TekPriest.DRONE_UPLINK);
  }
  
  
  static boolean testStasis(boolean graphics) {
    TestPowers.TestCondition test = new TestPowers.TestCondition(
      SCHOOL_TEK, ECOLOGIST, Tally.with(SKILL_EVADE, -1, STAT_SPEED, -1)
    );
    return test.rulerPowerTest(graphics, "STASIS FIELD", TekPriest.STASIS_FIELD);
  }
  
}





