


package content;
import game.*;
import static game.GameConstants.*;



public class Nobles {
  
  
  final static String
    //
    //  Highborn always have family/house names, depending on their planet of
    //  origin, and may have additional titles.
    HIGHBORN_MN[] = {
      "Calib", "Vladmar", "Ledo", "Cado", "Alexander", "Xerxes", "Poul",
      "Altan"
    },
    HIGHBORN_FN[] = {
      "Meina", "Mnestra", "Aria", "Belise", "Ylande", "Vana", "Portia", "Vi",
      "Lysandre"
    },
    HIGHBORN_TN[] = {
      "Prime", "Secundus", "Tertius", "Minor",
      "Alpha", "Beta", "Gamma", "Major"
    },
    //
    //  TODO:  HOUSE NAMES ***MUST*** BE CUSTOMISED BY HOMEWORLD.
    HIGHBORN_HN[] = {
      "Rigel", "Ursa", "Alyph", "Rana", "Maia", "Fomalhaut", "Aldebaran",
      "Regulus", "Suhail", "Antares", "Paleides", "Algol", "Orion",
      "Deneb", "Ares",
    };
  
  
  
  final public static HumanType NOBLE = new HumanType(
    "actor_noble", CLASS_NOBLE
  ) {
    public void initAsMigrant(ActorAsPerson p) {
      super.initAsMigrant(p);
      String forenames[] = p.health.man() ? HIGHBORN_MN : HIGHBORN_FN;
      final String name = generateName(forenames, HIGHBORN_HN, HIGHBORN_TN);
      p.setCustomName(name);
    }
  };
  static {
    NOBLE.name = "Noble";
    NOBLE.attachCostume(Nobles.class, "noble_skin.gif");
    
    NOBLE.coreSkills.setWith(
      SKILL_MELEE, 8,
      SKILL_SPEAK, 7,
      SKILL_WRITE, 4
    );
    NOBLE.initTraits.setWith(
      TRAIT_EMPATHY  , 40,
      TRAIT_DILIGENCE, 50,
      TRAIT_BRAVERY  , 70,
      TRAIT_CURIOSITY, 30
    );
  }
  
  
  final public static HumanType CONSORT = new HumanType(
    "actor_consort", CLASS_NOBLE
  ) {
    public void initAsMigrant(ActorAsPerson p) {
      super.initAsMigrant(p);
      String forenames[] = p.health.man() ? HIGHBORN_MN : HIGHBORN_FN;
      final String name = generateName(forenames, HIGHBORN_TN, null);
      p.setCustomName(name);
    }
  };
  static {
    CONSORT.name = "Consort";
    CONSORT.attachCostume(Nobles.class, "consort_skin.gif");
    
    CONSORT.coreSkills.setWith(
      SKILL_SPEAK, 8,
      SKILL_WRITE, 4,
      SKILL_EVADE, 6
    );
    CONSORT.initTraits.setWith(
      TRAIT_EMPATHY  , 50,
      TRAIT_DILIGENCE, 40,
      TRAIT_BRAVERY  , 30,
      TRAIT_CURIOSITY, 60
    );
  }
  
}











