

package content;
import game.*;
import game.Task.JOB;
import static game.GameConstants.*;
import static game.ActorTechnique.*;
import static game.ActorBonds.*;
import graphics.common.*;
import graphics.sfx.*;
import util.*;



public class SchoolShaper {
  
  
  final static String
    SHAPER_FN[] = {
      ""
    },
    SHAPER_LN[] = {
      ""
    }
  ;
  
  
  
  final public static int
    MAX_BEASTS      = 3,
    MAX_BEASTS_CL   = 6,
    BOND_PERSON_MAX = 50,
    BOND_NOVELTY    = -20,
    CAMO_DURATION   = 15,
    REGEN_DURATION  = 40
  ;
  final static PlaneFX.Model FX_MODEL = PlaneFX.imageModel(
    "sha_fx_model", SchoolShaper.class,
    "media/SFX/shaper_psy.png", 0.5f, 0, 0.25f, true, true
  );
  
  
  final public static ActorTechnique PHEREMONE_BOND = new ActorTechnique(
    "power_pheremone_bond", "Pheremone Bond"
  ) {
    
    float bondChance(Actor using, Actor subject) {
      float challenge = 100;
      if (subject.type().isAnimal()) {
        challenge = TaskCombat.attackPower(subject);
      }
      if (subject.type().isPerson()) {
        challenge = subject.traits.levelOf(TRAIT_SMART) / MAX_SKILL_LEVEL;
        challenge += 0.5f;
      }
      float skill  = using.traits.levelOf(SKILL_PRAY) / MAX_SKILL_LEVEL;
      float chance = Nums.clamp(skill + 1 - challenge, 0, 1);
      return chance;
    }
    
    int maxBeasts(Actor using) {
      float userLevel = using.traits.classLevel() * 1f / MAX_BEASTS_CL;
      int max = Nums.ceil(MAX_BEASTS * Nums.clamp(userLevel, 0, 1));
      return max;
    }
    
    int numBeasts(Actor using) {
      int num = 0;
      for (Focus a : using.bonds.allBondedWith(BOND_SERVANT)) {
        if (a.type().isAnimal()) num += 1;
      }
      return num;
    }
    
    public boolean canActorUse(Actor using, Target subject) {
      Actor affects = (Actor) subject;
      
      if (affects.type().isAnimal()) {
        if (numBeasts(using) >= maxBeasts(using)) return false;
        if (affects.base() == using.base()) return false;
      }
      else if (affects.type().isPerson()) {
        if (affects.inEmergency() || using.inEmergency()) return false;
        if (affects.bonds.bondNovelty(using) < 1) return false;
      }
      else {
        return false;
      }
      
      Actor master = (Actor) affects.bonds.bondedWith(BOND_MASTER);
      if (master != null) return false;
      
      return true;
    }
    
    public float rateUse(Actor using, Target subject) {
      float chance = bondChance(using, (Actor) subject);
      return super.rateUse(using, subject) * chance;
    }
    
    public void applyFromActor(Actor using, Target subject) {
      super.applyFromActor(using, subject);
      
      Actor affects = (Actor) subject;
      float chance = bondChance(using, affects);
      
      if (affects.type().isAnimal()) {
        if (Rand.num() < chance) {
          affects.wipeEmployment();
          affects.assignBase(using.base());
          ActorBonds.setBond(using, affects, BOND_MASTER, BOND_SERVANT, 1);
        }
      }
      if (affects.type().isPerson()) {
        if (Rand.num() < chance) {
          float boost = Rand.num() * BOND_PERSON_MAX / 100f;
          affects.bonds.incBond(using, boost, 1);
        }
        affects.bonds.incNovelty(using, BOND_NOVELTY / 100f);
      }
      
      dispenseXP(using, 1, SKILL_PRAY);
      
      AreaMap map = using.map();
      map.ephemera.addGhostFromModel(subject, FX_MODEL, 1, 0.5f, 1);
    }
  };
  static {
    PHEREMONE_BOND.attachMedia(
      SchoolShaper.class, "media/GUI/Powers/pheremone_bond.png",
      "Allows animals to be tamed in service of the caster, with a chance to "+
      "affect other relationships.",
      AnimNames.PSY_QUICK
    );
    PHEREMONE_BOND.setProperties(
      TARGET_OTHERS | SOURCE_TRAINED,
      Task.NO_HARM, MEDIUM_POWER
    );
    PHEREMONE_BOND.setCosting(400, MEDIUM_AP_COST, NO_TIRING, LONG_RANGE);
    PHEREMONE_BOND.setMinLevel(1);
  }
  
  
  final static Trait CAMOUFLAGE_CONDITION = new Trait(
    "condition_camouflage", "Camouflage"
  ) {
    
    protected float passiveBonus(Trait t) {
      if (t == SKILL_EVADE) return 20;
      return 0;
    }
    
    protected void passiveEffect(Actor actor) {
      if (Task.inCombat(actor)) {
        actor.health.clearConditions(this);
        return;
      }
      AreaMap map = actor.map();
      map.ephemera.updateGhost(actor, 1, FX_MODEL, 0.5f);
    }
  };
  
