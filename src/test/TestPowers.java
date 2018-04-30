

package test;
import game.*;
import static game.GameConstants.*;
import static content.GameContent.*;
import util.*;




public class TestPowers extends LogicTest {
  
  
  boolean actorPowerTest(boolean graphics, String title, ActorTechnique power) {
    return powerTest(graphics, title, power, false);
  }
  
  boolean rulerPowerTest(boolean graphics, String title, ActorTechnique power) {
    return powerTest(graphics, title, power, true);
  }
  
  boolean powerTest(
    boolean graphics, String title, ActorTechnique power, boolean guildCasts
  ) {
    Base base = LogicTest.setupTestBase(32, ALL_GOODS, false);
    base.setName("Client Base");
    Area map = base.activeMap();
    
    Building guild   = createGuild(map, base);
    Actor    caster  = guild.workers().first();
    Target   subject = createSubject(map, guild);
    
    boolean castOkay   = false;
    boolean effectOkay = false;
    boolean testOkay   = false;
    
    while (map.time() < 100 || graphics) {
      runLoop(base, 1, graphics, "saves/test_bounties.str");
      
      if (! castOkay) {
        castOkay = (! guildCasts) || castAsRuler(power, subject, base);
      }
      
      if (castOkay && ! effectOkay) {
        effectOkay = verifyEffect(subject, caster);
      }
      
      if (effectOkay && ! testOkay) {
        testOkay = true;
        I.say("\n"+title+" TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return true;
      }
    }
    
    I.say("\n"+title+" TEST FAILED!");
    return false;
  }
  
  
  Building createGuild(Area map, Base base) {
    return null;
  }
  
  
  Target createSubject(Area map, Building guild) {
    return null;
  }
  
  
  boolean castAsRuler(ActorTechnique power, Target subject, Base ruler) {
    return false;
  }
  
  
  boolean verifyEffect(Target subject, Actor caster) {
    return false;
  }
  
  
  
  /**  Other utility methods for common test-cases:
    */
  static Building createGuild(Area map, Base base, BuildType guildType) {
    Building guild = (Building) guildType.generate();
    guild.enterMap(map, 2, 2, 1, base);
    for (ActorType t : guildType.workerTypes.keys()) {
      ActorUtils.spawnActor(guild, t, false);
    }
    return guild;
  }
  
  
  static class TestCondition extends TestPowers {
    
    final BuildType guildType;
    final ActorType subjectType;
    final Tally <Trait> toCheck;
    Tally <Trait> initStats = new Tally();
    boolean boostOK = false;
    boolean fadeOK  = false;
    
    TestCondition(
      BuildType guildType, ActorType subjectType, Tally <Trait> toCheck
    ) {
      this.guildType   = guildType;
      this.subjectType = subjectType;
      this.toCheck     = toCheck;
    }
    
    Building createGuild(Area map, Base base) {
      return createGuild(map, base, guildType);
    }
    
    Target createSubject(Area map, Building guild) {
      Actor subject = (Actor) subjectType.generate();
      if (subjectType.isPerson()) {
        subjectType.initAsMigrant(subject);
      }
      if (subjectType.isAnimal()) {
        subjectType.initAsAnimal(subject);
      }
      subject.enterMap(map, 20, 20, 1, guild.base());
      subject.traits.updateTraits();
      for (Trait t : toCheck.keys()) {
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
      
      for (Trait t : toCheck.keys()) {
        float dir  = toCheck.valueFor(t) > 0 ? 1 : -1;
        float init = initStats.valueFor(t);
        float current = affects.traits.levelOf(t);
        
        if (! boostOK) {
          if (dir > 0 && current <= init) allStatsOK = false;
          if (dir < 0 && current >= init) allStatsOK = false;
        }
        else if (! fadeOK) {
          if (current != init) allStatsOK = false;
        }
      }
      
      if (allStatsOK) {
        if (! boostOK) boostOK = true;
        else fadeOK = true;
      }
      return allStatsOK;
    }
    
  }
  
  
}









