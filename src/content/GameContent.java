

package content;
import game.*;
import graphics.common.*;
import graphics.cutout.*;
import graphics.solids.*;
import util.*;
import static game.AreaMap.*;
import static game.GameConstants.*;
import static game.Type.*;



public class GameContent {
  
  
  final static int
    
    //  Industrial in brown.
    TINT_LITE_INDUSTRIAL  = colour(6, 3, 3),
    TINT_INDUSTRIAL       = colour(5, 2, 2),
    //  Military in red.
    TINT_LITE_MILITARY    = colour(8, 1, 1),
    TINT_MILITARY         = colour(7, 0, 0),
    //  Economic in blue.
    TINT_LITE_COMMERCIAL  = colour(3, 1, 8),
    TINT_COMMERCIAL       = colour(2, 0, 7),
    //  Residential in yellow.
    TINT_LITE_RESIDENTIAL = colour(8, 8, 1),
    TINT_RESIDENTIAL      = colour(7, 7, 0),
    //  Entertainment in purple.
    TINT_LITE_AMENITY     = colour(8, 1, 8),
    TINT_AMENITY          = colour(7, 0, 7),
    //  Health/education in white.
    TINT_LITE_HEALTH_ED   = colour(8, 8, 8),
    TINT_HEALTH_ED        = colour(7, 7, 7),
    //  Religious in orange.
    TINT_LITE_RELIGIOUS   = colour(8, 4, 1),
    TINT_RELIGIOUS        = colour(7, 3, 0),
    
    //  Crops in shades of green:
    TINT_CROPS[] = {
      colour(1, 8, 3),
      colour(0, 7, 2),
      colour(2, 8, 0),
    }
  ;
  
  
  final public static Type
    FLAG_STRIKE = new Type(null, "flag_strike", IS_MEDIA),
    FLAG_RECON  = new Type(null, "flag_recon" , IS_MEDIA)
  ;
  static {
    FLAG_STRIKE.model = CutoutModel.fromImage(
      GameContent.class, "flag_strike_model",
      "media/GUI/Missions/flag_strike.gif", 0.5f, 1
    );
    FLAG_RECON.model = CutoutModel.fromImage(
      GameContent.class, "flag_recon_model",
      "media/GUI/Missions/flag_recon.gif", 0.5f, 1
    );
  }
  
  
  final public static Good
    
    CARBS      = new Good("Carbs"       , 10 ),
    GREENS     = new Good("Greens"      , 12 ),
    PROTEIN    = new Good("Protein"     , 35 ),
    
    CARBONS    = new Good("Carbons"     , 10 ),
    ORES       = new Good("Ores"        , 10 ),
    SPYCE      = new Good("Spyce"       , 50 ),
    
    PLASTICS   = new Good("Plastics"    , 30 ),
    PARTS      = new Good("Parts"       , 50 ),
    SOMA       = new Good("Soma"        , 45 ),
    MEDICINE   = new Good("Medicine"    , 75 ),
    
    CROP_TYPES  [] = { CARBS, GREENS, CARBONS },
    FOOD_TYPES  [] = { CARBS, GREENS, PROTEIN },
    STONE_TYPES [] = { CARBONS , ORES  },
    BUILD_GOODS [] = { PLASTICS, PARTS },
    HOME_GOODS  [] = { PLASTICS, PARTS, MEDICINE },
    MARKET_GOODS[] = (Good[]) Visit.compose(Good.class, FOOD_TYPES, HOME_GOODS),
    
    ALL_GOODS[] = new Good[] {
      CASH,
      CARBS, GREENS, PROTEIN, CARBONS, ORES, SPYCE,
      PLASTICS, PARTS, SOMA, MEDICINE
    };
  
  static {
    
    int i = 0;
    for (Good c : CROP_TYPES) {
      c.tint = TINT_CROPS[i++ % 3];
      c.growRate    = 1f;
      c.isCrop      = true;
      c.flagKey     = IS_CROP;
      c.yields      = c;
      c.yieldAmount = CROP_YIELD / 100f;
    }

    CutoutModel CROP_MODELS[][] = CutoutModel.fromImageGrid(
      GameContent.class, "crop_models",
      "media/Buildings/res_all_crops.png",
      4, 4, 1, 1, false
    );
    CARBS .modelVariants = CROP_MODELS[1];
    GREENS.modelVariants = CROP_MODELS[2];
  }
  
  
  final public static Terrain
    MEADOW = new Terrain("Meadow", "terr_meadow", 5),
    JUNGLE = new Terrain("Jungle", "terr_jungle", 4),
    DESERT = new Terrain("Desert", "terr_desert", 3),
    LAKE   = new Terrain("Lake"  , "terr_lake"  , 2),
    OCEAN  = new Terrain("Ocean" , "terr_ocean" , 1),
    ALL_TERRAINS[] = {
      EMPTY, MEADOW, JUNGLE, DESERT, LAKE, OCEAN
    }
  ;
  final public static Type
    JUNGLE_TREE1   = new Type(Element.class, "fixture_j_tree1", IS_FIXTURE),
    DESERT_ROCK1   = new Type(Element.class, "fixture_d_rock1", IS_FIXTURE),
    DESERT_ROCK2   = new Type(Element.class, "fixture_d_rock2", IS_FIXTURE),
    CARBON_DEPOSIT = new Type(Element.class, "fixture_b_clay1", IS_FIXTURE),
    
