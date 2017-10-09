


package game;
import util.*;
import static game.GameConstants.*;
import static util.TileConstants.*;




public class CityMap implements Session.Saveable {
  
  
  /**  Data fields and initialisation-
    */
  City city;
  int size;
  Tile grid[][];
  int time = 0;
  
  byte fogVals[][], oldVals[][];
  
  List <Building> buildings = new List();
  List <Walker  > walkers   = new List();
  
  Table <City, Tile> transitPoints = new Table();
  int growScanIndex = 0;
  
  
  CityMap(City city) {
    this.city = city;
  }
  
  
  public CityMap(Session s) throws Exception {
    s.cacheInstance(this);
    
    city = (City) s.loadObject();
    performSetup(s.loadInt());
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      grid[c.x][c.y].loadState(s);
    }
    time = s.loadInt();
    
    s.loadByteArray(fogVals);
    s.loadByteArray(oldVals);
    
    s.loadObjects(buildings);
    s.loadObjects(walkers  );
    
    for (int n = s.loadInt(); n-- > 0;) {
      City with = (City) s.loadObject();
      Tile point = loadTile(this, s);
      transitPoints.put(with, point);
    }
    growScanIndex = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(city);
    s.saveInt(size);
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      grid[c.x][c.y].saveState(s);
    }
    s.saveInt(time);
    
    s.saveByteArray(fogVals);
    s.saveByteArray(oldVals);
    
    s.saveObjects(buildings);
    s.saveObjects(walkers  );
    
    s.saveInt(transitPoints.size());
    for (City c : transitPoints.keySet()) {
      s.saveObject(c);
      saveTile(transitPoints.get(c), this, s);
    }
    s.saveInt(growScanIndex);
  }
  
  
  void performSetup(int size) {
    this.size = size;
    this.grid = new Tile[size][size];
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      Tile t = grid[c.x][c.y] = new Tile();
      t.x = c.x;
      t.y = c.y;
    }
    this.fogVals = new byte[size][size];
    this.oldVals = new byte[size][size];
  }
  
  
  
  /**  Tiles and related setup/query methods-
    */
  public static class Tile implements Target {
    
    int x, y;
    
    Terrain terrain;
    Fixture above;
    boolean paved;
    
    List <Walker> focused = null;
    protected Object flag;
    
    
    void loadState(Session s) throws Exception {
      int terrID = s.loadInt();
      terrain = terrID == -1 ? null : ALL_TERRAINS[terrID];
      above   = (Fixture) s.loadObject();
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
    
    
    public void targetedBy(Walker w) {
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
    for (int dir : T_INDEX) {
      int x = spot.x + T_X[dir], y = spot.y + T_Y[dir];
      if (paveOnly) {
        if (map.paved(x, y)) temp[dir] = map.tileAt(x, y);
      }
      else {
        if (!map.blocked(x, y)) temp[dir] = map.tileAt(x, y);
      }
    }
    return temp;
  }
  
  
  public static float distance(Tile a, Tile b) {
    if (a == null || b == null) return 1000000000;
    float dx = a.x - b.x, dy = a.y - b.y;
    return Nums.sqrt((dx * dx) + (dy * dy));
  }
  
  
  Tile tileAt(int x, int y) {
    try { return grid[x][y]; }
    catch (Exception e) { return null; }
  }
  
  
  
  /**  Blockage and paving methods-
    */
  Fixture above(int x, int y) {
    Tile under = tileAt(x, y);
    return under == null ? null : under.above;
  }
  
  
  boolean blocked(int x, int y) {
    Tile under = tileAt(x, y);
    if (under == null) return true;
    
    Terrain terr = under.terrain;
    if (under.above != null) return under.above.type.blocks;
    else return terr == null ? false : terr.blocks;
  }
  
  
  boolean paved(int x, int y) {
    Tile under = tileAt(x, y);
    return under == null ? false : under.paved;
  }
  
  
  public static void applyPaving(
    CityMap map, int x, int y, int w, int h, boolean is
  ) {
    for (Coord c : Visit.grid(x, y, w, h, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t != null) t.paved = is;
    }
  }
  
  
  
  /**  Fog of war:
    */
  void liftFog(Tile around, float range) {
    if (! GameSettings.toggleFog) {
      return;
    }
    Box2D area = new Box2D(around.x, around.y, 0, 0);
    area.expandBy(Nums.round(range, 1, true));
    
    for (Coord c : Visit.grid(area)) {
      Tile t = tileAt(c.x, c.y);
      float distance = CityMap.distance(t, around);
      if (distance > range) continue;
      fogVals[c.x][c.y] = 100;
    }
  }
  
  
  float fogAt(int x, int y) {
    Tile t = tileAt(x, y);
    return t == null ? 0 : (oldVals[x][y] / 100f);
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
    for (Walker w : walkers) {
      w.update();
    }
    
    if (time % SCAN_PERIOD == 0) {
      transitPoints.clear();
    }
    
    if (time % DAY_LENGTH == 0) {
      CityBorders.spawnMigrants(this, DAY_LENGTH);
    }
    
    time += 1;
    updateFog();
    updateGrowth();
  }
  
  
  void updateFog() {
    if (! GameSettings.toggleFog) {
      return;
    }
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      oldVals[c.x][c.y] = fogVals[c.x][c.y];
      fogVals[c.x][c.y] = 0;
    }
  }
  
  
  void updateGrowth() {
    int totalTiles  = size * size;
    int targetIndex = (totalTiles * (time % SCAN_PERIOD)) / SCAN_PERIOD;
    if (targetIndex < growScanIndex) targetIndex = totalTiles;
    
    while (++growScanIndex < targetIndex) {
      int x = growScanIndex / size, y = growScanIndex % size;
      Fixture above = grid[x][y].above;
      if (above != null) above.updateGrowth();
    }
    
    if (targetIndex == totalTiles) {
      growScanIndex = 0;
    }
  }
  
}





