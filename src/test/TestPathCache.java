

package test;
import game.*;
import static content.GameContent.*;
import static content.GameWorld.*;
import static game.AreaPathCache.*;
import util.*;



//  TODO:  Include effects of fog...

//  And you have to hook this up to the flagging-maps.  That will
//  let you ensure that only accessible sources are visited, on a
//  tile-by-tile basis.

//  This is probably good enough for now.  If you wanted to optimise
//  further, you could keep a table of the the IDs of groups in each
//  16x16 unit, and use that filter any quad-tree descent that tries to
//  explore within.

//  As an even longer-term project, you could try to cache distance-
//  estimates between both tiles and regions, so that you can rank
//  possible flag-targets more accurately.  That is down the road a bit
//  though.



public class TestPathCache extends LogicTest {
  

  public static void main(String args[]) {
    testPathCache(true);
  }
  
  
  static boolean testPathCache(boolean graphics) {
    TestPathCache test = new TestPathCache();
    boolean allOkay = true;
    //
    //  Set up a small world initially:
    byte layout[][] = {
      { 0, 1, 0, 0, 1, 1, 1, 1 },
      { 1, 1, 0, 0, 1, 1, 0, 1 },
      { 1, 1, 0, 0, 1, 1, 0, 1 },
      { 0, 0, 0, 0, 0, 0, 0, 1 },
      { 0, 0, 0, 1, 1, 1, 0, 1 },
      { 1, 0, 1, 1, 1, 1, 1, 1 },
      { 1, 1, 1, 1, 1, 0, 1, 1 },
      { 1, 1, 1, 1, 1, 1, 1, 1 },
    };
    int miniSize = 8;
    Base miniBase = setupTestBase(BASE, FACTION_SETTLERS_A, ALL_GOODS, miniSize, false, ALL_TERRAINS);
    AreaMap miniMap = miniBase.activeMap();
    
    for (Coord c : Visit.grid(0, 0, miniSize, miniSize, 1)) {
      byte l = layout[c.x][c.y];
      AreaTile t = miniMap.tileAt(c);
      miniMap.setTerrain(t, l == 0 ? LAKE : MEADOW, (byte) 0, 0);
    }
    //
    //  First, verify that an zone's tiles conform to an expected shape:
    int coords[] = { 0, 1,   1, 0,   1, 1,   2, 0,   2, 1 };
    int numT = coords.length / 2;
    Zone zone = miniMap.pathCache.zoneFor(miniMap.tileAt(1, 1));
    if (zone == null) {
      I.say("\nArea was not generated!");
      allOkay = false;
      if (! graphics) return false;
    }
    if (zone.numTiles() != numT) {
      I.say("\nArea has "+zone.numTiles()+" tiles, expected "+numT);
      allOkay = false;
      if (! graphics) return false;
    }
    for (int i = 0; i < coords.length;) {
      AreaTile t = miniMap.tileAt(coords[i++], coords[i++]);
      if (! Visit.arrayIncludes(zone.tiles(), t)) {
        I.say("\nExpected zone to include "+t);
        I.say("  Tiles were: "+I.list(zone.tiles()));
        allOkay = false;
        if (! graphics) return false;
      }
    }
    //
    //  More broadly, we verify exhaustively that connection-queries
    //  accurately reflect ability to path between points:
    if (! verifyConnectionQueries(miniMap)) {
      allOkay = false;
      if (! graphics) return false;
    }
    //
    //  Connect a few old zones and partition others:
    byte newLayout[][] = {
      { 0, 1, 0, 0, 1, 1, 0, 1 },
      { 1, 1, 0, 0, 1, 1, 0, 1 },
      { 1, 1, 1, 1, 1, 1, 0, 1 },
      { 0, 0, 0, 0, 0, 0, 0, 1 },
      { 0, 0, 0, 1, 1, 1, 0, 1 },
      { 1, 1, 0, 1, 1, 1, 1, 1 },
      { 1, 1, 0, 0, 1, 1, 1, 1 },
      { 1, 1, 0, 1, 1, 1, 1, 1 },
    };
    for (Coord c : Visit.grid(0, 0, miniSize, miniSize, 1)) {
      byte l = newLayout[c.x][c.y];
      AreaTile t = miniMap.tileAt(c);
      miniMap.setTerrain(t, l == 0 ? LAKE : MEADOW, (byte) 0, 0);
    }
    miniMap.pathCache.updatePathCache();
    if (! verifyConnectionQueries(miniMap)) {
      allOkay = false;
      if (! graphics) return false;
    }
    //
    //  And finally, try modifying the map at random to see if the
    //  queries hold:
    for (AreaTile t : miniMap.allTiles()) {
      if (Rand.yes()) {
        miniMap.setTerrain(t, miniMap.blocked(t) ? MEADOW : LAKE, (byte) 0, 0);
      }
    }
    miniMap.pathCache.updatePathCache();
    if (! verifyConnectionQueries(miniMap)) {
      allOkay = false;
      if (! graphics) return false;
    }
    
    //
    //  Now, set up a larger map for testing of connections between
    //  more distant zones:
    Base base = setupTestBase(BASE, FACTION_SETTLERS_A, ALL_GOODS, 128, false, ALL_TERRAINS);
    AreaMap map = base.activeMap();
    World world = map.world;
    world.settings.toggleFog = false;
    test.viewPathMap = true;
    
    final int div = map.size() / layout.length, rand = div / 4;
    for (AreaTile t : map.allTiles()) {
      int x = t.x, y = t.y;
      
      x += Rand.range(-rand, rand);
      y += Rand.range(-rand, rand);
      x = Nums.clamp(x, map.size());
      y = Nums.clamp(y, map.size());
      
      byte l = layout[x / div][y / div];
      map.setTerrain(t, l == 0 ? LAKE : MEADOW, (byte) 0, 0);
    }
    AreaTerrain.populateFixtures(map);
    
    AreaTile land1   = map.tileAt(0.5f * div, 7.5f * div);
    AreaTile land2   = map.tileAt(7.5f * div, 0.5f * div);
    AreaTile island0 = map.tileAt(1.5f * div, 1.5f * div);
    land1   = AreaTile.nearestOpenTile(land1  , map);
    land2   = AreaTile.nearestOpenTile(land2  , map);
    island0 = AreaTile.nearestOpenTile(island0, map);
    test.keyTiles = new AreaTile[] { land1, land2, island0 };
    
    map.pathCache.updatePathCache();
    
    boolean landLinked = map.pathCache.pathConnects(land1, land2);
    if (! landLinked) {
      I.say("\nNearby regions not linked!");
      I.say("  Total groups: "+map.pathCache.groups().size());
      for (ZoneGroup g : map.pathCache.groups()) {
        I.say("    Group zones: ");
        for (Zone a : g.zones()) {
          I.say("      "+a.numTiles()+" tiles: "+a.coord());
        }
      }
      allOkay = false;
      if (! graphics) return false;
    }
    
    boolean islandLinked = map.pathCache.pathConnects(land1, island0);
    if (islandLinked) {
      I.say("\nSeparated regions should not be linked!");
      allOkay = false;
      if (! graphics) return false;
    }
    
    AreaPlanning.placeStructure(SHIELD_WALL, base, true, 88, 20, 48, 8);
    map.pathCache.updatePathCache();
    landLinked = map.pathCache.pathConnects(land1, land2);
    if (landLinked) {
      I.say("\nWall should have partitioned mainland!");
      allOkay = false;
      if (! graphics) return false;
    }
    
    AreaPlanning.placeStructure(WALKWAY, base, true, 20, 24, 8, 48);
    map.pathCache.updatePathCache();
    islandLinked = map.pathCache.pathConnects(land1, island0);
    if (! islandLinked) {
      I.say("\nRoad should have connected island to mainland!");
      allOkay = false;
      if (! graphics) return false;
    }
    
    AreaPlanning.markDemolish(map, true, 120, 20, 2, 8);
    Building gate = (Building) BLAST_DOOR.generate();
    gate.setFacing(TileConstants.N);
    gate.enterMap(map, 120, 23, 1, base);
    map.pathCache.updatePathCache();
    landLinked = map.pathCache.pathConnects(land1, land2);
    if (! landLinked) {
      I.say("\nGate should have bridged partition!");
      allOkay = false;
      if (! graphics) return false;
    }
    
    landLinked = map.pathCache.openPathConnects(land1, land2);
    if (landLinked) {
      I.say("\nGate should not allow open path!");
      allOkay = false;
      if (! graphics) return false;
    }
    
    Building tower = (Building) TURRET.generate();
    tower.setFacing(TileConstants.N);
    tower.enterMap(map, 110, 20, 1, base);
    map.pathCache.updatePathCache();
    
    AreaPlanning.markDemolish(map, true, 100, 19, 20, 1);
    AreaTile onWall = map.tileAt(110, 24);
    landLinked = map.pathCache.pathConnects(onWall, land2);
    if (! landLinked) {
      I.say("\nTower should have allowed pathing onto wall!");
      allOkay = false;
      if (! graphics) return false;
    }
    
    tower.exitMap(map);
    landLinked = map.pathCache.pathConnects(onWall, land2);
    map.pathCache.updatePathCache();
    if (landLinked) {
      I.say("\nTower's demolition should have destroyed path!");
      allOkay = false;
      if (! graphics) return false;
    }
    
    for (AreaTile t : map.allTiles()) {
      if (map.pathCache.rawZone(t) == null && ! map.blocked(t)) {
        I.say("\nUnblocked tile has no zone: "+t);
        allOkay = false;
        if (! graphics) return false;
      }
    }
    
    for (int n = 100; n-- > 0;) {
      AreaTile from, goes;
      int mapS = map.size();
      from = map.tileAt(Rand.index(mapS), Rand.index(mapS));
      goes = map.tileAt(Rand.index(mapS), Rand.index(mapS));
      from = AreaTile.nearestOpenTile(from, map);
      goes = AreaTile.nearestOpenTile(goes, map);
      if (from == null || goes == null || from == goes) continue;
      if (! verifyConnection(from, goes, map)) {
        allOkay = false;
        if (! graphics) return false;
      }
    }
    
    if (allOkay) {
      I.say("\nPATH-CACHE TESTS SUCCESSFUL!");
    }
    else {
      I.say("\nPATH_CACHE TESTS FAILED!");
    }
    
    if (graphics) world.settings.paused = true;
    while (map.time() < 10 || graphics) {
      test.runLoop(base, 1, graphics, "saves/test_path_cache.tlt");
    }
    
    return allOkay;
  }
  
  
  static boolean verifyConnectionQueries(AreaMap map) {
    for (Coord c : Visit.grid(0, 0, map.size(), map.size(), 1)) {
      if (map.blocked(c)) continue;
      
      for (Coord o : Visit.grid(0, 0, map.size(), map.size(), 1)) {
        if (map.blocked(o)) continue;
        
        AreaTile from = map.tileAt(c);
        AreaTile goes = map.tileAt(o);
        if (from == goes) continue;
        
        if (! verifyConnection(from, goes, map)) {
          return false;
        }
      }
    }
    return true;
  }
  
  
  static boolean verifyConnection(AreaTile from, AreaTile goes, AreaMap map) {
    
    boolean connects = map.pathCache.pathConnects(from, goes);
    ActorPathSearch search;
    search = new ActorPathSearch(map, from, goes, -1);
    search.doSearch();
    
    if (search.success() && ! connects) {
      I.say("\nFound path between "+from+" and "+goes);
      I.add("- no zone connection!");
      return false;
    }
    if (connects && ! search.success()) {
      I.say("\nFound zone connection between "+from+" and "+goes);
      I.add("- no path search!");
      return false;
    }
    return true;
  }
  
}




