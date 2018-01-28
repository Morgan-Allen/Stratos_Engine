

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
    VAGRANT  = new WalkerType(ActorAsPerson.class, "type_vagrant" , IS_PERSON_ACT, CLASS_COMMON),
    CITIZEN  = new WalkerType(ActorAsPerson.class, "type_citizen" , IS_PERSON_ACT, CLASS_COMMON),
    SERVANT  = new WalkerType(ActorAsPerson.class, "type_servant" , IS_PERSON_ACT, CLASS_SLAVE ),
    NOBLE    = new WalkerType(ActorAsPerson.class, "type_noble"   , IS_PERSON_ACT, CLASS_NOBLE ),
    CONSORT  = new WalkerType(ActorAsPerson.class, "type_consort" , IS_PERSON_ACT, CLASS_NOBLE ),
    WORKER   = new WalkerType(ActorAsPerson.class, "type_worker"  , IS_PERSON_ACT, CLASS_COMMON),
    MERCHANT = new WalkerType(ActorAsPerson.class, "type_merchant", IS_PERSON_ACT, CLASS_TRADER),
    PORTER   = new WalkerType(ActorAsPerson.class, "type_porter"  , IS_PERSON_ACT, CLASS_SLAVE ),
    HUNTER   = new WalkerType(ActorAsPerson.class, "type_hunter"  , IS_PERSON_ACT, CLASS_NOBLE ),
    SOLDIER  = new WalkerType(ActorAsPerson.class, "type_soldier" , IS_PERSON_ACT, CLASS_NOBLE ),
    PRIEST   = new WalkerType(ActorAsPerson.class, "type_priest"  , IS_PERSON_ACT, CLASS_NOBLE ),
    
    ALL_CITIZENS[] = {
      CITIZEN, VAGRANT, SERVANT, WORKER, MERCHANT, PORTER
    },
    ALL_SOLDIERS[] = {
      SOLDIER, HUNTER
    },
    ALL_NOBLES[] = {
      NOBLE, CONSORT, PRIEST
    },
    ALL_PEOPLE[] = (WalkerType[]) Visit.compose(
      WalkerType.class, ALL_CITIZENS, ALL_SOLDIERS, ALL_NOBLES
    )
  ;
  static {
    //  TODO:  Get rid of this background as a starting option.
    VAGRANT .name = "Vagrant";
    
    CITIZEN .name = "Citizen";
    CITIZEN.setInitTraits(SKILL_FARM, 1, SKILL_BUILD, 1, SKILL_CRAFT, 1);
    
    SERVANT .name = "Servant";
    SERVANT.setInitTraits(SKILL_SPEAK, 1, SKILL_WRITE, 1, SKILL_BUILD, 1);
    
    NOBLE   .name = "Noble";
    NOBLE.setInitTraits(SKILL_MELEE, 1, SKILL_SPEAK, 2, SKILL_WRITE, 2);
    
    CONSORT .name = "Consort";
    CONSORT.setInitTraits(SKILL_SPEAK, 2, SKILL_WRITE, 1, SKILL_EVADE, 2);
    
    WORKER  .name = "Worker";
    WORKER.setInitTraits(SKILL_FARM, 1, SKILL_BUILD, 1, SKILL_CRAFT, 1);
    
    MERCHANT.name = "Merchant";
    MERCHANT.setInitTraits(SKILL_SPEAK, 2, SKILL_WRITE, 2);
    
    PORTER  .name = "Porter";
    PORTER.setInitTraits();
    
    HUNTER  .name = "Hunter";
    HUNTER .rangeDamage = 4;
    HUNTER .armourClass = 3;
    HUNTER .rangeDist   = 6;
    HUNTER .genderRole  = SEX_MALE;
    HUNTER.setInitTraits(SKILL_RANGE, 2, SKILL_EVADE, 2);

    SOLDIER .name = "Soldier";
    SOLDIER.meleeDamage = 5;
    SOLDIER.rangeDamage = 2;
    SOLDIER.rangeDist   = 4;
    SOLDIER.armourClass = 4;
    SOLDIER.maxHealth   = 6;
    SOLDIER.genderRole  = SEX_MALE;
    SOLDIER.setInitTraits(SKILL_MELEE, 2, SKILL_RANGE, 1, SKILL_EVADE, 1);
    
    PRIEST  .name = "Priest";
    PRIEST.setInitTraits(SKILL_PRAY, 2, SKILL_WRITE, 2, SKILL_SPEAK, 2);
    
    for (Type t : ALL_PEOPLE) {
      t.foodsAllowed = FOOD_TYPES;
    }
  }
  
  
  final static BuildType
    
    ROAD          = new BuildType(Element.class         , "type_road"    , IS_STRUCTURAL),
    WALL          = new BuildType(Element.class         , "type_wall"    , IS_STRUCTURAL),
    GATE          = new BuildType(BuildingForWalls.class, "type_gate"    , IS_WALLS_BLD ),
    TOWER         = new BuildType(BuildingForWalls.class, "type_tower"   , IS_WALLS_BLD ),
    
    INFRASTRUCTURE_BUILDINGS[] = {
      ROAD, WALL, GATE, TOWER
    },
    
    PALACE        = new BuildType(BuildingForHome.class   , "type_palace"       , IS_HOME_BLD   ),
    HOUSE         = new BuildType(BuildingForHome.class   , "type_house"        , IS_HOME_BLD   ),
    HOUSE_T1      = new BuildType(BuildingForHome.class   , "type_house_tier1"  , IS_UPGRADE    ),
    HOUSE_T2      = new BuildType(BuildingForHome.class   , "type_house_tier2"  , IS_UPGRADE    ),
    BALL_COURT    = new BuildType(BuildingForAmenity.class, "type_ball_court"   , IS_AMENITY_BLD),
    RESIDENTIAL_BUILDINGS[] = { PALACE, HOUSE, BALL_COURT },
    
    FARM_PLOT     = new BuildType(BuildingForGather.class , "type_farm_plot"    , IS_GATHER_BLD ),
    SAWYER        = new BuildType(BuildingForGather.class , "type_sawyer"       , IS_GATHER_BLD ),
    QUARRY_PIT    = new BuildType(BuildingForGather.class , "type_quarry_pit"   , IS_GATHER_BLD ),
    RESOURCE_BUILDINGS[] = { FARM_PLOT, SAWYER, QUARRY_PIT },
    
    MARKET        = new BuildType(BuildingForCrafts.class , "type_market"       , IS_CRAFTS_BLD ),
    PORTER_POST   = new BuildType(BuildingForTrade.class  , "type_porter_post"  , IS_TRADE_BLD  ),
    
    //  TODO:  THIS HAS TO BE MERGED WITH THE PALACE/BASTION!
    COLLECTOR     = new BuildType(BuildingForCollect.class, "type_collector"    , IS_COLLECT_BLD),
    ECONOMIC_BUILDINGS[] = { MARKET, PORTER_POST, COLLECTOR },
    
    HUNTER_LODGE  = new BuildType(BuildingForHunt.class   , "type_hunter_lodge" , IS_HUNTS_BLD  ),
    GARRISON      = new BuildType(BuildingForArmy.class   , "type_garrison"     , IS_ARMY_BLD   ),
    MILITARY_BUILDINGS[] = { HUNTER_LODGE, GARRISON },
    
    ENGINEER_STATION  = new BuildType(BuildingForCrafts.class , "venue_engineer" , IS_CRAFTS_BLD ),
    PHYSICIAN_STATION = new BuildType(BuildingForCrafts.class , "venue_physician", IS_CRAFTS_BLD ),
    SCIENCE_BUILDINGS[] = { ENGINEER_STATION, PHYSICIAN_STATION },
    
    TEMPLE_QZ     = new BuildType(BuildingForFaith.class , "type_temple_qz"     , IS_FAITH_BLD  ),
    TEMPLE_TZ     = new BuildType(BuildingForFaith.class , "type_temple_tz"     , IS_FAITH_BLD  ),
    TEMPLE_HU     = new BuildType(BuildingForFaith.class , "type_temple_hu"     , IS_FAITH_BLD  ),
    TEMPLE_TL     = new BuildType(BuildingForFaith.class , "type_temple_tl"     , IS_FAITH_BLD  ),
    TEMPLE_MI     = new BuildType(BuildingForFaith.class , "type_temple_mi"     , IS_FAITH_BLD  ),
    TEMPLE_XT     = new BuildType(BuildingForFaith.class , "type_temple_xt"     , IS_FAITH_BLD  ),
    
    PSI_SCHOOL_BUILDINGS[] = {
      TEMPLE_QZ, TEMPLE_TZ, TEMPLE_HU, TEMPLE_TL, TEMPLE_MI, TEMPLE_XT
    }
  ;
  static {
    
    //
    //  1x1 infrastructure:
    ROAD.name = "Road";
    ROAD.tint = PAVE_COLOR;
    ROAD.pathing = PATH_PAVE;
    ROAD.setDimensions(1, 1, 0);
    ROAD.setBuildMaterials(PARTS, 1);
    
    WALL.name = "Wall";
    WALL.tint = TINT_LITE_MILITARY;
    WALL.pathing = PATH_WALLS;
    WALL.isWall  = true;
    WALL.setDimensions(1, 1, 2);
    WALL.setBuildMaterials(PARTS, 2);
    
    GATE.name = "Gate";
    GATE.tint = TINT_MILITARY;
    GATE.pathing = PATH_WALLS;
    GATE.isWall  = true;
    GATE.setDimensions(2, 2, 2);
    GATE.setBuildMaterials(PARTS, 10);
    GATE.setFeatures(IS_GATE);
    
    TOWER.name = "Tower";
    TOWER.tint = TINT_MILITARY;
    TOWER.pathing = PATH_BLOCK;
    TOWER.isWall  = true;
    TOWER.setDimensions(2, 2, 4);
    TOWER.setBuildMaterials(PARTS, 10);
    TOWER.setFeatures(IS_TOWER);
    
    //
    //  Residential structures:
    PALACE.name = "Palace";
    PALACE.tint = TINT_RESIDENTIAL;
    PALACE.setDimensions(5, 5, 2);
    PALACE.maxHealth = 300;
    PALACE.setBuildMaterials(PLASTICS, 10, PARTS, 25);
    ///PALACE.setHomeUsage(POTTERY, 5, COTTON, 10);
    PALACE.setWorkerTypes(NOBLE, SERVANT);
    PALACE.homeSocialClass = CLASS_NOBLE;
    PALACE.maxResidents = 2;
    PALACE.maxWorkers   = 2;
    PALACE.buildsWith   = new Good[] { PLASTICS, PARTS };
    PALACE.setFeatures(IS_HOUSING);
    PALACE.worksBeforeBuilt = true;
    
    HOUSE.name = "House";
    HOUSE.tint = TINT_LITE_RESIDENTIAL;
    HOUSE.setDimensions(2, 2, 1);
    HOUSE.setBuildMaterials(PLASTICS, 1);
    HOUSE.setWorkerTypes(CITIZEN);
    HOUSE.homeFoods    = FOOD_TYPES;
    HOUSE.maxResidents = 4;
    HOUSE.maxStock     = 1;
    HOUSE.buildsWith   = new Good[] { PLASTICS, PARTS };
    HOUSE.features     = new Good[] { IS_HOUSING };
    HOUSE.setUpgradeTiers(HOUSE, HOUSE_T1, HOUSE_T2);
    
    HOUSE_T1.name = "Improved House";
    HOUSE_T1.setBuildMaterials(PLASTICS, 2, PARTS, 1);
    HOUSE_T1.maxStock = 2;
    HOUSE_T1.setUpgradeNeeds(DIVERSION, 10);
    
    HOUSE_T2.name = "Fancy House";
    HOUSE_T2.setBuildMaterials(PLASTICS, 3, PARTS, 2);
    HOUSE_T2.setHomeUsage(MEDICINE, 1);
    HOUSE_T2.maxStock = 2;
    HOUSE_T2.setUpgradeNeeds(DIVERSION, 15, PHYSICIAN_STATION, 1);
    
    BALL_COURT.name = "Ball Court";
    BALL_COURT.tint = TINT_AMENITY;
    BALL_COURT.setDimensions(3, 3, 1);
    BALL_COURT.setBuildMaterials(PARTS, 10);
    BALL_COURT.setFeatures(DIVERSION);
    BALL_COURT.featureAmount = 15;

    //
    //  Industrial structures:
    
    FARM_PLOT.name = "Farm Plot";
    FARM_PLOT.tint = TINT_LITE_INDUSTRIAL;
    FARM_PLOT.setDimensions(2, 2, 1);
    FARM_PLOT.setBuildMaterials(PLASTICS, 5, PARTS, 2);
    FARM_PLOT.setWorkerTypes(WORKER);
    FARM_PLOT.worksBeforeBuilt = true;
    FARM_PLOT.gatherFlag = IS_CROP;
    FARM_PLOT.produced   = CROP_TYPES;
    FARM_PLOT.maxStock   = 25;
    FARM_PLOT.maxWorkers = 2;
    FARM_PLOT.craftSkill = SKILL_FARM;
    
    SAWYER.name = "Sawyer";
    SAWYER.tint = TINT_LITE_INDUSTRIAL;
    SAWYER.setDimensions(2, 2, 1);
    SAWYER.setBuildMaterials(PLASTICS, 5, PARTS, 2);
    SAWYER.setWorkerTypes(WORKER);
    SAWYER.worksBeforeBuilt = true;
    SAWYER.gatherFlag = IS_TREE;
    SAWYER.maxStock   = 25;
    SAWYER.produced   = new Good[] { CARBONS };
    SAWYER.maxWorkers = 2;
    SAWYER.craftSkill = SKILL_CRAFT;
    
    QUARRY_PIT.name = "Quarry Pit";
    QUARRY_PIT.tint = TINT_LITE_INDUSTRIAL;
    QUARRY_PIT.setDimensions(2, 2, 1);
    QUARRY_PIT.setBuildMaterials(PLASTICS, 2, PARTS, 5);
    QUARRY_PIT.setWorkerTypes(WORKER);
    QUARRY_PIT.worksBeforeBuilt = true;
    QUARRY_PIT.gatherFlag = IS_STONE;
    QUARRY_PIT.maxStock   = 25;
    QUARRY_PIT.produced   = new Good[] { CARBONS, ORES };
    QUARRY_PIT.maxWorkers = 2;
    QUARRY_PIT.craftSkill = SKILL_CRAFT;
    
    //
    //  Commercial structures:
    MARKET.name = "Marketplace";
    MARKET.tint = TINT_COMMERCIAL;
    MARKET.setDimensions(4, 4, 1);
    MARKET.setBuildMaterials(PLASTICS, 4, PARTS, 2);
    MARKET.setWorkerTypes(MERCHANT);
    MARKET.needed   = MARKET_GOODS;
    MARKET.features = new Good[] { IS_VENDOR };
    
    PORTER_POST.name = "Porter Post";
    PORTER_POST.tint = TINT_COMMERCIAL;
    PORTER_POST.setDimensions(3, 3, 1);
    PORTER_POST.setBuildMaterials(PLASTICS, 4, PARTS, 2);
    PORTER_POST.setWorkerTypes(PORTER, WORKER);
    PORTER_POST.worksBeforeBuilt = true;
    PORTER_POST.features = new Good[] { IS_TRADER };
    
    //  TODO:  Make this into a 'Guardhouse', or the nearest equivalent...
    COLLECTOR.name = "Collector";
    COLLECTOR.tint = TINT_COMMERCIAL;
    COLLECTOR.setDimensions(2, 2, 1);
    COLLECTOR.setBuildMaterials(PARTS, 4);
    COLLECTOR.setWorkerTypes(MERCHANT);
    COLLECTOR.produced = new Good[] { CASH };
    COLLECTOR.features = new Good[] { IS_ADMIN };
    
    //
    //  Military structures:
    HUNTER_LODGE.name = "Hunter Lodge";
    HUNTER_LODGE.tint = TINT_MILITARY;
    HUNTER_LODGE.setDimensions(4, 4, 1);
    HUNTER_LODGE.setBuildMaterials(PLASTICS, 7, PARTS, 1);
    HUNTER_LODGE.setWorkerTypes(HUNTER);
    HUNTER_LODGE.worksBeforeBuilt = true;
    HUNTER_LODGE.maxWorkers = 2;
    HUNTER_LODGE.maxHealth  = 100;
    HUNTER_LODGE.produced   = new Good[] { PROTEIN };
    
    GARRISON.name = "Garrison";
    GARRISON.tint = TINT_MILITARY;
    GARRISON.setDimensions(6, 6, 2);
    GARRISON.setBuildMaterials(PLASTICS, 1, PARTS, 7);
    GARRISON.setWorkerTypes(SOLDIER);
    GARRISON.maxWorkers = 2;
    GARRISON.maxHealth  = 250;
    
    //
    //  Science structures:
    ENGINEER_STATION.name = "Engineer Station";
    ENGINEER_STATION.tint = TINT_INDUSTRIAL;
    ENGINEER_STATION.setDimensions(2, 2, 1);
    ENGINEER_STATION.setBuildMaterials(PARTS, 8);
    ENGINEER_STATION.setWorkerTypes(WORKER);
    ENGINEER_STATION.needed   = new Good[] { ORES };
    ENGINEER_STATION.produced = new Good[] { PARTS };
    ENGINEER_STATION.maxStock = 3;
    ENGINEER_STATION.craftSkill = SKILL_CRAFT;
    
    PHYSICIAN_STATION.name = "Physician Station";
    PHYSICIAN_STATION.tint = TINT_INDUSTRIAL;
    PHYSICIAN_STATION.setDimensions(2, 2, 1);
    PHYSICIAN_STATION.setBuildMaterials(PLASTICS, 4, PARTS, 2);
    PHYSICIAN_STATION.setWorkerTypes(WORKER);
    PHYSICIAN_STATION.needed   = new Good[] { GREENS };
    PHYSICIAN_STATION.produced = new Good[] { MEDICINE };
    PHYSICIAN_STATION.maxStock = 3;
    PHYSICIAN_STATION.craftSkill = SKILL_CRAFT;
    PHYSICIAN_STATION.setFeatures(HEALTHCARE);
    PHYSICIAN_STATION.featureAmount = 20;
    
    //
    //  Religious structures:
    //
    //  Day/Order and Night/Freedom : Qz / Tz
    TEMPLE_QZ .name = "Temple to Quetzalcoatl";
    TEMPLE_TZ .name = "Temple to Tezcatlipoca";
    //
    //  Fire and Water: Hu / Tl
    TEMPLE_HU .name = "Temple to Huitzilopochtli";
    TEMPLE_TL .name = "Temple to Tlaloc";
    //
    //  Life and Death: Om / Mi
    TEMPLE_MI .name = "Temple to Mictecacehuatl";
    //
    //  Balancing/tension: Xt
    TEMPLE_XT .name = "Temple to Xipe Totec";
    
    for (Type t : PSI_SCHOOL_BUILDINGS) {
      t.tint = TINT_RELIGIOUS;
      t.setDimensions(6, 6, 3);
      t.setBuildMaterials(PARTS, 15);
      t.setWorkerTypes(PRIEST);
      t.maxWorkers      = 1;
      t.maxHealth       = 100;
      t.maxResidents    = 1;
      t.homeSocialClass = CLASS_NOBLE;
      t.features        = new Good[] { RELIGION };
    }
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
      GARRISON, 2f ,
      HOUSE   , 10f
    );
    world.addCities(cityA);
    
    cityB.setName("Tlacopan");
    cityB.setWorldCoords(3, 3);
    cityB.initTradeLevels(
      CARBS, 5f ,
      ORES , 10f
    );
    cityA.initBuildLevels(
      GARRISON, 0.75f,
      HOUSE   , 5f
    );
    world.addCities(cityB);
    
    City.setupRoute(cityA, cityB, AVG_CITY_DIST / 2);
    world.setMapSize(10, 10);
    
    return world;
  }
  
}





