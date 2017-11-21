

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class CityMapPlanning {
  
  
  final CityMap map;
  Element grid[][];
  
  
  
  CityMapPlanning(CityMap map) {
    this.map  = map;
  }
  
  
  void loadState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      grid[c.x][c.y] = (Element) s.loadObject();
    }
  }
  
  
  void saveState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      s.saveObject(grid[c.x][c.y]);
    }
  }
  
  
  void performSetup(int size) {
    this.grid = new Element[map.size][map.size];
  }
  
  
  
  /**  Support methods for object-placement within the plan:
    */
  void togglePlacement(Element e, boolean place) {
    if (e == null) return;
    
    Type type = e.type;
    Tile at   = e.at();
    
    for (Coord c : Visit.grid(at.x, at.y, type.wide, type.high, 1)) {
      if (place) {
        togglePlacement(map.above(c)  , false);
        togglePlacement(grid[c.x][c.y], false);
        grid[c.x][c.y] = e;
      }
      else {
        grid[c.x][c.y] = null;
        //  TODO:  Natural objects set for razing should be unflagged.
      }
    }
    
    //  TODO:  You need to ensure that any impeding structures are
    //  cleared before you flag the new structure.
    
    if (place) {
      /*
      for (Good m : mats) {
        CityMapDemands d = TaskBuilding2.demandsFor(m, map);
        TaskBuilding2.checkNeedForBuilding(e, m, d);
      }
      //*/
    }
    else {
      for (Good m : type.builtFrom) {
        CityMapDemands d = TaskBuilding2.demandsFor(m, map);
        TaskBuilding2.checkNeedForBuilding(e, m, d);
      }
    }
  }
  
  
  Element placeObject(Type type, int x, int y) {
    return placeObject(type, map.tileAt(x, y));
  }
  
  
  Element placeObject(Type type, Tile t) {
    Element e = (Element) type.generate();
    e.setLocation(t);
    togglePlacement(e, true);
    return e;
  }
  
  
  void unplaceObject(Element e) {
    togglePlacement(e, false);
  }
  
  
  Element objectAt(Tile t) {
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







