

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class CityMapPlanning {
  
  
  final CityMap map;
  Type grid[][];
  
  
  
  CityMapPlanning(CityMap map) {
    this.map  = map;
  }
  
  
  void loadState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      grid[c.x][c.y] = (Type) s.loadObject();
    }
  }
  
  
  void saveState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      s.saveObject(grid[c.x][c.y]);
    }
  }
  
  
  void performSetup(int size) {
    this.grid = new Type[map.size][map.size];
  }
  
  
  void placeObject(Type type, Tile t) {
    for (Coord c : Visit.grid(t.x, t.y, type.wide, type.high, 1)) {
      grid[c.x][c.y] = type;
      checkNeedForBuilding(t);
    }
  }
  
  
  Type objectAt(Tile t) {
    return grid[t.x][t.y];
  }
  
  
  boolean checkNeedForBuilding(Tile t) {
    Type type = objectAt(t);
    Element above = t.above;
    if (above == null || above.type != type || above.buildLevel() < 1) {
      map.flagType(NEED_BUILD, t.x, t.y, true);
      return true;
    }
    else {
      map.flagType(NEED_BUILD, t.x, t.y, false);
      return false;
    }
  }
  
  
  

  
  /**  Some helper methods for dealing with infrastructure:
    */
  public static void applyPaving(
    CityMap map, int x, int y, int w, int h, boolean is
  ) {
    for (Coord c : Visit.grid(x, y, w, h, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t == null) continue;
      if (is) {
        Element e = (Element) ROAD.generate();
        e.enterMap(map, x, y, 1);
      }
      else if (t.aboveType() == ROAD) {
        t.above.exitMap(map);
      }
    }
  }
  
  
  public static void demolish(
    CityMap map, int x, int y, int w, int h
  ) {
    for (Coord c : Visit.grid(x, y, w, h, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t       == null) continue;
      if (t.above != null) t.above.exitMap(map);
    }
  }
}







