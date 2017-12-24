
package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class TestBridging extends Test {
  
  
  public static void main(String args[]) {
    testBridging(true);
  }
  
  
  static boolean testBridging(boolean graphics) {
    Test test = new TestBridging();
    CityMap map = setupTestCity(16);
    map.settings.toggleFog     = false;
    map.settings.toggleFatigue = false;
    map.settings.toggleHunger  = false;
    
    //
    //  Configure some artificially partitioned terrain:
    Terrain terrTypes[] = { LAKE, MEADOW, JUNGLE };
    byte terrIDs[] = {
      1, 1, 1, 1,   2, 2, 0, 0,   0, 0, 2, 2,   2, 2, 2, 2
    };
    byte elevation[] = {
      1, 1, 1, 1,   0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0
    };
    for (Tile t : map.allTiles()) {
      Terrain ter = terrTypes[terrIDs[t.y]];
      int high = elevation[t.y];
      map.setTerrain(t, ter, high);
    }
    for (Tile t : map.tilesUnder(4, 1, 2, 2)) {
      map.setTerrain(t, LAKE, t.elevation);
    }
    
    //
    //  Set up some essential construction facilities:
    Building mason1 = (Building) MASON.generate();
    Building mason2 = (Building) MASON.generate();
    mason1.enterMap(map, 8, 12, 1);
    mason2.enterMap(map, 6, 12, 1);
    for (Good g : mason1.type.buildsWith) {
      mason1.inventory.set(g, 100);
      mason2.inventory.set(g, 100);
    }
    fillWorkVacancies(mason1);
    fillWorkVacancies(mason2);
    
    //
    //  Test to ensure that water does not collect in cisterns away
    //  from a water-source:
    Building cistern0 = (Building) CISTERN.generate();
    map.planning.placeObject(cistern0, 8, 0);
    
    //  ...and can flow along connected aqueducts, but not uphill:
    Building cistern1 = (Building) CISTERN.generate();
    map.planning.placeObject(cistern1, 1, 0);
    
    Building cistern2 = (Building) CISTERN.generate();
    map.planning.placeObject(cistern2, 12, 10);
    
    Building basin1 = (Building) BASIN.generate();
    map.planning.placeObject(basin1, 2, 13);
    
    Building basin2 = (Building) BASIN.generate();
    map.planning.placeObject(basin2, 14, 2);
    
    CityMapPlanning.placeStructure(AQUEDUCT, map, false , 2, 3, 1, 10);
    CityMapPlanning.placeStructure(AQUEDUCT, map, false, 15, 2, 1, 10);
    
    //
    //  First, we simulate the entire construction process by picking
    //  accessible buildings at random and increment their construction
    //  progress, while checking to ensure there are no discrepancies
    //  in pathing afterward.
    List <Element> toBuild = map.planning.toBuild.copy();
    Pick <Element> pick = new Pick();
    
    while (! toBuild.empty()) {
      
      pick.clear();
      for (Element e : toBuild) {
        if (! map.pathCache.pathConnects(mason1, e, true, false)) continue;
        pick.compare(e, Rand.index(16));
      }
      
      Element builds = pick.result();
      if (builds == null) break;
      
      Tile at = builds.at();
      Type type = builds.type;
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
      )) if (t != null && ! testPathing(t, mason1, map)) {
        return false;
      }
    }
    
    //
    //  If there are any unbuilt structures remaining, give a report on
    //  current status-
    
    if (! toBuild.empty()) {
      I.say("\nNOT ALL STRUCTURES WERE ACCESSIBLE!");
      I.say("  Buildings are: ");
      for (Building b : map.buildings) {
        I.say("  "+b+" at: "+b.at()+", entrances:");
        for (Tile e : b.entrances()) I.say("    "+e);
      }
      
      I.say("  Still need to build: ");
      for (Element e : toBuild) I.say("  "+e+" at "+e.at());
      
      if (! graphics) return false;
    }

    //  Otherwise, we need to ensure that actor-based construction will
    //  work.  So we tear down all of these structures, and get them
    //  ready to be rebuilt.
    else {
      Batch <Element> freshBuilt = new Batch();
      for (Element e : map.planning.toBuild) {
        Tile at = e.at();
        map.planning.unplaceObject(e);
        e.exitMap(map);
        
        Element copy = (Element) e.type.generate();
        copy.setLocation(at, map);
        freshBuilt.add(copy);
        
        if (e == cistern0) cistern0 = (BuildingForWater) copy;
        if (e == basin1  ) basin1   = (BuildingForWater) copy;
        if (e == basin2  ) basin2   = (BuildingForWater) copy;
      }
      
      for (Element e : freshBuilt) {
        map.planning.placeObject(e);
      }
    }
    
    //
    //  Run simulation:
    final int RUN_TIME = YEAR_LENGTH;
    boolean buildOkay = false;
    boolean waterOkay = false;
    
    while (map.time < RUN_TIME || graphics) {
      map = test.runLoop(map, 1, graphics, "saves/test_build_path.tlt");
      
      if (! buildOkay) {
        boolean buildDone = true;
        for (Tile t : map.allTiles()) {
          if (t.above == null || t.above.type.isNatural()) continue;
          if (t.above.buildLevel() < 1) buildDone = false;
        }
        buildOkay = buildDone;
      }
      
      if (buildOkay && ! waterOkay) {
        boolean fillsOkay = true;
        fillsOkay &= basin1  .inventory.valueFor(WATER) >= 5;
        fillsOkay &= basin2  .inventory.valueFor(WATER) == 0;
        fillsOkay &= cistern0.inventory.valueFor(WATER) == 0;
        waterOkay = fillsOkay;
        
        if (waterOkay) {
          I.say("\nBRIDGING TEST SUCCESSFUL!");
          if (! graphics) return true;
        }
      }
      
      if (map.time + 10 >= RUN_TIME && (! waterOkay) && (! graphics)) {
        graphics = true;
        map.settings.paused = true;
      }
    }
    
    I.say("\nBRIDGING TEST FAILED!");
    I.say("  Build okay: "+buildOkay);
    I.say("  Water okay: "+waterOkay);
    
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
    
    if (pathOK != backOK || (pathOK && backOK) != mapsOK) {
      I.say("\nMISMATCH IN PATHING OBSERVED AT "+t);
      I.say("  Path type:   "+t.pathType());
      I.say("  Above:       "+t.above);
      I.say("  Build level: "+(t.above == null ? 0 : t.above.buildLevel()));
      I.say("  Tile to Home OK: "+pathOK  );
      I.say("  Home to Tile OK: "+backOK  );
      I.say("  Map connection:  "+mapsOK  );
      I.say("  Ground access:   "+groundOK);
      return false;
    }
    else return true;
  }
  
}














