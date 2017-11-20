

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class CityMapPlanning {
  
  
  final CityMap map;
  Type grid[][];
  List <Element> toPlace = new List();
  
  
  
  CityMapPlanning(CityMap map) {
    this.map  = map;
  }
  
  
  void loadState(Session s) throws Exception {
    s.loadObjects(toPlace);
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      grid[c.x][c.y] = (Type) s.loadObject();
    }
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveObjects(toPlace);
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
    }
    
    Element e = (Element) type.generate();
    e.setLocation(t);
    toPlace.add(e);
    
    for (Good m : type.builtFrom) {
      CityMapDemands d = TaskBuilding2.demandsFor(m, map);
      TaskBuilding2.checkNeedForBuilding(e, m, d);
    }
  }
  
  
  Type objectAt(Tile t) {
    return grid[t.x][t.y];
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







