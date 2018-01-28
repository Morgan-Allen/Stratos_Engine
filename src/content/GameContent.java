

package content;
import game.*;
import util.*;
import static game.CityMap.*;
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
    STONE_TYPES [] = { CARBONS, ORES },
    BUILD_GOODS [] = { PLASTICS, PARTS },
    HOME_GOODS  [] = { PLASTICS, PARTS, MEDICINE },
    MARKET_GOODS[] = (Good[]) Visit.compose(Good.class, FOOD_TYPES, HOME_GOODS),
    
    ALL_GOODS[] = new Good[] {
      CASH,
      CARBS, GREENS, PROTEIN, CARBONS, ORES, SPYCE,
      PLASTICS, PARTS, SOMA, MEDICINE
    };
  
  
  final static Terrain
    MEADOW = new Terrain("Meadow", 1),
    JUNGLE = new Terrain("Jungle", 2),
    DESERT = new Terrain("Desert", 3),
    LAKE   = new Terrain("Lake"  , 4),
    OCEAN  = new Terrain("Ocean" , 5),
    ALL_TERRAINS[] = {
      EMPTY, MEADOW, JUNGLE, DESERT, LAKE, OCEAN
    }
  ;
  final static Type
    JUNGLE_TREE1 = new Type(Element.class, "fixture_j_tree1", IS_FIXTURE),
    DESERT_ROCK1 = new Type(Element.class, "fixture_d_rock1", IS_FIXTURE),
    DESERT_ROCK2 = new Type(Element.class, "fixture_d_rock2", IS_FIXTURE),
    CLAY_BANK1   = new Type(Element.class, "fixture_b_clay1", IS_FIXTURE),
    
    ALL_TREES[] = { JUNGLE_TREE1 },
    ALL_ROCKS[] = { DESERT_ROCK1, DESERT_ROCK2 },
    ALL_OILS[] = { CLAY_BANK1 }
  ;
  final static WalkerType
    TAPIR   = new WalkerType(ActorAsAnimal.class, "animal_tapir" , IS_ANIMAL_ACT),
    QUAIL   = new WalkerType(ActorAsAnimal.class, "animal_quail" , IS_ANIMAL_ACT),
    JAGUAR  = new WalkerType(ActorAsAnimal.class, "animal_jaguar", IS_ANIMAL_ACT),
    ALL_ANIMALS[] = { TAPIR, QUAIL, JAGUAR }
  ;
  static {
    JUNGLE.attachFixtures(JUNGLE_TREE1, 0.50f);
    MEADOW.attachFixtures(JUNGLE_TREE1, 0.05f);
    DESERT.attachFixtures(DESERT_ROCK1, 0.15f, DESERT_ROCK2, 0.20f);
    
    JUNGLE_TREE1.tint = colour(0, 3, 0);
    DESERT_ROCK1.tint = colour(6, 4, 4);
    DESERT_ROCK2.tint = colour(6, 4, 4);
    CLAY_BANK1  .tint = colour(5, 3, 3);
    JUNGLE      .tint = colour(1, 4, 1);
    DESERT      .tint = colour(5, 4, 3);
    MEADOW      .tint = colour(2, 5, 2);
    LAKE        .tint = colour(0, 0, 3);
    
    //  TODO:  UNIFY WITH CROPS BELOW!
    JUNGLE_TREE1.growRate = 0.5f;
    DESERT_ROCK1.setDimensions(2, 2, 1);
    CLAY_BANK1  .setDimensions(2, 2, 0);
    LAKE.pathing = PATH_BLOCK;
    LAKE.isWater = true;
    
    //  TODO:  UNIFY WITH WALKER-TYPES BELOW!
    TAPIR .name        = "Tapir";
    TAPIR .habitats    = new Terrain[] { JUNGLE, MEADOW };
    TAPIR .predator    = false;
    TAPIR .meleeDamage = 1;
    TAPIR .armourClass = 2;
    TAPIR .maxHealth   = 8;
    
    QUAIL .name        = "Quail";
    QUAIL .habitats    = new Terrain[] { MEADOW, DESERT };
    QUAIL .predator    = false;
    QUAIL .meleeDamage = 0;
    QUAIL .armourClass = 0;
    QUAIL .maxHealth   = 2;
    
    JAGUAR.name        = "Jaguar";
    JAGUAR.habitats    = new Terrain[] { JUNGLE };
    JAGUAR.predator    = true;
    JAGUAR.meleeDamage = 5;
    JAGUAR.armourClass = 1;
    JAGUAR.maxHealth   = 6;
    
    for (Type s : ALL_ANIMALS) {
      s.rangeDamage = -1;
      s.lifespan = s.predator ? HUNTER_LIFESPAN : GRAZER_LIFESPAN;
      s.meatType = PROTEIN;
    }
    
    int i = 0;
    for (Good c : CROP_TYPES) {
      c.tint = TINT_CROPS[i++ % 3];
      c.growRate    = 1f;
      c.isCrop      = true;
      c.flagKey     = IS_CROP;
      c.yields      = c;
      c.yieldAmount = CROP_YIELD / 100f;
    }
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
  }
  
  
  final static WalkerType
    NOBLE     = new WalkerType(ActorAsPerson.class, "actor_noble"    , IS_PERSON_ACT, CLASS_NOBLE ),
    CONSORT   = new WalkerType(ActorAsPerson.class, "actor_consort"  , IS_PERSON_ACT, CLASS_NOBLE ),
    
    TROOPER   = new WalkerType(ActorAsPerson.class, "actor_trooper"  , IS_PERSON_ACT, CLASS_COMMON),
    ENFORCER  = new WalkerType(ActorAsPerson.class, "actor_enforcer" , IS_PERSON_ACT, CLASS_COMMON),
    RUNNER    = new WalkerType(ActorAsPerson.class, "actor_runner"   , IS_PERSON_ACT, CLASS_COMMON),
    
    ECOLOGIST = new WalkerType(ActorAsPerson.class, "actor_ecologist", IS_PERSON_ACT, CLASS_COMMON),
    ENGINEER  = new WalkerType(ActorAsPerson.class, "actor_engineer" , IS_PERSON_ACT, CLASS_COMMON),
    PHYSICIAN = new WalkerType(ActorAsPerson.class, "actor_physician", IS_PERSON_ACT, CLASS_COMMON),
    
    AUDITOR   = new WalkerType(ActorAsPerson.class, "actor_auditor"  , IS_PERSON_ACT, CLASS_TRADER),
    VENDOR    = new WalkerType(ActorAsPerson.class, "actor_vendor"   , IS_PERSON_ACT, CLASS_TRADER),
    PYON      = new WalkerType(ActorAsPerson.class, "actor_pyon"     , IS_PERSON_ACT, CLASS_TRADER),
    
    ALL_CITIZENS[] = { PYON, VENDOR },
    ALL_SOLDIERS[] = { TROOPER, RUNNER, NOBLE },
    ALL_NOBLES  [] = { NOBLE },
    ALL_PEOPLE[] = (WalkerType[]) Visit.compose(
      WalkerType.class, ALL_CITIZENS, ALL_SOLDIERS, ALL_NOBLES
    )
  ;
  static {
    
    NOBLE.name = "Noble";
    NOBLE.setInitTraits(SKILL_MELEE, 1, SKILL_SPEAK, 2, SKILL_WRITE, 2);
    
    CONSORT.name = "Consort";
    CONSORT.setInitTraits(SKILL_SPEAK, 2, SKILL_WRITE, 1, SKILL_EVADE, 2);
    
    
    TROOPER.name = "Trooper";
    TROOPER.meleeDamage = 2;
    TROOPER.rangeDamage = 5;
    TROOPER.rangeDist   = 4;
    TROOPER.armourClass = 4;
    TROOPER.maxHealth   = 6;
    TROOPER.setInitTraits(SKILL_MELEE, 3, SKILL_RANGE, 4, SKILL_EVADE, 1);
    
    ENFORCER.name = "Enforcer";
    ENFORCER.meleeDamage = 2;
    ENFORCER.rangeDamage = 4;
    ENFORCER.rangeDist   = 6;
    ENFORCER.armourClass = 3;
    ENFORCER.maxHealth   = 4;
    ENFORCER.setInitTraits(SKILL_MELEE, 2, SKILL_RANGE, 5, SKILL_EVADE, 3);
    
    RUNNER.name = "Runner";
    RUNNER.rangeDamage = 6;
    RUNNER.rangeDist   = 8;
    RUNNER.armourClass = 3;
    RUNNER.maxHealth   = 3;
    RUNNER.setInitTraits(SKILL_RANGE, 5, SKILL_EVADE, 4);
    
    
    ECOLOGIST.name = "Ecologist";
    ECOLOGIST.rangeDamage = 4;
    ECOLOGIST.armourClass = 3;
    ECOLOGIST.rangeDist   = 6;
    ECOLOGIST.maxHealth   = 4;
    ECOLOGIST.setInitTraits(SKILL_RANGE, 5, SKILL_EVADE, 3, SKILL_FARM, 4);
    
    ENGINEER.name = "Engineer";
    ENGINEER.meleeDamage = 5;
    ENGINEER.armourClass = 5;
    ENGINEER.maxHealth   = 4;
    ENGINEER.setInitTraits(SKILL_MELEE, 3, SKILL_CRAFT, 5, SKILL_BUILD, 5);
    
    PHYSICIAN.name = "Physician";
    PHYSICIAN.meleeDamage = 0;
    PHYSICIAN.rangeDamage = 0;
    PHYSICIAN.armourClass = 1;
    PHYSICIAN.maxHealth   = 3;
    PHYSICIAN.setInitTraits(SKILL_CRAFT, 6, SKILL_WRITE, 4, SKILL_SPEAK, 3);
    
    
    AUDITOR.name = "Auditor";
    AUDITOR.setInitTraits(SKILL_SPEAK, 4, SKILL_WRITE, 4);
    
    VENDOR.name = "Vendor";
    VENDOR.setInitTraits(SKILL_SPEAK, 2, SKILL_WRITE, 2);
    
    PYON.name = "Pyon";
    PYON.setInitTraits(SKILL_FARM, 1, SKILL_BUILD, 1, SKILL_CRAFT, 1);
    
    
    for (Type t : ALL_PEOPLE) {
      t.foodsAllowed = FOOD_TYPES;
    }
  }
  
  
  final static BuildType
    
    //  TODO:  Add Runner Market and Pseer/Kommando School.
    
    BASTION           = new BuildType(BuildingForHome.class   , "venue_bastion"  , IS_HOME_BLD   ),
    TROOPER_LODGE     = new BuildType(BuildingForArmy.class   , "venue_trooper"  , IS_ARMY_BLD   ),
    ENFORCER_BLOC     = new BuildType(BuildingForCollect.class, "venue_enforcer" , IS_COLLECT_BLD),
    
    ECOLOGIST_STATION = new BuildType(BuildingForHunt.class   , "venue_ecologist", IS_HUNTS_BLD  ),
    ENGINEER_STATION  = new BuildType(BuildingForCrafts.class , "venue_engineer" , IS_CRAFTS_BLD ),
    PHYSICIAN_STATION = new BuildType(BuildingForCrafts.class , "venue_physician", IS_CRAFTS_BLD ),
    
    CANTINA           = new BuildType(BuildingForAmenity.class, "venue_cantina"  , IS_AMENITY_BLD),
    STOCK_EXCHANGE    = new BuildType(BuildingForCrafts.class , "venue_stock_ex" , IS_CRAFTS_BLD ),
    SUPPLY_DEPOT      = new BuildType(BuildingForTrade.class  , "venue_supply_d" , IS_TRADE_BLD  ),
    
    SCHOOL_LOG        = new BuildType(BuildingForFaith.class , "venue_logician"  , IS_FAITH_BLD  ),
    SCHOOL_COL        = new BuildType(BuildingForFaith.class , "venue_collective", IS_FAITH_BLD  ),
    SCHOOL_LEN        = new BuildType(BuildingForFaith.class , "venue_lensr"     , IS_FAITH_BLD  ),
    SCHOOL_SHA        = new BuildType(BuildingForFaith.class , "venue_shaper"    , IS_FAITH_BLD  ),
    SCHOOL_TEK        = new BuildType(BuildingForFaith.class , "venue_tek_priest", IS_FAITH_BLD  ),
    SCHOOL_SPA        = new BuildType(BuildingForFaith.class , "venue_spacer"    , IS_FAITH_BLD  ),
    
    
    WALKWAY       = new BuildType(Element.class           , "type_walkway"      , IS_STRUCTURAL ),
    SHIELD_WALL   = new BuildType(Element.class           , "type_shield_wall"  , IS_STRUCTURAL ),
    BLAST_DOOR    = new BuildType(BuildingForWalls.class  , "type_blast_door"   , IS_WALLS_BLD  ),
    TURRET        = new BuildType(BuildingForWalls.class  , "type_turret"       , IS_WALLS_BLD  ),
    
    HOLDING       = new BuildType(BuildingForHome.class   , "type_holding"      , IS_HOME_BLD   ),
    HOUSE_T1      = new BuildType(BuildingForHome.class   , "type_house_tier1"  , IS_UPGRADE    ),
    HOUSE_T2      = new BuildType(BuildingForHome.class   , "type_house_tier2"  , IS_UPGRADE    ),
    
    NURSERY       = new BuildType(BuildingForGather.class , "type_farm_plot"    , IS_GATHER_BLD ),
    FORMER_BAY    = new BuildType(BuildingForGather.class , "type_sawyer"       , IS_GATHER_BLD ),
    ORE_SMELTER   = new BuildType(BuildingForGather.class , "type_quarry_pit"   , IS_GATHER_BLD ),
    SOLAR_TOWER   = new BuildType(BuildingForGather.class , "type_solar_tower"  , IS_GATHER_BLD ),
    
    INFRASTRUCTURE_BUILDINGS[] = { WALKWAY, SHIELD_WALL, BLAST_DOOR, TURRET },
    RESIDENTIAL_BUILDINGS[] = { BASTION, HOLDING, CANTINA },
    RESOURCE_BUILDINGS[] = { NURSERY, FORMER_BAY, ORE_SMELTER },
    ECONOMIC_BUILDINGS[] = { STOCK_EXCHANGE, SUPPLY_DEPOT, ENFORCER_BLOC },
    MILITARY_BUILDINGS[] = { ECOLOGIST_STATION, TROOPER_LODGE },
    SCIENCE_BUILDINGS[] = { ENGINEER_STATION, PHYSICIAN_STATION },
    PSI_SCHOOL_BUILDINGS[] = {
      SCHOOL_LOG, SCHOOL_COL, SCHOOL_LEN, SCHOOL_SHA, SCHOOL_TEK, SCHOOL_SPA
    }
  ;
  static {

    BASTION.name = "Bastion";
    BASTION.tint = TINT_MILITARY;
    BASTION.setDimensions(5, 5, 2);
    BASTION.maxHealth = 300;
    BASTION.setBuildMaterials(PLASTICS, 10, PARTS, 25);
    BASTION.setWorkerTypes(NOBLE, AUDITOR, PYON);
    BASTION.homeSocialClass = CLASS_NOBLE;
    BASTION.maxResidents = 2;
    BASTION.maxWorkers   = 2;
    BASTION.buildsWith   = new Good[] { PLASTICS, PARTS };
    BASTION.setFeatures(IS_HOUSING);
    BASTION.worksBeforeBuilt = true;
    
    TROOPER_LODGE.name = "Trooper Lodge";
    TROOPER_LODGE.tint = TINT_MILITARY;
    TROOPER_LODGE.setDimensions(6, 6, 2);
    TROOPER_LODGE.setBuildMaterials(PLASTICS, 1, PARTS, 7);
    TROOPER_LODGE.setWorkerTypes(TROOPER);
    TROOPER_LODGE.maxWorkers = 2;
    TROOPER_LODGE.maxHealth  = 250;
    
    ENFORCER_BLOC.name = "Enforcer Bloc";
    ENFORCER_BLOC.tint = TINT_COMMERCIAL;
    ENFORCER_BLOC.setDimensions(2, 2, 1);
    ENFORCER_BLOC.setBuildMaterials(PARTS, 4);
    ENFORCER_BLOC.setWorkerTypes(ENFORCER);
    ENFORCER_BLOC.produced = new Good[] { CASH };
    ENFORCER_BLOC.features = new Good[] { IS_ADMIN };
    
    //
    //  Science structures:
    ECOLOGIST_STATION.name = "Ecologist Station";
    ECOLOGIST_STATION.tint = TINT_MILITARY;
    ECOLOGIST_STATION.setDimensions(4, 4, 1);
    ECOLOGIST_STATION.setBuildMaterials(PLASTICS, 7, PARTS, 1);
    ECOLOGIST_STATION.setWorkerTypes(ECOLOGIST);
    ECOLOGIST_STATION.worksBeforeBuilt = true;
    ECOLOGIST_STATION.maxWorkers = 2;
    ECOLOGIST_STATION.maxHealth  = 100;
    ECOLOGIST_STATION.produced   = new Good[] { PROTEIN };
    
    ENGINEER_STATION.name = "Engineer Station";
    ENGINEER_STATION.tint = TINT_INDUSTRIAL;
    ENGINEER_STATION.setDimensions(2, 2, 1);
    ENGINEER_STATION.setBuildMaterials(PARTS, 8);
    ENGINEER_STATION.setWorkerTypes(ENGINEER);
    ENGINEER_STATION.needed   = new Good[] { ORES };
    ENGINEER_STATION.produced = new Good[] { PARTS };
    ENGINEER_STATION.maxStock = 3;
    ENGINEER_STATION.craftSkill = SKILL_CRAFT;
    
    PHYSICIAN_STATION.name = "Physician Station";
    PHYSICIAN_STATION.tint = TINT_INDUSTRIAL;
    PHYSICIAN_STATION.setDimensions(2, 2, 1);
    PHYSICIAN_STATION.setBuildMaterials(PLASTICS, 4, PARTS, 2);
    PHYSICIAN_STATION.setWorkerTypes(PHYSICIAN);
    PHYSICIAN_STATION.needed   = new Good[] { GREENS };
    PHYSICIAN_STATION.produced = new Good[] { MEDICINE };
    PHYSICIAN_STATION.maxStock = 3;
    PHYSICIAN_STATION.craftSkill = SKILL_CRAFT;
    PHYSICIAN_STATION.setFeatures(HEALTHCARE);
    PHYSICIAN_STATION.featureAmount = 20;
    
    //
    //  Commercial structures:
    STOCK_EXCHANGE.name = "Stock Exchange";
    STOCK_EXCHANGE.tint = TINT_COMMERCIAL;
    STOCK_EXCHANGE.setDimensions(4, 4, 1);
    STOCK_EXCHANGE.setBuildMaterials(PLASTICS, 4, PARTS, 2);
    STOCK_EXCHANGE.setWorkerTypes(VENDOR);
    STOCK_EXCHANGE.needed   = MARKET_GOODS;
    STOCK_EXCHANGE.features = new Good[] { IS_VENDOR };
    
    SUPPLY_DEPOT.name = "Supply Depot";
    SUPPLY_DEPOT.tint = TINT_COMMERCIAL;
    SUPPLY_DEPOT.setDimensions(3, 3, 1);
    SUPPLY_DEPOT.setBuildMaterials(PLASTICS, 4, PARTS, 2);
    SUPPLY_DEPOT.setWorkerTypes(PYON);
    SUPPLY_DEPOT.maxWorkers = 2;
    SUPPLY_DEPOT.worksBeforeBuilt = true;
    SUPPLY_DEPOT.features = new Good[] { IS_TRADER };
    
    //
    //  Religious structures:
    SCHOOL_LOG.name = "Logician School";
    SCHOOL_COL.name = "Collective School";
    SCHOOL_LEN.name = "LENSR School";
    SCHOOL_SHA.name = "Shaper School";
    SCHOOL_TEK.name = "Tek Priest School";
    SCHOOL_SPA.name = "Spacer School";
    
    for (Type t : PSI_SCHOOL_BUILDINGS) {
      t.tint = TINT_RELIGIOUS;
      t.setDimensions(6, 6, 3);
      t.setBuildMaterials(PARTS, 15);
      //t.setWorkerTypes(PRIEST);
      t.maxWorkers      = 1;
      t.maxHealth       = 100;
      t.maxResidents    = 1;
      t.homeSocialClass = CLASS_NOBLE;
      t.features        = new Good[] { RELIGION };
    }
    
    
    //
    WALKWAY.name = "Walkway";
    WALKWAY.tint = PAVE_COLOR;
    WALKWAY.pathing = PATH_PAVE;
    WALKWAY.setDimensions(1, 1, 0);
    WALKWAY.setBuildMaterials(PARTS, 1);
    
    SHIELD_WALL.name = "Shield Wall";
    SHIELD_WALL.tint = TINT_LITE_MILITARY;
    SHIELD_WALL.pathing = PATH_WALLS;
    SHIELD_WALL.isWall  = true;
    SHIELD_WALL.setDimensions(1, 1, 2);
    SHIELD_WALL.setBuildMaterials(PARTS, 2);
    
    BLAST_DOOR.name = "Blast Door";
    BLAST_DOOR.tint = TINT_MILITARY;
    BLAST_DOOR.pathing = PATH_WALLS;
    BLAST_DOOR.isWall  = true;
    BLAST_DOOR.setDimensions(2, 2, 2);
    BLAST_DOOR.setBuildMaterials(PARTS, 10);
    BLAST_DOOR.setFeatures(IS_GATE);
    
    TURRET.name = "Turret";
    TURRET.tint = TINT_MILITARY;
    TURRET.pathing = PATH_BLOCK;
    TURRET.isWall  = true;
    TURRET.setDimensions(2, 2, 4);
    TURRET.setBuildMaterials(PARTS, 10);
    TURRET.setFeatures(IS_TOWER);
    
    
    
    HOLDING.name = "Holding";
    HOLDING.tint = TINT_LITE_RESIDENTIAL;
    HOLDING.setDimensions(2, 2, 1);
    HOLDING.setBuildMaterials(PLASTICS, 1);
    HOLDING.setWorkerTypes(PYON);
    HOLDING.homeFoods    = FOOD_TYPES;
    HOLDING.maxResidents = 4;
    HOLDING.maxStock     = 1;
    HOLDING.buildsWith   = new Good[] { PLASTICS, PARTS };
    HOLDING.features     = new Good[] { IS_HOUSING };
    HOLDING.setUpgradeTiers(HOLDING, HOUSE_T1, HOUSE_T2);
    
    HOUSE_T1.name = "Improved Holding";
    HOUSE_T1.setBuildMaterials(PLASTICS, 2, PARTS, 1);
    HOUSE_T1.maxStock = 2;
    HOUSE_T1.setUpgradeNeeds(DIVERSION, 10);
    
    HOUSE_T2.name = "Fancy Holding";
    HOUSE_T2.setBuildMaterials(PLASTICS, 3, PARTS, 2);
    HOUSE_T2.setHomeUsage(MEDICINE, 1);
    HOUSE_T2.maxStock = 2;
    HOUSE_T2.setUpgradeNeeds(DIVERSION, 15, PHYSICIAN_STATION, 1);
    
    CANTINA.name = "Cantina";
    CANTINA.tint = TINT_AMENITY;
    CANTINA.setDimensions(3, 3, 1);
    CANTINA.setBuildMaterials(PARTS, 10);
    CANTINA.setFeatures(DIVERSION);
    CANTINA.featureAmount = 15;
    
    //
    //  Industrial structures:
    NURSERY.name = "Nursery";
    NURSERY.tint = TINT_LITE_INDUSTRIAL;
    NURSERY.setDimensions(2, 2, 1);
    NURSERY.setBuildMaterials(PLASTICS, 5, PARTS, 2);
    NURSERY.setWorkerTypes(PYON);
    NURSERY.worksBeforeBuilt = true;
    NURSERY.gatherFlag = IS_CROP;
    NURSERY.produced   = CROP_TYPES;
    NURSERY.maxStock   = 25;
    NURSERY.maxWorkers = 2;
    NURSERY.craftSkill = SKILL_FARM;
    
    FORMER_BAY.name = "Former Bay";
    FORMER_BAY.tint = TINT_LITE_INDUSTRIAL;
    FORMER_BAY.setDimensions(2, 2, 1);
    FORMER_BAY.setBuildMaterials(PLASTICS, 5, PARTS, 2);
    FORMER_BAY.setWorkerTypes(PYON);
    FORMER_BAY.worksBeforeBuilt = true;
    FORMER_BAY.gatherFlag = IS_TREE;
    FORMER_BAY.maxStock   = 25;
    FORMER_BAY.produced   = new Good[] { CARBONS };
    FORMER_BAY.maxWorkers = 2;
    FORMER_BAY.craftSkill = SKILL_CRAFT;
    
    ORE_SMELTER.name = "Ore Smelter";
    ORE_SMELTER.tint = TINT_LITE_INDUSTRIAL;
    ORE_SMELTER.setDimensions(2, 2, 1);
    ORE_SMELTER.setBuildMaterials(PLASTICS, 2, PARTS, 5);
    ORE_SMELTER.setWorkerTypes(PYON);
    ORE_SMELTER.worksBeforeBuilt = true;
    ORE_SMELTER.gatherFlag = IS_STONE;
    ORE_SMELTER.maxStock   = 25;
    ORE_SMELTER.produced   = new Good[] { CARBONS, ORES };
    ORE_SMELTER.maxWorkers = 2;
    ORE_SMELTER.craftSkill = SKILL_CRAFT;
    
    SOLAR_TOWER.name = "Solar Tower";
    SOLAR_TOWER.tint = TINT_LITE_INDUSTRIAL;
    SOLAR_TOWER.setDimensions(2, 2, 1);
    SOLAR_TOWER.setBuildMaterials(PLASTICS, 2, PARTS, 2);
    SOLAR_TOWER.maxStock   = 25;
    SOLAR_TOWER.produced   = new Good[] {};
    SOLAR_TOWER.maxWorkers = 0;
  }
  
  
  
  /**  Default geography:
    */
  static World setupDefaultWorld() {
    World world = new World(ALL_GOODS);
    City  cityA = new City(world);
    City  cityB = new City(world);
    world.assignCitizenTypes(ALL_CITIZENS, ALL_SOLDIERS, ALL_NOBLES);
    
    cityA.setName("Xochimilco");
    cityA.setWorldCoords(1, 1);
    cityA.initTradeLevels(
      PARTS, 5f ,
      MEDICINE , 10f
    );
    cityA.initBuildLevels(
      TROOPER_LODGE, 2f ,
      HOLDING   , 10f
    );
    world.addCities(cityA);
    
    cityB.setName("Tlacopan");
    cityB.setWorldCoords(3, 3);
    cityB.initTradeLevels(
      CARBS, 5f ,
      ORES , 10f
    );
    cityA.initBuildLevels(
      TROOPER_LODGE, 0.75f,
      HOLDING   , 5f
    );
    world.addCities(cityB);
    
    City.setupRoute(cityA, cityB, AVG_CITY_DIST / 2);
    world.setMapSize(10, 10);
    
    return world;
  }
  
}





