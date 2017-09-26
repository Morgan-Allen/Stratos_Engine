

package game;
import util.*;
import static game.BuildingSet.*;
import static util.TileConstants.*;



public class Walker implements Session.Saveable {
  
  
  /**  Data fields and setup/initialisation-
    */
  final public static int
    JOB_NONE      = -1,
    JOB_RESTING   =  0,
    JOB_WANDER    =  1,
    JOB_DELIVER   =  2,
    JOB_SHOPPING  =  3,
    JOB_TRADING   =  4,
    JOB_VISITING  =  5,
    JOB_GATHERING =  6,
    
    MAX_WANDER_TIME = 20,
    TRADE_DIST_TIME = 50
  ;
  static int nextID = 0;
  int dirs[] = new int[4];
  
  
  ObjectType type;
  String ID;
  
  CityMap map;
  int x, y, facing = N;
  
  Building home;
  Building inside;
  int jobType = JOB_NONE;
  int timeSpent = 0;
  int maxTime = 20;
  
  Tile path[] = null;
  int pathIndex = -1;
  Tile target;
  Building visits;
  
  Good carried = null;
  float carryAmount = 0;
  
  
  Walker(ObjectType type) {
    this.type = type;
    this.ID = "#"+nextID++;
  }
  
  
  public Walker(Session s) throws Exception {
    s.cacheInstance(this);
    
    type = (ObjectType) s.loadObject();
    ID   = s.loadString();
    
    map    = (CityMap) s.loadObject();
    x      = s.loadInt();
    y      = s.loadInt();
    facing = s.loadInt();
    
    home      = (Building) s.loadObject();
    inside    = (Building) s.loadObject();
    jobType   = s.loadInt();
    timeSpent = s.loadInt();
    maxTime   = s.loadInt();
    
    int PL = s.loadInt();
    if (PL == -1) path = null;
    else {
      path = new Tile[PL];
      for (int i = 0; i < PL; i++) path[i] = Tile.loadTile(map, s);
    }
    pathIndex = s.loadInt();
    target = Tile.loadTile(map, s);
    visits = (Building) s.loadObject();
    
    carried = (Good) s.loadObject();
    carryAmount = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(type);
    s.saveString(ID);
    
    s.saveObject(map);
    s.saveInt(x);
    s.saveInt(y);
    s.saveInt(facing);
    
    s.saveObject(home);
    s.saveObject(inside);
    s.saveInt(jobType);
    s.saveInt(timeSpent);
    s.saveInt(maxTime);
    
    if (path == null) s.saveInt(-1);
    else {
      s.saveInt(path.length);
      for (Tile t : path) Tile.saveTile(t, map, s);
    }
    s.saveInt(pathIndex);
    Tile.saveTile(target, map, s);
    s.saveObject(visits);
    
    s.saveObject(carried);
    s.saveFloat(carryAmount);
  }
  
  
  
  /**  World entry and exit-
    */
  void enterMap(CityMap map, int x, int y) {
    this.map = map;
    this.x   = x  ;
    this.y   = y  ;
    map.walkers.add(this);
  }
  
  
  void exitMap() {
    if (home != null) home.walkers.remove(this);
    if (inside != null) setInside(inside, false);
    map.walkers.remove(this);
    home = null;
    map  = null;
  }
  
  
  
  /**  Regular updates-
    */
  void update() {
    
    if (home == null) {
      I.say(this+" is homeless!  Will exit world...");
      return;
    }
    
    if (path != null) {
      boolean visiting = visits != null;
      
      if (visiting && Visit.last(path) != visits.entrance) {
        pathToward(visits, target, jobType);
      }
      
      if (++pathIndex >= path.length) {
        if (visiting && inside != visits) {
          setInside(visits, true);
        }
        else if (timeSpent++ <= maxTime) {
          if (visiting) {
            onVisit(visits);
            home.walkerVisits(this, visits);
          }
          else {
            onTarget(target);
            home.walkerTargets(this, target);
          }
        }
        else {
          startReturnHome();
        }
      }
      
      else {
        Tile ahead = path[pathIndex];
        x = ahead.x;
        y = ahead.y;
        if (inside != null) setInside(inside, false);
      }
    }
    
    else if (jobType == JOB_WANDER) {
      if (inside != null) setInside(inside, false);
      
      int nx, ny, numDirs = 0;
      int backDir = (facing + 4) % 8;
      
      for (int dir : T_ADJACENT) {
        if (dir == backDir) continue;
        nx = x + T_X[dir];
        ny = y + T_Y[dir];
        if (! map.paved(nx, ny)) continue;
        dirs[numDirs] = dir;
        numDirs++;
      }
      if (numDirs == 0) {
        facing = backDir;
      }
      else if (numDirs > 1) {
        facing = dirs[Rand.index(numDirs)];
      }
      else {
        facing = dirs[0];
      }
      x = x + T_X[facing];
      y = y + T_Y[facing];
      
      if (++timeSpent >= maxTime) {
        startReturnHome();
      }
    }
  }
  
  
  
