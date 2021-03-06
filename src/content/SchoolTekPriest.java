

package content;
import game.*;
import static game.GameConstants.*;
import static game.ActorTechnique.*;
import static game.ActorTraits.*;
import static game.ActorBonds.*;
import graphics.common.*;
import graphics.sfx.*;
import util.*;



public class SchoolTekPriest {
  
  
  final static String
    TEK_PRIEST_FN[] = {
      "Reverend Mother", "Mother", "Mother", "Sister", "Sister", "Sister"
    },
    TEK_PRIEST_LN[] = {
      "Lucretia", "Sovia", "Xharyn", "Hypatia", "Andromeda", "Xiope"
    }
  ;
  
  
  
  final public static int
    MAX_DRONES        = 3,
    MAX_DRONES_CL     = 6,
    STASIS_DURATION   = 15,
    STASIS_MINI_DUR   = 5,
    ASSEMBLE_DURATION = 20
  ;
  final static PlaneFX.Model FX_MODEL = PlaneFX.imageModel(
    "tek_fx_model", SchoolTekPriest.class,
    "media/SFX/tek_priest_psy.png", 0.5f, 0, 0.25f, true, true
  );
  
  
  final public static ActorTechnique DRONE_UPLINK = new ActorTechnique(
    "power_drone_uplink", "Drone Uplink"
  ) {
    
    float hackChance(Actor using, Actor subject) {
      
      float challenge = 0, skill = 0;
      if (subject.type() == GameContent.DRONE  ) challenge = 0.33f;
      if (subject.type() == GameContent.TRIPOD ) challenge = 0.66f;
      if (subject.type() == GameContent.CRANIAL) challenge = 1.00f;
      
      if (using != null) {
        skill = using.traits.levelOf(SKILL_PRAY) / MAX_SKILL_LEVEL;
      }
      else {
        skill = 0.33f;
      }
      
      float chance = Nums.clamp(skill + 1 - challenge, 0, 1);
      return chance;
    }
    
    int maxDrones(Actor using) {
      float userLevel = using.traits.classLevel() * 1f / MAX_DRONES_CL;
      int max = Nums.ceil(MAX_DRONES * Nums.clamp(userLevel, 0, 1));
      return max;
    }
    
    int numDrones(Actor using) {
      int num = 0;
      for (Focus f : using.bonds.allBondedWith(BOND_SERVANT)) {
        Actor a = (Actor) f;
        if (! a.health.active()) continue;
        if (a.type().isConstruct()) num += 1;
      }
      return num;
    }
    
    Actor weakestDrone(Actor using) {
      Pick <Actor> pick = new Pick();
      for (Focus f : using.bonds.allBondedWith(BOND_SERVANT)) {
        Actor a = (Actor) f;
        if (! a.type().isConstruct()) continue;
        pick.compare(a, 0 - TaskCombat.attackPower(a));
      }
      return pick.result();
    }
    
    public boolean canActorUse(Actor using, Target subject) {
      if (subject == using) {
        if (numDrones(using) >= maxDrones(using)) return false;
        if (using.inside() != using.work()) return false;
        return true;
      }
      else {
        if (! subject.type().isConstruct()) return false;
        
        Actor affects = (Actor) subject;
        Actor master = (Actor) affects.bonds.bondedWith(BOND_MASTER);
        if (master != null) return false;
        
        return true;
      }
    }
    
    public float rateUse(Actor using, Target subject) {
      float chance = hackChance(using, (Actor) subject);
      return super.rateUse(using, subject) * chance;
    }
    
    public void applyFromActor(Actor using, Target subject) {
      super.applyFromActor(using, subject);
      
      if (subject == using) {
        AreaMap map = using.map();
        AreaTile at = ((Element) using.work()).centre();
        
        Actor drone = (Actor) GameContent.DRONE.generate();
        drone.enterMap(map, at.x, at.y, 1, using.base());
        drone.setInside(using.inside(), true);
        
        ActorBonds.setBond(using, drone, BOND_MASTER, BOND_SERVANT, 1);
      }
      else {
        Actor affects = (Actor) subject;
        float chance = hackChance(using, affects);
        
        if (Rand.num() < chance) {
          affects.wipeEmployment();
          affects.assignBase(using.base());
          ActorBonds.setBond(using, affects, BOND_MASTER, BOND_SERVANT, 1);
        }
        
        while (numDrones(using) > maxDrones(using)) {
          Actor weakest = weakestDrone(using);
          weakest.health.setAsKilled("Disposed of weakest link");
        }
      }
      
      dispenseXP(using, 1, SKILL_PRAY);
      
      AreaMap map = using.map();
      map.ephemera.addGhostFromModel(subject, FX_MODEL, 1, 0.5f, 1);
    }
    
    
    /*
    public boolean canRulerUse(Base ruler, Target subject) {
      if (! super.canRulerUse(ruler, subject)) return false;
      if (! subject.type().isConstruct()) return false;
      
      Actor affects = (Actor) subject;
      if (affects.base() == ruler.base()) return false;
      
      return false;
    }
    
    
    public void applyFromRuler(Base ruler, Target subject) {
      super.applyFromRuler(ruler, subject);
      
      Actor affects = (Actor) subject;
      float chance = hackChance(null, affects);
      
      if (Rand.num() < chance) {
        affects.wipeEmployment();
        affects.assignBase(ruler.base());
      }
    }
    //*/
  };
  static {
    DRONE_UPLINK.attachMedia(
      SchoolTekPriest.class, "media/GUI/Powers/power_drone_uplink.png",
      "Allows Drone minions to be controlled and/or hijacked, with a chance "+
      "to affect more powerful Artilects.",
      AnimNames.PSY_QUICK
    );
    DRONE_UPLINK.setProperties(
      TARGET_SELF | TARGET_OTHERS | SOURCE_TRAINED,
      Task.NO_HARM, MEDIUM_POWER
    );
    DRONE_UPLINK.setCosting(300, MEDIUM_AP_COST, NO_TIRING, LONG_RANGE);
    DRONE_UPLINK.setMinLevel(1);
  }
  
  
  final static Trait STASIS_FIELD_CONDITION = new Trait(
    "condition_stasis_field", "Stasis Field"
  ) {
    
    protected float passiveBonus(Trait t) {
      if (t == SKILL_MELEE) return -3;
      if (t == SKILL_SIGHT) return -3;
      if (t == SKILL_EVADE) return -3;
      if (t == SKILL_SPEAK) return -3;
      if (t == STAT_ACTION) return -0.5f;
      if (t == STAT_SPEED ) return -0.5f;
      return 0;
    }
    
    protected void passiveEffect(Actor actor) {
      AreaMap map = actor.map();
      map.ephemera.updateGhost(actor, 1, FX_MODEL, 0.5f);
    }
  };
  
