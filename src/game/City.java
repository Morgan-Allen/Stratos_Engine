package game;


import util.*;
//import static util.TileConstants.*;



public class City {
  
  
  /**  Data fields and initialisation-
    */
  int size;
  Tile grid[][];
  
  List <Building> buildings = new List();
  List <Walker  > walkers   = new List();
  
  Table <Object, AmountMap> demands = new Table();
  
  
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















