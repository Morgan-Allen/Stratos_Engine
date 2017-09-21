

import util.*;
import static util.TileConstants.*;



public class Walker {
  
  ObjectType type;
  
  City map;
  int x, y, facing = N;
  
  Building home;
  Building inside;
  int distWalked = 0, maxWalked = 20;
  
  int dirs[] = new int[4];
  Tile path[] = null;
  int pathIndex = -1;
  Building destination;
  
  
  
  Walker(ObjectType type) {
    this.type = type;
  }
  
  
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
  
  
  void update() {
    
    if (home == null) {
      I.say(this+" is homeless!  Will exit world...");
      return;
    }
    
    if (path != null) {
      pathIndex += 1;
      if (pathIndex >= path.length) {
        setInside(destination, true);
      }
      else {
        Tile ahead = path[pathIndex];
        x = ahead.x;
        y = ahead.y;
        if (inside != null) setInside(inside, false);
      }
    }
    
    else {
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
  
  
  void startRandomWalk() {
    I.say(this+" beginning random walk...");
    assignPath(null, null);
    distWalked = 0;
    
    Tile at = inside.entrance;
    x      = at.x;
    y      = at.y;
    facing = T_ADJACENT[Rand.index(4)];
    
    if (inside != null) setInside(inside, false);
  }
  
  
  void startReturnHome() {
    I.say(this+" will return home...");
    
    Tile at = map.tileAt(x, y);
    PathSearch search = new PathSearch(map, this, at, home.entrance);
    //search.verbosity = Search.SUPER_VERBOSE;
    search.doSearch();
    Tile path[] = search.fullPath(Tile.class);
    
    if (path != null) {
      I.say("  Path is: "+path.length+" tiles long...");
      assignPath(path, home);
    }
  }
  
  
  public String toString() {
    return type.name;
  }
}




