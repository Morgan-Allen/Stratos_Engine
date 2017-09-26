

package game;
import static game.GameConstants.*;
import util.*;



public class Tile {
  
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
  
  
  
  public static void applyPaving(
    CityMap map, int x, int y, int w, int h, boolean is
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




