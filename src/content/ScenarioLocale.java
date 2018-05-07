


package content;
import game.*;
import start.*;
import util.*;
import static game.GameConstants.*;
import static content.GameContent.*;



public class ScenarioLocale extends Scenario {
  
  
  static class NestConfig {
    BuildType nestType;
    Tally <ActorType> spawnTypes;
    int spawnInterval;
    boolean doRaids;
    boolean doContact;
  }
  
  static class SiteConfig {
    Faction belongs;
    NestConfig nest;
    int minCount, maxCount;
    int dirX, dirY;
    float areaNeed;
    List <SiteConfig> children = new List();
  }
  
  static class LocaleConfig {
    WorldLocale locale;
    int mapSize;
    Terrain gradient[];
    List <SiteConfig> sites = new List();
  }
  
  
  Faction faction;
  LocaleConfig config;
  
  String savePath = "saves/save_locale.str";
  
  List <BuildingForNest> nests = new List();
  
  
  
  public ScenarioLocale(Faction faction, LocaleConfig config) {
    super();
    this.faction = faction;
    this.config  = config ;
  }
  
  
  public ScenarioLocale(Session s) throws Exception {
    super(s);
    s.loadObjects(nests);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(nests);
  }
  
  
  
  protected String savePath() {
    return savePath;
  }
  
  
  protected World createWorld() {
    return GameContent.setupDefaultWorld();
  }
  
  
  protected Area createArea(World world) {
    
    int mapSize = config.mapSize;
    Terrain gradient[] = config.gradient;
    
    WorldLocale locale = world.addLocale(5, 5, "Test Area");
    Area map = AreaTerrain.generateTerrain(world, locale, mapSize, 0, gradient);
    AreaTerrain.populateFixtures(map);
    return map;
  }
  
  
  protected Base createBase(Area stage, World world) {
    
    Base patron = world.baseAt(faction.homeland());
    Base landing = new Base(world, stage.locale);
    landing.setName("Player Landing");
    landing.setHomeland(patron);
    landing.assignBuildTypes(RULER_BUILT);

    stage.addBase(landing);
    world.addBases(landing);
    
    Base.setPosture(patron, landing, Base.POSTURE.VASSAL, true);
    patron.updateOffmapTraders();
    
    return landing;
  }
  
  
  protected void configScenario(World world, Area stage, Base base) {

    Building bastion = (Building) BASTION.generate();
    int w = bastion.type().wide, h = bastion.type().high;
    AreaTile ideal = stage.tileAt(16, 16);
    Pick <AreaTile> pickLanding = new Pick();
    
    //
    //  Look for a suitable entry-point within the world...
    for (AreaTile t : stage.allTiles()) {
      bastion.setLocation(t, stage);
      if (! bastion.canPlace(stage)) continue;
      
      AreaTile sited = stage.tileAt(t.x + (w / 2), t.y + (h / 2));
      float dist = Area.distance(sited, ideal);
      pickLanding.compare(t, 0 - dist);
    }
    
    
    
    
    /*
    
    //
    //  And insert the Bastion at the appropriate site:
    if (! pickLanding.empty()) {
      AreaTile at = pickLanding.result();
      bastion.enterMap(stage, at.x, at.y, 1, base);

      BuildingForTrade depot = (BuildingForTrade) SUPPLY_DEPOT.generate();
      //at = ActorUtils.findEntryPoint(depot, stage, bastion, -1);
      depot.enterMap(stage, at.x, at.y - 5, 1, base);
      
      //BuildingForFaith school = (BuildingForFaith) SCHOOL_COL.generate();
      //at = ActorUtils.findEntryPoint(school, stage, bastion, -1);
      //school.enterMap(stage, at.x, at.y, 1, base);
      
      ActorUtils.fillWorkVacancies(bastion);
      ActorUtils.fillWorkVacancies(depot);
      base.setHeadquarters(bastion);
      playUI().setLookPoint(bastion);
      
      base.initFunds(4000);
      bastion.addInventory(35, PLASTICS);
      bastion.addInventory(15, PARTS   );
      depot.needLevels().set(CARBS, 20);
    }
    
    //  TODO:
    //  Ideally, you want to place the lairs in sectors where you don't find
    //  the main settlement.
    
    final int MIN_LAIR_DIST = 32;
    final int NUM_LAIRS = 1;
    
    class SiteOption { AreaTile at; float rating; }
    List <SiteOption> options = new List <SiteOption> () {
      protected float queuePriority(SiteOption r) {
        return r.rating;
      }
    };
    for (Coord c : Visit.grid(0, 0, stage.size(), stage.size(), 8)) {
      AreaTile at = stage.tileAt(c);
      float dist = Area.distance(bastion, at);
      if (dist <= MIN_LAIR_DIST) continue;
      
      float rating = 16f * Rand.num() * dist;
      SiteOption option = new SiteOption();
      option.at = at;
      option.rating = rating;
      options.add(option);
    }
    
    options.queueSort();
    
    //Object spawnArgs[] = { TRIPOD, 0.33f, DRONE, 0.66f };
    Object spawnArgs[] = { DRONE, 1 };
    
    for (int n = NUM_LAIRS; n-- > 0;) {
      SiteOption o = options.removeFirst();
      BuildingForNest nest = (BuildingForNest) RUINS_LAIR.generate();
      nest.enterMap(stage, o.at.x, o.at.y, 1, stage.locals);
      nest.assignSpawnParameters(DAY_LENGTH * 3, 2, false, spawnArgs);
      nests.add(nest);
    }
    
    Base.setPosture(base, stage.locals, Base.POSTURE.ENEMY, true);
    //*/
    return;
  }
  
  
  
  public void updateScenario() {
    super.updateScenario();
  }
  
}




