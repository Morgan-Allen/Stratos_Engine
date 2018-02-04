

package content;
import game.*;
import graphics.common.*;
import util.*;
import static game.GameConstants.*;
import static content.GameContent.*;



public class Trooper {
  
  
  final static String
    TROOPER_FN[] = {
      "Sergeant", "Corporal", "Captain", "Private", "Private", "Private"
    },
    TROOPER_LN[] = {
      "Santo", "Scully", "Huskins", "O'Mara", "Pitt", "Williams"
    },
    TROOPER_MN[] = {
      "Lance", "Psycho", "Booya", "Ace", "Flash", "Lighter"
    }
  ;
  
  
  final public static HumanType TROOPER = new HumanType(
    "actor_trooper", CLASS_SOLDIER
  ) {
    public void prepareMedia(Sprite s, Element e) {
      super.prepareMedia(s, e);
      
      ActorAsPerson p = (ActorAsPerson) e;
      final String name = generateName(TROOPER_FN, TROOPER_LN, TROOPER_MN);
      p.setCustomName(name);
    }
  };
  static {
    TROOPER.name = "Trooper";
    TROOPER.attachCostume("trooper_skin.gif");
    TROOPER.meleeDamage = 2;
    TROOPER.rangeDamage = 5;
    TROOPER.rangeDist   = 4;
    TROOPER.armourClass = 4;
    TROOPER.maxHealth   = 6;
    TROOPER.initTraits.setWith(SKILL_MELEE, 3, SKILL_RANGE, 4, SKILL_EVADE, 1);
  }
  
}







