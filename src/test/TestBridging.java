


package test;
import util.*;
import game.*;
import static content.GameContent.*;
import static content.GameWorld.FACTION_SETTLERS;
import static game.GameConstants.*;




public class TestBridging extends LogicTest {
  
  
  public static void main(String args[]) {
    testBridging(true);
  }
  
  
  static boolean testBridging(boolean graphics) {
    LogicTest test = new TestBridging();
    
    Terrain terrTypes[] = { LAKE, MEADOW, JUNGLE };
    Base base = setupTestBase(FACTION_SETTLERS, ALL_GOODS, 16, false, terrTypes);
    Area map = base.activeMap();
    World world = base.world;
    world.settings.toggleFog     = false;
    world.settings.toggleFatigue = false;
    world.settings.toggleHunger  = false;
    
    //
    //  Configure some artificially partitioned terrain:
    byte terrIDs[] = {
      1, 1, 1, 1,   2, 2, 0, 0,   0, 0, 2, 2,   2, 2, 2, 2
    };
    byte elevation[] = {
      1, 1, 1, 1,   0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0
    };
    for (AreaTile t : map.allTiles()) {
      Terrain ter = terrTypes[terrIDs[t.y]];
      int high = elevation[t.y];
      map.setTerrain(t, ter, (byte) 0, high);
    }
    for (AreaTile t : map.tilesUnder(4, 0, 2, 8)) {
      map.setTerrain(t, LAKE, (byte) 0, t.elevation());
    }
    
    
    Building palace = (Building) BASTION.generate();
    palace.enterMap(map, 6, 8, 1, base);
    for (Good g : palace.type().buildsWith) {
      palace.setInventory(g, 100);
    }
    ActorUtils.fillWorkVacancies(palace);
    
    
    AreaPlanning.placeStructure(SHIELD_WALL, base, false, 14, 2, 2, 10);
    AreaPlanning.placeStructure(WALKWAY, base, false, 2 , 2, 1, 10);
    
    Building tower1 = (Building) TURRET.generate();
    tower1.setFacing(TileConstants.E);
    map.planning.placeObject(tower1, 14, 12, base);
    
    Building tower2 = (Building) TURRET.generate();
    tower2.setFacing(TileConstants.E);
    map.planning.placeObject(tower2, 14, 2, base);
    
    Building kiln = (Building) ENGINEER_STATION.generate();
    map.planning.placeObject(kiln, 9, 1, base);
    
    //
    //  First, we simulate the entire construction process by picking
    //  accessible buildings at random and increment their construction
    //  progress, while checking to ensure there are no discrepancies
    //  in pathing afterward.
    map.planning.updatePlanning();
    List <Element> toBuild = map.planning.toBuildCopy();
    Pick <Element> pick = new Pick();
    
    while (! toBuild.empty()) {
      
      pick.clear();
      for (Element e : toBuild) {
        if (e.at() == null) {
          I.say("REMOVED ELEMENTS SHOULD NOT BE ON PLANNING MAP!");
          pick.clear();
          break;
        }
        
        if (! map.pathCache.pathConnects(palace, e, true, false)) continue;
        pick.compare(e, Rand.index(16));
      }
      
      Element builds = pick.result();
      if (builds == null) break;
      
      AreaTile at = builds.at();
      Type type = builds.type();
      if (! builds.onMap()) builds.enterMap(map, at.x, at.y, 0, base);
      
      float l = builds.buildLevel();
      for (Good b : type.builtFrom) {
        builds.setMaterialLevel(b, builds.materialNeed(b) * (l + 0.5f));
      }
      
      if (builds.complete()) toBuild.remove(builds);
      map.pathCache.updatePathCache();
      
      for (AreaTile t : map.tilesUnder(
        at.x - 1,
        at.y - 1,
        type.wide + 2,
        type.high + 2
      )) if (t != null && ! testPathing(t, palace, map)) {
        return false;
      }
    }
    
    //
    //  If there are any unbuilt structures remaining, give a report on
    //  current status-
    
    if (! toBuild.empty()) {
      I.say("\nBRIDGING TEST FAILED- NOT ALL STRUCTURES WERE ACCESSIBLE!");
      I.say("  Buildings are: ");
      for (Building b : map.buildings()) {
        I.say("  "+b+" at: "+b.at()+", entrances:");
        for (AreaTile e : b.entrances()) I.say("    "+e);
      }
      
      I.say("  Still need to build: ");
      for (Element e : toBuild) I.say("  "+e+" at "+e.at());
      
      if (! graphics) return false;
    }
    
    //  Otherwise, we still need to ensure that actor-based construction will
    //  work.  So we tear down all of these structures, and get them ready to
    //  be rebuilt.
    else {
      Batch <Element> freshBuilt = new Batch();
      for (Element e : map.planning.toBuildCopy()) {
        if (e == palace) continue;
        
        AreaTile at = e.at();
        map.planning.unplaceObject(e);
        e.exitMap(map);
        
        Element copy = (Element) e.type().generate();
        copy.setFacing(e.facing());
        copy.setLocation(at, map);
        copy.assignBase(e.base());
        freshBuilt.add(copy);
        
        if (e == tower1) tower1 = (Building) copy;
        if (e == tower2) tower2 = (Building) copy;
        if (e == kiln  ) kiln   = (Building) copy;
      }
      
      for (Element e : freshBuilt) {
        map.planning.placeObject(e);
      }
    }
    
    //
    //  Run simulation:
    final int RUN_TIME = YEAR_LENGTH;
    boolean buildOkay  = false;
    boolean accessOkay = false;
    boolean testOkay   = false;
    
    
    while (map.time() < RUN_TIME || graphics) {
      test.runLoop(base, 1, graphics, "saves/test_build_path.tlt");
      
      if (! buildOkay) {
        boolean buildDone = true;
        for (AreaTile t : map.allTiles()) {
          Element a = map.above(t);
          if (a == null || a.type().isNatural()) continue;
          if (a.buildLevel() < 1) buildDone = false;
        }
        buildOkay = buildDone;
      }
      
      if (! accessOkay) {
        AreaTile from = map.tileAt(1, 1), goes = map.tileAt(15, 0);
        
        boolean connected = true;
        connected &= map.pathCache.pathConnects(from, goes);
        
        accessOkay = connected;
      }
      
      if (accessOkay && buildOkay && ! testOkay) {
        I.say("\nBRIDGING TEST CONCLUDED SUCCESSFULLY!");
        testOkay = true;
        if (! graphics) return true;
      }
    }
    
    I.say("\nBRIDGING TEST FAILED!");
    I.say("  Build okay:  "+buildOkay );
    I.say("  Access okay: "+accessOkay);
    
    return false;
  }
  
  
  private static boolean testPathing(
    AreaTile t, Building base, Area map
  ) {
    ActorPathSearch s = new ActorPathSearch(map, t, base, -1);
    s.doSearch();
    ActorPathSearch b = new ActorPathSearch(map, base, t, -1);
    b.doSearch();
    
    boolean pathOK   = s.success();
    boolean backOK   = b.success();
    boolean mapsOK   = map.pathCache.pathConnects(t, base.mainEntrance());
    boolean groundOK = map.pathCache.hasGroundAccess(t);
    Element above    = map.above(t);
    
    if (pathOK != backOK || (pathOK && backOK) != mapsOK) {
      I.say("\nBRIDGING TEST FAILED- MISMATCH IN PATHING OBSERVED AT "+t);
      I.say("  Path type:   "+t.pathType());
      I.say("  Above:       "+above);
      I.say("  Build level: "+(above == null ? 0 : above.buildLevel()));
      I.say("  Tile to Home OK: "+pathOK  );
      I.say("  Home to Tile OK: "+backOK  );
      I.say("  Map connection:  "+mapsOK  );
      I.say("  Ground access:   "+groundOK);
      return false;
    }
    else return true;
  }
  
}










