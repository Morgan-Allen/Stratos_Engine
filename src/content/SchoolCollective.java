

package content;
import game.*;
import game.Task.JOB;
import static game.GameConstants.*;
import static game.ActorTechnique.*;
import graphics.common.*;
import graphics.sfx.*;
import util.*;



public class SchoolCollective {
  
  
  final static String
    COLLECTIVE_FN[] = {
      "Une", "Bena", "Blis", "Pax", "Sela", "Nami", "Oolen", "Nioba"
    },
    COLLECTIVE_LN[] = {
      "of 9", "Primus", "003", "Iambis", "Orela", "the Zen", " of Melding", "10"
    }
  ;
  
  
  
  final public static int
    PSY_HEAL_AMOUNT        = 10,
    HARMONICS_SHIELD_BONUS = 6 ,
    HARMONICS_STATS_BONUS  = 2 ,
    HARMONICS_DURATION     = 30,
    SYNERGY_PASSIVE        = 2 ,
    SYNERGY_GROUP_MULT     = 30
  ;
  final static PlaneFX.Model FX_MODEL = PlaneFX.imageModel(
    "col_fx_model", SchoolCollective.class,
    "media/SFX/collective_psy.png", 0.5f, 0, 0.25f, true, true
  );
  
  
  final public static ActorTechnique PSY_HEAL = new ActorTechnique(
    "power_psy_heal", "Psy Heal"
  ) {
    
    boolean canHeal(Target subject) {
      if (! subject.type().isActor()) return false;
      
      final Actor a = (Actor) subject;
      if (! a.health.organic()) return false;
      if (a.health.injury() <= 0) return false;
      
      return true;
    }
    
    public boolean canActorUse(Actor using, Target subject) {
      if (! super.canActorUse(using, subject)) return false;
      return canHeal(subject);
    }
    
    public float rateUse(Actor using, Target subject) {
      float rating = super.rateUse(using, subject);
      if (rating <= 0) return 0;
      
      final Actor a = (Actor) subject;
      float hurtLevel = a.health.injury() / a.health.maxHealth();
      rating *= hurtLevel * 1.5f;
      return rating;
    }
    
    public void applyFromActor(Actor using, Target subject) {
      super.applyFromActor(using, subject);
      
      final Actor healed = (Actor) subject;
      AreaMap map = healed.map();
      
      healed.health.liftDamage(PSY_HEAL_AMOUNT);
      healed.health.incBleed(-1000);
      dispenseXP(using, 1, SKILL_PRAY);
      
      map.ephemera.addGhostFromModel(healed, FX_MODEL, 1, 0.5f, 1);
    }
    
    public boolean canRulerUse(Base ruler, Target subject) {
      if (! super.canRulerUse(ruler, subject)) return false;
      return canHeal(subject);
    }
    
    public void applyFromRuler(Base ruler, Target subject) {
      super.applyFromRuler(ruler, subject);
      
      final Actor healed = (Actor) subject;
      AreaMap map = healed.map();
      
      healed.health.liftDamage(PSY_HEAL_AMOUNT);
      healed.health.incBleed(-1000);
      
      map.ephemera.addGhostFromModel(healed, FX_MODEL, 1, 0.5f, 1);
    }
  };
  static {
    PSY_HEAL.attachMedia(
      SchoolCollective.class, "media/GUI/Powers/power_psy_heal.png",
      "Heals the subject for up to "+PSY_HEAL_AMOUNT+" damage.",
      AnimNames.LOOK
    );
    PSY_HEAL.setProperties(TARGET_OTHERS | SOURCE_TRAINED, Task.FULL_HELP, MEDIUM_POWER);
    PSY_HEAL.setCosting(150, MEDIUM_AP_COST, NO_TIRING, LONG_RANGE);
    PSY_HEAL.setMinLevel(4);
  }
  
  
  
  final static Trait SHIELD_HARMONICS_CONDITION = new Trait(
    "condition_shield_harmonics", "Shield Harmonics"
  ) {
    
    protected float passiveBonus(Trait t) {
      if (t == STAT_SHIELD) return HARMONICS_SHIELD_BONUS;
      if (t == SKILL_EVADE) return HARMONICS_STATS_BONUS;
      if (t == SKILL_MELEE) return HARMONICS_STATS_BONUS;
      return 0;
    }
    
    protected void passiveEffect(Actor actor) {
      AreaMap map = actor.map();
      map.ephemera.updateGhost(actor, 1, FX_MODEL, 0.5f);
    }
  };
  
