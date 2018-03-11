/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package content;
import game.*;
import static game.GameConstants.*;
import graphics.common.*;
import graphics.sfx.*;


//  TODO:  Attach all these!

//Trooper-    Halberd Gun & Power Armour
//Noble-      Dirk & Body Armour
//Enforcer-   Stun Wand & Body Armour
//Kommando-   Zweihander & Stealth Suit
//Runner-     Blaster & Stealth Suit
//Ace-        Blaster & Seal Suit

//Pseer-      Psy Staff
//Palatine-   Arc Sabre & Shield Bracer
//Xenopath-   Inhibitor
//Physician-  Biocorder
//Artificer-  Maniples & Golem Frame
//Ecologist-  Stun Wand & Seal Suit

//Collective- Gestalt Psy
//Archon-     Zero Point Energy
//Jil Baru-   Pets & Microbes
//Logician-   Unarmed
//Navigator-  Psy Projection
//Tek Priest- Drone Minions



public final class Devices {
  

  final static ShotFX.Model
    LASER_FX_MODEL = new ShotFX.Model(
      "laser_beam_fx", Devices.class,
      "media/SFX/blast_beam.gif", 0.05f, 0, 0.05f, 3, true, true
    ),
    PISTOL_FX_MODEL = new ShotFX.Model(
      "pistol_shot_fx", Devices.class,
      "media/SFX/pistol_shot.gif", 0.02f, 0, 0.03f, 1.5f, true, true
    ),
    SPEAR_FX_MODEL = new ShotFX.Model(
      "spear_fx", Devices.class,
      "media/SFX/spear_throw.gif",
      0.1f, 0.2f,
      0.12f, 1.2f,
      false, false
    );
  final static PlaneFX.Model
    SLASH_FX_MODEL = PlaneFX.imageModel(
      "slash_fx", Devices.class,
      "media/SFX/melee_slash.png", 0.5f, 0, 0, false, false
    ),
    LASER_BURST_MODEL = PlaneFX.imageModel(
      "laser_burst_fx", Devices.class,
      "media/SFX/laser_burst.png", 0.75f, 0, 0, true, true
    ),
    PISTOL_BURST_MODEL = PlaneFX.imageModel(
      "pistol_burst_fx", Devices.class,
      "media/SFX/pistol_burst.png", 0.2f, 180, 0, true, true
    );
  
  
  final public static Good
    INTRINSIC_BEAM   = new Good("Intrinsic Beam"  , -1),
    INTRINSIC_ARMOUR = new Good("Intrinsic Armour", -1)
  ;
  static {
    INTRINSIC_BEAM.setAsWeapon(
      8, true, new int[0],
      null, AnimNames.FIRE,
      Devices.LASER_FX_MODEL, Devices.LASER_BURST_MODEL
    );
    INTRINSIC_ARMOUR.setAsArmour(8, true);
  }
  
  
  
  /*
  final static Class BC = Devices.class;
  
  final public static Traded AMMO_CLIPS = new Traded(
    BC, "Ammo Clips", null, FORM_MATERIAL, 4,
    "Spare ammunition for weapons."
  ) {
    public int normalCarry(Actor actor) {
      if (actor.gear.maxAmmoUnits() == 0) return 0;
      return ActorGear.MAX_AMMO_COUNT / ActorGear.AMMO_PER_UNIT;
    }
  };
  
  final public static DeviceType
    STUN_WAND = new DeviceType(
      BC, "Stun Wand",
      "pistol", AnimNames.FIRE,
      6, RANGED | ENERGY | STUN | MELEE, 35,
      EngineerStation.class, 1, PARTS, 10, ASSEMBLY
    ),
    CARBINE = new DeviceType(
      BC, "Carbine",
      "pistol", AnimNames.FIRE,
      8, RANGED | KINETIC, 30,
      EngineerStation.class, 1, PARTS, 10, ASSEMBLY
    ),
    BLASTER = new DeviceType(
      BC, "Blaster",
      "pistol", AnimNames.FIRE,
      10, RANGED | ENERGY, 25,
      EngineerStation.class, 1, PARTS, 10, ASSEMBLY
    ),
    HALBERD_GUN = new DeviceType(
      BC, "Halberd Gun",
      "pistol", AnimNames.FIRE,
      13, RANGED | MELEE | KINETIC, 40,
      EngineerStation.class, 1, PARTS, 10, ASSEMBLY
    ),
    
    HUNTING_LANCE = new DeviceType(
      BC, "Hunting Lance",
      "spear", AnimNames.STRIKE,
      10, RANGED | KINETIC | NO_AMMO, 5,
      null, 5, HANDICRAFTS
    ),
    SIDE_SABRE = new DeviceType(
      BC, "Side Sabre",
      "light blade", AnimNames.STRIKE,
      8, MELEE | KINETIC, 10,
      EngineerStation.class, 1, PARTS, 5, ASSEMBLY
    ),
    ZWEIHANDER = new DeviceType(
      BC, "Zweihander",
      "heavy blade", AnimNames.STRIKE_BIG,
      18, MELEE | KINETIC, 35,
      EngineerStation.class, 1, PARTS, 10, ASSEMBLY
    ),
    ARC_SABRE = new DeviceType(
      BC, "Arc Sabre",
      "sabre", AnimNames.STRIKE,
      24, MELEE | ENERGY, 100,
      EngineerStation.class, 3, PARTS, 15, ASSEMBLY
    ),
    
    LIMB_AND_MAW = new DeviceType(
      BC, "Limb and Maw",
      null, AnimNames.STRIKE,
      0, MELEE | KINETIC, 0,
      null
    ),
    INTRINSIC_BEAM = new DeviceType(
      BC, "Intrinsic Beam",
      null, AnimNames.FIRE,
      0, RANGED | ENERGY, 0,
      null
    ),
    
    MANIPULATOR = new DeviceType(
      BC, "Manipulator",
      "maniples", AnimNames.STRIKE,
      5, MELEE | KINETIC, 10,
      EngineerStation.class, 1, PARTS, 5, ASSEMBLY
    ),
    MODUS_LUTE = new DeviceType(
      BC, "Modus Lute",
      "modus lute", AnimNames.TALK_LONG,
      0, NONE, 40,
      EngineerStation.class, 1, PARTS, 10, ASSEMBLY
    ),
    BIOCORDER = new DeviceType(
      BC, "Biocorder",
      "biocorder", AnimNames.LOOK,
      0, NONE, 55,
      EngineerStation.class, 2, PARTS, 15, ASSEMBLY
    );
  final public static Traded
    ALL_DEVICES[] = Traded.INDEX.soFar(Traded.class);
  //*/
}




