

package game;
import util.*;
import static game.GameConstants.*;



public class Building extends Fixture implements Session.Saveable {
  
  
  /**  Data fields and setup/initialisation-
    */
  static int nextID = 0;
  
  String ID;
  
  Tile entrance;
  int walkerCountdown = 0;
  List <Walker> walkers  = new List();
  List <Walker> visitors = new List();
  
  float craftProgress;
  Tally <Good> inventory = new Tally();
  Tally <Good> demands   = new Tally();
  
  
  Building(ObjectType type) {
    super(type);
    this.ID = "#"+nextID++;
  }
  
  
  public Building(Session s) throws Exception {
    super(s);
    ID = s.loadString();
    
    entrance = Tile.loadTile(map, s);
    walkerCountdown = s.loadInt();
    s.loadObjects(walkers );
    s.loadObjects(visitors);
    
    craftProgress = s.loadFloat();
    s.loadTally(inventory);
    s.loadTally(demands);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveString(ID);
    
    Tile.saveTile(entrance, map, s);
    s.saveInt(walkerCountdown);
    s.saveObjects(walkers);
    s.saveObjects(visitors);
    
    s.saveFloat(craftProgress);
    s.saveTally(inventory);
    s.saveTally(demands);
  }
  
  
  
  /**  World entry and exit-
    */
  void enterMap(CityMap map, int x, int y) {
    super.enterMap(map, x, y);
    map.buildings.add(this);
    selectEntrance();
  }
  

  void exitMap(CityMap map) {
    super.exitMap(map);
    map.buildings.remove(this);
  }
  
  
  boolean destroyed() {
    return buildLevel >= 0 && map == null;
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
      
      for (ObjectType typeW : type.walkerTypes) {
        if (numWalkers(typeW) >= walkersNeeded(typeW)) continue;
        Walker walker = (Walker) typeW.generate();
        walker.enterMap(map, x, y);
        walker.inside = this;
        walker.home   = this;
        walkers .add(walker);
        visitors.add(walker);
      }
      
      for (Walker walker : walkers) {
        if (walker.inside == this) selectWalkerBehaviour(walker);
      }
      
      walkerCountdown = type.walkerCountdown;
    }
  }
  
  
  protected int numWalkers(ObjectType type) {
    int sum = 0;
    for (Walker w : walkers) if (w.type == type) sum++;
    return sum;
  }
  
  
  protected int walkersNeeded(ObjectType type) {
    return type.maxWalkers;
  }
  
  
  
  /**  Customising walker behaviour:
    */
  void selectWalkerBehaviour(Walker walker) {
    walker.startRandomWalk();
  }
  
  
  void walkerPasses(Walker walker, Building other) {
    return;
  }
  
  
  void walkerTargets(Walker walker, Tile other) {
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
  
  
  Building findNearestWithFeature(Good feature, int maxDist) {
    return findNearestDemanding(null, feature, null, -1);
  }
  
  
  Building findNearestDemanding(
    ObjectType type, Good needed, int maxDist
  ) {
    return findNearestDemanding(type, null, needed, maxDist);
  }
  
  
  Building findNearestDemanding(
    ObjectType type, Good feature,
    Good needed, int maxDist
  ) {
    Pick <Building> pick = new Pick();
    boolean trades = this.type.isTradeBuilding();
    
    for (Building b : map.buildings) {
      if (type != null && b.type != type) continue;
      
      boolean otherTrades = b.type.isTradeBuilding();
      if (trades && otherTrades) continue;
      
      boolean featured = b.type.hasFeature(feature);
      if (feature != null && ! featured) continue;
      
      float dist = PathSearch.distance(entrance, b.entrance);
      if (maxDist > 0 && dist > maxDist) continue;
      
      float rating = 1;
      if (needed != null) rating *= b.demands.valueFor(needed);
      if (rating <= 0) continue;
      if (otherTrades) rating /= 2;
      
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



