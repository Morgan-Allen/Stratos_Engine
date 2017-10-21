


package game;
import util.*;
import static game.GameConstants.*;
import static util.TileConstants.*;




public class CityMap implements Session.Saveable {
  
  
  /**  Data fields and initialisation-
    */
  final static int SCAN_RES = 16;
  
  final CityMapSettings settings = new CityMapSettings();
  
  City city;
  int size, scanSize;
  Tile grid[][];
  CityMapFog fog = new CityMapFog(this);
  
  int time = 0;
  
  List <Building> buildings = new List();
  List <Actor   > walkers   = new List();
  
  Table <City, Tile> transitPoints = new Table();
  
  
  int growScanIndex = 0;
  static class HabitatScan {
    int numTiles = 0;
    int densities[][];
  }
  HabitatScan scans[][] = new HabitatScan[2][ALL_TERRAINS.length];
  
  String saveName;
  
  
  CityMap(City city) {
    this.city = city;
  }
  
  
  public CityMap(Session s) throws Exception {
    s.cacheInstance(this);
    
    settings.loadState(s);
    
    city = (City) s.loadObject();
    performSetup(s.loadInt());
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      grid[c.x][c.y].loadState(s);
    }
    fog.loadState(s);
    
    time = s.loadInt();
    
    s.loadObjects(buildings);
    s.loadObjects(walkers  );
    
    for (int n = s.loadInt(); n-- > 0;) {
      City with = (City) s.loadObject();
      Tile point = loadTile(this, s);
      transitPoints.put(with, point);
    }
    
    growScanIndex = s.loadInt();
    for (int i = 2; i-- > 0;) for (int h = ALL_TERRAINS.length; h-- > 0;) {
      scans[i][h] = loadScan(s);
    }
    
    saveName = s.loadString();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    settings.saveState(s);
    
