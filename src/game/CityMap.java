  

package game;
import graphics.common.*;
import start.*;
import util.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



public class CityMap implements Session.Saveable {
  
  
  /**  Data fields and initialisation-
    */
  final public static int
    SCAN_RES    = 16,
    FLAG_RES    =  4,
    
    PATH_NONE   = -1,
    PATH_WATER  =  0,
    PATH_PAVE   =  1,
    PATH_FREE   =  2,
    PATH_HINDER =  3,
    PATH_BLOCK  =  4,
    PATH_WALLS  =  5
  ;
  
  Terrain terrainTypes[] = { EMPTY };
  
  final public World world;
  final public World.Locale locale;
  final public City locals;
  List <City> cities = new List();
  
  int size, scanSize, flagSize;
  Tile grid[][];
  List <Actor> actorGrid[][];
  
  int time = 0;
  int numUpdates = 0, ticksPS = PlayLoop.UPDATES_PER_SECOND;
  
  final public CityMapPlanning planning = new CityMapPlanning(this);
  final public CityMapFog      fog      = new CityMapFog     (this);
  final public CityMapTerrain  terrain  = new CityMapTerrain (this);
  
  Table <City, Tile> transitPoints = new Table();
  Table <Type, CityMapFlagging> flagging = new Table();
  Table <String, CityMapDemands> demands = new Table();
  
  List <Building> buildings = new List();
  List <Actor   > actors    = new List();
  final public CityMapPathCache pathCache = new CityMapPathCache(this);
  
  String saveName;
  
  
  public CityMap(World world, World.Locale locale, City... cities) {
    this.world = world;
    this.locale = locale;
    this.locals = new City(world, locale, "Locals: "+locale);
    
    locals.setGovernment(City.GOVERNMENT.BARBARIAN);
    locals.council.setTypeAI(CityCouncil.AI_OFF);
    addCity(locals);
    
    for (City c : cities) addCity(c);
  }
  
  
  public CityMap(Session s) throws Exception {
    s.cacheInstance(this);
    
    terrainTypes = (Terrain[]) s.loadObjectArray(Terrain.class);
    
    world = (World) s.loadObject();
    locale = world.locales.atIndex(s.loadInt());
    locals = (City) s.loadObject();
    s.loadObjects(cities);
    
    performSetup(s.loadInt(), terrainTypes);
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      grid[c.x][c.y].loadState(s, this);
    }
    for (Coord c : Visit.grid(0, 0, flagSize, flagSize, 1)) {
      s.loadObjects(actorGrid[c.x][c.y]);
    }
    
    time = s.loadInt();
    numUpdates = s.loadInt();
    ticksPS    = s.loadInt();
    
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
    
    s.saveObjectArray(terrainTypes);
    
    s.saveObject(world);
    s.saveInt(world.locales.indexOf(locale));
    s.saveObject(locals);
    s.saveObjects(cities);
    
