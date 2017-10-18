

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class CityMapTerrain implements TileConstants {
  
  
  /**  Initial terrain setup-
    */
  public static CityMap generateTerrain(
    City city, int size, Terrain... gradient
  ) {
    CityMap map = new CityMap(city);
    map.performSetup(size);
    populateTerrain(map, gradient);
    return map;
  }
  
  
  public static void populateTerrain(CityMap map, Terrain... gradient) {
    
    HeightMap mapH = new HeightMap(map.size, 1, 0.5f);
    int numG = gradient.length;
    
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      Tile    tile = map.tileAt(c.x, c.y);
      float   high = mapH.value()[c.x][c.y];
      Terrain terr = gradient[Nums.clamp((int) (high * numG), numG)];
      
      tile.terrain = terr;
    }
  }
  
  
  
  /**  Adding fixtures-
    */
  public static void populateFixtures(CityMap map) {
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      Tile    tile = map.tileAt(c.x, c.y);
      Terrain terr = tile.terrain;
      
      //  TODO:  You also need to mark these tiles for regeneration later, if
      //  the trees are cut down.
      
      for (int i = terr.fixtures.length; i-- > 0;) {
        Type t = terr.fixtures[i];
        float      w = terr.weights [i] / (t.wide * t.high);
        
        if (Rand.num() < w && checkPlacingOkay(tile, t, map)) {
          Element f = (Element) t.generate();
          f.enterMap(map, tile.x, tile.y, 1);
        }
      }
    }
  }
  
  
  public static void populateFixture(Type t, int x, int y, CityMap map) {
    Element f = (Element) t.generate();
    f.enterMap(map, x, y, 1);
  }
  
  
  static boolean checkPlacingOkay(Tile at, Type t, CityMap map) {
    
    for (Coord c : Visit.grid(at.x, at.y, t.wide, t.high, 1)) {
      Tile u = map.tileAt(c.x, c.y);
      if (u == null || u.above != null) return false;
    }
    
    int inGap = -1, firstTile = -1, numGaps = 0;
    //I.say("\nChecking...");
    
    for (Coord c : Visit.perimeter(at.x, at.y, t.wide, t.high)) {
      
      Tile tile = map.tileAt(c.x, c.y);
      boolean blocked = map.blocked(c.x, c.y) || tile.above != null;
      if (firstTile == -1) firstTile = blocked ? 0 : 1;
      //I.say("  B: "+blocked);
      
      if (blocked) {
        if (inGap != 0) {
          inGap = 0;
        }
      }
      else {
        if (inGap != 1) {
          inGap = 1;
          numGaps += 1;
        }
      }
    }
    
    if (inGap == 1 && firstTile == inGap) numGaps -= 1;
    //I.say("Total gaps: "+numGaps);
    
    return numGaps < 2;
  }
  
  
  static Tile nearestOpenTile(Tile from, CityMap map) {
    if (! map.blocked(from.x, from.y)) return from;
    
    for (Tile t : CityMap.adjacent(from, null, map, false)) {
      if (t == null || map.blocked(t.x, t.y)) continue;
      return t;
    }
    return null;
  }
  
  
  
  /**  Adding predators and prey:
    */
  public static void populateAnimals(CityMap map, Type... species) {
    if (Visit.empty(species)) species = ALL_ANIMALS;
    
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      map.scanHabitat(map.grid[c.x][c.y]);
    }
    map.endScan();
    
    for (Type s : species) {
      float idealPop = idealPopulation(s, map);
      
      while (idealPop-- > 0) {
        ActorAsAnimal a = (ActorAsAnimal) s.generate();
        Tile point = findGrazePoint(s, map);
        s.initAsAnimal(a);
        a.enterMap(map, point.x, point.y, 1);
      }
    }
  }
  
  
  static float habitatDensity(Tile tile, Terrain t, CityMap map) {
    HabitatScan scan = map.scans[0][t.terrainIndex];
    if (scan == null) return 0;
    float d = scan.densities[tile.x / SCAN_RES][tile.y / SCAN_RES];
    return d / (SCAN_RES * SCAN_RES);
  }
  
  
  static float idealPopulation(Type species, CityMap map) {
    float numTiles = 0;
    for (Terrain h : species.habitats) {
      HabitatScan scan = map.scans[0][h.terrainIndex];
      numTiles += scan == null ? 0 : scan.numTiles;
    }
    if (species.predator) {
      return numTiles / TILES_PER_HUNTER;
    }
    else {
      return numTiles / TILES_PER_GRAZER;
    }
  }
  
  
  static Tile findGrazePoint(Type species, CityMap map) {
    int x = SCAN_RES / 2, y = SCAN_RES / 2;
    
    Batch <Tile > points  = new Batch();
    Batch <Float> ratings = new Batch();
    
    for (Coord c : Visit.grid(x, y, map.size, map.size, SCAN_RES)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t == null) continue;
      float rating = 0;
      for (Terrain h : species.habitats) rating += habitatDensity(t, h, map);
      points.add(t);
      ratings.add(rating);
    }
    
    Tile point = (Tile) Rand.pickFrom(points, ratings);
    if (point == null) return null;
    
    point = nearestOpenTile(point, map);
    return point;
  }
  
}









