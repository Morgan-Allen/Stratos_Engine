


package game;
import static game.GameConstants.*;

import game.GameConstants.Terrain;
import util.*;




public class CityMap implements Session.Saveable {
  
  
  /**  Data fields and initialisation-
    */
  int size;
  Tile grid[][];
  int time = 0;
  
  City city = null;
  List <Building> buildings = new List();
  List <Walker  > walkers   = new List();
  
  Table <Object, AmountMap> demands = new Table();
  Table <City, Tile> transitPoints = new Table();
  
  int growScanIndex = 0;
  
  
  CityMap() {
    return;
  }
  
  
  public CityMap(Session s) throws Exception {
    s.cacheInstance(this);
    
    performSetup(s.loadInt());
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      grid[c.x][c.y].loadState(s);
    }
    time = s.loadInt();
    
    city = (City) s.loadObject();
    s.loadObjects(buildings);
    s.loadObjects(walkers  );
    growScanIndex = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveInt(size);
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      grid[c.x][c.y].saveState(s);
    }
    s.saveInt(time);
    
    s.saveObject(city);
    s.saveObjects(buildings);
    s.saveObjects(walkers  );
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
  }
  
  
  void attachCity(City city) {
    this.city = city;
  }
  
  
  
  
  /**  Tiles, blockage and paving:
    */
  Tile tileAt(int x, int y) {
    try { return grid[x][y]; }
    catch (Exception e) { return null; }
  }
  
  
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
  
  
  public static class Tile {
  
    int x, y;
    
    Terrain terrain;
    Fixture above;
    boolean paved;
    
    protected Object flag;
    
    
    void loadState(Session s) throws Exception {
      int terrID = s.loadInt();
      terrain = terrID == -1 ? null : ALL_TERRAINS[terrID];
      above   = (Fixture) s.loadObject();
      paved   = s.loadBool();
    }
    
    
    void saveState(Session s) throws Exception {
      s.saveInt(terrain == null ? -1 : terrain.terrainIndex);
      s.saveObject(above);
      s.saveBool(paved);
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
    
    
    public String toString() {
      return "T"+x+"|"+y;
    }
  }
  
  
  public static void applyPaving(
    CityMap map, int x, int y, int w, int h, boolean is
  ) {
    for (Coord c : Visit.grid(x, y, w, h, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t != null) t.paved = is;
    }
  }
  
  
  
  /**  Active updates:
    */
  void update() {
    if (city != null) city.world.updateFrom(this);
    for (Building b : buildings) b.update();
    for (Walker   w : walkers  ) w.update();
    
    time += 1;
    updateGrowth();
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





