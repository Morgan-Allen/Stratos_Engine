

import util.*;
import static util.TileConstants.*;



public class Fixture {
  
  ObjectType type;
  
  City map;
  int x, y, facing = N;
  
  float buildLevel;
  boolean complete;
  
  
  Fixture(ObjectType type) {
    this.type = type;
  }
  
  
  void enterMap(City map, int x, int y) {
    this.map = map;
    this.x   = x  ;
    this.y   = y  ;
    
    for (Coord c : Visit.grid(x, y, type.wide, type.high, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      t.above = this;
    }
  }
  
}