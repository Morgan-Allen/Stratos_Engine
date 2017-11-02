

package game;
import util.*;



public class CityMapPlanning {
  
  
  CityMap map;
  Type grid[][];
  List <Element> toPlace = new List();
  List <Element> toRaze  = new List();
  
  
  
  CityMapPlanning(CityMap map) {
    this.map  = map;
    this.grid = new Type[map.size][map.size];
  }
  
  
  void placeObject(Type t, int x, int y) {
    for (Coord c : Visit.grid(x, y, t.wide, t.high, 1)) {
      grid[c.x][c.y] = t;
    }
  }
  
  
}