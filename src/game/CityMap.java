

package game;
import util.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



public class CityMap implements Session.Saveable {
  
  
  /**  Data fields and initialisation-
    */
  final static int
    SCAN_RES = 16,
    FLAG_RES = 4
  ;
  
  City city;
  int size, scanSize, flagSize;
  Tile grid[][];
  List <Actor> actorGrid[][];
  
  int time = 0;
  
  final CityMapSettings settings = new CityMapSettings(this);
  final CityMapPlanning planning = new CityMapPlanning(this);
  final CityMapFog      fog      = new CityMapFog     (this);
  final CityMapTerrain  terrain  = new CityMapTerrain (this);
  
  Table <City, Tile           > transitPoints = new Table();
  Table <Type, CityMapFlagging> flagging      = new Table();
  Table <String, CityMapDemands> demands = new Table();
  
  List <Building> buildings = new List();
  List <Actor   > actors    = new List();
  CityMapPathCache pathCache = new CityMapPathCache(this);
  
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
    for (Coord c : Visit.grid(0, 0, flagSize, flagSize, 1)) {
      s.loadObjects(actorGrid[c.x][c.y]);
    }
    time = s.loadInt();
    
    settings.loadState(s);
    planning.loadState(s);
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
    
    for (int n = s.loadInt(); n-- > 0;) {
      String key = s.loadString();
      CityMapDemands forKey = new CityMapDemands(this, key);
      forKey.loadState(s);
      demands.put(key, forKey);
    }
    
    s.loadObjects(buildings);
    s.loadObjects(actors   );
    pathCache.loadState(s);
    
