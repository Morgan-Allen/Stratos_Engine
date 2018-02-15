

package game;
import gameUI.play.*;
import graphics.common.Viewport;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



public class Tile implements Pathing, Selection.Focus {
  
  final public int x, y;
  
  int elevation = 0;
  Terrain terrain = EMPTY;
  Element above = null;
  
  List <Actor > inside  = null;
  List <Active> focused = null;
  private Object pathFlag;  //  Only used during temporary path-searches...
  
  
  Tile(int x, int y) {
    this.x = x;
    this.y = y;
  }
  
  
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
  
  
  
  /**  Satisfying target and pathing interface-
    */
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
  
  
  public void targetedBy(Active w) {
    return;
  }
  
  
  public void setFocused(Active a, boolean is) {
    focused = Element.setMember(a, is, focused);
  }
  
  
  public Series <Active> focused() {
    return focused == null ? NONE_ACTIVE : focused;
  }
  
  
  public boolean hasFocus() {
    return focused != null;
  }
  
  
  public boolean isTile() {
    return true;
  }
  
  
  public boolean indoors() {
    return false;
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
  
  
  
  /**  Terrain and elevation methods-
    */
  public int elevation() {
    return elevation;
  }
  
  
  public Terrain terrain() {
    return terrain;
  }
  
  
  
  /**  Various utility and convenience methods-
    */
  public static Tile nearestOpenTile(Tile from, CityMap map) {
    return nearestOpenTile(from, map, 1);
  }
  

  public static Tile nearestOpenTile(Tile from, CityMap map, int maxRange) {
    
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
  
  
  public String fullName() {
    return terrain.name;
  }
  
  
  public void whenClicked(Object context) {
    // TODO Auto-generated method stub
  }
  
  
  public boolean testSelection(PlayUI UI, City base, Viewport port) {
    return false;
  }


  public boolean setSelected(PlayUI UI) {
    return false;
  }
  
  
  public boolean trackSelection() {
    return false;
  }
  
  
  public Vec3D trackPosition() {
    return new Vec3D(x, y, 0);
  }
}






