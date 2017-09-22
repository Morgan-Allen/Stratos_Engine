


package game;
import util.*;
//import static util.TileConstants.*;



public class City implements Session.Saveable {
  
  
  /**  Data fields and initialisation-
    */
  int size;
  Tile grid[][];
  
  List <Building> buildings = new List();
  List <Walker  > walkers   = new List();
  
  Table <Object, AmountMap> demands = new Table();
  
  
  City() {
    return;
  }
  
  
  public City(Session s) throws Exception {
    s.cacheInstance(this);
    size = s.loadInt();
    
    performSetup(size);
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      grid[c.x][c.y].loadState(s);
    }
    
    s.loadObjects(buildings);
    s.loadObjects(walkers  );
    //s.saveTally(demands);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(size);
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      grid[c.x][c.y].saveState(s);
    }
    s.saveObjects(buildings);
    s.saveObjects(walkers  );
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
    return under == null ? true : (under.above != null);
  }
  
  
  boolean paved(int x, int y) {
    Tile under = tileAt(x, y);
    return under == null ? false : under.paved;
  }
  
  
  
  
  /**  Active updates:
    */
  void update() {
    for (Building b : buildings) b.update();
    for (Walker   w : walkers  ) w.update();
  }
}















