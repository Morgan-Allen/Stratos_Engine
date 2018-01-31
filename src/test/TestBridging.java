


package test;
import util.*;
import game.*;
import static content.GameContent.*;
import static game.GameConstants.*;




public class TestBridging extends Test {
  
  
  public static void main(String args[]) {
    testBridging(true);
  }
  
  
  static boolean testBridging(boolean graphics) {
    Test test = new TestBridging();
    
    Terrain terrTypes[] = { LAKE, MEADOW, JUNGLE };
    CityMap map = setupTestCity(16, ALL_GOODS, false, terrTypes);
    World world = map.city.world;
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
    for (Tile t : map.allTiles()) {
      Terrain ter = terrTypes[terrIDs[t.y]];
      int high = elevation[t.y];
      map.setTerrain(t, ter, (byte) 0, high);
    }
    for (Tile t : map.tilesUnder(4, 0, 2, 8)) {
      map.setTerrain(t, LAKE, (byte) 0, t.elevation());
    }
    
    
    Building palace = (Building) BASTION.generate();
    palace.enterMap(map, 8, 10, 1);
    for (Good g : palace.type().buildsWith) {
      palace.setInventory(g, 100);
    }
    fillWorkVacancies(palace);
    
    
    CityMapPlanning.placeStructure(SHIELD_WALL, map, false, 14, 2, 2, 10);
    CityMapPlanning.placeStructure(WALKWAY, map, false, 2 , 2, 1, 10);
    
    Building tower1 = (Building) TURRET.generate();
    tower1.setFacing(TileConstants.E);
    map.planning.placeObject(tower1, 14, 12);
    
    Building tower2 = (Building) TURRET.generate();
    tower2.setFacing(TileConstants.E);
    map.planning.placeObject(tower2, 14, 2);
    
    Building kiln = (Building) ENGINEER_STATION.generate();
    map.planning.placeObject(kiln, 10, 2);
    
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
      
      Tile at = builds.at();
      Type type = builds.type();
      if (! builds.onMap()) builds.enterMap(map, at.x, at.y, 0);
      
      float l = builds.buildLevel();
      for (Good b : type.builtFrom) {
        builds.setMaterialLevel(b, builds.materialNeed(b) * (l + 0.5f));
      }
      
      if (builds.complete()) toBuild.remove(builds);
      map.pathCache.updatePathCache();
      
      for (Tile t : map.tilesUnder(
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
      I.say("\nNOT ALL STRUCTURES WERE ACCESSIBLE!");
      I.say("  Buildings are: ");
      for (Building b : map.buildings()) {
        I.say("  "+b+" at: "+b.at()+", entrances:");
        for (Tile e : b.entrances()) I.say("    "+e);
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
        Tile at = e.at();
        map.planning.unplaceObject(e);
        e.exitMap(map);
        
        Element copy = (Element) e.type().generate();
        if (e.type().isBuilding()) {
          ((Building) copy).setFacing(((Building) e).facing());
        }
        copy.setLocation(at, map);
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
      map = test.runLoop(map, 1, graphics, "saves/test_build_path.tlt");
      
      if (! buildOkay) {
        boolean buildDone = true;
        for (Tile t : map.allTiles()) {
          Element a = map.above(t);
          if (a == null || a.type().isNatural()) continue;
          if (a.buildLevel() < 1) buildDone = false;
        }
        buildOkay = buildDone;
      }
      
      if (! accessOkay) {
        Tile from = map.tileAt(1, 1), goes = map.tileAt(15, 0);
        
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
    Tile t, Building base, CityMap map
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
      I.say("\nMISMATCH IN PATHING OBSERVED AT "+t);
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










