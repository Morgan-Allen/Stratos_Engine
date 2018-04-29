

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
      
      boolean verifyEffect(Target subject, Actor caster) {
        Actor healed = (Actor) subject;
        return healed.health.injury() < (healed.health.maxHealth() * 0.25f);
      }
      
    };
    return test.actorPowerTest(graphics, "PSY HEAL", Collective.PSY_HEAL);
  }
  
  
  static boolean testHarmonics(boolean graphics) {
    TestPowers test = new TestPowers() {
      
      final Trait toCheck[] = { SKILL_EVADE, STAT_SHIELD };
      Tally <Trait> initStats = new Tally();
      boolean boostOK = false;
      boolean fadeOK  = false;
      
      
      Building createGuild(Area map, Base base) {
        return createGuildCommon(map, base);
      }
      
      Target createSubject(Area map, Building guild) {
        Actor subject = (Actor) ECOLOGIST.generate();
        subject.type().initAsMigrant(subject);
        subject.enterMap(map, 20, 20, 1, guild.base());
        subject.traits.updateTraits();
        for (Trait t : toCheck) {
          initStats.set(t, subject.traits.levelOf(t));
        }
        return subject;
      }
      
      boolean castAsRuler(ActorTechnique power, Target subject, Base ruler) {
        power.applyFromRuler(ruler, subject);
        return true;
      }
      
      boolean verifyEffect(Target subject, Actor caster) {
        Actor affects = (Actor) subject;
        boolean allStatsOK = true;
        
        for (Trait t : toCheck) {
          float init = initStats.valueFor(t);
          float current = affects.traits.levelOf(t);
          
          if (! boostOK) {
            if (current <= init) allStatsOK = false;
          }
          else if (! fadeOK) {
            if (current > init) allStatsOK = false;
          }
        }
        
        if (allStatsOK) {
          if (! boostOK) boostOK = true;
          else fadeOK = true;
        }
        return allStatsOK;
      }
    };
    return test.rulerPowerTest(graphics, "SHIELD HARMONICS", Collective.SHIELD_HARMONICS);
  }
  
}











