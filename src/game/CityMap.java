

package game;
import util.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



public class CityMap implements Session.Saveable {
  
  
  /**  Data fields and initialisation-
    */
  final static int SCAN_RES = 16;
  
  City city;
  int size, scanSize;
  Tile grid[][];
  
  int time = 0;
  
  final CityMapSettings settings = new CityMapSettings();
  final CityMapFog      fog      = new CityMapFog    (this);
  final CityMapTerrain  terrain  = new CityMapTerrain(this);
  Table <City, Tile           > transitPoints = new Table();
  Table <Type, CityMapFlagging> flagging      = new Table();
  
  List <Building> buildings = new List();
  List <Actor   > actors    = new List();
  
  String saveName;
  
  
  CityMap(City city) {
    this.city = city;
  }
  
  
  public CityMap(Session s) throws Exception {
    s.cacheInstance(this);
    
    city = (City) s.loadObject();
    performSetup(s.loadInt());
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      grid[c.x][c.y].loadState(s);
    }
    time = s.loadInt();
    
    settings.loadState(s);
    fog     .loadState(s);
    terrain .loadState(s);
    
    for (int n = s.loadInt(); n-- > 0;) {
      City with = (City) s.loadObject();
      Tile point = loadTile(this, s);
      transitPoints.put(with, point);
    }
    
    for (int n = s.loadInt(); n-- > 0;) {
      Type key = (Type) s.loadObject();
      CityMapFlagging forKey = new CityMapFlagging(this, key, 1);
      forKey.setupWithSize(size);
      forKey.loadState(s);
      flagging.put(key, forKey);
    }
    
    s.loadObjects(buildings);
    s.loadObjects(actors   );
    
