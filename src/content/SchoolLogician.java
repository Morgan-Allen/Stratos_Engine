

package content;
import static game.GameConstants.*;
import static game.ActorTechnique.*;
import game.*;
import game.Task.JOB;
import graphics.common.*;
import graphics.sfx.*;
import util.*;



public class SchoolLogician {
  
  
  final static String
    LOGICIAN_FN[] = {
      ""
    },
    LOGICIAN_LN[] = {
      ""
    }
  ;
  
  
  final public static int
    CONC_DURATION   = 120,
    CONC_DILIGENT   = 50 ,
    CONC_BONUS      = 2  ,
    CONC_TIRE       = 10 ,
    NERVE_DAMAGE    = 10 ,
    NERVE_HUMAN_MUL = 150,
    INTEG_ARMOUR    = 5  ,
    INTEG_HEALTH    = 15 ,
    INTEG_DURATION  = 40 
  ;
  final static PlaneFX.Model FX_MODEL = PlaneFX.imageModel(
    "log_fx_model", SchoolLogician.class,
    "media/SFX/logician_psy.png", 0.5f, 0, 0.25f, true, true
  );
  
  
  final public static Trait CONCENTRATION_CONDITION = new Trait(
    "condition_concentration", "Concentration"
  ) {
    
    protected float passiveBonus(Trait t) {
      if (Visit.arrayIncludes(ALL_SKILLS, t)) {
        return CONC_BONUS;
      }
      if (t == TRAIT_DILIGENCE) return 0.5f;
      return 0;
    }
    
    protected void passiveEffect(Actor actor) {
      Area map = actor.map();
      
      float tire = CONC_TIRE;
      tire *= 1f / (map.ticksPerSecond() * CONC_DURATION);
      actor.health.takeFatigue(tire);
      
      if (! actor.health.active()) {
        actor.health.clearConditions(this);
        return;
      }
      
      map.ephemera.updateGhost(actor, 1, FX_MODEL, 0.5f);
    }
  };
  
  final public static ActorTechnique CONCENTRATION = new ActorTechnique(
    "power_concentration", "Concentration"
  ) {
    public boolean canRulerUse(Base ruler, Target subject) {
      if (! super.canRulerUse(ruler, subject)) return false;
      
      Actor affects = (Actor) subject;
      if (! affects.health.organic()) return false;
      if (affects.health.hasCondition(CONCENTRATION_CONDITION)) return false;
      if (! affects.health.active()) return false;
      return true;
    }
    
    public void applyFromRuler(Base ruler, Target subject) {
      super.applyFromRuler(ruler, subject);
      Actor affects = (Actor) subject;
      affects.health.addCondition(
        null, CONCENTRATION_CONDITION, CONC_DURATION
      );
    }
  };
  static {
    CONCENTRATION.attachMedia(
      SchoolLogician.class, "media/GUI/Powers/power_concentration.png",
      "Provides a modest long-term bonus to most skills and boosts Diligence,"+
      " at the cost of morale and fatigue.  Subject must stay awake.",
      AnimNames.PSY_QUICK
    );
    CONCENTRATION.setProperties(0, Task.MILD_HELP, MEDIUM_POWER);
    CONCENTRATION.setCosting(100, MEDIUM_AP_COST, NO_TIRING, LONG_RANGE);
  }
  
  
  
  final public static Trait INTEGRITY_CONDITION = new Trait(
    "condition_integrity", "Integrity"
  ) {
    
    protected float passiveBonus(Trait t) {
      if (t == STAT_ARMOUR) return INTEG_ARMOUR;
      if (t == STAT_HEALTH) return INTEG_HEALTH;
      return 0;
    }
    
    protected void passiveEffect(Actor actor) {
      //Area map = actor.map();
      //map.ephemera.updateGhost(actor, 1, FX_MODEL, 0.5f);
    }
  };
  
  final public static ActorTechnique INTEGRITY = new ActorTechnique(
    "power_integrity", "Integrity"
  ) {
    public boolean canRulerUse(Base ruler, Target subject) {
      if (! super.canRulerUse(ruler, subject)) return false;
      
      Actor affects = (Actor) subject;
      if (affects.health.hasCondition(INTEGRITY_CONDITION)) return false;
      return true;
    }
    
    public void applyFromRuler(Base ruler, Target subject) {
      super.applyFromRuler(ruler, subject);
      
      Actor affects = (Actor) subject;
      affects.health.addCondition(
        null, INTEGRITY_CONDITION, INTEG_DURATION
      );
      
      Area map = affects.map();
      map.ephemera.addGhostFromModel(subject, FX_MODEL, 1, 0.5f, 1);
    }
    
    public boolean canActorUse(Actor using, Target subject) {
      if (! super.canActorUse(using, subject)) return false;
      if (! Task.inCombat(using)) return false;
      if (using.health.hasCondition(INTEGRITY_CONDITION)) return false;
      return true;
    }
    
    public void applyFromActor(Actor using, Target subject) {
      super.applyFromActor(using, subject);
      
      Actor affects = (Actor) subject;
      affects.health.addCondition(
        null, INTEGRITY_CONDITION, INTEG_DURATION
      );
      
      Area map = affects.map();
      map.ephemera.addGhostFromModel(subject, FX_MODEL, 1, 0.5f, 1);
    }
  };
  static {
    INTEGRITY.attachMedia(
      SchoolLogician.class, "media/GUI/Powers/power_integrity.png",
      "Temporarily boosts the subject's physical health and armour class.",
      AnimNames.PSY_QUICK
    );
    INTEGRITY.setProperties(TARGET_SELF | SOURCE_TRAINED, Task.MILD_HELP, MEDIUM_POWER);
    INTEGRITY.setCosting(350, NO_AP_COST, NO_TIRING, NO_RANGE);
    INTEGRITY.setMinLevel(4);
  }
  
  
  
