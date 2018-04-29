

package content;
import game.*;
import static game.GameConstants.*;
import graphics.common.*;
import util.*;



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
  
  
  final public static Good
    BLASTER     = new Good("Blaster"    , -1),
    BODY_ARMOUR = new Good("Body Armour", -1)
  ;
  static {
    BLASTER.setAsWeapon(
      8, 4, new int[] { 150, 250, 350 },
      "pistol", AnimNames.FIRE,
      Devices.LASER_FX_MODEL, Devices.LASER_BURST_MODEL
    );
    BODY_ARMOUR.setAsArmour(8, true, 150, 250, 350);
  }
  
  
  final public static HumanType TROOPER = new HumanType(
    "actor_trooper", CLASS_SOLDIER
  ) {
    public void initAsMigrant(Actor p) {
      super.initAsMigrant(p);
      final String name = generateName(TROOPER_FN, TROOPER_LN, TROOPER_MN);
      p.setCustomName(name);
    }
  };
  static {
    TROOPER.name = "Trooper";
    TROOPER.attachCostume(Trooper.class, "trooper_skin.gif");
    TROOPER.weaponType   = BLASTER;
    TROOPER.armourType   = BODY_ARMOUR;
    TROOPER.useItemTypes = StockExGoods.ALL_SOLD;
    
    //  TODO:  These should be removed.  Information from weapon/armour-types
    //  supercedes this data.
    TROOPER.meleeDamage = 4;
    TROOPER.rangeDamage = 10;
    TROOPER.rangeDist   = 4;
    TROOPER.armourClass = 8;
    
    TROOPER.maxHealth    = 20;
    TROOPER.moveSpeed    = 80;
    
    TROOPER.coreSkills.setWith(
      SKILL_SIGHT, 10,
      SKILL_PILOT, 7 ,
      SKILL_MELEE, 6 ,
      SKILL_EVADE, 4 ,
      SKILL_CRAFT, 3 ,
      SKILL_HEAL , 3
    );
    TROOPER.initTraits.setWith(
      TRAIT_EMPATHY  , 50,
      TRAIT_DILIGENCE, 85,
      TRAIT_BRAVERY  , 70,
      TRAIT_CURIOSITY, 25
    );
    //TROOPER.initTraits.setWith(SKILL_MELEE, 4, SKILL_RANGE, 6, SKILL_EVADE, 2);
  }
  
}







