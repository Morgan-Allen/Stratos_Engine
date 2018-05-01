  

package game;
import graphics.common.*;
import start.*;
import util.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



public class Area implements Session.Saveable {
  
  
  /**  Data fields and initialisation-
    */
  final public static int
    SCAN_RES  = 16,
    FLAG_RES  = 4,
    FLAG_AREA = 4 * 4
  ;
  
  Terrain terrainTypes[] = { EMPTY };
  
  final public World world;
  final public World.Locale locale;
  final public Base locals;
  List <Base> bases = new List();
  
  int size, scanSize, flagSize;
  AreaTile grid[][];
  List <Active> actorGrid[][];
  
  int time = 0;
  int numUpdates = 0, ticksPS = 1;
  
  final public AreaPlanning  planning  = new AreaPlanning (this);
  final public AreaTerrain   terrain   = new AreaTerrain  (this);
  final public AreaPathCache pathCache = new AreaPathCache(this);
  float lightLevel;
  
  Table <Base, AreaFog   > fogMaps       = new Table();
  Table <Base, AreaDanger> dangerMaps    = new Table();
  Table <Base, AreaTile  > transitPoints = new Table();
  
  Table <Type, AreaFlagging> flagging = new Table();
  Table <String, AreaDemands> demands = new Table();
  
  List <Building> claimants = new List();
  List <Building> buildings = new List();
  List <Actor   > actors    = new List();
  List <Actor   > vessels   = new List();
  
  
  String saveName;
  final public Ephemera ephemera = new Ephemera(this);
  
  
  
  public Area(World world, World.Locale locale, Base... cities) {
    this.world = world;
    this.locale = locale;
    this.locals = new Base(world, locale, "Locals: "+locale);
    
    locals.setGovernment(Base.GOVERNMENT.BARBARIAN);
    locals.council.setTypeAI(BaseCouncil.AI_OFF);
    addBase(locals);
    
    for (Base c : cities) addBase(c);
  }
  
  
  public Area(Session s) throws Exception {
    s.cacheInstance(this);
    //
    //  NOTE:  Tiles MUST be set up before all other objects in the world to
    //  avoid problems with target-loading!
    terrainTypes = (Terrain[]) s.loadObjectArray(Terrain.class);
    performSetup(s.loadInt(), terrainTypes);
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      grid[c.x][c.y].loadState(s, this);
    }
    for (Coord c : Visit.grid(0, 0, flagSize, flagSize, 1)) {
      s.loadObjects(actorGrid[c.x][c.y]);
    }
    
    world = (World) s.loadObject();
    locale = world.locales.atIndex(s.loadInt());
    locals = (Base) s.loadObject();
    s.loadObjects(bases);
    
    time       = s.loadInt();
    numUpdates = s.loadInt();
    ticksPS    = s.loadInt();
    
    planning .loadState(s);
    terrain  .loadState(s);
    pathCache.loadState(s);
    lightLevel = s.loadFloat();
    
    for (int n = s.loadInt(); n-- > 0;) {
      Base with = (Base) s.loadObject();
      AreaFog fog = new AreaFog(with, this);
      fog.performSetup(size);
      fog.loadState(s);
      fogMaps.put(with, fog);
    }
    
    for (int n = s.loadInt(); n-- > 0;) {
      Base with = (Base) s.loadObject();
      AreaDanger danger = new AreaDanger(with, this);
      danger.performSetup(flagSize);
      danger.loadState(s);
      dangerMaps.put(with, danger);
    }
    
    for (int n = s.loadInt(); n-- > 0;) {
      Base with = (Base) s.loadObject();
      AreaTile point = loadTile(this, s);
      transitPoints.put(with, point);
    }
    
    for (int n = s.loadInt(); n-- > 0;) {
      Type key = (Type) s.loadObject();
      AreaFlagging forKey = new AreaFlagging(this, key, 1);
      forKey.setupWithSize(size);
      forKey.loadState(s);
      flagging.put(key, forKey);
    }
    
    for (int n = s.loadInt(); n-- > 0;) {
      String key = s.loadString();
      AreaDemands forKey = new AreaDemands(this, key);
      forKey.loadState(s);
      demands.put(key, forKey);
    }
    
    s.loadObjects(claimants);
    s.loadObjects(buildings);
    s.loadObjects(actors   );
    s.loadObjects(vessels  );
    
