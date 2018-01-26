

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



public class Building extends Element implements Pathing, Employer {
  
  
  /**  Data fields and setup/initialisation-
    */
  static int nextID = 0;
  
  String ID;
  
  private int facing = TileConstants.N;
  private Tile entrances[] = new Tile[0];
  private int updateGap = 0;
  
  List <Actor> workers   = new List();
  List <Actor> residents = new List();
  List <Actor> visitors  = new List();
  
  Tally <Type> materials = new Tally();
  Tally <Good> inventory = new Tally();
  
  
  public Building(Type type) {
    super(type);
    this.ID = "#"+nextID++;
  }
  
  
  public Building(Session s) throws Exception {
    super(s);
    ID = s.loadString();

    facing = s.loadInt();
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
    
    s.saveInt(facing);
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
  void setFacing(int facing) {
    this.facing = facing;
  }
  
  
  int facing() {
    return facing;
  }
  
  
  void enterMap(CityMap map, int x, int y, float buildLevel) {
    
    //I.say("ENTERING MAP: "+this);
    
    super.enterMap(map, x, y, buildLevel);
    map.buildings.add(this);
  }
  
  
  void exitMap(CityMap map) {
    
    //I.say("EXITING MAP: "+this);
    
    refreshEntrances(new Tile[0]);
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
  
  
  
  /**  Construction and upgrade-related methods:
    */
  void onCompletion() {
    refreshEntrances(selectEntrances());
    super.onCompletion();
    updateOnPeriod(0);
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
    
    for (int i = 0; i < entrances.length; i++) {
      Tile e = entrances[i];
      if (! checkEntranceOkay(e, i)) {
        refreshEntrances(selectEntrances());
        break;
      }
    }
    
    if (--updateGap <= 0) {
      refreshEntrances(selectEntrances());
      updateOnPeriod(type.updateTime);
      updateGap = type.updateTime;
    }
  }
  
  
  void updateOnPeriod(int period) {
    return;
  }
  
  
  
  /**  Dealing with entrances and facing:
    */
  protected Tile tileAt(int initX, int initY, int facing) {
    int wide = type.wide - 1, high = type.high - 1;
    int x = 0, y = 0;
    
    switch (facing) {
      case(N): x = initX       ; y = initY       ; break;
      case(E): x = initY       ; y = high - initX; break;
      case(S): x = wide - initX; y = high - initY; break;
      case(W): x = wide - initY; y = initX       ; break;
    }
    
    return map.tileAt(x + at().x, y + at().y);
  }
  
  
  public Tile[] entrances() {
    return entrances;
  }
  
  
  public Tile mainEntrance() {
    if (Visit.empty(entrances)) return null;
    return entrances[0];
  }
  
  
  void refreshEntrances(Tile newE[]) {
    Tile oldE[] = this.entrances;
    
    boolean flagUpdate = true;
    flagUpdate &= Nums.max(oldE.length, newE.length) > 1;
    flagUpdate &= ! Visit.arrayEquals(oldE, newE);
    
    if (flagUpdate) for (Tile e : oldE) {
      map.pathCache.checkPathingChanged(e);
    }
    
    this.entrances = newE;
    
    if (flagUpdate) for (Tile e : newE) {
      map.pathCache.checkPathingChanged(e);
    }
  }
  
  
  boolean checkEntranceOkay(Tile e, int index) {
    if (e.above == this) return true;
    int pathT = e.pathType();
    if (pathT == PATH_FREE || pathT == PATH_PAVE) return true;
    return false;
  }
  
  
  Tile[] selectEntrances() {
    Tile at = at();
    Pick <Tile> pick = new Pick();
    
    for (Coord c : Visit.perimeter(at.x, at.y, type.wide, type.high)) {
      Tile t = map.tileAt(c);
      if (t == null) continue;
      boolean outx = t.x == at.x - 1 || t.x == at.x + type.wide;
      boolean outy = t.y == at.y - 1 || t.y == at.y + type.high;
      if (outx && outy) continue;
      
      int pathT = t.pathType();
      if (pathT != PATH_FREE && pathT != PATH_PAVE) continue;
      
      float rating = 1;
      if (pathT != PATH_PAVE) rating /= 2;
      pick.compare(t, rating);
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
  
  
  public void setWorker(Actor a, boolean is) {
    a.work = is ? this : null;
    workers.toggleMember(a, is);
  }
  
  
  public void setResident(Actor a, boolean is) {
    a.home = is ? this : null;
    residents.toggleMember(a, is);
  }
  
  
  public Series <Actor> recruits() {
    return NO_ACTORS;
  }
  
  
  public void deployInFormation(Formation f, boolean is) {
    return;
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
      return CityMap.adjacent(this, actor);
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



