

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
  
  List <Walker> workers   = new List();
  List <Walker> residents = new List();
  List <Walker> visitors  = new List();
  
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
    
    s.loadObjects(workers  );
    s.loadObjects(residents);
    s.loadObjects(visitors );
    
    s.loadTally(materials);
    s.loadTally(inventory);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveString(ID);
    
    saveTile(entrance, map, s);
    s.saveInt(updateGap);
    
    s.saveObjects(workers  );
    s.saveObjects(residents);
    s.saveObjects(visitors );
    
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
    for (Walker w : workers) if (w.work == this) {
      w.work = null;
    }
    for (Walker w : residents) if (w.home == this) {
      w.home = null;
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
    return;
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
      
      pick.compare(b, rating * CityMap.distancePenalty(dist));
    }
    
    return pick.result();
  }
  
  
  float craftProgress() {
    return 0;
  }
  
  
  
  /**  Spawning walkers and customising walker behaviour:
    */
  protected int numWorkers(ObjectType type) {
    int sum = 0;
    for (Walker w : workers) if (w.type == type) sum++;
    return sum;
  }
  
  
  protected int numResidents(int socialClass) {
    int sum = 0;
    for (Walker w : residents) if (w.type.socialClass == socialClass) sum++;
    return sum;
  }
  
  
  protected int maxWorkers(ObjectType w) {
    if (! Visit.arrayIncludes(type.workerTypes, w)) return 0;
    return type.maxWorkers;
  }
  
  
  protected int maxResidents(int socialClass) {
    if (type.homeSocialClass != socialClass) return 0;
    return type.maxResidents;
  }
  
  
  public void setWorker(Walker w, boolean is) {
    w.work = is ? this : null;
    workers.toggleMember(w, is);
  }
  
  
  public void setResident(Walker w, boolean is) {
    w.home = is ? this : null;
    residents.toggleMember(w, is);
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