  final public static ActorTechnique STASIS_FIELD = new ActorTechnique(
    "power_stasis_field", "Stasis Field"
  ) {
    
    public boolean canRulerUse(Base ruler, Target subject) {
      if (! super.canRulerUse(ruler, subject)) return false;
      return subject.type().isActor();
    }
    
    public void applyFromRuler(Base ruler, Target subject) {
      super.applyFromRuler(ruler, subject);
      Actor affects = (Actor) subject;
      affects.health.addCondition(
        null, STASIS_FIELD_CONDITION, STASIS_DURATION
      );
      
      AreaMap map = affects.map();
      map.ephemera.addGhostFromModel(subject, FX_MODEL, 1, 0.5f, 1);
    }
    
    public boolean canActorUse(Actor using, Target subject) {
      if (! super.canActorUse(using, subject)) return false;
      return subject.type().isActor();
    }
    
    public void applyFromActor(Actor using, Target subject) {
      super.applyFromActor(using, subject);

      Actor affects = (Actor) subject;
      affects.health.addCondition(
        null, STASIS_FIELD_CONDITION, STASIS_MINI_DUR
      );
      dispenseXP(using, 1, SKILL_PRAY);
      
      AreaMap map = using.map();
      map.ephemera.addGhostFromModel(subject, FX_MODEL, 1, 0.5f, 1);
    }
  };
  static {
    STASIS_FIELD.attachMedia(
      SchoolTekPriest.class, "media/GUI/Powers/power_stasis_field.png",
      "Reduces the target's combat stats and slows all action for "+
      STASIS_DURATION+" seconds.",
      AnimNames.PSY_QUICK
    );
    STASIS_FIELD.setProperties(0, Task.MILD_HARM, MEDIUM_POWER);
    STASIS_FIELD.setCosting(250, MEDIUM_AP_COST, NO_TIRING, LONG_RANGE);
  }
  
  
  final static Trait REASSEMBLY_CONDITION = new Trait(
    "condition_reassembly", "Reassembly"
  ) {
    
    protected float passiveBonus(Trait t) {
      return 0;
    }
    
    protected void passiveEffect(Actor actor) {
      AreaMap map = actor.map();
      
      float lift = actor.health.maxHealth();
      float wake = actor.health.maxHealth() / 2;
      lift *= 1f / (map.ticksPerSecond() * ASSEMBLE_DURATION);
      actor.health.liftDamage(lift);
      actor.health.incBleed(-1000);
      
      if (
        actor.health.dead() && (! actor.health.organic()) &&
        actor.health.injury() <= wake
      ) {
        actor.health.setAsAlive("Regenerated full health!");
      }
      
      map.ephemera.updateGhost(actor, 1, FX_MODEL, 0.5f);
    }
  };
  
