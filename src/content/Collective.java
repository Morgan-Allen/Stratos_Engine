

package content;
import game.*;
import graphics.common.*;
import static game.GameConstants.*;
import static game.ActorTechnique.*;



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
  
  
  final public static ActorTechnique PSY_HEAL = new ActorTechnique(
    "power_psy_heal", "Psy Heal"
  ) {
    
    public boolean canTarget(Target subject) {
      return subject.type().isActor();
    }
    
    public void applyCommonEffects(Target subject, Base ruler, Actor actor) {
      final Actor healed = (Actor) subject;
      healed.liftDamage(PSY_HEAL_AMOUNT);
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
    COLLECTIVE.meleeDamage = 0;
    COLLECTIVE.rangeDamage = 0;
    COLLECTIVE.rangeDist   = 0;
    COLLECTIVE.armourClass = 0;
    COLLECTIVE.maxHealth   = 3;
    COLLECTIVE.initTraits.setWith(SKILL_SPEAK, 3, SKILL_PRAY, 4, SKILL_WRITE, 1);
    COLLECTIVE.classTechniques = new ActorTechnique[] { PSY_HEAL };
  }
}







