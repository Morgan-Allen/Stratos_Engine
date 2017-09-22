

package game;
import util.*;



public class Tile {
  
  int x, y;
  Fixture above;
  boolean paved;
  
  protected Object flag;
  
  
  void loadState(Session s) throws Exception {
    above = (Fixture) s.loadObject();
    paved = s.loadBool();
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveObject(above);
    s.saveBool(paved);
  }
  
  
  static Tile loadTile(City map, Session s) throws Exception {
    int x = s.loadInt();
    if (x == -1) return null;
    int y = s.loadInt();
    return map.tileAt(x, y);
  }
  
  
  static void saveTile(Tile t, City map, Session s) throws Exception {
    if (t == null) {
      s.saveInt(-1);
      return;
    }
    s.saveInt(t.x);
    s.saveInt(t.y);
  }
  
  
  
  public static void applyPaving(
    City map, int x, int y, int w, int h, boolean is
  ) {
    for (Coord c : Visit.grid(x, y, w, h, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t != null) t.paved = is;
    }
  }
  
  
  public String toString() {
    return "T"+x+"|"+y;
  }
}




