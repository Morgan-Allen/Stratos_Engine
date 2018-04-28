

package test;
import game.*;
import game.GameConstants.*;
import static content.GameContent.*;
import content.*;



public class TestPowersTekPriest {
  
  
  public static void main(String args[]) {
    testDrones(true);
  }
  
  
  static Building createGuildCommon(Area map, Base base) {
    Building guild = (Building) SCHOOL_TEK.generate();
    guild.enterMap(map, 2, 2, 1, base);
    ActorUtils.spawnActor(guild, TekPriest.TEK_PRIEST, false);
    return guild;
  }
  
  
  static boolean testDrones(boolean graphics) {
    TestPowers test = new TestPowers() {
      
      Actor toCharm = null;
      
      Building createGuild(Area map, Base base) {
        map.world.settings.toggleInjury = false;
        return createGuildCommon(map, base);
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
    return test.powerTest(graphics, "DRONE UPLINK", Collective.PSY_HEAL);
  }
  
}





