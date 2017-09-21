

import util.*;
//import static util.TileConstants.*;



public class City {
  
  int size;
  Tile grid[][];
  
  List <Building> buildings = new List();
  List <Walker  > walkers   = new List();
  
  
  void performSetup(int size) {
    this.size = size;
    this.grid = new Tile[size][size];
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      Tile t = grid[c.x][c.y] = new Tile();
      t.x = c.x;
      t.y = c.y;
    }
  }
  
  
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
  
  
  void update() {
    for (Building b : buildings) b.update();
    for (Walker   w : walkers  ) w.update();
  }
}




class Tile {
  
  int x, y;
  Fixture above;
  boolean paved;
  
  //List <Walker> inside = new List();
  protected Object flag;
  
  
  public static void applyPaving(
    City map, int x, int y, int w, int h, boolean is
  ) {
    for (Coord c : Visit.grid(x, y, w, h, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t != null) t.paved = is;
    }
  }
}