    ALL_TREES[] = { JUNGLE_TREE1 },
    ALL_ROCKS[] = { DESERT_ROCK1, DESERT_ROCK2 },
    ALL_OILS [] = { CARBON_DEPOSIT }
  ;
  final public static ActorType
    QUDU     = new ActorType(ActorAsAnimal.class, "animal_qudu"     , IS_ANIMAL_ACT),
    VAREEN   = new ActorType(ActorAsAnimal.class, "animal_vareen"   , IS_ANIMAL_ACT),
    MICOVORE = new ActorType(ActorAsAnimal.class, "animal_micovore" , IS_ANIMAL_ACT),
    ALL_ANIMALS[] = { QUDU, VAREEN, MICOVORE },
    
    DRONE    = new ActorType(ActorAsAnimal.class, "artilect_drone"  , IS_ANIMAL_ACT),
    TRIPOD   = new ActorType(ActorAsAnimal.class, "artilect_tripod" , IS_ANIMAL_ACT),
    CRANIAL  = new ActorType(ActorAsAnimal.class, "artilect_cranial", IS_ANIMAL_ACT),
    ALL_ARTILECTS[] = { DRONE, TRIPOD, CRANIAL }
  ;
  static {
    
    JUNGLE.attachGroundTex(
      GameContent.class, "media/Terrain/",
      "meadows_ground.gif"
    );
    JUNGLE.attachFixtures(JUNGLE_TREE1, 0.25f);
    
    MEADOW.attachGroundTex(
      GameContent.class, "media/Terrain/",
      "barrens_ground.gif"
    );
    MEADOW.attachFixtures(JUNGLE_TREE1, 0.05f);

    DESERT.attachGroundTex(
      GameContent.class, "media/Terrain/",
      "desert_ground.gif"
    );
    DESERT.attachFixtures(DESERT_ROCK1, 0.15f, DESERT_ROCK2, 0.20f);
    
    
    JUNGLE_TREE1  .tint = colour(0, 3, 0);
    DESERT_ROCK1  .tint = colour(6, 4, 4);
    DESERT_ROCK2  .tint = colour(6, 4, 4);
    CARBON_DEPOSIT.tint = colour(5, 3, 3);
    JUNGLE        .tint = colour(1, 4, 1);
    DESERT        .tint = colour(5, 4, 3);
    MEADOW        .tint = colour(2, 5, 2);
    LAKE          .tint = colour(0, 0, 3);
    
    ModelAsset JUNGLE_TREE_MODELS[][] = CutoutModel.fromImageGrid(
      GameContent.class, "model_jungle_tree",
      "media/Terrain/basic_flora.png", 4, 4,
      1.33f, 1, false
    );
    JUNGLE_TREE1.modelVariants = (ModelAsset[]) Visit.compose(
      ModelAsset.class, (Object[][]) JUNGLE_TREE_MODELS
    );
    
    ModelAsset DESERT_ROCK_MODELS[][] = CutoutModel.fromImageGrid(
      GameContent.class, "model_desert_rock",
      "media/Terrain/all_outcrops.png", 3, 3,
      2, 2, false
    );
    DESERT_ROCK1.modelVariants = DESERT_ROCK_MODELS[2];
    DESERT_ROCK2.modelVariants = DESERT_ROCK_MODELS[0];
    CARBON_DEPOSIT.modelVariants = DESERT_ROCK_MODELS[1];
    
    JUNGLE_TREE1.growRate = 0.5f;
    DESERT_ROCK1  .setDimensions(2, 2, 1);
    CARBON_DEPOSIT.setDimensions(2, 2, 0);
    LAKE.pathing = PATH_BLOCK;
    LAKE.isWater = true;
    
    
    for (Type t : ALL_TREES) {
      t.name = "Forest";
      t.growRate    = CROP_YIELD * 0.5f / 100f;
      t.flagKey     = IS_TREE;
      t.yields      = CARBONS;
      t.yieldAmount = 1f;
    }
    for (Type r : ALL_ROCKS) {
      r.name = "Ore Deposit";
      r.flagKey     = IS_STONE;
      r.yields      = ORES;
      r.yieldAmount = 1f;
    }
    for (Type c : ALL_OILS) {
      c.name = "Oil Deposit";
      c.flagKey     = IS_STONE;
      c.yields      = CARBONS;
      c.yieldAmount = 1f;
    }
    
    
    final String ANIMALS_DIR = "media/Actors/fauna/";
    final String ANIMALS_XML = "FaunaModels.xml";
    
    QUDU.name  = "Qudu";
    QUDU.model = MS3DModel.loadFrom(
      ANIMALS_DIR, "Qudu.ms3d",
      GameContent.class, ANIMALS_XML, "Qudu"
    );
    
    QUDU.habitats    = new Terrain[] { JUNGLE, MEADOW };
    QUDU.predator    = false;
    QUDU.meleeDamage = 1;
    QUDU.armourClass = 2;
    QUDU.maxHealth   = 8;
    
    
    VAREEN.name  = "Vareen";
    VAREEN.model = MS3DModel.loadFrom(
      ANIMALS_DIR, "Vareen.ms3d",
      GameContent.class, ANIMALS_XML, "Vareen"
    );
    
    VAREEN.habitats    = new Terrain[] { MEADOW, DESERT };
    VAREEN.predator    = false;
    VAREEN.meleeDamage = 0;
    VAREEN.armourClass = 0;
    VAREEN.maxHealth   = 2;
    
    
    MICOVORE.name  = "Micovore";
    MICOVORE.model = MS3DModel.loadFrom(
      ANIMALS_DIR, "Micovore.ms3d",
      GameContent.class, ANIMALS_XML, "Micovore"
    );
    
    MICOVORE.habitats    = new Terrain[] { JUNGLE };
    MICOVORE.predator    = true;
    MICOVORE.meleeDamage = 5;
    MICOVORE.armourClass = 1;
    MICOVORE.maxHealth   = 6;
    
    
    for (ActorType s : ALL_ANIMALS) {
      s.rangeDamage = -1;
      s.lifespan = s.predator ? HUNTER_LIFESPAN : GRAZER_LIFESPAN;
      s.meatType = PROTEIN;
    }
    
    
    final String ARTILECTS_DIR = "media/Actors/artilects/";
    final String ARTILECTS_XML = "ArtilectModels.xml";
    
    DRONE.name  = "Drone";
    DRONE.model = MS3DModel.loadFrom(
      ARTILECTS_DIR, "DefenceDrone.ms3d",
      GameContent.class, ARTILECTS_XML, "Defence Drone"
    );
    
    DRONE.predator    = false;
    DRONE.meleeDamage = 0;
    DRONE.rangeDamage = 2;
    DRONE.armourClass = 4;
    DRONE.maxHealth   = 8;
    
    
    TRIPOD.name  = "Tripod";
    TRIPOD.model = MS3DModel.loadFrom(
      ARTILECTS_DIR, "Tripod.ms3d",
      GameContent.class, ARTILECTS_XML, "Tripod"
    );
    
    TRIPOD.predator    = false;
    TRIPOD.deep        = 2;
    TRIPOD.meleeDamage = 4;
    TRIPOD.rangeDamage = 6;
    TRIPOD.armourClass = 6;
    TRIPOD.maxHealth   = 15;
    
    
    CRANIAL.name  = "Cranial";
    CRANIAL.model = MS3DModel.loadFrom(
      ARTILECTS_DIR, "Cranial.ms3d",
      GameContent.class, ARTILECTS_XML, "Cranial"
    );
    
    CRANIAL.predator    = false;
    CRANIAL.meleeDamage = 6;
    CRANIAL.armourClass = 8;
    CRANIAL.maxHealth   = 12;
    
    
    for (ActorType t : ALL_ARTILECTS) {
      t.organic = false;
    }
  }
  
  
  
