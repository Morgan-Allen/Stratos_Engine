


package content;
import game.*;
import start.CityMapScenario;
import util.*;
import static game.GameConstants.*;
import static content.GameContent.*;



public class ScenarioBlankMap extends CityMapScenario {
  
  
  protected String savePath() {
    return "Test_Blank_Map.str";
  }
  
  
  protected World createWorld() {
    World world = new World(ALL_GOODS);
    world.assignTypes(ALL_BUILDINGS, ALL_CITIZENS, ALL_SOLDIERS, ALL_NOBLES);
    return world;
  }
  
  
  protected CityMap createStage(World world) {
    City city = new City(world);
    city.setName("Player Base");
    
    final int MAP_SIZE = 64;
    final Terrain GRADIENT[] = { JUNGLE, MEADOW, DESERT };
    
    CityMap map = CityMapTerrain.generateTerrain(city, MAP_SIZE, 0, GRADIENT);
    CityMapTerrain.populateFixtures(map);
    return map;
  }
  
  
  protected City createBase(CityMap stage, World verse) {
    return stage.city;
  }
  
  
  protected void configScenario(World verse, CityMap stage, City base) {
    
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
      bastion.enterMap(stage, at.x, at.y, 1);
      
      base.initFunds(4000);
      bastion.addInventory(100, PLASTICS);
      bastion.addInventory(100, PARTS   );
      
      playUI().assignHomePoint(bastion);
    }
    
  }
  
  
  
}





/*

package start;
import content.civic.*;
import content.wild.*;
import game.stage.*;
import game.maps.*;
import game.plans.*;
import game.actor.*;
import game.venue.*;
import game.verse.*;
import gameUI.play.*;
import util.*;
import static content.campaign.MainSetting.*;



public class DebugStage extends Scenario implements
  Spawning.Control, Siting.Control
{
  
  
  public static void main(String args[]) {
    MainGame.playScenario(new DebugStage());
  }
  
  
  public DebugStage() {
  }
  
  
  public DebugStage(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  protected String savePath() {
    return "saves/debug_stage";
  }
  
  
  protected Verse createVerse() {
    return new Verse(ALL_SECTORS);
  }
  
  
  protected Stage createStage(Verse verse) {
    Habitat habitats[] = {
      Habitat.FOREST,
      Habitat.MEADOW,
      Habitat.BARRENS,
      Habitat.DUNE
    };
    float weights[] = { 1, 1, 1, 1 };
    return new StageSetup().generateStage(96, 73, 24, habitats, weights);
  }
  
  
  protected Base createBase(Stage stage, Verse verse) {
    return FACTION_SUHAIL.createBase(SECTOR_LOCAL, verse);
  }
  
  
  protected void configScenario(Verse verse, Stage stage, Base base) {
    
    //  Phase 3:  The overall campaign-structure, planet-map and maybe some
    //            trade dynamics and resource-mixing?
    
    //  Trees should appear, at least.
    //  Animation-states have to be non-jerky.
    
    //  Polish the existing content, in any case.  Multiple levels, proper
    //  shading & scaffolds, more sovereign spells, full items & techniques.
    
    //  Manufacture, gathering, trade, dialogue.  A reputation system.
    
    base.initCredits(9000);
    
    Siting siteBase = new Siting("Main Base", stage, base.faction(), this);
    siteBase.assignToPlace(
      16, 50, TileConstants.W,
      Bastion     .BLUEPRINT.sampleFor(base.faction()),
      TrooperLodge.BLUEPRINT.sampleFor(base.faction())
    );
    siteBase.performPlacements();
    
    
    Spawning spawning = new Spawning(
      "Roaches!", FACTION_CRITTER, this
    );
    spawning.setupForces(1, GiantRoach.SPECIES);
    spawning.findEntryOnBorders(TileConstants.E, 30);
    spawning.assignInterval(10, false);
    stage.addEvent(spawning);
    
    final Nest nest = new Nest(
      Vermin.NEST_BLUEPRINT, FACTION_CRITTER,
      3.5f, Stage.STANDARD_DAY_LENGTH, GiantRoach.SPECIES, Roachman.SPECIES
    );
    final Siting siting = new Siting(
      "Roach Nests!", stage, nest.faction(), nest.blueprint()
    );
    siting.assignToPlace(16, 50, TileConstants.NE, nest);
    siting.performPlacements();
    
  }
  
  
  public float ratePlacing(Siting s, Spot from, int resolution) {
    return 1;
  }
  
  
  public void onPlacement(Element placed, Siting s) {
    I.say("PLACED "+placed+" AT "+placed.origin());
    if (placed.kind() == Bastion.BLUEPRINT) {
      base.fogMap().liftFogAround(placed, 16);
      base.assignHQ((Bastion) placed);
      
      UI.assignHomePoint(placed);
      PlayUI.pushSelection(placed);
    }
  }


  public void onSpawning(Spawning s) {
    
    Pick <Venue> pick = new Pick();
    for (Venue v : stage.allVenues()) {
      if (v.faction() == s.faction) continue;
      pick.compare(v, 0 - Stage.roughDistance(v, s.enterPoint()));
    }
    
    Base base = stage.baseFor(s.faction);
    Venue siege = pick.result();
    Mission strike = MissionStrike.strikeFor(siege, base);
    if (strike == null) return;
    
    strike.setMotiveBonus(Plan.PARAMOUNT, true);
    for (Actor a : s.spawning()) a.assignMission(strike);
  }
  
  
  public Stage stage() {
    return stage;
  }
  
}
//*/










