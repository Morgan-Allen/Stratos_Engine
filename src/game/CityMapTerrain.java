

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class CityMapTerrain implements TileConstants {
  
  
  final CityMap map;
  
  //  TODO:  Move this into the terrain class!
  int growScanIndex = 0;
  static class HabitatScan {
    int numTiles = 0;
    int densities[][];
  }
  HabitatScan scans[][] = new HabitatScan[2][ALL_TERRAINS.length];
  
  
  CityMapTerrain(CityMap map) {
    this.map = map;
  }
  
  
  void loadState(Session s) throws Exception {
    growScanIndex = s.loadInt();
    for (int i = 2; i-- > 0;) for (int h = ALL_TERRAINS.length; h-- > 0;) {
      if (! s.loadBool()) { scans[i][h] = null; continue; }
      
      HabitatScan scan = initHabitatScan();
      scan.numTiles = s.loadInt();
      for (Coord c : Visit.grid(0, 0, map.scanSize, map.scanSize, 1)) {
        scan.densities[c.x][c.y] = s.loadInt();
      }
      scans[i][h] = scan;
    }
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveInt(growScanIndex);
    for (int i = 2; i-- > 0;) for (int h = ALL_TERRAINS.length; h-- > 0;) {
      HabitatScan scan = scans[i][h];
      if (scan == null) { s.saveBool(false); continue; }
      
      s.saveInt(scan.numTiles);
      for (Coord c : Visit.grid(0, 0, map.scanSize, map.scanSize, 1)) {
        s.saveInt(scan.densities[c.x][c.y]);
      }
    }
  }
  
  
  HabitatScan initHabitatScan() {
    HabitatScan scan = new HabitatScan();
    scan.densities = new int[map.scanSize][map.scanSize];
    return scan;
  }
  
  
  
  /**  Regular map updates:
    */
  void updateTerrain() {
    int size        = map.size;
    int totalTiles  = size * size;
    int targetIndex = (totalTiles * (map.time % SCAN_PERIOD)) / SCAN_PERIOD;
    if (targetIndex < growScanIndex) targetIndex = totalTiles;
    
    while (++growScanIndex < targetIndex) {
      int x = growScanIndex / size, y = growScanIndex % size;
      Element above = map.grid[x][y].above;
      if (above != null) above.updateGrowth();
      scanHabitat(map.grid[x][y]);
    }
    
    if (targetIndex == totalTiles) {
      endScan();
    }
  }
  
  
  void endScan() {
    growScanIndex = 0;
    for (int i = ALL_TERRAINS.length; i-- > 0;) {
      scans[0][i] = scans[1][i];
      scans[1][i] = null;
    }
  }
  
  
  void scanHabitat(Tile tile) {
    Terrain t = tile.terrain;
    Type above = tile.aboveType();

    if (t == null) return;
    if (above != null) {
      if (above.paved) return;
      if (above.category != Type.IS_FIXTURE) return;
    }
    
    HabitatScan scan = scans[1][t.terrainIndex];
    if (scan == null) scan = scans[1][t.terrainIndex] = initHabitatScan();
    
    scan.numTiles += 1;
    scan.densities[tile.x / SCAN_RES][tile.y / SCAN_RES] += 1;
  }
  
  
  
  
  
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
      
      //  TODO:  You also need to mark these tiles for regeneration
      //  later, if the trees are cut down.
      
      for (int i = terr.fixtures.length; i-- > 0;) {
        Type t = terr.fixtures[i];
        float      w = terr.weights [i] / (t.wide * t.high);
        
        if (Rand.num() < w && checkPlacingOkay(tile, t, map)) {
          Element f = (Element) t.generate();
          float level = t.growRate > 0 ? (Rand.num() + 0.5f) : 1;
          f.enterMap(map, tile.x, tile.y, 1);
          f.setGrowLevel(level);
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
    
    for (Tile t : CityMap.adjacent(from, null, map)) {
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
      map.terrain.scanHabitat(map.grid[c.x][c.y]);
    }
    map.terrain.endScan();
    
    for (Type s : species) {
      float idealPop = idealPopulation(s, map);
      
      while (idealPop-- > 0) {
        Tile point = findGrazePoint(s, map);
        if (point == null) continue;
        
        ActorAsAnimal a = (ActorAsAnimal) s.generate();
        s.initAsAnimal(a);
        a.enterMap(map, point.x, point.y, 1);
      }
    }
  }
  
  
  static float habitatDensity(Tile tile, Terrain t, CityMap map) {
    HabitatScan scan = map.terrain.scans[0][t.terrainIndex];
    if (scan == null) return 0;
    float d = scan.densities[tile.x / SCAN_RES][tile.y / SCAN_RES];
    return d / (SCAN_RES * SCAN_RES);
  }
  
  
  static float idealPopulation(Type species, CityMap map) {
    float numTiles = 0;
    for (Terrain h : species.habitats) {
      HabitatScan scan = map.terrain.scans[0][h.terrainIndex];
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
    int x = SCAN_RES / 2, y = SCAN_RES / 2, QR = SCAN_RES / 4;
    
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
    
    point = map.tileAt(
      Nums.clamp(point.x + Rand.index(QR * 2) - QR, map.size),
      Nums.clamp(point.y + Rand.index(QR * 2) - QR, map.size)
    );
    
    point = nearestOpenTile(point, map);
    return point;
  }
  
}