  final public static HumanType
    CHILD     = new HumanType("actor_child"    , CLASS_COMMON ),
    
    ENFORCER  = new HumanType("actor_enforcer" , CLASS_SOLDIER),
    RUNNER    = new HumanType("actor_runner"   , CLASS_SOLDIER),
    
    ECOLOGIST = new HumanType("actor_ecologist", CLASS_SOLDIER),
    ENGINEER  = new HumanType("actor_engineer" , CLASS_SOLDIER),
    PHYSICIAN = new HumanType("actor_physician", CLASS_SOLDIER)
  ;
  
  public static Type[] ALL_CITIZENS() {
    return new Type[]{ Vassals.PYON, Vassals.VENDOR, Vassals.AUDITOR };
  }
  
  public static Type[] ALL_SOLDIERS() {
    return new Type[]{ Trooper.TROOPER, RUNNER, Nobles.NOBLE };
  }
  
  public static Type[] ALL_NOBLES() {
    return new Type[]{ Nobles.NOBLE, Nobles.CONSORT };
  }
  
  
  static {
    CHILD.name = "Child";
    CHILD.attachCostume(GameContent.class, "child_skin.gif");

    CHILD.meleeDamage = 0;
    CHILD.rangeDamage = 0;
    CHILD.rangeDist   = 0;
    CHILD.armourClass = 0;
    CHILD.maxHealth   = 2;
    CHILD.initTraits.setWith();
    
    ENFORCER.name = "Enforcer";
    ENFORCER.attachCostume(GameContent.class, "enforcer_skin.gif");

    ENFORCER.meleeDamage = 2;
    ENFORCER.rangeDamage = 4;
    ENFORCER.rangeDist   = 6;
    ENFORCER.armourClass = 3;
    ENFORCER.maxHealth   = 4;
    ENFORCER.initTraits.setWith(SKILL_MELEE, 2, SKILL_RANGE, 5, SKILL_EVADE, 3);
    
    RUNNER.name = "Runner";
    RUNNER.attachCostume(GameContent.class, "runner_skin.gif");

    RUNNER.rangeDamage = 6;
    RUNNER.rangeDist   = 8;
    RUNNER.armourClass = 3;
    RUNNER.maxHealth   = 3;
    RUNNER.initTraits.setWith(SKILL_RANGE, 5, SKILL_EVADE, 4);
    
    ECOLOGIST.name = "Ecologist";
    ECOLOGIST.attachCostume(GameContent.class, "ecologist_skin.gif");

    ECOLOGIST.rangeDamage = 4;
    ECOLOGIST.armourClass = 3;
    ECOLOGIST.rangeDist   = 6;
    ECOLOGIST.maxHealth   = 4;
    ECOLOGIST.initTraits.setWith(SKILL_RANGE, 5, SKILL_EVADE, 3, SKILL_FARM, 4);
    
    ENGINEER.name = "Engineer";
    ENGINEER.attachCostume(GameContent.class, "engineer_skin.gif");
    
    ENGINEER.meleeDamage = 5;
    ENGINEER.armourClass = 5;
    ENGINEER.maxHealth   = 4;
    ENGINEER.initTraits.setWith(SKILL_MELEE, 3, SKILL_CRAFT, 5, SKILL_BUILD, 5);
    
    PHYSICIAN.name = "Physician";
    PHYSICIAN.attachCostume(GameContent.class, "physician_skin.gif");
    
    PHYSICIAN.meleeDamage = 0;
    PHYSICIAN.rangeDamage = 0;
    PHYSICIAN.armourClass = 1;
    PHYSICIAN.maxHealth   = 3;
    PHYSICIAN.initTraits.setWith(SKILL_CRAFT, 6, SKILL_WRITE, 4, SKILL_SPEAK, 3);
  }
  
  
  final public static BuildType
    