    saveName = s.loadString();
    ephemera.loadState(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    //
    //  NOTE:  Tiles MUST be set up before all other objects in the world to
    //  avoid problems with target-loading!
    s.saveObjectArray(terrainTypes);
    s.saveInt(size);
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      grid[c.x][c.y].saveState(s, this);
    }
    for (Coord c : Visit.grid(0, 0, flagSize, flagSize, 1)) {
      s.saveObjects(actorGrid[c.x][c.y]);
    }
    
    s.saveObject(world);
    s.saveInt(world.locales.indexOf(locale));
    s.saveObject(locals);
    s.saveObjects(bases);
    
    s.saveInt(time);
    s.saveInt(numUpdates);
    s.saveInt(ticksPS);
    
    planning .saveState(s);
    terrain  .saveState(s);
    pathCache.saveState(s);
    s.saveFloat(lightLevel);
    
    s.saveInt(fogMaps.size());
    for (Base b : fogMaps.keySet()) {
      s.saveObject(b);
      AreaFog fog = fogMaps.get(b);
      fog.saveState(s);
    }
    
    s.saveInt(dangerMaps.size());
    for (Base b : dangerMaps.keySet()) {
      s.saveObject(b);
      AreaDanger danger = dangerMaps.get(b);
      danger.saveState(s);
    }
    
    s.saveInt(transitPoints.size());
    for (Base c : transitPoints.keySet()) {
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
    
    s.saveObjects(claimants);
    s.saveObjects(buildings);
    s.saveObjects(actors   );
    s.saveObjects(vessels  );
    
    s.saveString(saveName);
    ephemera.saveState(s);
  }
  
  
  public void addBase(Base base) {
    bases.include(base);
    base.attachMap(this);
  }
  
  
  public void performSetup(int size, Terrain terrainTypes[]) {
    
    this.terrainTypes = terrainTypes;
    
    int s = 1;
    while (s < size) s *= 2;
    size = s;
    
    this.size     = size;
    this.scanSize = Nums.round(size * 1f / SCAN_RES, 1, true);
    this.flagSize = Nums.round(size * 1f / FLAG_RES, 1, true);
    
    this.grid = new AreaTile[size][size];
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      grid[c.x][c.y] = new AreaTile(c.x, c.y);
    }
    
    this.actorGrid = new List[flagSize][flagSize];
    for (Coord c : Visit.grid(0, 0, flagSize, flagSize, 1)) {
      actorGrid[c.x][c.y] = new List();
    }
    
    terrain  .performSetup(size);
    planning .performSetup(size);
    pathCache.performSetup(size);
    
