


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
      String forenames[] = p.man() ? HIGHBORN_MN : HIGHBORN_FN;
      final String name = generateName(forenames, HIGHBORN_HN, HIGHBORN_TN);
      p.setCustomName(name);
    }
  };
  static {
    NOBLE.name = "Noble";
    NOBLE.attachCostume(Nobles.class, "noble_skin.gif");
    NOBLE.initTraits.setWith(SKILL_MELEE, 1, SKILL_SPEAK, 2, SKILL_WRITE, 2);
  }
  
  
  final public static HumanType CONSORT = new HumanType(
    "actor_consort", CLASS_NOBLE
  ) {
    public void initAsMigrant(ActorAsPerson p) {
      super.initAsMigrant(p);
      String forenames[] = p.man() ? HIGHBORN_MN : HIGHBORN_FN;
      final String name = generateName(forenames, HIGHBORN_TN, null);
      p.setCustomName(name);
    }
  };
  static {
    CONSORT.name = "Consort";
    CONSORT.attachCostume(Nobles.class, "consort_skin.gif");
    CONSORT.initTraits.setWith(SKILL_SPEAK, 2, SKILL_WRITE, 1, SKILL_EVADE, 2);
  }
  
}








