


package content;
import game.*;
import start.*;
import util.*;
import static game.GameConstants.*;
import static content.GameContent.*;



public class ScenarioBlankMap extends CityMapScenario {
  
  
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
    return world;
  }
  
  
  protected AreaMap createStage(World world) {
    
    final int MAP_SIZE = 64;
    final Terrain GRADIENT[] = { JUNGLE, MEADOW, DESERT };
    
    World.Locale locale = world.addLocale(5, 5, "Test Area");
    AreaMap map = CityMapTerrain.generateTerrain(world, locale, MAP_SIZE, 0, GRADIENT);
    CityMapTerrain.populateFixtures(map);
    return map;
  }
  
  
  protected Base createBase(AreaMap stage, World world) {
    World.Locale homeworld = world.addLocale(1, 1, "Homeworld");
    
    Base patron = new Base(world, homeworld);
    patron.setName("Homeworld Base");
    
    Base landing = new Base(world, stage.locale);
    landing.setName("Player Landing");
    landing.setHomeland(patron);
    stage.addCity(landing);
    
    world.addCities(patron, landing);
    
    return landing;
  }
  
  
  protected void configScenario(World world, AreaMap stage, Base base) {
    
    Building bastion = (Building) BASTION.generate();
    int w = bastion.type().wide, h = bastion.type().high;
    Tile centre = stage.tileAt(stage.size() / 2, stage.size() / 2);
    Pick <Tile> pickLanding = new Pick();
    
    //
    //  Look for a suitable entry-point within the world...
    for (Tile t : stage.allTiles()) {
      
      boolean canPlace = true;
      
      for (Tile f : stage.tilesUnder(t.x - 1, t.y - 1, w + 2, h + 2)) {
        if (f == null || f.terrain().pathing != AreaMap.PATH_FREE) {
          canPlace = false;
          break;
        }
      }
      if (! canPlace) continue;
      
      Tile sited = stage.tileAt(t.x + (w / 2), t.y + (h / 2));
      float dist = AreaMap.distance(sited, centre);
      pickLanding.compare(t, 0 - dist);
    }
    
    //
    //  And insert the Bastion at the appropriate site:
    if (! pickLanding.empty()) {
      Tile at = pickLanding.result();
      CityMapPlanning.markDemolish(stage, true, at.x - 1, at.y - 1, w + 2, h + 2);
      bastion.enterMap(stage, at.x, at.y, 1, base);
      
      base.initFunds(4000);
      bastion.addInventory(100, PLASTICS);
      bastion.addInventory(100, PARTS   );
      
      Test.fillWorkVacancies(bastion);
      playUI().assignHomePoint(bastion);
    }
    
    
    //  TODO:
    //  Ideally, you want to place the lairs in sectors where you don't find
    //  the main settlement.
    
    class SiteOption { Tile at; float rating; }
    List <SiteOption> options = new List <SiteOption> () {
      protected float queuePriority(SiteOption r) {
        return r.rating;
      }
    };
    for (Coord c : Visit.grid(0, 0, stage.size(), stage.size(), 8)) {
      Tile at = stage.tileAt(c);
      float dist = AreaMap.distance(bastion, at);
      if (dist <= 16) continue;
      
      float rating = 16f * Rand.num() * dist;
      SiteOption option = new SiteOption();
      option.at = at;
      option.rating = rating;
      options.add(option);
    }
    
    options.queueSort();
    
    int NUM_LAIRS = 3;
    Object spawnArgs[] = { TRIPOD, 0.33f, DRONE, 0.66f };
    
    for (int n = NUM_LAIRS; n-- > 0;) {
      SiteOption o = options.removeFirst();
      BuildingForNest nest = (BuildingForNest) RUINS_LAIR.generate();
      nest.enterMap(stage, o.at.x, o.at.y, 1, stage.locals);
      nest.assignSpawnParameters(MONTH_LENGTH, 4, spawnArgs);
      nests.add(nest);
    }
    
    Base.setPosture(base, stage.locals, Base.POSTURE.ENEMY, true);
  }
  
  
  
  public void updateScenario() {
    super.updateScenario();
  }
  
}




