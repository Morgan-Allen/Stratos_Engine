

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;

import game.GameConstants.Good;



public class Building extends Element implements Session.Saveable, Employer {
  
  
  /**  Data fields and setup/initialisation-
    */
  static int nextID = 0;
  
  String ID;
  
  private Tile entrance;
  private int updateGap = 0;
  
  List <Actor> workers   = new List();
  List <Actor> residents = new List();
  List <Actor> visitors  = new List();
  
  Tally <Type> materials = new Tally();
  Tally <Good> inventory = new Tally();
  
  
  Building(Type type) {
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
  }
  

  void exitMap(CityMap map) {
    super.exitMap(map);
    map.buildings.remove(this);
    for (Actor w : workers) if (w.work == this) {
      w.work = null;
    }
    for (Actor w : residents) if (w.home == this) {
      w.home = null;
    }
    for (Actor a : visitors) if (a.inside == this) {
      a.setInside(this, false);
      a.setLocation(entrance);
    }
    
    if (! inventory.empty()) {
      I.say(this+" exited with goods!");
      I.say("  "+inventory);
    }
    
    entrance = null;
  }
  
  
  boolean destroyed() {
    return buildLevel() <= 0 && map == null;
  }
  
  
  public CityMap.Tile centre() {
    Tile at = at();
    return map.tileAt(
      at.x + (type.wide / 2),
      at.y + (type.high / 2)
    );
  }
  
  
  
  /**  Construction and upgrade-related methods:
    */
  void onCompletion() {
    //
    //  Absorb any actors within the site:
    Tile at = at();
    for (Coord c : Visit.grid(at.x, at.y, type.wide, type.high, 1)) {
      Tile t = map.tileAt(c);
      for (Actor a : t.inside()) {
        a.setInside(this, true);
      }
    }
  }
  
  
  boolean accessible() {
    return complete() || type.worksBeforeBuilt;
  }
  
  
  float ambience() {
    if (! complete()) return 0;
    return type.ambience;
  }
  
  
  
  /**  Regular updates:
    */
  void update() {
    if (! accessible()) {
      return;
    }
    
    if (entrance == null || map.blocked(entrance.x, entrance.y)) {
      selectEntrance();
    }
    
    if (--updateGap <= 0) {
      selectEntrance();
      updateOnPeriod(type.updateTime);
      updateGap = type.updateTime;
    }
  }
  
  
  void updateOnPeriod(int period) {
    return;
  }
  
  
  public CityMap.Tile entrance() {
    return entrance;
  }
  
  
  void selectEntrance() {
    Tile at = at();
    Pick <Tile> pick = new Pick();
    
    for (Coord c : Visit.perimeter(at.x, at.y, type.wide, type.high)) {
      boolean outx = c.x == at.x - 1 || c.x == at.x + type.wide;
      boolean outy = c.y == at.y - 1 || c.y == at.y + type.high;
      if (outx && outy  ) continue;
      if (map.blocked(c)) continue;
      
      float rating = 1;
      if (! map.paved(c.x, c.y)) rating /= 2;
      pick.compare(map.tileAt(c), rating);
    }
    
    entrance = pick.result();
  }
  
  
  
  /**  Utility methods for finding points of supply/demand:
    */
  float demandFor(Good g) {
    float need = razing() ? 0 : materialNeed(g);
    float hasB = materialLevel(g);
    float hasG = inventory.valueFor(g);
    return need - (hasB + hasG);
  }
  
  
  float setMaterialLevel(Good material, float level) {
    if (level == -1) {
      return materials.valueFor(material);
    }
    else {
      level = materials.set(material, level);
      updateBuildState();
      return level;
    }
  }
  
  
  float maxStock(Good g) {
    return type.maxStock;
  }
  
  
  Good[] needed  () { return type.needed  ; }
  Good[] produced() { return type.produced; }
  
  
  float stockNeeded(Good need) { return type.maxStock; }
  float stockLimit (Good made) { return type.maxStock; }
  
  
  Tally <Good> homeUsed() {
    return new Tally();
  }
  
  
  float craftProgress() {
    return 0;
  }
  
  
  
  /**  Moderating and udpating recruitment and residency:
    */
  protected int numWorkers(Type type) {
    int sum = 0;
    for (Actor w : workers) if (w.type == type) sum++;
    return sum;
  }
  
  
  protected int maxWorkers(Type w) {
    if (! Visit.arrayIncludes(type.workerTypes, w)) return 0;
    return type.maxWorkers;
  }
  
  
  protected int numResidents(int socialClass) {
    return residents.size();
    //  TODO:  Restore this later once you have multiple housing types...
    //int sum = 0;
    //for (Actor w : residents) if (w.type.socialClass == socialClass) sum++;
    //return sum;
  }
  
  
  protected int maxResidents(int socialClass) {
    return type.maxResidents;
    //  TODO:  Restore this later once you have multiple housing types...
    //if (type.homeSocialClass != socialClass) return 0;
    //return type.maxResidents;
  }
  
  
  public void setWorker(Actor w, boolean is) {
    w.work = is ? this : null;
    workers.toggleMember(w, is);
  }
  
  
  public void setResident(Actor w, boolean is) {
    w.home = is ? this : null;
    residents.toggleMember(w, is);
  }
  
  
  Formation formation() {
    return null;
  }
  
  
  
  /**  Customising actor behaviour-
    */
  public void selectActorBehaviour(Actor actor) {
    returnActorHere(actor);
  }
  
  
  boolean actorIsHereWithPrompt(Actor actor) {
    if (actorIsHere(actor)) return true;
    returnActorHere(actor);
    return false;
  }
  
  
  void returnActorHere(Actor actor) {
    if (actorIsHere(actor)) return;
    Task t = new Task(actor);
    if (complete()) {
      t.configTask(this, this, null, Task.JOB.RETURNING, 0);
    }
    else {
      t.configTask(this, null, entrance, Task.JOB.RETURNING, 0);
    }
    actor.assignTask(t);
  }
  
  
  boolean actorIsHere(Actor actor) {
    if (complete()) {
      return actor.inside == this;
    }
    else {
      Tile at = actor.at(), t = this.at();
      boolean adjX = at.x >= t.x - 1 && at.x <= t.x + type.wide;
      boolean adjY = at.y >= t.y - 1 && at.y <= t.y + type.high;
      return adjX && adjY;
    }
  }
  
  
  public void actorUpdates(Actor w) {
    return;
  }
  
  
  public void actorPasses(Actor actor, Building other) {
    return;
  }
  
  
  public void actorTargets(Actor actor, Target other) {
    return;
  }
  
  
  public void actorEnters(Actor actor, Building enters) {
    return;
  }
  
  
  public void actorVisits(Actor actor, Building visits) {
    return;
  }
  
  
  public void actorExits(Actor actor, Building enters) {
    return;
  }
  
  
  public void visitedBy(Actor actor) {
    return;
  }
  
  
  
  /**  Handling fog and visibility-
    */
  public float sightLevel() {
    return sightLevel(false);
  }
  
  
  public float maxSightLevel() {
    return sightLevel(true);
  }
  
  
  private float sightLevel(boolean max) {
    Tile at = at();
    float avg = 0;
    for (int x = 2; x-- > 0;) for (int y = 2; y-- > 0;) {
      Tile t = map.tileAt(at.x + (x * type.wide), at.y + (y * type.high));
      avg += max ? map.fog.maxSightLevel(t) : map.fog.sightLevel(t);
    }
    return avg / 4;
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