    BASTION           = new BuildType(BuildingForGovern.class , "venue_bastion"  , IS_GOVERN_BLD ),
    BASTION_L2        = new BuildType(BuildingForGovern.class , "venue_bastion_2", IS_UPGRADE    ),
    BASTION_L3        = new BuildType(BuildingForGovern.class , "venue_bastion_3", IS_UPGRADE    ),
    ENFORCER_BLOC     = new BuildType(BuildingForGovern.class , "venue_enforcer" , IS_GOVERN_BLD ),
    
    //  TODO:  Make the enforcer bloc a turret-building?
    //  TODO:  Attach placement-mechanics for walls.
    SHIELD_WALL       = new BuildType(Element.class           , "type_shield_wl" , IS_STRUCTURAL ),
    BLAST_DOOR        = new BuildType(BuildingForWalls.class  , "type_blast_door", IS_WALLS_BLD  ),
    TURRET            = new BuildType(BuildingForWalls.class  , "type_turret"    , IS_WALLS_BLD  ),
    
    TROOPER_LODGE     = new BuildType(BuildingForArmy.class   , "venue_trooper"  , IS_ARMY_BLD   ),
    //  TODO:  Add the Runner Market for assassinations and contraband.
    //  TODO:  Add the Kommando Redoubt for hunting and scouting.
    //KOMMANDO_REDOUBT
    MILITARY_BUILDINGS[] =
    {
      BASTION, ENFORCER_BLOC, TROOPER_LODGE,  SHIELD_WALL, BLAST_DOOR, TURRET
    },
    
    ECOLOGIST_STATION = new BuildType(BuildingForCrafts.class , "venue_ecologist", IS_CRAFTS_BLD ),
    ENGINEER_STATION  = new BuildType(BuildingForCrafts.class , "venue_engineer" , IS_CRAFTS_BLD ),
    PHYSICIAN_STATION = new BuildType(BuildingForCrafts.class , "venue_physician", IS_CRAFTS_BLD ),
    
    NURSERY           = new BuildType(BuildingForGather.class , "type_nursery"   , IS_GATHER_BLD ),
    HARVESTER         = new BuildType(BuildingForGather.class , "type_harvester" , IS_GATHER_BLD ),
    EXCAVATOR         = new BuildType(BuildingForGather.class , "type_excavator" , IS_GATHER_BLD ),
    //  TODO:  These aren't needed for now.  Add later.
    //SOLAR_MAST   = new BuildType(BuildingForGather.class , "type_solar_tower"  , IS_GATHER_BLD ),
    //REACTOR
    //CULTURE_VATS
    GUILD_BUILDINGS[] =
    {
      ECOLOGIST_STATION, ENGINEER_STATION, PHYSICIAN_STATION,
      NURSERY, HARVESTER, EXCAVATOR
    },
    
    CANTINA           = new BuildType(BuildingForAmenity.class, "venue_cantina"  , IS_AMENITY_BLD),
    STOCK_EXCHANGE    = new BuildType(BuildingForCrafts.class , "venue_stock_ex" , IS_CRAFTS_BLD ),
    SUPPLY_DEPOT      = new BuildType(BuildingForTrade.class  , "venue_supply_d" , IS_TRADE_BLD  ),
    //RUNNER_MARKET
    
    WALKWAY           = new BuildType(Element.class           , "type_walkway"   , IS_STRUCTURAL ),
    //SERVICE_HATCH
    HOLDING           = new BuildType(BuildingForHome.class   , "type_holding"   , IS_HOME_BLD   ),
    HOUSE_T1          = new BuildType(BuildingForHome.class   , "type_house_t1"  , IS_UPGRADE    ),
    HOUSE_T2          = new BuildType(BuildingForHome.class   , "type_house_t2"  , IS_UPGRADE    ),
    //HOUSE_T3
    COMMERCE_BUILDINGS[] =
    {
      CANTINA, STOCK_EXCHANGE, SUPPLY_DEPOT,  // RUNNER_MARKET,
      WALKWAY, HOLDING,  //SERVICE_HATCH
    },
    
    SCHOOL_LOG        = new BuildType(BuildingForFaith.class , "venue_logician"  , IS_FAITH_BLD  ),
    SCHOOL_COL        = new BuildType(BuildingForFaith.class , "venue_collective", IS_FAITH_BLD  ),
    SCHOOL_LEN        = new BuildType(BuildingForFaith.class , "venue_lensr"     , IS_FAITH_BLD  ),
    SCHOOL_SHA        = new BuildType(BuildingForFaith.class , "venue_shaper"    , IS_FAITH_BLD  ),
    SCHOOL_TEK        = new BuildType(BuildingForFaith.class , "venue_tek_priest", IS_FAITH_BLD  ),
    SCHOOL_SPA        = new BuildType(BuildingForFaith.class , "venue_spacer"    , IS_FAITH_BLD  ),
    PSI_SCHOOL_BUILDINGS[] =
    {
      SCHOOL_LOG, SCHOOL_COL, SCHOOL_LEN, SCHOOL_SHA, SCHOOL_TEK, SCHOOL_SPA
    },
    
