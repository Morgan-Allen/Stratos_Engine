

package game;
import util.*;
import static game.GameConstants.*;
import static game.CityMap.*;



public class Building extends Fixture implements Session.Saveable, Employer {
  
  
  /**  Data fields and setup/initialisation-
    */
  static int nextID = 0;
  
  String ID;
  
  Tile entrance;
  int updateGap = 0;
  
  List <Walker> resident = new List();
  List <Walker> visitors = new List();
  
  Tally <Good> materials = new Tally();
  Tally <Good> inventory = new Tally();
  
  
  Building(ObjectType type) {
    super(type);
    this.ID = "#"+nextID++;
  }
  
  
  public Building(Session s) throws Exception {
    super(s);
    ID = s.loadString();
    
    entrance = loadTile(map, s);
    updateGap = s.loadInt();
    s.loadObjects(resident);
    s.loadObjects(visitors);
    
    s.loadTally(materials);
    s.loadTally(inventory);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveString(ID);
    
    saveTile(entrance, map, s);
    s.saveInt(updateGap);
    s.saveObjects(resident);
    s.saveObjects(visitors);
    
    s.saveTally(materials);
    s.saveTally(inventory);
  }
  
  
  
  /**  World entry and exit-
    */
  void enterMap(CityMap map, int x, int y, float buildLevel) {
    super.enterMap(map, x, y, buildLevel);
    map.buildings.add(this);
    selectEntrance();
    updateOnPeriod(0);
    
    for (Good g : type.builtFrom) {
      int need = type.materialNeed(g);
      materials.set(g, need * buildLevel);
    }
  }
  

  void exitMap(CityMap map) {
    super.exitMap(map);
    map.buildings.remove(this);
    for (Walker w : resident) {
      if (w.home == this) w.home = null;
      if (w.work == this) w.work = null;
    }
  }
  
  
  boolean destroyed() {
    return buildLevel < 0 && map == null;
  }
  
  
  public CityMap.Tile centre() {
    return map.tileAt(
      at.x + (type.wide / 2),
      at.y + (type.high / 2)
    );
  }
  
  
  public CityMap.Tile entrance() {
    return entrance;
  }
  
  
  void selectEntrance() {
    for (Coord c : Visit.perimeter(at.x, at.y, type.wide, type.high)) {
      boolean outx = c.x == at.x - 1 || c.x == at.x + type.wide;
      boolean outy = c.y == at.y - 1 || c.y == at.y + type.high;
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
    
    if (--updateGap <= 0) {
      updateOnPeriod(type.updateTime);
      updateGap = type.updateTime;
    }
  }
  
  
  void updateOnPeriod(int period) {
    for (ObjectType typeW : type.walkerTypes) {
      if (numWalkers(typeW) >= walkersNeeded(typeW)) continue;
      addWalker(typeW);
    }
  }
  
  
  
  /**  Utility methods for finding points of supply/demand:
    */
  float demandFor(Good g) {
    int   need = type.materialNeed(g);
    float hasB = materials.valueFor(g);
    float hasG = inventory.valueFor(g);
    return need - (hasB + hasG);
  }
  
  
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
      
      float dist = CityMap.distance(entrance, b.entrance);
      if (maxDist > 0 && dist > maxDist) continue;
      
      float rating = 1;
      if (needed != null) rating *= b.demandFor(needed);
      if (rating <= 0) continue;
      if (otherTrades) rating /= 2;
      
      pick.compare(b, rating * 10 / (10 + dist));
    }
    
    return pick.result();
  }
  
  
  float craftProgress() {
    return 0;
  }
  
  
  
  /**  Spawning walkers and customising walker behaviour:
    */
  protected int numWalkers(ObjectType type) {
    int sum = 0;
    for (Walker w : resident) if (w.type == type) sum++;
    return sum;
  }
  
  
  protected int walkersNeeded(ObjectType type) {
    return this.type.maxWalkers;
  }
  
  
  protected Walker addWalker(ObjectType type) {
    Walker walker = (Walker) type.generate();
    walker.enterMap(map, at.x, at.y);
    walker.inside = this;
    walker.home   = this;
    resident.add(walker);
    visitors.add(walker);
    //  TODO:  You need to start distinguishing between home and work...
    return walker;
  }
  
  
  Formation formation() {
    return null;
  }
  
  
  public void selectWalkerBehaviour(Walker walker) {
    walker.returnTo(this);
  }
  
  
  public void walkerUpdates(Walker w) {
    return;
  }
  
  
  public void walkerPasses(Walker walker, Building other) {
    return;
  }
  
  
  public void walkerTargets(Walker walker, Target other) {
    return;
  }
  
  
  public void walkerEnters(Walker walker, Building enters) {
    return;
  }
  
  
  public void walkerVisits(Walker walker, Building visits) {
    return;
  }
  
  
  public void walkerExits(Walker walker, Building enters) {
    return;
  }
  
  
  public void visitedBy(Walker walker) {
    return;
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  protected boolean reports() {
    return I.talkAbout == this;
  }
  
  
  public String toString() {
    return type.name+" "+ID;
  }
}