    s.saveInt(size);
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      grid[c.x][c.y].saveState(s, this);
    }
    for (Coord c : Visit.grid(0, 0, flagSize, flagSize, 1)) {
      s.saveObjects(actorGrid[c.x][c.y]);
    }
    
    s.saveInt(time);
    s.saveInt(numUpdates);
    s.saveInt(ticksPS   );
    
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
  
  
  public void performSetup(int size, Terrain terrainTypes[]) {
    
    this.terrainTypes = terrainTypes;
    
    int s = 1;
    while (s < size) s *= 2;
    size = s;
    
    this.size     = size;
    this.scanSize = Nums.round(size * 1f / SCAN_RES, 1, true);
    this.flagSize = Nums.round(size * 1f / FLAG_RES, 1, true);
    
    this.grid = new Tile[size][size];
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      grid[c.x][c.y] = new Tile(c.x, c.y);
    }
    
    this.actorGrid = new List[flagSize][flagSize];
    for (Coord c : Visit.grid(0, 0, flagSize, flagSize, 1)) {
      actorGrid[c.x][c.y] = new List();
    }
    
    terrain  .performSetup(size);
    planning .performSetup(size);
    fog      .performSetup(size);
    pathCache.performSetup(size);
  }
  
  
  
  /**  Basic public access methods-
    */
  public Series <Building> buildings() {
    return buildings;
  }
  
  
  public Series <Actor> actors() {
    return actors;
  }
  
  
  public void addCity(City city) {
    cities.include(city);
    city.attachMap(this);
  }
  
  
  public Series <City> cities() {
    return cities;
  }
  
  
  
  /**  Tiles and related setup/query methods-
    */
  static Tile loadTile(CityMap map, Session s) throws Exception {
    int x = s.loadInt();
    if (x == -1) return null;
    int y = s.loadInt();
    return map.tileAt(x, y);
  }
  
  
  static void saveTile(Tile t, CityMap map, Session s) throws Exception {
    if (t != null && map == null) {
      I.complain("CANNOT SAVE TILE WITHOUT MAP");
      return;
    }
    if (t == null) {
      s.saveInt(-1);
      return;
    }
    s.saveInt(t.x);
    s.saveInt(t.y);
  }
  
  
  static Target loadTarget(CityMap map, Session s) throws Exception {
    if (s.loadBool()) return loadTile(map, s);
    else return (Target) s.loadObject();
  }
  
  
  static void saveTarget(Target t, CityMap map, Session s) throws Exception {
    if (t == null ) { s.saveBool(false); s.saveObject(null); return; }
    if (t.isTile()) { s.saveBool(true ); saveTile((Tile) t, map, s); }
    else            { s.saveBool(false); s.saveObject(t)           ; }
  }
  
  
  public static Tile[] adjacent(Tile spot, Tile temp[], CityMap map) {
    if (temp == null) temp = new Tile[9];
    if (map == null || spot == null) return temp;
    for (int dir : T_INDEX) {
      int x = spot.x + T_X[dir], y = spot.y + T_Y[dir];
      temp[dir] = map.tileAt(x, y);
    }
    return temp;
  }
  
  
  public static float distance(Tile a, Tile b) {
    if (a == null || b == null) return 1000000000;
    return distance(a.x, a.y, b.x, b.y);
  }
  
  
  public static float distance(Target a, Target b) {
    if (a == null || b == null) return 1000000000;
    return distance(a.at(), b.at());
  }
  
  
  public static boolean adjacent(Target a, Target b) {
    if (a == null || b == null) return false;
    Tile AA = a.at(), AB = b.at();
    if (AA == null || AB == null) return false;
    Type TA = a.type(), TB = b.type();
    if (AA.x > AB.x + TB.wide) return false;
    if (AA.y > AB.y + TB.high) return false;
    if (AB.x > AA.x + TA.wide) return false;
    if (AB.y > AA.y + TA.high) return false;
    return true;
  }
  
  
  public int size() {
    return size;
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
  
  
  public static float distancePenalty(Target a, Target b) {
    return distancePenalty(distance(a, b));
  }
  
  
  public int time() {
    return time;
  }
  
  
  public static int timeSince(int time, int from) {
    if (time == -1) return -1;
    return from - time;
  }
  
  
  public Tile tileAt(int x, int y) {
    if (x < 0 || x >= size || y < 0 || y >= size) return null;
    return grid[x][y];
  }
  
  
  public Tile tileAt(float x, float y) {
    return tileAt((int) x, (int) y);
  }
  
  
  public Tile tileAt(Coord c) {
    return tileAt(c.x, c.y);
  }
  
  
  public Visit <Tile> allTiles() {
    return tilesUnder(0, 0, size, size);
  }
  
  
  public Visit <Tile> tilesUnder(int x, int y, int w, int h) {
    final Visit <Coord> VC = Visit.grid(x, y, w, h, 1);
    Visit <Tile> VT = new Visit <Tile> () {
      public boolean hasNext() { return VC.hasNext(); }
      public Tile next() { return tileAt(VC.next()); }
    };
    return VT;
  }
  
  
  public Visit <Tile> tilesAround(int x, int y, int w, int h) {
    final Visit <Coord> VC = Visit.perimeter(x, y, w, h);
    Visit <Tile> VT = new Visit <Tile> () {
      public boolean hasNext() { return VC.hasNext(); }
      public Tile next() { return tileAt(VC.next()); }
    };
    return VT;
  }
  
  
  
  /**  Blockage and paving methods-
    */
  public Element above(int x, int y) {
    Tile under = tileAt(x, y);
    return under == null ? null : under.above;
  }
  
  
  public Element above(Tile t) {
    if (t == null) return null;
    return above(t.x, t.y);
  }
  
  
  public Element above(Coord c) {
    if (c == null) return null;
    return above(c.x, c.y);
  }
  
  
  public int pathType(Tile t) {
    if (t == null) return PATH_BLOCK;
    return t.pathType();
  }
  
  
  public int pathType(Coord c) {
    if (c == null) return PATH_BLOCK;
    return pathType(c.x, c.y);
  }
  
  
  public int pathType(int x, int y) {
    return pathType(tileAt(x, y));
  }
  
  
  public boolean blocked(Tile t) {
    int pathing = pathType(t);
    return pathing == PATH_BLOCK || pathing == PATH_WATER;
  }
  
  
  public boolean blocked(Coord c) {
    return blocked(tileAt(c));
  }
  
  
  public boolean blocked(int x, int y) {
    return blocked(tileAt(x, y));
  }
  
  
  public void setTerrain(Tile t, Terrain ter, byte var, int elevation) {
    int oldP = t.pathType();
    t.terrain   = ter;
    t.elevation = elevation;
    this.terrain.updateFrom(t);
    this.terrain.setVariant(t, var);
    if (oldP != t.pathType()) pathCache.checkPathingChanged(t);
  }
  
  
  public void setTerrain(Coord c, Terrain ter, byte var, int elevation) {
    setTerrain(tileAt(c), ter, var, elevation);
  }
  
  
  public void setTerrain(int x, int y, Terrain ter, byte var, int elevation) {
    setTerrain(tileAt(x, y), ter, var, elevation);
  }
  
  
  public void setAbove(Tile t, Element above) {
    int oldP = t.pathType();
    t.above = above;
    if (oldP != t.pathType()) pathCache.checkPathingChanged(t);
  }
  
  
  public void setAbove(Coord c, Element above) {
    setAbove(tileAt(c), above);
  }
  
  
  public void setAbove(int x, int y, Element above) {
    setAbove(tileAt(x, y), above);
  }
  
  
  
  /**  Active updates:
    */
  public void update() {
    
    world.updateWithTime(time);
    
    for (Building b : buildings) {
      if (b.map == null) {
        I.complain("\n"+b+" has no map but still registered on map!");
        continue;
      }
      b.update();
    }
    for (Actor a : actors) {
      if (a.map == null) {
        I.complain("\n"+a+" has no map but still registered on map!");
        continue;
      }
      a.update();
    }
    
    if (time % SCAN_PERIOD == 0) {
      transitPoints.clear();
    }
    
    time += 1;
    
    fog.updateFog();
    terrain.updateTerrain();
    planning.updatePlanning();
    
    pathCache.updatePathCache();
  }
  
  
  
  /**  Various presence-related queries and updates-
    */
  public CityMapFlagging flagMap(Type key, boolean init) {
    CityMapFlagging forKey = flagging.get(key);
    if (forKey != null) return forKey;
    if (init) {
      forKey = new CityMapFlagging(this, key, 1);
      forKey.setupWithSize(size);
      flagging.put(key, forKey);
    }
    return forKey;
  }
  
  
  void flagType(Type key, int x, int y, boolean is) {
    CityMapFlagging forKey = flagMap(key, true);
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
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    return "Map for "+locale;
  }
  
  
  public void renderStage(Rendering rendering, City playing) {
    float renderTime = (numUpdates + Rendering.frameAlpha()) / ticksPS;
    
    Box2D area = new Box2D(0, 0, size, size);
    terrain.readyAllMeshes();
    int totalChunks = 0, chunksShown = 0;
    
    List <Box2D> descent = new List();
    descent.add(area);
    Vec3D centre = new Vec3D();
    
    while (descent.size() > 0) {
      Box2D b = descent.removeFirst();
      centre.x = (b.xpos() + b.xmax()) / 2;
      centre.y = (b.ypos() + b.ymax()) / 2;
      float radius = 1.5f * b.xdim() / 2;
      
      if (! rendering.view.intersects(centre, radius)) continue;
      
      if (b.xdim() <= 4) {
        int x = (int) b.xpos(), y = (int) b.ypos();
        for (Tile t : tilesUnder(x, y, 4, 4)) {
          if (t.above == null || t.above.type().isBuilding()) continue;
          
          if (t.above.at() == t) {
            if (t.above.canRender(playing, rendering.view)) {
              t.above.renderElement(rendering, playing);
            }
          }
        }
        terrain.renderFor(b, rendering, Rendering.activeTime());
        chunksShown += 1;
      }
      else {
        float hS = b.xdim() / 2;
        descent.add(new Box2D(b.xpos(), b.ypos(), hS, hS));
        descent.add(new Box2D(centre.x, b.ypos(), hS, hS));
        descent.add(new Box2D(b.xpos(), centre.y, hS, hS));
        descent.add(new Box2D(centre.x, centre.y, hS, hS));
      }
    }
    
    totalChunks = (size / 4) * (size / 4);
    if (I.used60Frames) {
      //I.say("Displayed "+chunksShown+" chunks out of "+totalChunks);
    }
    
    fog.renderFor(renderTime, rendering);
    
    /*
    for (Ghost ghost : ephemera.visibleFor(rendering, playing, renderTime)) {
      ghost.renderFor(rendering, playing);
    }
    //*/
    
    for (Building venue : buildings) {
      if (! venue.canRender(playing, rendering.view)) continue;
      venue.renderElement(rendering, playing);
    }
    
    for (Actor actor : actors) {
      if (! actor.canRender(playing, rendering.view)) continue;
      actor.renderElement(rendering, playing);
    }
    
    for (City base : cities) {
      for (Mission mission : base.missions()) {
        if (! mission.canRender(playing, rendering.view)) continue;
        mission.renderFlag(rendering);
      }
    }
    
  }
  
  
  //public Ephemera ephemera() {
    //return ephemera;
  //}
}


