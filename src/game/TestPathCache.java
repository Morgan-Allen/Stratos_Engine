

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;
import static game.CityMapPathCache.*;



public class TestPathCache extends Test {
  

  public static void main(String args[]) {
    testPathCache(true);
  }
  
  
  static boolean testPathCache(boolean graphics) {
    Test test = new TestPathCache();
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
    CityMap miniMap = setupTestCity(miniSize);
    
    for (Coord c : Visit.grid(0, 0, miniSize, miniSize, 1)) {
      byte l = layout[c.x][c.y];
      Tile t = miniMap.tileAt(c);
      t.terrain = l == 0 ? LAKE : MEADOW;
    }
    //
    //  First, verify that an area's tiles conform to an expected shape:
    int coords[] = { 0, 1,   1, 0,   1, 1,   2, 0,   2, 1 };
    int numT = coords.length / 2;
    Area area = miniMap.pathCache.areaFor(miniMap.tileAt(1, 1));
    if (area == null) {
      I.say("\nArea was not generated!");
      return false;
    }
    if (area.numTiles != numT) {
      I.say("\nArea has "+area.numTiles+" tiles, expected "+numT);
      return false;
    }
    for (int i = 0; i < coords.length;) {
      Tile t = miniMap.tileAt(coords[i++], coords[i++]);
      if (! Visit.arrayIncludes(area.tiles, t)) {
        I.say("\nExpected area to include "+t);
        I.say("  Tiles were: "+I.list(area.tiles));
        return false;
      }
    }
    //
    //  More broadly, we verify exhaustively that connection-queries
    //  accurately reflect ability to path between points:
    if (! verifyConnectionQueries(miniMap)) {
      return false;
    }
    //
    //  Connect a few old areas and partition others:
    layout = new byte[][] {
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
      byte l = layout[c.x][c.y];
      Tile t = miniMap.tileAt(c);
      t.terrain = l == 0 ? LAKE : MEADOW;
      miniMap.pathCache.flagPathingChanged(t);
    }
    miniMap.pathCache.updatePathCache();
    if (! verifyConnectionQueries(miniMap)) {
      return false;
    }
    //
    //  And finally, try modifying the map at random to see if the
    //  queries hold:
    for (Coord c : Visit.grid(0, 0, miniSize, miniSize, 1)) {
      if (Rand.yes()) {
        Tile t = miniMap.tileAt(c);
        t.terrain = miniMap.blocked(t.x, t.y) ? MEADOW : LAKE;
        miniMap.pathCache.flagPathingChanged(t);
      }
    }
    miniMap.pathCache.updatePathCache();
    if (! verifyConnectionQueries(miniMap)) {
      return false;
    }
    
    //
    //  Now, set up a larger map for testing of connections between
    //  more distant areas:
    CityMap map = setupTestCity(128);
    map.settings.toggleFog = false;
    
    int div = map.size / layout.length, rand = div / 4;
    
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      int x = c.x, y = c.y;
      
      x += Rand.range(-rand, rand);
      y += Rand.range(-rand, rand);
      x = Nums.clamp(x, map.size);
      y = Nums.clamp(y, map.size);
      
      byte l = layout[x / div][y / div];
      Tile t = map.tileAt(c);
      t.terrain = l == 0 ? LAKE : MEADOW;
    }
    
    CityMapTerrain.populateFixtures(map);
    
    Tile land1   = map.tileAt(7.5f * div, 0.5f * div);
    Tile land2   = map.tileAt(7.5f * div, 2.5f * div);
    Tile island0 = map.tileAt(1.5f * div, 1.5f * div);
    
    boolean landLinked = map.pathCache.pathConnects(land1, land2);
    if (! landLinked) {
      I.say("\nNearby regions not linked!");
      
      I.say("  Total groups: "+map.pathCache.groups.size());
      for (AreaGroup g : map.pathCache.groups) {
        I.say("    Group areas: ");
        for (Area a : g.areas) {
          I.say("      "+a.numTiles+" tiles: "+a.aX+"|"+a.aY);
        }
      }
      return false;
    }
    
    boolean islandLinked = map.pathCache.pathConnects(land1, island0);
    if (islandLinked) {
      I.say("\nSeparated regions should not be linked!");
      return false;
    }
    
    if (true) {
      I.say("Tests so far okay...");
    }
    
    //  TODO:  And include effects of fog.
    
    //  And you have to hook this up to the flagging-maps.  That will
    //  let you ensure that only accessible sources are visited, on a
    //  tile-by-tile basis.
    
    //  That's probably good enough for now.  If you wanted to optimise
    //  further, you could keep a table of the the IDs of groups in
    //  each 16x16 unit, and use that filter any quad-tree descent that
    //  tries to explore within.
    
    //  As an even longer-term project, you could try to cache distance-
    //  estimates between both tiles and regions, so that you can rank
    //  possible flag-targets more accurately.  That is down the road a
    //  bit though.
    
    
    map.settings.paused = true;
    while (map.time < 1000 || graphics) {
      map = test.runLoop(map, 1, graphics, "saves/test_path_cache.tlt");
    }
    
    return true;
  }
  
  
  static boolean verifyConnectionQueries(CityMap map) {
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      if (map.blocked(c)) continue;
      
      for (Coord o : Visit.grid(0, 0, map.size, map.size, 1)) {
        if (map.blocked(o)) continue;
        
        Tile from = map.tileAt(c);
        Tile goes = map.tileAt(o);
        if (from == goes) continue;
        
        boolean connects = map.pathCache.pathConnects(from, goes);
        ActorPathSearch search;
        search = new ActorPathSearch(map, from, goes, -1);
        search.doSearch();
        
        if (search.success() && ! connects) {
          I.say("\nFound path between "+from+" and "+goes);
          I.add("- no area connection!");
          
          //  The areas are changing within the query...
          //boolean nc = map.pathCache.pathConnects(from, goes);
          
          return false;
        }
        if (connects && ! search.success()) {
          I.say("\nFound area connection between "+from+" and "+goes);
          I.add("- no path search!");
          return false;
        }
      }
    }
    return true;
  }
  

  void updateCityMapView(CityMap map) {
    configGraphic(map.size, map.size);
    
    Tile hovered = map.tileAt(hover.x, hover.y);
    Area area = null, around[] = null;
    if (hovered != null) area = map.pathCache.areaFor(hovered);
    if (area != null) around = area.borders.toArray(Area.class);
    
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      int fill = WHITE_COLOR;
      if (map.blocked(c)) fill = BLACK_COLOR;
      if (area != null) {
        Area under = map.pathCache.areaLookup[c.x][c.y];
        if (area == under) fill = WALKER_COLOR;
        if (Visit.arrayIncludes(around, under)) fill = MISSED_COLOR;
      }
      graphic[c.x][c.y] = fill;
    }
  }
  
}



