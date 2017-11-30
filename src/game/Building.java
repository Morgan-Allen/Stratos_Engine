

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;

import game.GameConstants.Pathing;



public class Building extends Element implements Pathing, Employer {
  
  
  /**  Data fields and setup/initialisation-
    */
  static int nextID = 0;
  
  String ID;
  
  private Tile entrances[] = new Tile[0];
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
    
    int numE = s.loadInt();
    entrances = new Tile[numE];
    for (int i = 0; i < numE; i++) entrances[i] = loadTile(map, s);
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
    
    s.saveInt(entrances.length);
    for (Tile e : entrances) saveTile(e, map, s);
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
    entrances = selectEntrances();
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
    }
    
    if (! inventory.empty()) {
      I.say(this+" exited with goods!");
      I.say("  "+inventory);
    }
    
    entrances = null;
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
  
  
  
  /**  Auxiliary pathing-assist methods:
    */
  public Pathing[] adjacent(Pathing[] temp, CityMap map) {
    if (temp == null) temp = new Pathing[1];
    for (int i = temp.length; i-- > 0;) temp[i] = null;
    if (entrances == null) return temp;
    for (int i = entrances.length; i-- > 0;) temp[i] = entrances[i];
    return temp;
  }
  
  
  public boolean allowsEntryFrom(Pathing p) {
    return Visit.arrayIncludes(entrances, p);
  }
  
  
  public boolean allowsEntry(Actor a) {
    return true;
  }
  
  
  public void setInside(Actor a, boolean is) {
    visitors.toggleMember(a, is);
  }
  
  
  public Series <Actor> inside() {
    return visitors;
  }
  
  
  public boolean isTile() {
    return false;
  }
  
  
  
  /**  Construction and upgrade-related methods:
    */
  void onCompletion() {
    super.onCompletion();
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
    
    boolean refreshE = entrances == null;
    if (entrances != null) for (Tile e : entrances) {
      if (map.blocked(e)) refreshE = true;
    }
    if (refreshE) {
      entrances = selectEntrances();
    }
    
    if (--updateGap <= 0) {
      selectEntrances();
      updateOnPeriod(type.updateTime);
      updateGap = type.updateTime;
    }
  }
  
  
  void updateOnPeriod(int period) {
    return;
  }
  
  
  public Tile[] entrances() {
    return entrances;
  }
  
  
  public Tile mainEntrance() {
    if (Visit.empty(entrances)) return null;
    return entrances[0];
  }
  
  
  Tile[] selectEntrances() {
    Tile at = at();
    Pick <Tile> pick = new Pick();
    
    for (Coord c : Visit.perimeter(at.x, at.y, type.wide, type.high)) {
      boolean outx = c.x == at.x - 1 || c.x == at.x + type.wide;
      boolean outy = c.y == at.y - 1 || c.y == at.y + type.high;
      if (outx && outy  ) continue;
      if (map.blocked(c)) continue;
      
      float rating = 1;
      if (map.pathType(c) != PATH_PAVE) rating /= 2;
      pick.compare(map.tileAt(c), rating);
    }
    
    if (pick.empty()) return new Tile[0];
    return new Tile[] { pick.result() };
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
      t.configTask(this, null, mainEntrance(), Task.JOB.RETURNING, 0);
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
  
  
  public void visitedBy(Actor actor) {
    return;
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
  
  
  public void actorVisits(Actor actor, Building visits) {
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



