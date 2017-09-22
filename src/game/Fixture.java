

package game;
import util.*;
import static util.TileConstants.*;



public class Fixture implements Session.Saveable {
  
  ObjectType type;
  
  City map;
  int x, y, facing = N;
  
  float buildLevel;
  boolean complete;
  
  
  Fixture(ObjectType type) {
    this.type = type;
  }
  
  
  public Fixture(Session s) throws Exception {
    s.cacheInstance(this);
    
    type   = (ObjectType) s.loadObject();
    map    = (City) s.loadObject();
    x      = s.loadInt();
    y      = s.loadInt();
    facing = s.loadInt();
    
    buildLevel = s.loadFloat();
    complete   = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(type);
    s.saveObject(map);
    s.saveInt(x);
    s.saveInt(y);
    s.saveInt(facing);
    
    s.saveFloat(buildLevel);
    s.saveBool(complete);
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
  
  
  
  public String toString() {
    return type.name;
  }
}







