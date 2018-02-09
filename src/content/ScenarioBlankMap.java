


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
  
  
  protected CityMap createStage(World world) {
    
    final int MAP_SIZE = 64;
    final Terrain GRADIENT[] = { JUNGLE, MEADOW, DESERT };
    
    World.Locale locale = world.addLocale(5, 5, "Test Area");
    CityMap map = CityMapTerrain.generateTerrain(world, locale, MAP_SIZE, 0, GRADIENT);
    CityMapTerrain.populateFixtures(map);
    return map;
  }
  
  
  protected City createBase(CityMap stage, World world) {
    City city = new City(world, stage.locale);
    city.setName("Player Base");
    stage.addCity(city);
    return city;
  }
  
  
  protected void configScenario(World world, CityMap stage, City base) {
    
    Building bastion = (Building) BASTION.generate();
    int w = bastion.type().wide, h = bastion.type().high;
    Tile centre = stage.tileAt(stage.size() / 2, stage.size() / 2);
    Pick <Tile> pickLanding = new Pick();
    
    //
    //  Look for a suitable entry-point within the world...
    for (Tile t : stage.allTiles()) {
      
      boolean canPlace = true;
      
      for (Tile f : stage.tilesUnder(t.x - 1, t.y - 1, w + 2, h + 2)) {
        if (f == null || f.terrain().pathing != CityMap.PATH_FREE) {
          canPlace = false;
          break;
        }
      }
      if (! canPlace) continue;
      
      Tile sited = stage.tileAt(t.x + (w / 2), t.y + (h / 2));
      float dist = CityMap.distance(sited, centre);
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
    
    //
    //  TODO:  Insert lairs and add those to a list for subsequent spawning
    //  updates!
    
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
      float dist = CityMap.distance(bastion, at);
      if (dist <= 16) continue;
      
      float rating = 16f * Rand.num() * dist;
      SiteOption option = new SiteOption();
      option.at = at;
      option.rating = rating;
      options.add(option);
    }
    
    options.queueSort();
    
    int NUM_LAIRS = 3;
    for (int n = NUM_LAIRS; n-- > 0;) {
      SiteOption o = options.removeFirst();
      BuildingForNest nest = (BuildingForNest) RUINS_LAIR.generate();
      nest.enterMap(stage, o.at.x, o.at.y, 1, stage.locals);
      nests.add(nest);
    }
    
    City.setPosture(base, stage.locals, City.POSTURE.ENEMY, true);
  }
  
  
  
  public void updateScenario() {
    super.updateScenario();
    
    //  TODO:  Update spawning routines.  And arrange for an assault of some
    //  kind upon the player's settlement.
    
    //  TODO:  Move this out to the Nest class itself!
    
    int time = stage().time();
    
    for (BuildingForNest nest : nests) {
      if ((time + (nest.varID() * 25)) % 100 == 0) {
        if (nest.residents().size() < 4) {
          ActorType spawned = Rand.yes() ? TRIPOD : DRONE;
          Test.spawnWalker(nest, spawned, true);
        }
        else {
          
          Pick <Building> pick = new Pick();
          for (Building b : nest.map().buildings()) {
            if (! TaskCombat.hostile(b, nest)) continue;
            if (! nest.map().pathCache.pathConnects(nest, b, true, false)) continue;
            
            float rating = 1f;
            rating *= CityMap.distancePenalty(nest, b);
            pick.compare(b, rating);
          }
          
          if (! pick.empty()) {
            Mission assault = new Mission(
              Mission.OBJECTIVE_CONQUER, nest.homeCity(), false
            );
            assault.setFocus(pick.result(), 0, nest.map());
          }
        }
      }
    }
    
    
  }
  
}