    for (Base b : bases) {
      fogMap(b, true);
      dangerMap(b, true);
    }
  }
  
  
  
  /**  Basic public access methods-
    */
  public Series <Building> claimants() {
    return claimants;
  }
  
  
  public Series <Building> buildings() {
    return buildings;
  }
  
  
  public Series <Actor> actors() {
    return actors;
  }
  
  
  public Series <Actor> vessels() {
    return vessels;
  }
  
  
  public Series <Base> bases() {
    return bases;
  }
  
  
  
  /**  Tiles and related setup/query methods-
    */
  static AreaTile loadTile(Area map, Session s) throws Exception {
    int x = s.loadInt();
    if (x == -1) return null;
    int y = s.loadInt();
    return map.tileAt(x, y);
  }
  
  
  static void saveTile(AreaTile t, Area map, Session s) throws Exception {
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
  
  
  static Target loadTarget(Area map, Session s) throws Exception {
    if (s.loadBool()) return loadTile(map, s);
    else return (Target) s.loadObject();
  }
  
  
  static void saveTarget(Target t, Area map, Session s) throws Exception {
    if (t == null ) { s.saveBool(false); s.saveObject(null); return; }
    if (t.isTile()) { s.saveBool(true ); saveTile((AreaTile) t, map, s); }
    else            { s.saveBool(false); s.saveObject(t)           ; }
  }
  
  
  public static AreaTile[] adjacent(AreaTile spot, AreaTile temp[], Area map) {
    if (temp == null) temp = new AreaTile[9];
    if (map == null || spot == null) return temp;
    for (int dir : T_INDEX) {
      int x = spot.x + T_X[dir], y = spot.y + T_Y[dir];
      temp[dir] = map.tileAt(x, y);
    }
    return temp;
  }
  
  
  public static float distance(AreaTile a, AreaTile b) {
    if (a == null || b == null) return 1000000000;
    return distance(a.x, a.y, b.x, b.y);
  }
  
  
  private static Vec3D v1 = new Vec3D(), v2 = new Vec3D();
  
  public static float distance(Target a, Target b) {
    if (a == null || b == null) return 1000000000;
    a.exactPosition(v1);
    b.exactPosition(v2);
    v1.z = v2.z = 0;
    return v1.distance(v2);
  }
  
  
  public static boolean adjacent(Target a, Target b) {
    if (a == null || b == null) return false;
    AreaTile AA = a.at(), AB = b.at();
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
  
  
  public static float distancePenalty(AreaTile a, AreaTile b) {
    return distancePenalty(distance(a, b));
  }
  
  
  public static float distancePenalty(Target a, Target b) {
    return distancePenalty(distance(a, b));
  }
  
  
  public int time() {
    return time;
  }
  
  
  public int ticksPerSecond() {
    return ticksPS;
  }
  
  
  public float timeInUpdate() {
    return (numUpdates % ticksPS) * 1f / ticksPS;
  }
  
  
  public static int timeSince(int time, int from) {
    if (time == -1) return -1;
    return from - time;
  }
  
  
  public AreaTile tileAt(int x, int y) {
    if (x < 0 || x >= size || y < 0 || y >= size) return null;
    return grid[x][y];
  }
  
  
  public AreaTile tileAt(float x, float y) {
    return tileAt((int) x, (int) y);
  }
  
  
  public AreaTile tileAt(Coord c) {
    return tileAt(c.x, c.y);
  }
  
  
  public Visit <AreaTile> allTiles() {
    return tilesUnder(0, 0, size, size);
  }
  
  
  public Visit <AreaTile> tilesUnder(int x, int y, int w, int h) {
    final Visit <Coord> VC = Visit.grid(x, y, w, h, 1);
    Visit <AreaTile> VT = new Visit <AreaTile> () {
      public boolean hasNext() { return VC.hasNext(); }
      public AreaTile next() { return tileAt(VC.next()); }
    };
    return VT;
  }
  
  
  public Visit <AreaTile> tilesAround(int x, int y, int w, int h) {
    final Visit <Coord> VC = Visit.perimeter(x, y, w, h);
    Visit <AreaTile> VT = new Visit <AreaTile> () {
      public boolean hasNext() { return VC.hasNext(); }
      public AreaTile next() { return tileAt(VC.next()); }
    };
    return VT;
  }
  
  
  public Visit <AreaTile> tilesUnder(Box2D area) {
    return tilesUnder(
      (int) area.xpos(), (int) area.ypos(),
      (int) area.xdim(), (int) area.ydim()
    );
  }
  
  
  public Visit <AreaTile> tilesAround(Box2D area) {
    return tilesAround(
      (int) area.xpos(), (int) area.ypos(),
      (int) area.xdim(), (int) area.ydim()
    );
  }
  
  
  
  /**  Blockage and paving methods-
    */
  public Element above(int x, int y) {
    AreaTile under = tileAt(x, y);
    return under == null ? null : under.above;
  }
  
  
  public Element above(AreaTile t) {
    if (t == null) return null;
    return above(t.x, t.y);
  }
  
  
  public Element above(Coord c) {
    if (c == null) return null;
    return above(c.x, c.y);
  }
  
  
  public int pathType(AreaTile t) {
    if (t == null) return Type.PATH_BLOCK;
    return t.pathType();
  }
  
  
  public int pathType(Coord c) {
    if (c == null) return Type.PATH_BLOCK;
    return pathType(c.x, c.y);
  }
  
  
  public int pathType(int x, int y) {
    return pathType(tileAt(x, y));
  }
  
  
  public boolean blocked(AreaTile t) {
    int pathing = pathType(t);
    return pathing == Type.PATH_BLOCK || pathing == Type.PATH_WATER;
  }
  
  
  public boolean blocked(Coord c) {
    return blocked(tileAt(c));
  }
  
  
  public boolean blocked(int x, int y) {
    return blocked(tileAt(x, y));
  }
  
  
  public void setTerrain(AreaTile t, Terrain ter, byte var, int elevation) {
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
  
  
  public void setAbove(AreaTile t, Element above) {
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
  public void update(int ticksPS) {
    
    this.ticksPS = ticksPS;
    numUpdates += 1;
    time = numUpdates / ticksPS;
    boolean exactTick = numUpdates % ticksPS == 0;
    
    float dayProg = world.calendar.dayProgress();
    lightLevel = (dayProg * (1 - dayProg)) / 4;
    
    if (exactTick) {
      world.updateWithTime(time);
    }
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

    if (time % SCAN_PERIOD == 0 && exactTick) {
      transitPoints.clear();
    }
    if (exactTick) for (AreaDanger danger : dangerMaps.values()) {
      danger.updateDanger();
    }
    for (AreaFog fog : fogMaps.values()) {
      fog.updateFog();
    }
    terrain.updateTerrain();
    planning.updatePlanning();
    pathCache.updatePathCache();
  }
  
  
  
  /**  Various presence-related queries and updates-
    */
  public AreaFlagging flagMap(Type key, boolean init) {
    AreaFlagging forKey = flagging.get(key);
    if (forKey != null) return forKey;
    if (init) {
      forKey = new AreaFlagging(this, key, 1);
      forKey.setupWithSize(size);
      flagging.put(key, forKey);
    }
    return forKey;
  }
  
  
  void flagType(Type key, int x, int y, boolean is) {
    AreaFlagging forKey = flagMap(key, true);
    forKey.setFlagVal(x, y, is ? 1 : 0);
  }
  
  
  void flagActive(Active a, AreaTile at, boolean is) {
    if (at == null || a == null) return;
    List <Active> inBigGrid = actorGrid[at.x / FLAG_RES][at.y / FLAG_RES];
    inBigGrid.toggleMember(a, is);
    if (a.mobile()) at.setInside((Actor) a, is);
  }
  
  
  Series <Active> activeInRange(AreaTile point, float range) {
    
    Box2D area = new Box2D(point.x / FLAG_RES, point.y / FLAG_RES, 0, 0);
    area.expandBy(Nums.round(range / FLAG_RES, 1, true));
    
    Batch <Active> all = new Batch();
    for (Coord c : Visit.grid(area)) try {
      List <Active> inBigGrid = actorGrid[c.x][c.y];
      
      for (Active a : inBigGrid) {
        if (! a.onMap()) I.complain("\nACTIVE ON GRID BUT NOT ON MAP!");
        if (distance(a.at(), point) > range) continue;
        all.add(a);
      }
    }
    catch (ArrayIndexOutOfBoundsException e) {}
    
    return all;
  }
  
  
  public Series <Active> gridActive(AreaTile at) {
    return actorGrid[at.x / FLAG_RES][at.y / FLAG_RES];
  }
  
  
  public float lightLevel() {
    return lightLevel;
  }
  
  
  public AreaFog fogMap(Base base, boolean init) {
    AreaFog fog = fogMaps.get(base);
    if (fog == null && init) {
      fog = new AreaFog(base, this);
      fog.performSetup(size);
      fogMaps.put(base, fog);
    }
    return fog;
  }
  
  
  public AreaDanger dangerMap(Base base, boolean init) {
    AreaDanger danger = dangerMaps.get(base);
    if (danger == null && init) {
      danger = new AreaDanger(base, this);
      danger.performSetup(flagSize);
      dangerMaps.put(base, danger);
    }
    return danger;
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    return "Map for "+locale;
  }
  
  
  public void renderStage(Rendering rendering, Base playing) {
    
    //  TODO:  You might watch out for loss-of-precision over longer games?
    float renderTime = (numUpdates + Rendering.frameAlpha()) / ticksPS;
    
    
    Box2D area = new Box2D(0, 0, size, size);
    terrain.readyAllMeshes();
    
    boolean report = false;
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
        for (AreaTile t : tilesUnder(x, y, 4, 4)) {
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
    if (I.used60Frames && report) {
      I.say("Displayed "+chunksShown+" chunks out of "+totalChunks);
    }
    
    AreaFog fog = fogMap(playing, false);
    if (fog != null) fog.renderFor(renderTime, rendering);
    
    for (Ephemera.Ghost ghost : ephemera.visibleFor(rendering, playing, renderTime)) {
      ephemera.renderGhost(ghost, rendering, playing);
    }
    
    for (Building venue : buildings) {
      if (! venue.canRender(playing, rendering.view)) continue;
      venue.renderElement(rendering, playing);
    }
    
    for (Actor actor : actors) {
      if (! actor.canRender(playing, rendering.view)) continue;
      actor.renderElement(rendering, playing);
    }
    
    for (Base base : bases) {
      //  TODO:  Reconsider this bit?
      if (base != playing) continue;
      
      for (Mission mission : base.missions()) {
        if (! mission.canRender(playing, rendering.view)) continue;
        mission.renderFlag(rendering);
      }
    }
    
  }
}


