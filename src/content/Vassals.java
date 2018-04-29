

package content;
import game.*;
import graphics.common.*;
import graphics.solids.*;
import static game.GameConstants.*;
import util.*;



public class Vassals {
  
  //
  //  TODO:  DERIVE NAMES FROM HOMEWORLD OF ORIGIN IF POSSIBLE
  final static String
    //
    //  Natives only have first names, but might use son/daughter of X as a
    //  title, or a conspicuous trait.
    NATIVE_MN[] = {
      "Duor", "Huno", "Umun", "Tunto", "Parab", "Sxumo", "Zhaka", "Hoka"
    },
    NATIVE_FN[] = {
      "Khasi", "Mari", "Tesza", "Borab", "Hana", "Kaeli", "Hlara", "Ote"
    },
    //
    //  Pyons have first and second names as standard.
    PYON_MN[] = {
      "Haber", "Danyl", "Jeme", "Marec", "Hoeb", "Ombar", "Tober", "Alav",
      "Dann", "Gereg", "Sony", "Terev", "Olvar", "Man", "Halan", "Yohn"
    },
    PYON_FN[] = {
      "Besa", "Linn", "Mina", "Abi", "Nana", "Dova", "Saba", "Kela", "Aryl",
      "Vina", "Nena", "Lanu", "Mai", "Nevi", "Mona", "Ambi", "Kayt", "Tesa",
    },
    PYON_LN[] = {
      "Foyle", "Orphy", "Samsun", "Ulga", "Yimon", "Timan", "Jo", "Yonson"
    },
    //
    //  Citizens have first and second names as standard.
    CITIZEN_MN[] = {
      "Sarles", "Mortan", "Daneel", "Trevize", "Tedrick", "Arnalt", "Bictor"
    },
    CITIZEN_FN[] = {
      "Becca", "Norema", "Catrin", "Xinia", "Max", "Sovia", "Unize", "Vonda"
    },
    CITIZEN_LN[] = {
      "Vasov", "Olvaw", "Mallo", "Palev", "Unterhaussen", "Valiz", "Ryod",
      "Obar", "Tiev", "Hanem", "Tsolo", "Matson", "Prestein", "Valter"
    }
  ;
  final static int
    TINT_VEHICLE = colour(6, 3, 3)
  ;
  
  
  final public static HumanType AUDITOR = new HumanType(
    "actor_auditor", CLASS_COMMON
  ) {
    public void initAsMigrant(Actor p) {
      super.initAsMigrant(p);
      String forenames[] = p.health.man() ? CITIZEN_MN : CITIZEN_FN;
      final String name = generateName(forenames, CITIZEN_LN, null);
      p.setCustomName(name);
    }
  };
  static {
    AUDITOR.name = "Auditor";
    AUDITOR.attachCostume(Vassals.class, "auditor_skin.gif");
    
    AUDITOR.coreSkills.setWith(
      SKILL_SPEAK, 8,
      SKILL_WRITE, 8
    );
    AUDITOR.initTraits.setWith(
      TRAIT_EMPATHY  , 30,
      TRAIT_DILIGENCE, 60,
      TRAIT_BRAVERY  , 20,
      TRAIT_CURIOSITY, 40
    );
  }
  
  
  final public static HumanType VENDOR = new HumanType(
    "actor_vendor", CLASS_COMMON
  ) {
    public void initAsMigrant(Actor p) {
      super.initAsMigrant(p);
      String forenames[] = p.health.man() ? CITIZEN_MN : CITIZEN_FN;
      final String name = generateName(forenames, CITIZEN_LN, null);
      p.setCustomName(name);
    }
  };
  static {
    VENDOR.name = "Vendor";
    VENDOR.attachCostume(Vassals.class, "vendor_skin.gif");
    
    VENDOR.coreSkills.setWith(
      SKILL_SPEAK, 6,
      SKILL_WRITE, 4
    );
    VENDOR.initTraits.setWith(
      TRAIT_EMPATHY  , 30,
      TRAIT_DILIGENCE, 60,
      TRAIT_BRAVERY  , 20,
      TRAIT_CURIOSITY, 40
    );
  }
  
  
  final public static HumanType PYON = new HumanType(
    "actor_pyon", CLASS_COMMON
  ) {
    public void initAsMigrant(Actor p) {
      super.initAsMigrant(p);
      String forenames[] = p.health.man() ? PYON_MN : PYON_FN;
      final String name = generateName(forenames, PYON_LN, null);
      p.setCustomName(name);
    }
  };
  static {
    PYON.name = "Pyon";
    PYON.attachCostume(Vassals.class, "pyon_skin.gif");
    
    PYON.coreSkills.setWith(
      SKILL_FARM , 4,
      SKILL_BUILD, 4,
      SKILL_CRAFT, 4
    );
    PYON.initTraits.setWith(
      TRAIT_EMPATHY  , 55,
      TRAIT_DILIGENCE, 65,
      TRAIT_BRAVERY  , 20,
      TRAIT_CURIOSITY, 40
    );
  }
  
  
  final public static HumanType SUPPLY_CORPS = new HumanType(
    "actor_supply_corps", CLASS_COMMON
  ) {
    public void initAsMigrant(Actor p) {
      super.initAsMigrant(p);
      String forenames[] = p.health.man() ? PYON_MN : PYON_FN;
      final String name = generateName(forenames, PYON_LN, null);
      p.setCustomName(name);
    }
  };
  static {
    SUPPLY_CORPS.name = "Supply Corps";
    SUPPLY_CORPS.attachCostume(Vassals.class, "pyon_skin.gif");
    
    SUPPLY_CORPS.coreSkills.setWith(
      SKILL_CRAFT, 2,
      SKILL_PILOT, 6
    );
    SUPPLY_CORPS.initTraits.setWith(
      TRAIT_EMPATHY  , 55,
      TRAIT_DILIGENCE, 65,
      TRAIT_BRAVERY  , 20,
      TRAIT_CURIOSITY, 40
    );
  }
  
  
  final public static ActorType CARGO_BARGE = new ActorType(
    ActorAsVessel.class, "vessel_cargo_barge", Type.IS_VESSEL_ACT
  );
  static {
    CARGO_BARGE.name = "Cargo Barge";
    CARGO_BARGE.model = MS3DModel.loadFrom(
      "media/Actors/vehicles/", "loader_2.ms3d",
      Vassals.class, "VehicleModels.xml", "CargoBarge"
    );
    CARGO_BARGE.tint = TINT_VEHICLE;
    
    CARGO_BARGE.organic = false;
    CARGO_BARGE.maxHealth = 35;
    CARGO_BARGE.carryLimit = 40;
  }
  
  
  final public static ActorType DROPSHIP = new ActorType(
    ActorAsVessel.class, "vessel_dropship", Type.IS_VESSEL_ACT
  );
  static {
    DROPSHIP.name = "Dropship";
    DROPSHIP.model = MS3DModel.loadFrom(
      "media/Actors/vehicles/", "dropship.ms3d",
      Vassals.class, "VehicleModels.xml", "Dropship"
    );
    DROPSHIP.tint = TINT_VEHICLE;
    
    DROPSHIP.organic    = false;
    DROPSHIP.moveMode   = Type.MOVE_AIR;
    DROPSHIP.maxHealth  = 150;
    DROPSHIP.carryLimit = 120;
    DROPSHIP.setDimensions(4, 4, 2, Type.WIDE_MARGIN);
    DROPSHIP.crewTypes.setWith(SUPPLY_CORPS, 2);
  }
  
  
}




