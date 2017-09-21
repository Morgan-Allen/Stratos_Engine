package game;


import util.*;



public class Tile {
  
  int x, y;
  Fixture above;
  boolean paved;
  
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
