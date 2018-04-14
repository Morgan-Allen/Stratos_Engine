


package content;
import game.*;
import start.*;
import util.*;
import static game.GameConstants.*;
import static content.GameContent.*;



public class ScenarioBlankMap extends AreaScenario {
  
  
  List <BuildingForNest> nests = new List();
  
  
  public ScenarioBlankMap() {
    super();
  }
  
  
  public ScenarioBlankMap(Session s) throws Exception {
    super(s);
    s.loadObjects(nests);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(nests);
  }
  
  
  
  protected String savePath() {
    return "saves/Test_Blank_Map.str";
  }
  
  
  protected World createWorld() {
    World world = new World(ALL_GOODS);
    world.assignTypes(
      ALL_BUILDINGS,
      ALL_CITIZENS(),
      ALL_SOLDIERS(),
      ALL_NOBLES()
    );
    world.assignMedia(
      World.KEY_ATTACK_FLAG , FLAG_STRIKE,
      World.KEY_EXPLORE_FLAG, FLAG_RECON
    );
    
    //world.settings.toggleFog = false;
    
    return world;
  }
  
  
  protected Area createArea(World world) {
    
    final int MAP_SIZE = 64;
    final Terrain GRADIENT[] = { JUNGLE, MEADOW, DESERT };
    
    World.Locale locale = world.addLocale(5, 5, "Test Area");
    Area map = AreaTerrain.generateTerrain(world, locale, MAP_SIZE, 0, GRADIENT);
    AreaTerrain.populateFixtures(map);
    return map;
  }
  
  
  protected Base createBase(Area stage, World world) {
    World.Locale homeworld = world.addLocale(1, 1, "Homeworld");
    World.setupRoute(homeworld, stage.locale, 1, Type.MOVE_AIR);
    
    Base patron = new Base(world, homeworld);
    patron.setName("Homeworld Base");
    
    Base landing = new Base(world, stage.locale);
    landing.setName("Player Landing");
    landing.setHomeland(patron);
    landing.assignBuildTypes(RULER_BUILT);
    
    stage.addBase(landing);
    world.addBases(patron, landing);
    
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
      boolean canPlace = true;
      
      for (AreaTile f : stage.tilesUnder(t.x - 1, t.y - 1, w + 2, h + 2)) {
        if (f == null || f.terrain().pathing != Type.PATH_FREE) {
          canPlace = false;
          break;
        }
      }
      if (! canPlace) continue;
      
      AreaTile sited = stage.tileAt(t.x + (w / 2), t.y + (h / 2));
      float dist = Area.distance(sited, ideal);
      pickLanding.compare(t, 0 - dist);
    }
    
    //
    //  And insert the Bastion at the appropriate site:
    if (! pickLanding.empty()) {
      BuildingForTrade depot = (BuildingForTrade) SUPPLY_DEPOT.generate();
      
      AreaTile at = pickLanding.result();
      AreaPlanning.markDemolish(stage, true, at.x - 1, at.y - 1, w + 2, h + 2);
      AreaPlanning.markDemolish(stage, true, at.x - 6, at.y - 1, 5, 5);
      bastion.enterMap(stage, at.x, at.y, 1, base);
      depot.enterMap(stage, at.x, at.y - 5, 1, base);
      
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
  }
  
  
  
  public void updateScenario() {
    super.updateScenario();
  }
  
}