    saveName = s.loadString();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(city);
    s.saveInt(size);
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      grid[c.x][c.y].saveState(s);
    }
    for (Coord c : Visit.grid(0, 0, flagSize, flagSize, 1)) {
      s.saveObjects(actorGrid[c.x][c.y]);
    }
    s.saveInt(time);
    
    settings.saveState(s);
    planning.saveState(s);
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
    
    s.saveInt(demands.size());
    for (String key : demands.keySet()) {
      s.saveString(key);
      demands.get(key).saveState(s);
    }
    
    s.saveObjects(buildings);
    s.saveObjects(actors   );
    pathCache.saveState(s);
    
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
    this.flagSize = Nums.round(size * 1f / FLAG_RES, 1, true);
    
    this.grid = new Tile[size][size];
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      Tile t = grid[c.x][c.y] = new Tile();
      t.x = c.x;
      t.y = c.y;
    }
    
    this.actorGrid = new List[flagSize][flagSize];
    for (Coord c : Visit.grid(0, 0, flagSize, flagSize, 1)) {
      actorGrid[c.x][c.y] = new List();
    }
    
    planning.performSetup(size);
    fog.performSetup(size);
    city.attachMap(this);
    
    pathCache.performSetup(size);
  }
  
  
  
  /**  Tiles and related setup/query methods-
    */
  public static class Tile implements Target, Flood.Fill {
    
    int x, y;
    
    int     elevation = 0   ;
    Terrain terrain   = null;
    Element above     = null;
    
    List <Actor> inside  = null;
    List <Actor> focused = null;
    Object pathFlag;  //  Note- this is used purely during path-searches, and
                      //  doesn't have to be saved or loaded.
    
    
    void loadState(Session s) throws Exception {
      elevation = s.loadInt();
      int terrID = s.loadInt();
      terrain = terrID == -1 ? null : ALL_TERRAINS[terrID];
      above   = (Element) s.loadObject();
      
      if (s.loadBool()) s.loadObjects(focused = new List());
      if (s.loadBool()) s.loadObjects(inside  = new List());
    }
    
    
    void saveState(Session s) throws Exception {
      s.saveInt(elevation);
      s.saveInt(terrain == null ? -1 : terrain.terrainIndex);
      s.saveObject(above);
      
      s.saveBool(focused != null);
      if (focused != null) s.saveObjects(focused);
      s.saveBool(inside  != null);
      if (inside  != null) s.saveObjects(inside );
    }
    
    
    public CityMap.Tile at() {
      return this;
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
    
    
    public Type aboveType() {
      return above == null ? null : above.type;
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
  
  
  public static Tile[] adjacent(Tile spot, Tile temp[], CityMap map) {
    if (temp == null) temp = new Tile[8];
    for (int dir : T_INDEX) {
      int x = spot.x + T_X[dir], y = spot.y + T_Y[dir];
      temp[dir] = map.tileAt(x, y);
    }
    return temp;
  }
  
  
  public static Tile[] pathAdjacent(
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
    return distance(a.x, a.y, b.x, b.y);
  }
  
  
  public static float distance(int ox, int oy, int dx, int dy) {
    float sx = ox - dx, sy = oy - dy;
    return Nums.sqrt((sx * sx) + (sy * sy));
  }
  
  
  public static float distancePenalty(float dist) {
    float range = MAX_WANDER_RANGE / 2;
    return range / (range + dist);
  }
  
  
  public static float distancePenalty(Tile a, Tile b) {
    return distancePenalty(distance(a, b));
  }
  
  
  Tile tileAt(int x, int y) {
    if (x < 0 || x >= size || y < 0 || y >= size) return null;
    return grid[x][y];
  }
  
  
  Tile tileAt(float x, float y) {
    return tileAt((int) x, (int) y);
  }
  
  
  Tile tileAt(Coord c) {
    return tileAt(c.x, c.y);
  }
  
  
  Visit <Tile> allTiles() {
    return tilesUnder(0, 0, size, size);
  }
  
  
  Visit <Tile> tilesUnder(int x, int y, int w, int h) {
    final Visit <Coord> VC = Visit.grid(x, y, w, h, 1);
    Visit <Tile> VT = new Visit <Tile> () {
      public boolean hasNext() { return VC.hasNext(); }
      public Tile next() { return tileAt(VC.next()); }
    };
    return VT;
  }
  
  
  
  /**  Blockage and paving methods-
    */
  Element above(int x, int y) {
    Tile under = tileAt(x, y);
    return under == null ? null : under.above;
  }
  
  
  Element above(Tile t) {
    return above(t.x, t.y);
  }
  
  
  Element above(Coord c) {
    return above(c.x, c.y);
  }
  
  
  boolean blocked(int x, int y) {
    Tile under = tileAt(x, y);
    if (under == null) {
      return true;
    }
    if (under.above != null) {
      return under.above.blocksPath();
    }
    if (under.terrain != null) {
      return under.terrain.blocks;
    }
    return false;
  }
  
  
  boolean blocked(Tile t) {
    return blocked(t.x, t.y);
  }
  
  
  boolean blocked(Coord c) {
    return blocked(c.x, c.y);
  }
  
  
  boolean paved(int x, int y) {
    Tile under = tileAt(x, y);
    if (under == null || under.above == null) return false;
    return under.above.type.paved;
  }
  
  
  boolean paved(Tile t) {
    return paved(t.x, t.y);
  }
  
  
  boolean paved(Coord c) {
    return paved(c.x, c.y);
  }
  
  
  void setTerrain(Tile t, Terrain ter, int elevation) {
    Terrain old = t.terrain;
    t.terrain   = ter;
    t.elevation = elevation;
    if (ter != old) pathCache.checkPathingChanged(t);
  }
  
  
  void setTerrain(Coord c, Terrain ter, int elevation) {
    setTerrain(tileAt(c), ter, elevation);
  }
  
  
  void setTerrain(int x, int y, Terrain ter, int elevation) {
    setTerrain(tileAt(x, y), ter, elevation);
  }
  
  
  void setAbove(Tile t, Element above) {
    Element old = t.above;
    t.above = above;
    if (old != above) pathCache.checkPathingChanged(t);
  }
  
  
  void setAbove(Coord c, Element above) {
    setAbove(tileAt(c), above);
  }
  
  
  void setAbove(int x, int y, Element above) {
    setAbove(tileAt(x, y), above);
  }
  
  
  
  /**  Active updates:
    */
  void update() {
    
    if (city != null) {
      city.world.updateWithTime(time);
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
    planning.updatePlanning();
    
    pathCache.updatePathCache();
  }
  
  
  
  /**  Various presence-related queries and updates-
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
  
  
  void flagActor(Actor a, Tile at, boolean is) {
    if (at == null || a == null) return;
    List <Actor> inBigGrid = actorGrid[at.x / FLAG_RES][at.y / FLAG_RES];
    inBigGrid.toggleMember(a, is);
    at.setInside(a, is);
  }
  
  
  Series <Actor> actorsInRange(Tile point, float range) {
    
    Box2D area = new Box2D(point.x / FLAG_RES, point.y / FLAG_RES, 0, 0);
    area.expandBy(Nums.round(range / FLAG_RES, 1, true));
    
    Batch <Actor> all = new Batch();
    for (Coord c : Visit.grid(area)) try {
      List <Actor> inBigGrid = actorGrid[c.x][c.y];
      
      for (Actor a : inBigGrid) {
        if (distance(a.at(), point) > range) continue;
        all.add(a);
      }
    }
    catch (ArrayIndexOutOfBoundsException e) {}
    
    return all;
  }
}





