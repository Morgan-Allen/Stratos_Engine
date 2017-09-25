


package game;
import static game.Terrains.*;
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
    if (under == null || under.above == null) return false;
    return under.above.type.blocks;
  }
  
  
  boolean paved(int x, int y) {
    Tile under = tileAt(x, y);
    return under == null ? false : under.paved;
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





