package game;


import util.*;
import static util.TileConstants.*;



public class Walker {
  
  
  /**  Data fields and setup/initialisation-
    */
  final public static int
    JOB_NONE     = -1,
    JOB_RESTING  =  0,
    JOB_WANDER   =  1,
    JOB_DELIVER  =  2,
    JOB_SHOPPING =  3,
    JOB_VISITING =  4
  ;
  
  
  ObjectType type;
  
  City map;
  int x, y, facing = N;
  
  Building home;
  Building inside;
  int distWalked = 0, maxWalked = 20;
  
  int jobType = JOB_NONE;
  int dirs[] = new int[4];
  Tile path[] = null;
  int pathIndex = -1;
  Building destination;
  
  Goods.Good carried = null;
  float carryAmount = 0;
  
  
  Walker(ObjectType type) {
    this.type = type;
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
      pathIndex += 1;
      if (pathIndex >= path.length) {
        setInside(destination, true);
        home.walkerEnters(this, destination);
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
      
      if (++distWalked >= maxWalked) {
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
    if (yes) {
      b.visitors.include(this);
      inside = b;
    }
    else {
      b.visitors.remove(this);
      inside = null;
    }
  }
  
  
  void pathToward(Building goes, int jobType) {
    
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
    distWalked = 0;
    
    Tile at = inside.entrance;
    x      = at.x;
    y      = at.y;
    facing = T_ADJACENT[Rand.index(4)];
    
    if (inside != null) setInside(inside, false);
  }
  
  
  void startReturnHome() {
    if (home == null || home.entrance == null) return;
    
    I.say(this+" will return home...");
    
    pathToward(home, JOB_RESTING);
  }
  
  
  void beginDelivery(
    Building from, Building goes, int jobType,
    Goods.Good carried, float amount
  ) {
    if (from == null || goes == null || goes.entrance == null) return;
    
    I.say(this+" will deliver "+amount+" "+carried+" to "+goes);
    
    from.inventory.add(0 - amount, carried);
    this.carried     = carried;
    this.carryAmount = amount ;
    pathToward(goes, jobType);
  }
  
  
  void offloadGood(Goods.Good carried, Building store) {
    if (store == null || carried != this.carried) return;
    
    I.say(this+" Depositing "+" "+carried+" at "+store);
    
    store.inventory.add(carryAmount, carried);
    this.carried = null;
    this.carryAmount = 0;
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  public String toString() {
    return type.name;
  }
}




