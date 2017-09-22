

package game;
import util.*;
import static game.Goods.*;
import static util.TileConstants.*;



public class Walker implements Session.Saveable {
  
  
  /**  Data fields and setup/initialisation-
    */
  final public static int
    JOB_NONE     = -1,
    JOB_RESTING  =  0,
    JOB_WANDER   =  1,
    JOB_DELIVER  =  2,
    JOB_SHOPPING =  3,
    JOB_VISITING =  4,
    
    MAX_WANDER_TIME = 20
  ;
  static int nextID = 0;
  int dirs[] = new int[4];
  
  
  ObjectType type;
  String ID;
  
  City map;
  int x, y, facing = N;
  
  Building home;
  Building inside;
  int jobType = JOB_NONE;
  int timeSpent = 0;
  int maxTime = 20;
  
  Tile path[] = null;
  int pathIndex = -1;
  Building destination;
  
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
    
    map    = (City) s.loadObject();
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
    destination = (Building) s.loadObject();
    
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
    s.saveObject(destination);
    
    s.saveObject(carried);
    s.saveFloat(carryAmount);
  }
  
  
  
  /**  World entry and exit-
    */
  void enterMap(City map, int x, int y) {
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
      if (Visit.last(path) != destination.entrance) {
        pathToward(destination, jobType);
      }
      
      if (++pathIndex >= path.length) {
        if (inside != destination) {
          setInside(destination, true);
        }
        else if (++timeSpent <= maxTime) {
          home.walkerVisits(this, destination);
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
  void assignPath(Tile path[], Building destination) {
    this.path        = path;
    this.destination = destination;
    this.pathIndex   = -1;
  }
  
  
  void setInside(Building b, boolean yes) {
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
  
  
  void pathToward(Building goes, int jobType) {
    I.say(this+" going to "+goes);
    
    this.jobType = jobType;
    
    Tile at = (inside == null) ? map.tileAt(x, y) : inside.entrance;
    PathSearch search = new PathSearch(map, this, at, goes.entrance);
    search.doSearch();
    Tile path[] = search.fullPath(Tile.class);
    
    if (path != null) {
      I.say("  Path is: "+path.length+" tiles long...");
      assignPath(path, goes);
    }
    else {
      I.say("  Could not find path!");
    }
  }
  
  
  
  /**  Miscellaneous behaviour triggers:
    */
  void startRandomWalk() {
    if (inside == null || inside.entrance == null) return;
    
    I.say(this+" beginning random walk...");
    
    this.jobType = JOB_WANDER;
    assignPath(null, null);
    timeSpent = 0;
    maxTime = MAX_WANDER_TIME;
    
    Tile at = inside.entrance;
    x      = at.x;
    y      = at.y;
    facing = T_ADJACENT[Rand.index(4)];
    
    if (inside != null) setInside(inside, false);
  }
  
  
  void embarkOnVisit(Building goes, int maxTime) {
    if (goes == null) return;
    
    I.say(this+" will visit "+goes+" for time "+maxTime);
    
    this.timeSpent = 0;
    this.maxTime   = maxTime;
    
    pathToward(goes, JOB_VISITING);
  }
  
  
  void startReturnHome() {
    if (home == null || home.entrance == null || inside == home) return;
    
    I.say(this+" will return home...");
    
    pathToward(home, JOB_RESTING);
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
    pathToward(goes, jobType);
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