  final public static ActorTechnique REASSEMBLY = new ActorTechnique(
    "power_reassembly", "Reassembly"
  ) {
    public boolean canRulerUse(Base ruler, Target subject) {
      if (! super.canRulerUse(ruler, subject)) return false;
      //
      //  TODO:  Allow casting on cybrid actors (such as tek priests themselves)
      //  as well.
      if (subject.type().isActor()) {
        Actor affects = (Actor) subject;
        if (! affects.health.organic()) return false;
        if (affects.health.injury() <= 0) return false;
        return true;
      }
      //  TODO:  You need the ability to apply persistent conditions to
      //  buildings for this.
      /*
      if (subject.type().isBuilding()) {
        Building affects = (Building) subject;
        if (! affects.complete()) return false;
        if (affects.buildLevel() >= 1) return false;
        return true;
      }
      //*/
      return false;
    }
    
    public void applyFromRuler(Base ruler, Target subject) {
      super.applyFromRuler(ruler, subject);

      Actor affects = (Actor) subject;
      affects.health.addCondition(
        null, REASSEMBLY_CONDITION, ASSEMBLE_DURATION
      );
    }
  };
  static {
    REASSEMBLY.attachMedia(
      SchoolTekPriest.class, "media/GUI/Powers/power_reassembly.png",
      "Regenerates the health of artilects or vehicles over "+
      ASSEMBLE_DURATION+" seconds.",
      AnimNames.PSY_QUICK
    );
    REASSEMBLY.setProperties(0, Task.FULL_HELP, MAJOR_POWER);
    REASSEMBLY.setCosting(350, MEDIUM_AP_COST, NO_TIRING, LONG_RANGE);
  }
  
  
  final public static HumanType TEK_PRIEST = new HumanType(
    "actor_tek_priest", CLASS_SOLDIER
  ) {
    public void initAsMigrant(Actor p) {
      super.initAsMigrant(p);
      final String name = generateName(TEK_PRIEST_FN, TEK_PRIEST_LN, null);
      p.setCustomName(name);
    }
  };
  static {
    TEK_PRIEST.name = "Tek Priest";
    TEK_PRIEST.attachCostume(SchoolTekPriest.class, "tek_priest_skin.gif");
    
    TEK_PRIEST.maxHealth   = 12;
    TEK_PRIEST.meleeDamage = 0;
    TEK_PRIEST.rangeDamage = 0;
    TEK_PRIEST.rangeDist   = 0;
    TEK_PRIEST.armourClass = 4;
    
    TEK_PRIEST.genderRole = SEX_FEMALE;
    
    TEK_PRIEST.coreSkills.setWith(
      SKILL_PRAY , 8 ,
      SKILL_SPEAK, 6 ,
      SKILL_WRITE, 8 ,
      SKILL_CRAFT, 7
    );
    TEK_PRIEST.initTraits.setWith(
      TRAIT_EMPATHY  , 15,
      TRAIT_DILIGENCE, 60,
      TRAIT_BRAVERY  , 65,
      TRAIT_CURIOSITY, 60
    );
    
    TEK_PRIEST.classTechniques = new ActorTechnique[] { DRONE_UPLINK, STASIS_FIELD };
  }
}







