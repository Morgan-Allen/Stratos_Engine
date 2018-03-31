

package content;
import game.*;
import static game.GameConstants.*;
import static game.ActorTechnique.*;
import graphics.common.*;
import graphics.sfx.*;



public class Collective {
  
  
  final static String
    COLLECTIVE_FN[] = {
      "Une", "Bena", "Blis", "Pax", "Sela", "Nami", "Oolen", "Nioba"
    },
    COLLECTIVE_LN[] = {
      "of 9", "Primus", "003", "Iambis", "Orela", "the Zen", " of Melding"
    }
  ;
  
  
  
  final public static int
    PSY_HEAL_AMOUNT = 10
  ;
  final static PlaneFX.Model FX_MODEL = PlaneFX.imageModel(
    "heal_fx_model", Collective.class,
    "media/SFX/collective_psy.png", 0.5f, 0, 0.25f, true, true
  );
  
  
  final public static ActorTechnique PSY_HEAL = new ActorTechnique(
    "power_psy_heal", "Psy Heal"
  ) {
    
    public boolean canTarget(Target subject, boolean asRuler) {
      if (! subject.type().isActor()) return false;
      final Actor a = (Actor) subject;
      if (a.type().isVessel() || ! a.type().organic) {
        return false;
      }
      if (asRuler) {
        float hurtLevel = a.health.injury() + a.health.fatigue() + a.health.hunger();
        if (hurtLevel < 1) return false;
      }
      return true;
    }
    
    public float rateUse(Actor using, Target subject) {
      float rating = super.rateUse(using, subject);
      if (rating <= 0) return 0;
      
      final Actor a = (Actor) subject;
      float hurtLevel = a.health.injury() / a.health.maxHealth();
      rating *= hurtLevel;
      return rating;
    }
    
    public void applyCommonEffects(Target subject, Base ruler, Actor actor) {
      final Actor healed = (Actor) subject;
      Area map = healed.map();
      
      if (ruler != null) {
        healed.health.liftDamage (PSY_HEAL_AMOUNT    );
        healed.health.liftFatigue(PSY_HEAL_AMOUNT / 2);
        healed.health.liftHunger (PSY_HEAL_AMOUNT / 2);
        healed.health.incBleed(-1000);
        
        if (map.ephemera.active()) {
          map.ephemera.addGhostFromModel(healed, FX_MODEL, 1, 0.5f, 1);
        }
      }
      if (actor != null) {
        healed.health.liftDamage(PSY_HEAL_AMOUNT);
        healed.health.incBleed(-1000);
        
        if (map.ephemera.active()) {
          map.ephemera.addGhostFromModel(healed, FX_MODEL, 1, 0.5f, 1);
        }
      }
    }
  };
  static {
    PSY_HEAL.attachMedia(
      Collective.class, "media/GUI/Powers/power_psy_heal.png",
      "Heals the subject for up to "+PSY_HEAL_AMOUNT+" damage.",
      AnimNames.LOOK
    );
    PSY_HEAL.setProperties(TARGET_OTHERS | SOURCE_TRAINED, Task.FULL_HELP, MEDIUM_POWER);
    PSY_HEAL.setCosting(150, MEDIUM_AP_COST, NO_TIRING, LONG_RANGE);
    PSY_HEAL.setMinLevel(1);
  }
  
  
  
  final public static HumanType COLLECTIVE = new HumanType(
    "actor_collective", CLASS_SOLDIER
  ) {
    public void initAsMigrant(ActorAsPerson p) {
      super.initAsMigrant(p);
      final String name = generateName(COLLECTIVE_FN, COLLECTIVE_LN, null);
      p.setCustomName(name);
    }
  };
  static {
    COLLECTIVE.name = "Collective";
    COLLECTIVE.attachCostume(Collective.class, "collective_skin.gif");
    COLLECTIVE.maxHealth   = 12;
    COLLECTIVE.meleeDamage = 0;
    COLLECTIVE.rangeDamage = 0;
    COLLECTIVE.rangeDist   = 0;
    COLLECTIVE.armourClass = 0;
    COLLECTIVE.initTraits.setWith(SKILL_SPEAK, 3, SKILL_PRAY, 4, SKILL_WRITE, 1);
    COLLECTIVE.classTechniques = new ActorTechnique[] { PSY_HEAL };
  }
}