    saveName = s.loadString();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(city);
    s.saveInt(size);
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      grid[c.x][c.y].saveState(s);
    }
    s.saveInt(time);
    
    settings.saveState(s);
    fog     .saveState(s);
    terrain .saveState(s);
    
    s.saveInt(transitPoints.size());
    for (City c : transitPoints.keySet()) {
      s.saveObject(c);
      saveTile(transitPoints.get(c), this, s);
    }
    
    s.saveInt(flagging.size());
    for (Type key : flagging.keySet()) {
      s.saveObject(key);
      flagging.get(key).saveState(s);
    }
    
    s.saveObjects(buildings);
    s.saveObjects(actors   );
    
    s.saveString(saveName);
  }
  
  
  void performSetup(int size) {
    
    //  TODO:  This might not be strictly required?
    //*
    int s = 1;
    while (s < size) s *= 2;
    size = s;
    //*/
    
    this.size     = size;
    this.scanSize = Nums.round(size * 1f / SCAN_RES, 1, true);
    
    this.grid = new Tile[size][size];
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      Tile t = grid[c.x][c.y] = new Tile();
      t.x = c.x;
      t.y = c.y;
    }
    
    fog.performSetup(size);
    city.assignMap(this);
  }
  
  
  
  /**  Tiles and related setup/query methods-
    */
  public static class Tile implements Target {
    
    int x, y;
    
    Terrain terrain;
    Element above;
    boolean paved;
    
    List <Actor> inside  = null;
    List <Actor> focused = null;
    Object pathFlag;  //  Note- this is used purely during path-searches, and
                      //  doesn't have to be saved or loaded.
    
    
    void loadState(Session s) throws Exception {
      int terrID = s.loadInt();
      terrain = terrID == -1 ? null : ALL_TERRAINS[terrID];
      above   = (Element) s.loadObject();
      paved   = s.loadBool();
      
      if (s.loadBool()) s.loadObjects(focused = new List());
      if (s.loadBool()) s.loadObjects(inside  = new List());
    }
    
    
    void saveState(Session s) throws Exception {
      s.saveInt(terrain == null ? -1 : terrain.terrainIndex);
      s.saveObject(above);
      s.saveBool(paved);
      
      s.saveBool(focused != null);
      if (focused != null) s.saveObjects(focused);
      s.saveBool(inside  != null);
      if (inside  != null) s.saveObjects(inside );
    }
    
    
    public CityMap.Tile at() {
      return this;
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
    
    
    public void setInside(Actor a, boolean is) {
      inside = Element.setMember(a, is, inside);
    }
    
    
    public Series <Actor> inside() {
      return inside == null ? NO_ACTORS : inside;
    }
    
    
    public boolean hasFocus() {
      return focused != null;
    }
    
    
    public String toString() {
      return "T"+x+"|"+y;
    }
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
  
  
  public static Tile[] adjacent(
    Tile spot, Tile temp[], CityMap map, boolean paveOnly
  ) {
    if (temp == null) temp = new Tile[8];
    
    for (int dir : T_INDEX) {
      temp[dir] = null;
      int x = spot.x + T_X[dir], y = spot.y + T_Y[dir];
      if (paveOnly) {
        if (map.paved(x, y)) temp[dir] = map.tileAt(x, y);
      }
      else {
        if (! map.blocked(x, y)) temp[dir] = map.tileAt(x, y);
      }
    }
    return temp;
  }
  
  
  public static float distance(Tile a, Tile b) {
    if (a == null || b == null) return 1000000000;
    float dx = a.x - b.x, dy = a.y - b.y;
    return Nums.sqrt((dx * dx) + (dy * dy));
  }
  
  
  public static float distancePenalty(float dist) {
    float range = MAX_WANDER_RANGE / 2;
    return range / (range + dist);
  }
  
  
  public static float distancePenalty(Tile a, Tile b) {
    return distancePenalty(distance(a, b));
  }
  
  
  Tile tileAt(int x, int y) {
    try { return grid[x][y]; }
    catch (Exception e) { return null; }
  }
  
  
  Tile tileAt(Coord c) {
    return tileAt(c.x, c.y);
  }
  
  
  
  /**  Blockage and paving methods-
    */
  Element above(int x, int y) {
    Tile under = tileAt(x, y);
    return under == null ? null : under.above;
  }
  
  
  Element above(Coord c) {
    return above(c.x, c.y);
  }
  
  
  boolean blocked(int x, int y) {
    Tile under = tileAt(x, y);
    if (under == null) return true;
    
    Terrain terr = under.terrain;
    if (under.above != null) return under.above.type.blocks;
    else return terr == null ? false : terr.blocks;
  }
  
  
  boolean blocked(Coord c) {
    return blocked(c.x, c.y);
  }
  
  
  boolean paved(int x, int y) {
    Tile under = tileAt(x, y);
    return under == null ? false : under.paved;
  }
  
  
  boolean paved(Coord c) {
    return paved(c.x, c.y);
  }
  
  
  public static void applyPaving(
    CityMap map, int x, int y, int w, int h, boolean is
  ) {
    for (Coord c : Visit.grid(x, y, w, h, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t == null) continue;
      t.paved = is;
      if (t.above == null) continue;
      t.above.exitMap(map);
    }
  }
  
  
  public static void demolish(
    CityMap map, int x, int y, int w, int h
  ) {
    for (Coord c : Visit.grid(x, y, w, h, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t       == null) continue;
      if (t.paved        ) t.paved = false;
      if (t.above != null) t.above.exitMap(map);
    }
  }
  
  
  
  /**  Active updates:
    */
  void update() {
    if (city != null) {
      city.world.updateFrom(this);
    }
    for (Building b : buildings) {
      b.update();
    }
    for (Actor w : actors) {
      w.update();
    }
    
    if (time % SCAN_PERIOD == 0) {
      transitPoints.clear();
    }
    
    if (time % MONTH_LENGTH == 0) {
      CityBorders.spawnMigrants(this, MONTH_LENGTH);
    }
    
    time += 1;
    
    fog.updateFog();
    terrain.updateTerrain();
  }
  
  
  
  /**  Flagging the presence/absence of specific object-types:
    */
  void flagType(Type key, int x, int y, boolean is) {
    CityMapFlagging forKey = flagging.get(key);
    if (forKey == null) {
      forKey = new CityMapFlagging(this, key, 1);
      forKey.setupWithSize(size);
      flagging.put(key, forKey);
    }
    forKey.setFlagVal(x, y, is ? 1 : 0);
  }
}





