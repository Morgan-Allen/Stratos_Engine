

package content;
import game.*;
import static game.GameConstants.*;
import static game.ActorTechnique.*;
import graphics.common.*;
import graphics.sfx.*;
import util.Visit;



public class Logician {
  
  
  final static String
    LOGICIAN_FN[] = {
      ""
    },
    LOGICIAN_LN[] = {
      ""
    }
  ;
  
  
  final public static int
    CONC_DURATION  = 120,
    CONC_DILIGENT  = 50,
    CONC_BONUS     = 2 ,
    CONC_TIRE      = 10,
    INTEG_ARMOUR   = 5 ,
    INTEG_HEALTH   = 15,
    INTEG_DURATION = 40
  ;
  final static PlaneFX.Model FX_MODEL = PlaneFX.imageModel(
    "log_fx_model", Logician.class,
    "media/SFX/logician_psy.png", 0.5f, 0, 0.25f, true, true
  );
  
  
  final static Trait CONCENTRATION_CONDITION = new Trait(
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
      Logician.class, "media/GUI/Powers/power_shield_harmonics.png",
      "Provides a modest long-term bonus to most skills and boosts Diligence,"+
      " at the cost of morale and fatigue.  Subject must stay awake.",
      AnimNames.PSY_QUICK
    );
    CONCENTRATION.setProperties(0, Task.MILD_HELP, MEDIUM_POWER);
    CONCENTRATION.setCosting(100, MEDIUM_AP_COST, NO_TIRING, LONG_RANGE);
  }
  
  
  
  final static Trait INTEGRITY_CONDITION = new Trait(
    "condition_integrity", "Integrity"
  ) {
    
    protected float passiveBonus(Trait t) {
      if (t == STAT_ARMOUR) return INTEG_ARMOUR;
      if (t == STAT_HEALTH) return INTEG_HEALTH;
      return 0;
    }
    
    protected void passiveEffect(Actor actor) {
      Area map = actor.map();
      map.ephemera.updateGhost(actor, 1, FX_MODEL, 0.5f);
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
    }
  };
  static {
    INTEGRITY.attachMedia(
      Logician.class, "media/GUI/Powers/power_shield_harmonics.png",
      "Temporarily boosts the subject's physical health and armour class.",
      AnimNames.PSY_QUICK
    );
    INTEGRITY.setProperties(0, Task.MILD_HELP, MEDIUM_POWER);
    INTEGRITY.setCosting(350, MEDIUM_AP_COST, NO_TIRING, LONG_RANGE);
  }
  
  
  /*
  final public static ActorTechnique NERVE_STRIKE = new ActorTechnique(
    "power_nerve_strike", "NerveStrike"
  ) {
    //  TODO:  Fill this in.  Use during close combat to rapidly (but non-
    //  fatally) incapacitate organic targets, especially humans.
  };
  //*/
  
  
  
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
    LOGICIAN.attachCostume(Logician.class, "logician_skin.gif");
    
    LOGICIAN.maxHealth   = 18;
    LOGICIAN.meleeDamage = 6;
    LOGICIAN.rangeDamage = 0;
    LOGICIAN.rangeDist   = 0;
    LOGICIAN.armourClass = 0;
    
    LOGICIAN.coreSkills.setWith(
      SKILL_PRAY , 8 ,
      SKILL_HEAL , 6 ,
      SKILL_SPEAK, 6 ,
      SKILL_WRITE, 2
    );
    LOGICIAN.initTraits.setWith(
      TRAIT_EMPATHY  , 95,
      TRAIT_DILIGENCE, 50,
      TRAIT_BRAVERY  , 65,
      TRAIT_CURIOSITY, 30
    );
    
    //  TODO:  Work these out...
    
    //LOGICIAN.classTechniques = new ActorTechnique[] { NERVE_PINCH, INTEGRITY };
  }
}