  final public static ActorTechnique NERVE_STRIKE = new ActorTechnique(
    "power_nerve_strike", "Nerve Strike"
  ) {
    
    public boolean canActorUse(Actor using, Target subject) {
      if (! super.canActorUse(using, subject)) return false;
      if (! subject.type().isActor()) return false;
      
      Actor affects = (Actor) using;
      if (! affects.health.organic()) return false;
      return true;
    }
    
    public float rateUse(Actor using, Target subject) {
      float rating = super.rateUse(using, subject);
      if (rating <= 0) return rating;
      
      Actor affects = (Actor) subject;
      float hurt = affects.health.hurtLevel() / 2;
      
      return rating * (0.75f + hurt);
    }
    
    public void applyFromActor(Actor using, Target subject) {
      super.applyFromActor(using, subject);
      
      Area map = using.map();
      Actor struck = (Actor) subject;
      
      float resist = struck.traits.levelOf(SKILL_MELEE) / MAX_SKILL_LEVEL;
      float skill  = using .traits.levelOf(SKILL_MELEE) / MAX_SKILL_LEVEL;
      float chance = (skill + 1 - resist) / 2;
      
      dispenseXP(using, 1, SKILL_PRAY);
      
      if (Rand.num() < chance) {
        float damage = NERVE_DAMAGE;
        if (struck.type().isPerson()) damage *= NERVE_HUMAN_MUL / 100f;
        struck.health.takeFatigue(damage);
        
        Task stun = struck.targetTask(struck, 1, JOB.FLINCH, null);
        struck.assignReaction(stun);
        
        map.ephemera.addGhostFromModel(struck, FX_MODEL, 1, 0.5f, 1);
        //I.say("Nerve strike success!");
      }
      else {
        //I.say("Nerve strike failed!");
      }
    }
  };
  static {
    NERVE_STRIKE.attachMedia(
      SchoolLogician.class, "media/GUI/Powers/power_nerve_strike.png",
      "A debilitating close-range attack that inflicts nonlethal damage on "+
      "organic targets.",
      AnimNames.STRIKE
    );
    NERVE_STRIKE.setProperties(TARGET_OTHERS | SOURCE_TRAINED, Task.MILD_HARM, MEDIUM_POWER);
    NERVE_STRIKE.setCosting(100, MEDIUM_AP_COST, NO_TIRING, MELEE_RANGE);
    NERVE_STRIKE.setMinLevel(1);
  }
  
  
  
  final public static HumanType LOGICIAN = new HumanType(
    "actor_logician", CLASS_SOLDIER
  ) {
    public void initAsMigrant(Actor p) {
      super.initAsMigrant(p);
      final String name = generateName(LOGICIAN_FN, LOGICIAN_LN, null);
      p.setCustomName(name);
    }
  };
  static {
    LOGICIAN.name = "Logician";
    LOGICIAN.attachCostume(SchoolLogician.class, "logician_skin.gif");
    
    LOGICIAN.maxHealth   = 18;
    LOGICIAN.meleeDamage = 6;
    LOGICIAN.rangeDamage = 0;
    LOGICIAN.rangeDist   = 0;
    LOGICIAN.armourClass = 0;
    
    LOGICIAN.coreSkills.setWith(
      SKILL_PRAY , 8 ,
      SKILL_HEAL , 2 ,
      SKILL_SPEAK, 6 ,
      SKILL_WRITE, 8 ,
      SKILL_MELEE, 7 ,
      SKILL_EVADE, 7
    );
    LOGICIAN.initTraits.setWith(
      TRAIT_EMPATHY  , 40,
      TRAIT_DILIGENCE, 95,
      TRAIT_BRAVERY  , 65,
      TRAIT_CURIOSITY, 75
    );
    
    LOGICIAN.classTechniques = new ActorTechnique[] { NERVE_STRIKE, INTEGRITY };
  }
}