    ALL_BUILDINGS[] = (BuildType[]) Visit.compose(BuildType.class,
      MILITARY_BUILDINGS, GUILD_BUILDINGS, COMMERCE_BUILDINGS,
      PSI_SCHOOL_BUILDINGS
    ),
    RULER_BUILT[] = {
      BASTION, TROOPER_LODGE, SCHOOL_COL,
      ECOLOGIST_STATION, PHYSICIAN_STATION, ENGINEER_STATION,
      CANTINA, SUPPLY_DEPOT, STOCK_EXCHANGE,
      NURSERY, HARVESTER, EXCAVATOR,
    }
  ;
  static {
    
    BASTION.name = "Bastion";
    BASTION.tint = TINT_MILITARY;
    BASTION.model = CutoutModel.fromImage(
      GameContent.class, "bastion_model",
      "media/Buildings/mil_bastion.png", 6, 3
    );
    
    BASTION.setDimensions(6, 6, 3);
    BASTION.maxHealth = 300;
    BASTION.setBuildMaterials(PLASTICS, 10, PARTS, 25);
    BASTION.workerTypes.setWith(Nobles.NOBLE, 1, Vassals.AUDITOR, 1, Vassals.PYON, 2);
    BASTION.homeSocialClass = CLASS_NOBLE;
    BASTION.maxResidents = 2;
    BASTION.buildsWith   = new Good[] { PLASTICS, PARTS };
    BASTION.needed       = BASTION.buildsWith;
    BASTION.maxStock     = 5;
    BASTION.setFeatures(IS_HOUSING, IS_REFUGE);
    BASTION.worksBeforeBuilt = true;
    BASTION.uniqueBuilding = true;
    
    BASTION_L2.name = "Bastion Level 2";
    BASTION_L2.setBuildMaterials(PLASTICS, 5, PARTS, 15);
    
    BASTION_L3.name = "Bastion Level 3";
    BASTION_L3.setBuildMaterials(PLASTICS, 5, PARTS, 15);
    
    BASTION.setUpgradeTiers(BASTION, BASTION_L2, BASTION_L3);
    BASTION.setAllUpgrades(BASTION.upgradeTiers);
    
    
    TROOPER_LODGE.name = "Trooper Lodge";
    TROOPER_LODGE.tint = TINT_MILITARY;
    TROOPER_LODGE.model = CutoutModel.fromImage(
      GameContent.class, "trooper_lodge_model",
      "media/Buildings/mil_trooper_lodge.png", 3, 2
    );
    TROOPER_LODGE.setDimensions(3, 3, 2);
    TROOPER_LODGE.setBuildMaterials(PLASTICS, 1, PARTS, 7);
    TROOPER_LODGE.workerTypes.setWith(Trooper.TROOPER, 2);
    TROOPER_LODGE.maxHealth = 250;
    
    ENFORCER_BLOC.name = "Enforcer Bloc";
    ENFORCER_BLOC.tint = TINT_MILITARY;
    ENFORCER_BLOC.model = CutoutModel.fromImage(
      GameContent.class, "enforcer_bloc_model",
      "media/Buildings/mil_enforcer_bloc.png", 3, 2
    );
    ENFORCER_BLOC.setDimensions(2, 2, 1);
    ENFORCER_BLOC.setBuildMaterials(PARTS, 4);
    ENFORCER_BLOC.workerTypes.setWith(ENFORCER, 2);
    ENFORCER_BLOC.produced = new Good[] { CASH };
    ENFORCER_BLOC.features = new Good[] { IS_ADMIN };
    
    //
    //  Science structures:
    ECOLOGIST_STATION.name = "Ecologist Station";
    ECOLOGIST_STATION.tint = TINT_INDUSTRIAL;
    ECOLOGIST_STATION.model = CutoutModel.fromImage(
      GameContent.class, "ecologist_station_model",
      "media/Buildings/station_ecologist.png", 3, 2
    );
    ECOLOGIST_STATION.setDimensions(3, 3, 2);
    ECOLOGIST_STATION.setBuildMaterials(PLASTICS, 7, PARTS, 1);
    ECOLOGIST_STATION.workerTypes.setWith(ECOLOGIST, 2);
    ECOLOGIST_STATION.worksBeforeBuilt = true;
    ECOLOGIST_STATION.maxHealth  = 100;
    ECOLOGIST_STATION.produced   = new Good[] { PROTEIN };
    
    ENGINEER_STATION.name = "Engineer Station";
    ENGINEER_STATION.tint = TINT_INDUSTRIAL;
    ENGINEER_STATION.model = CutoutModel.fromImage(
      GameContent.class, "engineer_station_model",
      "media/Buildings/station_engineer.png", 3, 2
    );
    ENGINEER_STATION.setDimensions(3, 3, 2);
    ENGINEER_STATION.setBuildMaterials(PARTS, 8);
    ENGINEER_STATION.workerTypes.setWith(ENGINEER, 2);
    ENGINEER_STATION.needed   = new Good[] { CARBONS, ORES };
    ENGINEER_STATION.produced = new Good[] { PLASTICS, PARTS };
    ENGINEER_STATION.maxStock = 3;
    ENGINEER_STATION.recipes = new Recipe[] {
      new Recipe(PLASTICS           , SKILL_CRAFT, AVG_CRAFT_TIME, CARBONS),
      new Recipe(PARTS              , SKILL_CRAFT, AVG_CRAFT_TIME, ORES   ),
      new Recipe(Trooper.BLASTER    , SKILL_CRAFT, AVG_CRAFT_TIME, ORES   ),
      new Recipe(Trooper.BODY_ARMOUR, SKILL_CRAFT, AVG_CRAFT_TIME, ORES   ),
    };
    ENGINEER_STATION.buildsWith = new Good[] { PLASTICS, PARTS };
    
    PHYSICIAN_STATION.name = "Physician Station";
    PHYSICIAN_STATION.tint = TINT_INDUSTRIAL;
    PHYSICIAN_STATION.model = CutoutModel.fromImage(
      GameContent.class, "physician_station_model",
      "media/Buildings/station_physician.png", 3, 2
    );
    PHYSICIAN_STATION.setDimensions(3, 3, 2);
    PHYSICIAN_STATION.setBuildMaterials(PLASTICS, 4, PARTS, 2);
    PHYSICIAN_STATION.workerTypes.setWith(PHYSICIAN, 2);
    PHYSICIAN_STATION.needed   = new Good[] { GREENS };
    PHYSICIAN_STATION.produced = new Good[] { MEDICINE };
    PHYSICIAN_STATION.maxStock = 3;
    PHYSICIAN_STATION.recipes = new Recipe[] {
      new Recipe(MEDICINE, SKILL_CRAFT, AVG_CRAFT_TIME, GREENS)
    };
    PHYSICIAN_STATION.setFeatures(HEALTHCARE);
    PHYSICIAN_STATION.featureAmount = 20;
    
    //
    //  Commercial structures:
    STOCK_EXCHANGE.name = "Stock Exchange";
    STOCK_EXCHANGE.tint = TINT_COMMERCIAL;
    STOCK_EXCHANGE.model = CutoutModel.fromImage(
      GameContent.class, "stock_exchange_model",
      "media/Buildings/com_stock_exchange.png", 3, 1
    );
    STOCK_EXCHANGE.setDimensions(3, 3, 1);
    STOCK_EXCHANGE.setBuildMaterials(PLASTICS, 4, PARTS, 2);
    STOCK_EXCHANGE.workerTypes.setWith(Vassals.VENDOR, 1);
    STOCK_EXCHANGE.needed    = MARKET_GOODS;
    STOCK_EXCHANGE.features  = new Good[] { IS_VENDOR };
    STOCK_EXCHANGE.shopItems = new Good[] {
      StockExGoods.MEDIKIT//, StockExGoods.SHIELD_BAND, StockExGoods.COMM_RELAY
    };
    
    SUPPLY_DEPOT.name = "Supply Depot";
    SUPPLY_DEPOT.tint = TINT_COMMERCIAL;
    SUPPLY_DEPOT.model = CutoutModel.fromImage(
      GameContent.class, "supply_depot_model",
      "media/Buildings/com_supply_depot.png", 3, 1
    );
    SUPPLY_DEPOT.setDimensions(3, 3, 1);
    SUPPLY_DEPOT.setBuildMaterials(PLASTICS, 4, PARTS, 2);
    SUPPLY_DEPOT.workerTypes.setWith(Vassals.PYON, 2);
    SUPPLY_DEPOT.worksBeforeBuilt = true;
    SUPPLY_DEPOT.features = new Good[] { IS_TRADER };
    SUPPLY_DEPOT.buildsWith = new Good[] { PLASTICS, PARTS };
    
    //
    //  Religious structures:
    
    SCHOOL_LOG.name = "Logician School";
    SCHOOL_LOG.model = CutoutModel.fromImage(
      GameContent.class, "logician_school_model",
      "media/Buildings/school_logician.png", 4, 2
    );
    
    SCHOOL_COL.name = "Collective School";
    SCHOOL_COL.model = CutoutModel.fromImage(
      GameContent.class, "collective_school_model",
      "media/Buildings/school_collective.png", 4, 2
    );
    
    SCHOOL_COL.workerTypes.setWith(Collective.COLLECTIVE, 2);
    SCHOOL_COL.rulerPowers = new Technique[] { Collective.PSY_HEAL };
    
    SCHOOL_LEN.name = "LENSR School";
    SCHOOL_LEN.model = CutoutModel.fromImage(
      GameContent.class, "lensr_school_model",
      "media/Buildings/school_LENSR.png", 4, 2
    );
    
    SCHOOL_SHA.name = "Shaper School";
    SCHOOL_SHA.model = CutoutModel.fromImage(
      GameContent.class, "shaper_school_model",
      "media/Buildings/school_shaper.png", 4, 2
    );
    
    SCHOOL_TEK.name = "Tek Priest School";
    SCHOOL_TEK.model = CutoutModel.fromImage(
      GameContent.class, "tek_priest_school_model",
      "media/Buildings/school_tek_priest.png", 4, 2
    );
    
    SCHOOL_SPA.name = "Spacer School";
    SCHOOL_SPA.model = CutoutModel.fromImage(
      GameContent.class, "spacer_school_model",
      "media/Buildings/school_spacer.png", 4, 2
    );
    
    for (BuildType t : PSI_SCHOOL_BUILDINGS) {
      t.tint = TINT_RELIGIOUS;
      t.setDimensions(4, 4, 2);
      t.setBuildMaterials(PARTS, 15);
      t.maxHealth       = 150;
      t.homeSocialClass = CLASS_NOBLE;
      t.features        = new Good[] { RELIGION };
    }
    
    
    WALKWAY.name = "Walkway";
    WALKWAY.tint = PAVE_COLOR;
    WALKWAY.pathing = PATH_PAVE;
    WALKWAY.setDimensions(1, 1, 0);
    WALKWAY.setBuildMaterials(PARTS, 1);
    WALKWAY.rulerBuilt = false;
    
    CutoutModel WALL_MODELS[][] = CutoutModel.fromImageGrid(
      GameContent.class, "shield_wall_models",
      "media/Buildings/mil_shield_walls.png",
      4, 3, 2, 2, false
    );
    
    SHIELD_WALL.name = "Shield Wall";
    SHIELD_WALL.modelVariants = WALL_MODELS[0];
    
    SHIELD_WALL.tint = TINT_LITE_MILITARY;
    SHIELD_WALL.pathing = PATH_WALLS;
    SHIELD_WALL.isWall  = true;
    SHIELD_WALL.setDimensions(1, 1, 2);
    SHIELD_WALL.setBuildMaterials(PARTS, 2);
    SHIELD_WALL.rulerBuilt = false;
    
    BLAST_DOOR.name = "Blast Door";
    BLAST_DOOR.modelVariants = WALL_MODELS[1];
    
    BLAST_DOOR.tint = TINT_MILITARY;
    BLAST_DOOR.pathing = PATH_WALLS;
    BLAST_DOOR.isWall  = true;
    BLAST_DOOR.setDimensions(2, 2, 2);
    BLAST_DOOR.setBuildMaterials(PARTS, 10);
    BLAST_DOOR.setFeatures(IS_GATE);
    BLAST_DOOR.rulerBuilt = false;
    
    TURRET.name = "Turret";
    TURRET.modelVariants = WALL_MODELS[2];
    
    TURRET.tint = TINT_MILITARY;
    TURRET.pathing = PATH_BLOCK;
    TURRET.isWall  = true;
    TURRET.setDimensions(2, 2, 4);
    TURRET.setBuildMaterials(PARTS, 10);
    TURRET.setFeatures(IS_TOWER, IS_TURRET);
    TURRET.rangeDamage = 4;
    TURRET.rangeDist   = 6;
    TURRET.armourClass = 0;
    TURRET.sightRange  = 8;
    TURRET.rulerBuilt = false;
    
    
    CutoutModel HOUSE_MODELS[][] = CutoutModel.fromImageGrid(
      GameContent.class, "housing_models",
      "media/Buildings/civ_holdings.png",
      4, 3, 2, 2, false
    );
    
    HOLDING.name = "Holding";
    HOLDING.tint = TINT_LITE_RESIDENTIAL;
    HOLDING.modelVariants = HOUSE_MODELS[0];
    
    HOLDING.setDimensions(2, 2, 1);
    HOLDING.setBuildMaterials(PLASTICS, 1, PARTS, 0);
    HOLDING.homeFoods    = FOOD_TYPES;
    HOLDING.maxResidents = 4;
    HOLDING.maxStock     = 1;
    HOLDING.buildsWith   = new Good[] { PLASTICS, PARTS };
    HOLDING.setFeatures(IS_HOUSING);
    HOLDING.setUpgradeTiers(HOLDING, HOUSE_T1, HOUSE_T2);
    HOLDING.rulerBuilt = false;
    
    HOUSE_T1.name = "Improved Holding";
    HOUSE_T1.modelVariants = HOUSE_MODELS[1];
    
    HOUSE_T1.setBuildMaterials(PLASTICS, 1, PARTS, 1);
    HOUSE_T1.maxStock = 2;
    HOUSE_T1.serviceNeeds.setWith(DIVERSION, 10);
    
    HOUSE_T2.name = "Fancy Holding";
    HOUSE_T2.modelVariants = HOUSE_MODELS[2];
    
    HOUSE_T2.setBuildMaterials(PLASTICS, 1, PARTS, 1);
    HOUSE_T2.homeUseGoods.setWith(MEDICINE, 1);
    HOUSE_T2.maxStock = 2;
    HOUSE_T2.serviceNeeds.setWith(DIVERSION, 15, PHYSICIAN_STATION, 1);
    
    
    CANTINA.name = "Cantina";
    CANTINA.tint = TINT_AMENITY;
    CANTINA.model = CutoutModel.fromImage(
      GameContent.class, "cantina_model",
      "media/Buildings/com_cantina.png", 3, 1
    );
    CANTINA.setDimensions(3, 3, 1);
    CANTINA.setBuildMaterials(PARTS, 10);
    CANTINA.setFeatures(DIVERSION, IS_REFUGE);
    CANTINA.featureAmount = 15;
    
    //
    //  Industrial structures:
    NURSERY.name = "Nursery";
    NURSERY.tint = TINT_LITE_INDUSTRIAL;
    NURSERY.model = CutoutModel.fromImage(
      GameContent.class, "nursery_model",
      "media/Buildings/res_nursery.png", 2, 1
    );
    NURSERY.setDimensions(2, 2, 1);
    NURSERY.setBuildMaterials(PLASTICS, 5, PARTS, 2);
    NURSERY.workerTypes.setWith(Vassals.PYON, 1);
    NURSERY.worksBeforeBuilt = true;
    NURSERY.gatherFlag  = IS_CROP;
    NURSERY.claimMargin = 4;
    NURSERY.produced    = new Good[] { CARBS, GREENS };
    NURSERY.maxStock    = 25;
    NURSERY.recipes = new Recipe[] {
      new Recipe(CARBS , SKILL_FARM, -1),
      new Recipe(GREENS, SKILL_FARM, -1)
    };
    
    HARVESTER.name = "Harvester";
    HARVESTER.tint = TINT_LITE_INDUSTRIAL;
    HARVESTER.model = CutoutModel.fromImage(
      GameContent.class, "harvester_model",
      "media/Buildings/res_harvester.png", 3, 1
    );
    HARVESTER.setDimensions(3, 3, 1);
    HARVESTER.setBuildMaterials(PLASTICS, 5, PARTS, 2);
    HARVESTER.workerTypes.setWith(Vassals.PYON, 1);
    HARVESTER.worksBeforeBuilt = true;
    HARVESTER.gatherFlag = IS_TREE;
    HARVESTER.maxStock   = 25;
    HARVESTER.produced   = new Good[] { CARBONS };
    HARVESTER.recipes = new Recipe[] {
      new Recipe(CARBONS, SKILL_CRAFT, -1)
    };
    
    EXCAVATOR.name = "Excavator";
    EXCAVATOR.tint = TINT_LITE_INDUSTRIAL;
    EXCAVATOR.model = CutoutModel.fromImage(
      GameContent.class, "excavator_model",
      "media/Buildings/res_excavator.png", 4, 2
    );
    EXCAVATOR.setDimensions(4, 4, 1);
    EXCAVATOR.setBuildMaterials(PLASTICS, 2, PARTS, 5);
    EXCAVATOR.workerTypes.setWith(Vassals.PYON, 2);
    EXCAVATOR.worksBeforeBuilt = true;
    EXCAVATOR.gatherFlag = IS_STONE;
    EXCAVATOR.maxStock   = 25;
    EXCAVATOR.produced   = new Good[] { CARBONS, ORES };
    EXCAVATOR.recipes = new Recipe[] {
      new Recipe(CARBONS, SKILL_CRAFT, -1),
      new Recipe(ORES   , SKILL_CRAFT, -1),
    };
    
    //  This isn't actually needed for now, and won't be until you get
    //  power-supplies working.
    //  Rename to 'Solar Mast'.
    /*
    SOLAR_TOWER.name = "Solar Tower";
    SOLAR_TOWER.tint = TINT_LITE_INDUSTRIAL;
    SOLAR_TOWER.model = CutoutModel.fromImage(
      GameContent.class, "solar_tower_model",
      "media/Buildings/solar_tower.png", 2, 3
    );
    SOLAR_TOWER.setDimensions(2, 2, 1);
    SOLAR_TOWER.setBuildMaterials(PLASTICS, 2, PARTS, 2);
    SOLAR_TOWER.maxStock   = 25;
    SOLAR_TOWER.produced   = new Good[] {};
    //*/
  }
  
  
  final public static BuildType
    RUINS_LAIR = new BuildType(BuildingForNest.class, "nest_ruins_lair", IS_NEST_BLD),
    
