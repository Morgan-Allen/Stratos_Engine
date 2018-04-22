

package test;
import game.*;
import game.GameConstants.Target;
import content.*;
import static content.GameContent.*;



public class TestPowersCollective {
  
  
  public static void main(String args[]) {
    testHeal(true);
  }
  
  
  static Building createGuildCommon(Area map, Base base) {
    Building guild = (Building) SCHOOL_COL.generate();
    guild.enterMap(map, 2, 2, 1, base);
    ActorUtils.spawnActor(guild, Collective.COLLECTIVE, false);
    return guild;
  }
  
  
  static boolean testHeal(boolean graphics) {
    TestPowers test = new TestPowers() {
      
      Building createGuild(Area map, Base base) {
        return createGuildCommon(map, base);
      }
      
      Target createSubject(Area map, Building guild) {
        Actor subject = (Actor) ECOLOGIST.generate();
        subject.enterMap(map, 20, 20, 1, guild.base());
        subject.takeDamage(subject.health.maxHealth() * 0.75f);
        subject.health.incBleed(-1000);
        return subject;
      }
      
      boolean verifyEffect(Target subject) {
        Actor healed = (Actor) subject;
        return healed.health.injury() < (healed.health.maxHealth() * 0.25f);
      }
      
    };
    return test.powerTest(graphics, "PSY HEAL", Collective.PSY_HEAL);
  }
  
}