  final public static ActorTechnique SHIELD_HARMONICS = new ActorTechnique(
    "power_shield_harmonics", "Shield Harmonics"
  ) {
    public boolean canRulerUse(Base ruler, Target subject) {
      if (! super.canRulerUse(ruler, subject)) return false;
      return subject.type().isActor();
    }
    
    public void applyFromRuler(Base ruler, Target subject) {
      super.applyFromRuler(ruler, subject);
      Actor affects = (Actor) subject;
      affects.health.addCondition(
        null, SHIELD_HARMONICS_CONDITION, HARMONICS_DURATION
      );
    }
  };
  static {
    SHIELD_HARMONICS.attachMedia(
      SchoolCollective.class, "media/GUI/Powers/power_shield_harmonics.png",
      "Boosts target shields by "+HARMONICS_SHIELD_BONUS+" and provides a "+
      "mild bonus to defensive skills for "+HARMONICS_DURATION+" seconds.",
      AnimNames.PSY_QUICK
    );
    SHIELD_HARMONICS.setProperties(0, Task.MILD_HELP, MEDIUM_POWER);
    SHIELD_HARMONICS.setCosting(100, MEDIUM_AP_COST, NO_TIRING, LONG_RANGE);
  }
  
  
  
  final public static ActorTechnique SYNERGY = new ActorTechnique(
    "power_synergy", "Synergy"
  ) {
    
    protected float passiveBonus(Trait t) {
      if (t == SKILL_SPEAK) return SYNERGY_PASSIVE;
      if (t == SKILL_HEAL ) return SYNERGY_PASSIVE;
      return 0;
    }
    
    protected void passiveEffect(Actor actor) {
      //  TODO:  Flesh this out a bit more.  Allow a chance to interrupt attacks
      //  by enemies and prompt conversation, say?
      /*
      float stunChance = 0.5f / actor.map().ticksPerSecond();
      float range = actor.sightRange();
      
      for (Active a : actor.focused()) {
        if (! a.mobile()) continue;
        Actor other = (Actor) a;
        
        if (! Task.inCombat(other)) continue;
        if (Area.distance(actor, other) > range) continue;
        
        if (Rand.num() < stunChance) {
          Task stun = other.targetTask(other, 1, JOB.FLINCH, null);
          other.assignReaction(stun);
        }
      }
      //*/
      return;
    }
  };
  static {
    SYNERGY.attachMedia(
      SchoolCollective.class, "media/GUI/Powers/power_synergy.png",
      "Provides passive bonuses to diplomacy skills and health recovery.",
      AnimNames.PSY_QUICK
    );
    SYNERGY.setProperties(SOURCE_TRAINED | IS_PASSIVE, Task.MILD_HELP, MEDIUM_POWER);
    SYNERGY.setCosting(100, NO_AP_COST, NO_TIRING, NO_RANGE);
    SYNERGY.setMinLevel(1);
  }
  
  
  
  final public static HumanType COLLECTIVE = new HumanType(
    "actor_collective", CLASS_SOLDIER
  ) {
    public void initAsMigrant(Actor p) {
      super.initAsMigrant(p);
      final String name = generateName(COLLECTIVE_FN, COLLECTIVE_LN, null);
      p.setCustomName(name);
    }
  };
  static {
    COLLECTIVE.name = "Collective";
    COLLECTIVE.attachCostume(SchoolCollective.class, "collective_skin.gif");
    
    COLLECTIVE.maxHealth   = 12;
    COLLECTIVE.meleeDamage = 0;
    COLLECTIVE.rangeDamage = 0;
    COLLECTIVE.rangeDist   = 0;
    COLLECTIVE.armourClass = 0;
    
    COLLECTIVE.coreSkills.setWith(
      SKILL_PRAY , 8 ,
      SKILL_HEAL , 6 ,
      SKILL_SPEAK, 6 ,
      SKILL_WRITE, 2
    );
    COLLECTIVE.initTraits.setWith(
      TRAIT_EMPATHY  , 95,
      TRAIT_DILIGENCE, 50,
      TRAIT_BRAVERY  , 65,
      TRAIT_CURIOSITY, 30
    );
    
    COLLECTIVE.classTechniques = new ActorTechnique[] { PSY_HEAL, SYNERGY };
  }
}







