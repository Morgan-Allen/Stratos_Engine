

package game;
import gameUI.play.*;
import graphics.common.Viewport;
import util.*;
import static game.AreaMap.*;
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
  
  
  void loadState(Session s, AreaMap map) throws Exception {
    elevation = s.loadInt();
    int terrID = s.loadInt();
    terrain = terrID == -1 ? EMPTY : map.terrainTypes[terrID];
    above   = (Element) s.loadObject();
    
    if (s.loadBool()) s.loadObjects(focused = new List());
    if (s.loadBool()) s.loadObjects(inside  = new List());
  }
  
  
  void saveState(Session s, AreaMap map) throws Exception {
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
  
  
  public float radius() {
    return 0.5f;
  }
  
  
  public float height() {
    return 0;
  }
  
  
  public boolean onMap() {
    return true;
  }
  
  
  public Type type() {
    return terrain;
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
      if (pathing != Type.PATH_NONE) return pathing;
    }
    if (terrain != null) {
      return terrain.pathing;
    }
    return Type.PATH_FREE;
  }
  
  
  public Pathing[] adjacent(Pathing[] temp, AreaMap map) {
    if (temp == null) temp = new Pathing[9];
    //
    //  Determine whether this tile is blocked-
    int pathT = pathType();
    boolean blocked = pathT == Type.PATH_BLOCK || pathT == Type.PATH_WATER;
    //
    //  Allow access to the element above this tile if permitted-
    if (above != null && above.allowsEntryFrom(this)) {
      temp[8] = (Pathing) above;
    }
    else {
      temp[8] = null;
    }
    //
    //  Visit each adjacent tile and see if they permit entry-
    for (int dir : T_INDEX) {
      Tile n = map.tileAt(x + T_X[dir], y + T_Y[dir]);
      if (n == null || blocked) { temp[dir] = null; continue; }
      
      int pathN = n.pathType();
      if (n.above != above && n.above != null && n.above.allowsEntryFrom(this)) {
        temp[dir] = (Pathing) n.above;
      }
      else if (pathN == Type.PATH_BLOCK || pathN == Type.PATH_WATER) {
        temp[dir] = null;
      }
      else if (pathN == Type.PATH_WALLS || pathT == Type.PATH_WALLS) {
        temp[dir] = pathN == pathT ? n : null;
      }
      else {
        temp[dir] = n;
      }
    }
    //
    //  Only allow access to diagonal tiles if both the tiles on either side
    //  are clear.
    for (int dir : T_DIAGONAL) {
      Pathing bef = temp[(dir + 1) % 8];
      Pathing aft = temp[(dir + 7) % 8];
      if (bef == null || aft == null) temp[dir] = null;
    }
    //
    //  And return-
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
  
  
  public Vec3D exactPosition(Vec3D store) {
    if (store == null) store = new Vec3D();
    store.set(x + 0.5f, y + 0.5f, 0);
    return store;
  }
  
  
  public Terrain terrain() {
    return terrain;
  }
  
  
  
  /**  Various utility and convenience methods-
    */
  public static Tile nearestOpenTile(Tile from, AreaMap map) {
    return nearestOpenTile(from, map, 1);
  }
  

  public static Tile nearestOpenTile(Tile from, AreaMap map, int maxRange) {
    
    if (from == null || ! map.blocked(from)) return from;
    if (maxRange <= 0) return null;

    for (Tile t : AreaMap.adjacent(from, null, map)) {
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
  
  
  public boolean testSelection(PlayUI UI, Base base, Viewport port) {
    return false;
  }


  public boolean setSelected(PlayUI UI) {
    return false;
  }
  
  
  public boolean trackSelection() {
    return false;
  }
  
  
  public Vec3D trackPosition() {
    return new Vec3D(x + 0.5f, y + 0.5f, 0);
  }
  
  
  public Vec3D renderedPosition(Vec3D store) {
    if (store == null) store = new Vec3D();
    store.set(x + 0.5f, y + 0.5f, 0);
    return store;
  }
}