    //  TODO:  Add Animal Nests.
    
    //  TODO:  Add Native Huts.
    
    ALL_NESTS[] = { RUINS_LAIR };
  
  static {
    RUINS_LAIR.name = "Ruins";
    RUINS_LAIR.tint = TINT_LITE_INDUSTRIAL;
    RUINS_LAIR.setDimensions(3, 3, 1);
    RUINS_LAIR.setBuildMaterials(VOID, 10);
    RUINS_LAIR.maxHealth = 300;
  }
  
  
  
  /**  Default geography:
    */
  static World setupDefaultWorld() {
    World world = new World(ALL_GOODS);
    Base  cityA = new Base(world, world.addLocale(1, 1, "Elysium Sector"));
    Base  cityB = new Base(world, world.addLocale(3, 3, "Pavonis Sector"));
    world.assignTypes(ALL_BUILDINGS, ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES());
    
    cityA.setName("Elysium Base");
    cityA.setTradeLevel(PARTS   , 0, 5 );
    cityA.setTradeLevel(MEDICINE, 0, 10);
    //cityA.initTradeLevels(
    //  PARTS   , 5f ,
    //  MEDICINE, 10f
    //);
    cityA.initBuildLevels(
      TROOPER_LODGE, 2f ,
      HOLDING      , 10f
    );
    world.addCities(cityA);
    
    cityB.setName("Pavonis Base");
    cityB.setTradeLevel(CARBS, 0, 5 );
    cityB.setTradeLevel(ORES , 0, 10);
    //cityB.initTradeLevels(
    //  CARBS, 5f ,
    //  ORES , 10f
    //);
    cityA.initBuildLevels(
      TROOPER_LODGE, 0.75f,
      HOLDING      , 5f
    );
    world.addCities(cityB);
    
    World.setupRoute(cityA.locale, cityB.locale, AVG_CITY_DIST / 2);
    world.setMapSize(10, 10);
    
    return world;
  }
  
}