  final public static ActorTechnique CAMOUFLAGE = new ActorTechnique(
    "power_camouflage", "Camouflage"
  ) {
    
    void applyCamoEffects(Actor affects) {
      affects.health.addCondition(
        null, CAMOUFLAGE_CONDITION, CAMO_DURATION
      );
      for (Active a : affects.focused()) if (Task.inCombat((Element) a)) {
        a.assignTask(null, a);
      }
    }
    
    public boolean canRulerUse(Base ruler, Target subject) {
      if (! super.canRulerUse(ruler, subject)) return false;
      if (! subject.type().isActor()) return false;
      
      Actor affects = (Actor) subject;
      if (Task.inCombat(affects)) return false;
      if (! affects.health.organic()) return false;
      if (affects.health.hasCondition(CAMOUFLAGE)) return false;
      return true;
    }
    
    public void applyFromRuler(Base ruler, Target subject) {
      super.applyFromRuler(ruler, subject);
      applyCamoEffects((Actor) subject);
    }
    
    public boolean canActorUse(Actor using, Target subject) {
      if (! super.canActorUse(using, subject)) return false;
      if (Task.inCombat(using)) return false;
      if (using.jobType() == JOB.RETREAT  ) return true;
      if (using.jobType() == JOB.EXPLORING) return true;
      return false;
    }
    
    public void applyFromActor(Actor using, Target subject) {
      super.applyFromActor(using, subject);
      applyCamoEffects((Actor) subject);
    }
  };
  static {
    CAMOUFLAGE.attachMedia(
      SchoolShaper.class, "media/GUI/Powers/power_camouflage.png",
      "Conceals an organic target from attack as long as they do not engage "+
      "enemies.",
      AnimNames.PSY_QUICK
    );
    CAMOUFLAGE.setProperties(TARGET_SELF | SOURCE_TRAINED, Task.MILD_HELP, MEDIUM_POWER);
    CAMOUFLAGE.setCosting(250, NO_AP_COST, NO_TIRING, NO_RANGE);
    CAMOUFLAGE.setMinLevel(4);
  }
  
  
  
  final static Trait REGENERATE_CONDITION = new Trait(
    "condition_regenerate", "Regenerate"
  ) {
    
    protected float passiveBonus(Trait t) {
      if (t == STAT_HEALTH) return 5;
      return 0;
    }
    
    protected void passiveEffect(Actor actor) {
      AreaMap map = actor.map();
      
      float lift = actor.health.maxHealth();
      float wake = actor.health.maxHealth() / 2;
      lift *= 1f / (map.ticksPerSecond() * REGEN_DURATION);
      actor.health.liftDamage(lift);
      actor.health.incBleed(0 - lift * 2);
      
      if (actor.health.dead() && actor.health.injury() <= wake) {
        actor.health.setAsAlive("Regenerated full health!");
      }
      
      map.ephemera.updateGhost(actor, 1, FX_MODEL, 0.5f);
    }
  };
  
  final public static ActorTechnique REGENERATE = new ActorTechnique(
    "power_regenerate", "Regenerate"
  ) {
    
    public boolean canRulerUse(Base ruler, Target subject) {
      if (! super.canRulerUse(ruler, subject)) return false;
      if (! subject.type().isActor()) return false;
      
      Actor affects = (Actor) subject;
      if (! affects.health.organic()) return false;
      if (affects.health.hasCondition(REGENERATE)) return false;
      return true;
    }
    
    public void applyFromRuler(Base ruler, Target subject) {
      super.applyFromRuler(ruler, subject);
      
      Actor affects = (Actor) subject;
      affects.health.addCondition(
        null, REGENERATE_CONDITION, REGEN_DURATION
      );
    }
  };
  static {
    REGENERATE.attachMedia(
      SchoolShaper.class, "media/GUI/Powers/power_regenerate.png",
      "Regenerates the subject's health over "+REGEN_DURATION+" seconds. "+
      "Can be used to revive the recently dead.  Must target organics.",
      AnimNames.PSY_QUICK
    );
    REGENERATE.setProperties(TARGET_SELF | SOURCE_TRAINED, Task.MILD_HELP, MEDIUM_POWER);
    REGENERATE.setCosting(400, MINOR_AP_COST, NO_TIRING, NO_RANGE);
  }
  
  
  final public static HumanType SHAPER = new HumanType(
    "actor_shaper", CLASS_SOLDIER
  ) {
    public void initAsMigrant(Actor p) {
      super.initAsMigrant(p);
      final String name = generateName(SHAPER_FN, SHAPER_LN, null);
      p.setCustomName(name);
    }
  };
  static {
    SHAPER.name = "Shaper";
    SHAPER.attachCostume(SchoolShaper.class, "shaper_skin.gif");
    
    SHAPER.maxHealth   = 15;
    SHAPER.meleeDamage = 4;
    SHAPER.rangeDamage = 0;
    SHAPER.rangeDist   = 0;
    SHAPER.armourClass = 6;
    
    SHAPER.coreSkills.setWith(
      SKILL_PRAY , 8 ,
      SKILL_SPEAK, 6 ,
      SKILL_SIGHT, 6 ,
      SKILL_MELEE, 4 ,
      SKILL_HEAL , 6 ,
      SKILL_CRAFT, 3 
    );
    SHAPER.initTraits.setWith(
      TRAIT_EMPATHY  , 45,
      TRAIT_DILIGENCE, 20,
      TRAIT_BRAVERY  , 50,
      TRAIT_CURIOSITY, 70
    );
    
    SHAPER.classTechniques = new ActorTechnique[] { PHEREMONE_BOND, CAMOUFLAGE };
  }
}