  /**  Pathing and visitation utilities:
    */
  private void setInside(Building b, boolean yes) {
    if (b == null || home == null) return;
    
    if (yes && b != inside) {
      b.visitors.include(this);
      inside = b;
      home.walkerEnters(this, inside);
    }
    if (b == inside && ! yes) {
      home.walkerExits(this, inside);
      b.visitors.remove(this);
      inside = null;
    }
  }

  
  private void assignPath(Tile path[], Building visits, Tile target) {
    this.path      = path;
    this.visits    = visits;
    this.target    = target;
    this.pathIndex = -1;
  }
  
  
  private void pathToward(Building visits, Tile target, int jobType) {
    
    boolean visiting = visits != null;
    this.jobType = jobType;

    I.say(this+" going to "+(visiting ? visits : target));
    
    Tile at    = (inside == null) ? map.tileAt(x, y) : inside.entrance;
    Tile heads = null;
    if (visiting) heads = visits.entrance;
    else          heads = target;
    
    PathSearch search = new PathSearch(map, this, at, heads);
    search.setPaveOnly(visiting && map.paved(at.x, at.y));
    search.doSearch();
    
    Tile path[] = search.fullPath(Tile.class);
    if (path != null) {
      I.say("  Path is: "+path.length+" tiles long...");
      assignPath(path, visits, target);
    }
    else {
      I.say("  Could not find path!");
    }
  }
  
  
  protected void onVisit(Building visits) {
    return;
  }
  
  
  protected void onTarget(Tile target) {
    return;
  }
  
  
  
  /**  Miscellaneous behaviour triggers:
    */
  void startRandomWalk() {
    if (inside == null || inside.entrance == null) return;
    
    I.say(this+" beginning random walk...");
    
    this.jobType = JOB_WANDER;
    assignPath(null, null, null);
    timeSpent = 0;
    maxTime = MAX_WANDER_TIME;
    
    Tile at = inside.entrance;
    x      = at.x;
    y      = at.y;
    facing = T_ADJACENT[Rand.index(4)];
    
    if (inside != null) setInside(inside, false);
  }
  
  
  void embarkOnVisit(Building goes, int maxTime, int jobType) {
    if (goes == null) return;
    
    I.say(this+" will visit "+goes+" for time "+maxTime);
    
    this.timeSpent = 0;
    this.maxTime   = maxTime;
    
    pathToward(goes, null, jobType);
  }
  
  
  void embarkOnTarget(Tile goes, int maxTime, int jobType) {
    if (goes == null) return;
    
    I.say(this+" will gather "+goes+" for time "+maxTime);
    
    this.timeSpent = 0;
    this.maxTime   = maxTime;
    
    pathToward(null, goes, jobType);
  }
  
  
  void startReturnHome() {
    if (home == null || home.entrance == null || inside == home) return;
    
    I.say(this+" will return home...");
    
    this.timeSpent = 0;
    this.maxTime   = 0;
    
    pathToward(home, null, JOB_RESTING);
  }
  
  
  void beginDelivery(
    Building from, Building goes, int jobType,
    Good carried, float amount
  ) {
    if (from == null || goes == null || goes.entrance == null) return;
    
    I.say(this+" will deliver "+amount+" "+carried+" to "+goes);
    
    from.inventory.add(0 - amount, carried);
    this.carried     = carried;
    this.carryAmount = amount ;
    pathToward(goes, null, jobType);
  }
  
  
  
  void offloadGood(Good carried, Building store) {
    if (store == null || carried != this.carried) return;
    
    I.say(this+" Depositing "+carried+" at "+store);
    
    store.inventory.add(carryAmount, carried);
    this.carried = null;
    this.carryAmount = 0;
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  public String toString() {
    return type.name+" "+ID;
  }
}




