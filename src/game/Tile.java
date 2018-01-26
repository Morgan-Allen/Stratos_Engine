

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



public class Tile implements Pathing {
  
  int x, y;
  
  int elevation = 0;
  Terrain terrain = EMPTY;
  Element above = null;
  
  List <Actor> inside  = null;
  List <Actor> focused = null;
  private Object pathFlag;  //  Only used during temporary path-searches...
  
  
  void loadState(Session s, CityMap map) throws Exception {
    elevation = s.loadInt();
    int terrID = s.loadInt();
    terrain = terrID == -1 ? EMPTY : map.terrainTypes[terrID];
    above   = (Element) s.loadObject();
    
    if (s.loadBool()) s.loadObjects(focused = new List());
    if (s.loadBool()) s.loadObjects(inside  = new List());
  }
  
  
  void saveState(Session s, CityMap map) throws Exception {
    s.saveInt(elevation);
    s.saveInt(Visit.indexOf(terrain, map.terrainTypes));
    s.saveObject(above);
    
    s.saveBool(focused != null);
    if (focused != null) s.saveObjects(focused);
    s.saveBool(inside  != null);
    if (inside  != null) s.saveObjects(inside );
  }
  
  
  public Tile at() {
    return this;
  }
  
  
  public Type type() {
    return terrain;
  }
  
  
  public boolean onMap() {
    return true;
  }
  
  
  public void flagWith(Object o) {
    pathFlag = o;
  }
  
  
  public Object flaggedWith() {
    return pathFlag;
  }
  
  
  public void targetedBy(Actor w) {
    return;
  }
  
  
  public void setFocused(Actor a, boolean is) {
    focused = Element.setMember(a, is, focused);
  }
  
  
  public Series <Actor> focused() {
    return focused == null ? NO_ACTORS : focused;
  }
  
  
  public boolean hasFocus() {
    return focused != null;
  }
  
  
  public boolean isTile() {
    return true;
  }
  
  
  public int pathType() {
    if (above != null) {
      int pathing = above.pathType();
      if (pathing != PATH_NONE) return pathing;
    }
    if (terrain != null) {
      return terrain.pathing;
    }
    return PATH_FREE;
  }
  
  
  public Pathing[] adjacent(Pathing[] temp, CityMap map) {
    if (temp == null) temp = new Pathing[9];
    
    int pathT = pathType();
    boolean blocked = pathT == PATH_BLOCK || pathT == PATH_WATER;
    
    if (above != null && above.allowsEntryFrom(this)) {
      temp[8] = (Pathing) above;
    }
    else {
      temp[8] = null;
    }
    
    for (int dir : T_INDEX) {
      Tile n = map.tileAt(x + T_X[dir], y + T_Y[dir]);
      if (n == null || blocked) { temp[dir] = null; continue; }
      
      int pathN = n.pathType();
      if (n.above != above && n.above != null && n.above.allowsEntryFrom(this)) {
        temp[dir] = (Pathing) n.above;
      }
      else if (pathN == PATH_BLOCK || pathN == PATH_WATER) {
        temp[dir] = null;
      }
      else if (pathN == PATH_WALLS || pathT == PATH_WALLS) {
        temp[dir] = pathN == pathT ? n : null;
      }
      else {
        temp[dir] = n;
      }
    }
    
    return temp;
  }
  
  
  public boolean allowsEntryFrom(Pathing p) {
    return true;
  }
  
  
  public boolean allowsEntry(Actor a) {
    return true;
  }
  
  
  public void setInside(Actor a, boolean is) {
    inside = Element.setMember(a, is, inside);
  }
  
  
  public Series <Actor> inside() {
    return inside == null ? NO_ACTORS : inside;
  }
  
  
  
  /**  Various utility and convenience methods-
    */
  static Tile nearestOpenTile(Tile from, CityMap map) {
    return nearestOpenTile(from, map, 1);
  }
  

  static Tile nearestOpenTile(Tile from, CityMap map, int maxRange) {
    
    if (from == null || ! map.blocked(from)) return from;
    if (maxRange <= 0) return null;

    for (Tile t : CityMap.adjacent(from, null, map)) {
      if (t == null || map.blocked(t)) continue;
      return t;
    }
    if (maxRange == 1) return null;
    
    int x = from.x, y = from.y, w = 1, h = 1;
    for (int range = 2; range <= maxRange; range++) {
      x -= 1;
      y -= 1;
      w += 2;
      h += 2;
      for (Tile t : map.tilesAround(x, y, w, h)) {
        if (t == null || map.blocked(t)) continue;
        return t;
      }
    }
    
    return null;
  }
  
  
  
  /**  Graphical debug and interface methods-
    */
  public String toString() {
    return "T"+x+"|"+y;
  }
}


