
import util.*;



public class Building extends Fixture {
  
  float craftProgress;
  byte inventory[] = new byte[12];
  
  Tile entrance;
  int walkerCountdown = 0;
  List <Walker> walkers  = new List();
  List <Walker> visitors = new List();
  
  
  Building(ObjectType type) {
    super(type);
  }
  

  void enterMap(City map, int x, int y) {
    super.enterMap(map, x, y);
    map.buildings.add(this);
  }
  
  
  void update() {
    
    if (entrance == null || map.blocked(entrance.x, entrance.y)) {
      for (Coord c : Visit.perimeter(x, y, type.wide, type.high)) {
        boolean outx = c.x == x - 1 || c.x == x + type.wide;
        boolean outy = c.y == y - 1 || c.y == y + type.high;
        if (outx && outy         ) continue;
        if (map.blocked(c.x, c.y)) continue;
        if (! map.paved(c.x, c.y)) continue;
        entrance = map.tileAt(c.x, c.y);
        break;
      }
    }
    
    if (--walkerCountdown <= 0) {
      
      if (walkers.size() < type.maxWalkers && type.walkerType != null) {
        Walker walker = new Walker(type.walkerType);
        walker.enterMap(map, x, y);
        walker.inside = this;
        walker.home   = this;
        walkers.add(walker);
      }
      
      for (Walker walker : walkers) {
        if (walker.inside == this) walker.startRandomWalk();
      }
      
      walkerCountdown = type.walkerCountdown;
    }
  }
  
  
  void walkerPasses(Walker walker, Building other) {
  }
  
  
  void walkerEnters(Walker walker, Building enters) {
  }
}