    s.saveObject(city);
    s.saveInt(size);
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      grid[c.x][c.y].saveState(s);
    }
    fog.saveState(s);
    
    s.saveInt(time);
    
    s.saveObjects(buildings);
    s.saveObjects(walkers  );
    
    s.saveInt(transitPoints.size());
    for (City c : transitPoints.keySet()) {
      s.saveObject(c);
      saveTile(transitPoints.get(c), this, s);
    }
    
    s.saveInt(growScanIndex);
    for (int i = 2; i-- > 0;) for (int h = ALL_TERRAINS.length; h-- > 0;) {
      saveScan(scans[i][h], s);
    }
    
    s.saveString(saveName);
  }
  
  
  void performSetup(int size) {
    
    int s = 1;
    while (s < size) s *= 2;
    size = s;
    
    this.size     = size;
    this.scanSize = Nums.round(size * 1f / SCAN_RES, 1, true);
    
    this.grid = new Tile[size][size];
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      Tile t = grid[c.x][c.y] = new Tile();
      t.x = c.x;
      t.y = c.y;
    }
    
    fog.performSetup(size);
    city.assignMap(this);
  }
  
  
  
  /**  Tiles and related setup/query methods-
    */
  public static class Tile implements Target {
    
    int x, y;
    
    Terrain terrain;
    Element above;
    boolean paved;
    
    List <Actor> focused = null;
    protected Object flag;
    
    
    void loadState(Session s) throws Exception {
      int terrID = s.loadInt();
      terrain = terrID == -1 ? null : ALL_TERRAINS[terrID];
      above   = (Element) s.loadObject();
      paved   = s.loadBool();
      
      if (s.loadBool()) s.loadObjects(focused = new List());
    }
    
    
    void saveState(Session s) throws Exception {
      s.saveInt(terrain == null ? -1 : terrain.terrainIndex);
      s.saveObject(above);
      s.saveBool(paved);
      
      s.saveBool(focused != null);
      if (focused != null) s.saveObjects(focused);
    }
    
    
    public CityMap.Tile at() {
      return this;
    }
    
    
    public void targetedBy(Actor w) {
      return;
    }
    
    /*
    public void setFocused(Walker w, boolean is) {
      if (is) {
        if (focused == null) focused = new List();
        focused.include(w);
      }
      else if (focused != null) {
        focused.remove(w);
        if (focused.size() == 0) focused = null;
      }
    }
    
    
    public boolean hasFocus() {
      return focused != null;
    }
    //*/
    
    
    public String toString() {
      return "T"+x+"|"+y;
    }
  }
  
  
  static Tile loadTile(CityMap map, Session s) throws Exception {
    int x = s.loadInt();
    if (x == -1) return null;
    int y = s.loadInt();
    return map.tileAt(x, y);
  }
  
  
  static void saveTile(Tile t, CityMap map, Session s) throws Exception {
    if (t == null) {
      s.saveInt(-1);
      return;
    }
    s.saveInt(t.x);
    s.saveInt(t.y);
  }
  
  
  public static Tile[] adjacent(
    Tile spot, Tile temp[], CityMap map, boolean paveOnly
  ) {
    if (temp == null) temp = new Tile[8];
    
    for (int dir : T_INDEX) {
      temp[dir] = null;
      int x = spot.x + T_X[dir], y = spot.y + T_Y[dir];
      if (paveOnly) {
        if (map.paved(x, y)) temp[dir] = map.tileAt(x, y);
      }
      else {
        if (! map.blocked(x, y)) temp[dir] = map.tileAt(x, y);
      }
    }
    return temp;
  }
  
  
  public static float distance(Tile a, Tile b) {
    if (a == null || b == null) return 1000000000;
    float dx = a.x - b.x, dy = a.y - b.y;
    return Nums.sqrt((dx * dx) + (dy * dy));
  }
  
  
  public static float distancePenalty(float dist) {
    float range = MAX_WANDER_RANGE / 2;
    return range / (range + dist);
  }
  
  
  public static float distancePenalty(Tile a, Tile b) {
    return distancePenalty(distance(a, b));
  }
  
  
  Tile tileAt(int x, int y) {
    try { return grid[x][y]; }
    catch (Exception e) { return null; }
  }
  
  
  Tile tileAt(Coord c) {
    return tileAt(c.x, c.y);
  }
  
  
  
  /**  Blockage and paving methods-
    */
  Element above(int x, int y) {
    Tile under = tileAt(x, y);
    return under == null ? null : under.above;
  }
  
  
  Element above(Coord c) {
    return above(c.x, c.y);
  }
  
  
  boolean blocked(int x, int y) {
    Tile under = tileAt(x, y);
    if (under == null) return true;
    
    Terrain terr = under.terrain;
    if (under.above != null) return under.above.type.blocks;
    else return terr == null ? false : terr.blocks;
  }
  
  
  boolean blocked(Coord c) {
    return blocked(c.x, c.y);
  }
  
  
  boolean paved(int x, int y) {
    Tile under = tileAt(x, y);
    return under == null ? false : under.paved;
  }
  
  
  boolean paved(Coord c) {
    return paved(c.x, c.y);
  }
  
  
  public static void applyPaving(
    CityMap map, int x, int y, int w, int h, boolean is
  ) {
    for (Coord c : Visit.grid(x, y, w, h, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t       != null) t.paved = is;
      if (t.above != null) t.above.exitMap(map);
    }
  }
  
  
  public static void demolish(
    CityMap map, int x, int y, int w, int h
  ) {
    for (Coord c : Visit.grid(x, y, w, h, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t       == null) continue;
      if (t.paved        ) t.paved = false;
      if (t.above != null) t.above.exitMap(map);
    }
  }
  
  
  
  
  /**  Active updates:
    */
  void update() {
    if (city != null) {
      city.world.updateFrom(this);
    }
    for (Building b : buildings) {
      b.update();
    }
    for (Actor w : walkers) {
      w.update();
    }
    
    if (time % SCAN_PERIOD == 0) {
      transitPoints.clear();
    }
    
    if (time % MONTH_LENGTH == 0) {
      CityBorders.spawnMigrants(this, MONTH_LENGTH);
    }
    
    time += 1;
    
    fog.updateFog();
    updateGrowth();
  }
  
  
  
  /**  Growth, habitats and fertility:
    */
  void updateGrowth() {
    int totalTiles  = size * size;
    int targetIndex = (totalTiles * (time % SCAN_PERIOD)) / SCAN_PERIOD;
    if (targetIndex < growScanIndex) targetIndex = totalTiles;
    
    while (++growScanIndex < targetIndex) {
      int x = growScanIndex / size, y = growScanIndex % size;
      Element above = grid[x][y].above;
      if (above != null) above.updateGrowth();
      scanHabitat(grid[x][y]);
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
  
  
  HabitatScan initHabitatScan() {
    HabitatScan scan = new HabitatScan();
    scan.densities = new int[scanSize][scanSize];
    return scan;
  }
  
  
  HabitatScan loadScan(Session s) throws Exception {
    if (! s.loadBool()) return null;
    
    HabitatScan scan = initHabitatScan();
    scan.numTiles = s.loadInt();
    for (Coord c : Visit.grid(0, 0, scanSize, scanSize, 1)) {
      scan.densities[c.x][c.y] = s.loadInt();
    }
    return scan;
  }
  
  
  void saveScan(HabitatScan scan, Session s) throws Exception {
    if (scan == null) { s.saveBool(false); return; }
    
    s.saveInt(scan.numTiles);
    for (Coord c : Visit.grid(0, 0, scanSize, scanSize, 1)) {
      s.saveInt(scan.densities[c.x][c.y]);
    }
  }
  
  
  void scanHabitat(Tile tile) {
    Terrain t = tile.terrain;
    if (t == null || tile.paved) {
      return;
    }
    if (tile.above != null && tile.above.type.category != Type.IS_FIXTURE) {
      return;
    }
    
    HabitatScan scan = scans[1][t.terrainIndex];
    if (scan == null) scan = scans[1][t.terrainIndex] = initHabitatScan();
    
    scan.numTiles += 1;
    scan.densities[tile.x / SCAN_RES][tile.y / SCAN_RES] += 1;
  }
}





