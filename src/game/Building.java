

package game;
import util.*;



public class Building extends Fixture {
  
  
  /**  Data fields and setup/initialisation-
    */
  static int nextID = 0;
  
  String ID;
  
  Tile entrance;
  int walkerCountdown = 0;
  List <Walker> walkers  = new List();
  List <Walker> visitors = new List();
  
  float craftProgress;
  Tally <Goods.Good> inventory = new Tally();
  Tally <Goods.Good> demands   = new Tally();
  
  
  Building(ObjectType type) {
    super(type);
    this.ID = "#"+nextID++;
  }
  
  
  
  /**  World entry and exit-
    */
  void enterMap(City map, int x, int y) {
    super.enterMap(map, x, y);
    map.buildings.add(this);
    selectEntrance();
  }
  
  
  void selectEntrance() {
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
  
  
  
  /**  Regular updates:
    */
  void update() {
    
    if (entrance == null || map.blocked(entrance.x, entrance.y)) {
      selectEntrance();
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
        if (walker.inside == this) selectWalkerBehaviour(walker);
      }
      
      walkerCountdown = type.walkerCountdown;
    }
  }
  
  
  
  /**  Customising walker behaviour:
    */
  void selectWalkerBehaviour(Walker walker) {
    walker.startRandomWalk();
  }
  
  
  void walkerPasses(Walker walker, Building other) {
    return;
  }
  
  
  void walkerEnters(Walker walker, Building enters) {
    return;
  }
  
  
  void walkerVisits(Walker walker, Building visits) {
    return;
  }
  
  
  void walkerExits(Walker walker, Building enters) {
    return;
  }
  
  
  
  /**  Handling goods and inventory:
    */
  Building findNearestOfType(ObjectType type, int maxDist) {
    return findNearestDemanding(type, null, null, -1);
  }
  
  
  Building findNearestWithFeature(Goods.Good feature, int maxDist) {
    return findNearestDemanding(null, feature, null, -1);
  }
  
  
  Building findNearestDemanding(
    ObjectType type, Goods.Good needed, int maxDist
  ) {
    return findNearestDemanding(type, null, needed, maxDist);
  }
  
  
  Building findNearestDemanding(
    ObjectType type, Goods.Good feature,
    Goods.Good needed, int maxDist
  ) {
    Pick <Building> pick = new Pick();
    
    for (Building b : map.buildings) {
      if (type != null && b.type != type) continue;
      
      boolean featured = Visit.arrayIncludes(b.type.features, feature);
      if (feature != null && ! featured) continue;
      
      float dist = PathSearch.distance(entrance, b.entrance);
      if (maxDist > 0 && dist > maxDist) continue;
      
      float rating = 1;
      if (needed != null) rating *= b.demands.valueFor(needed);
      if (rating <= 0) continue;
      
      pick.compare(b, rating - dist);
    }
    
    return pick.result();
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  public String toString() {
    return type.name+" "+ID;
  }
}



