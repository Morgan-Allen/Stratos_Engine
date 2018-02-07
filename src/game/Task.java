

package game;
import graphics.common.*;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class Task implements Session.Saveable {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public static enum JOB {
    NONE      ,
    COLLECTING,
    RETURNING ,
    DEPARTING ,
    RESTING   ,
    WANDERING ,
    DELIVER   ,
    SHOPPING  ,
    TRADING   ,
    VISITING  ,
    PLANTING  ,
    HARVEST   ,
    CRAFTING  ,
    BUILDING  ,
    SALVAGE   ,
    MILITARY  ,
    FORAGING  ,
    HUNTING   ,
    LOOTING   ,
    EXPLORING ,
    COMBAT    ,
    RETREAT   ,
    DIALOG    ,
  };
  final public static float
    NO_PRIORITY = -1.0f,
    CASUAL      =  2.5f,
    ROUTINE     =  5.0f,
    URGENT      =  7.5f,
    PARAMOUNT   =  10.0f
  ;
  final public static float
    EXTRA_HARM  =  1.5f,
    FULL_HARM   =  1.0f,
    MILD_HARM   =  0.5f,
    NO_HARM     =  0.0f,
    MILD_HELP   = -0.5f,
    FULL_HELP   = -1.0f,
    EXTRA_HELP  = -1.5f
  ;
  
  Actor actor;
  Employer origin;
  
  JOB type      = JOB.NONE;
  int timeSpent = 0 ;
  int maxTime   = 20;
  
  Pathing path[] = null;
  int pathIndex = -1;
  boolean  offMap;
  Target   target;
  Building visits;
  
  private float priorityEval = NO_PRIORITY;
  private boolean inContact = false;
  
  
  
  Task(Actor actor) {
    this.actor = actor;
  }
  
  
  public Task(Session s) throws Exception {
    s.cacheInstance(this);
    
    actor     = (Actor   ) s.loadObject();
    origin    = (Employer) s.loadObject();
    type      = JOB.values()[s.loadInt()];
    timeSpent = s.loadInt();
    maxTime   = s.loadInt();
    
    int PL = s.loadInt();
    if (PL == -1) {
      path = null;
    }
    else {
      path = new Pathing[PL];
      for (int i = 0; i < PL; i++) {
        if (s.loadBool()) path[i] = loadTile(actor.map, s);
        else              path[i] = (Pathing) s.loadObject();
      }
    }
    pathIndex = s.loadInt();
    offMap    = s.loadBool();
    target    = loadTarget(actor.map, s);
    visits    = (Building) s.loadObject();
    
    priorityEval = s.loadFloat();
    inContact = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(actor );
    s.saveObject(origin);
    s.saveInt(type.ordinal());
    s.saveInt(timeSpent);
    s.saveInt(maxTime);
    
    if (path == null) s.saveInt(-1);
    else {
      s.saveInt(path.length);
      for (Pathing t : path) {
        if (t.isTile()) {
          s.saveBool(true);
          saveTile((Tile) t, actor.map, s);
        }
        else {
          s.saveBool(false);
          s.saveObject(t);
        }
      }
    }
    s.saveInt(pathIndex);
    s.saveBool(offMap);
    saveTarget(target, actor.map, s);
    s.saveObject(visits);
    
    s.saveFloat(priorityEval);
    s.saveBool(inContact);
  }
  
  
  
  /**  Supplemental setup methods-
    */
  Task configTask(
    Employer origin, Building visits, Target target, JOB jobType, int maxTime
  ) {
    //  Note- the un/assign task calls are needed to ensure that current focus
    //  for the actor is updated correctly.
    boolean active = actor.task() == this;
    if (active) actor.assignTask(null);
    
    this.origin    = origin ;
    this.type      = jobType;
    this.timeSpent = 0      ;
    this.maxTime   = maxTime;
    this.visits    = visits ;
    this.target    = target ;
    
    if (maxTime == -1) this.maxTime = AVG_VISIT_TIME;
    this.pathIndex = -1;
    path = updatePathing();
    
    if (Visit.empty(path)) return null;
    if (active) actor.assignTask(this);
    return this;
  }
  
  
  void toggleFocus(boolean active) {
    Target t = Task.focusTarget(this);
    if (t == null) return;
    t.setFocused(actor, active);
  }
  
  
  
  /**  Evaluating priority and win-chance:
    */
  protected float successChance() {
    return 1.0f;
  }
  
  
  protected float successPriority() {
    return ROUTINE;
  }
  
  
  protected float failCostPriority() {
    return 0;
  }
  
  
  public float priority() {
    if (priorityEval == NO_PRIORITY) {
      float
        chance  = successChance(),
        success = successPriority(),
        failure = failCostPriority()
      ;
      priorityEval = (chance * success) + ((1 - chance) * failure);
    }
    return priorityEval;
  }
  
  
  public float harmLevel() {
    return NO_HARM;
  }
  
  
  
  /**  Regular updates:
    */
  boolean checkAndUpdateTask() {
    
    Target focusT = focusTarget(this);
    if (focusT == null || ! focusT.onMap()) {
      return false;
    }
    Target pathT = pathTarget();
    if (pathT == null || ! pathT.onMap()) {
      return false;
    }
    
    if (! checkPathing(pathTarget())) {
      this.path = updatePathing();
      this.pathIndex = -1;
      
      if (! checkPathing(pathTarget())) {
        return false;
      }
    }
    
    CityMap  map      = actor.map;
    Pathing  inside   = actor.inside();
    Pathing  path[]   = this.path;
    Pathing  pathEnd  = (Pathing) Visit.last(path);
    float    distance = CityMap.distance(actor.at(), pathEnd);
    float    minRange = actionRange();
    Building visits   = this.visits;
    Target   target   = this.target;
    
    priorityEval = NO_PRIORITY;
    inContact = false;
    
    if (visits != null && inside == visits) {
      if (timeSpent++ <= maxTime) {
        inContact = true;
        onVisit(visits);
        actor.onVisit(visits);
        visits.visitedBy(actor);
        if (origin != null) origin.actorVisits(actor, visits);
        return true;
      }
      else {
        return false;
      }
    }
    else if (distance <= minRange) {
      if (target != null && timeSpent++ <= maxTime) {
        inContact = true;
        onTarget(target);
        actor.onTarget(target);
        target.targetedBy(actor);
        if (origin != null) origin.actorTargets(actor, target);
        return true;
      }
      else {
        return false;
      }
    }
    else {
      int index = Nums.clamp(pathIndex + 1, path.length);
      Pathing ahead = path[pathIndex = index];
      if (ahead.isTile()) actor.setInside(inside, false);
      else actor.setInside(ahead, true);
      actor.setLocation(ahead.at(), map);
      return true;
    }
  }
  
  
  
  /**  Activity calls-
    */
  protected void onVisit(Building visits) {
    return;
  }
  
  
  protected void onTarget(Target target) {
    return;
  }
  
  
  protected void onArrival(City goes, World.Journey journey) {
    return;
  }
  
  
  protected void onCancel() {
    return;
  }
  
  
  
  /**  Pathing and focus-related methods:
    */
  public static Target focusTarget(Task t) {
    if (t        == null) return null;
    if (t.target != null) return t.target;
    if (t.visits != null) return t.visits;
    if (t.path   != null) return (Pathing) Visit.last(t.path);
    return null;
  }
  

  public static boolean hasTaskFocus(Target t, JOB type, Actor except) {
    if (t.focused().empty()) return false;
    for (Actor a : t.focused()) {
      if (a != except && a.jobType() == type) return true;
    }
    return false;
  }
  
  
  public static boolean hasTaskFocus(Target t, JOB type) {
    return hasTaskFocus(t, type, null);
  }
  
  
  public Target target() {
    return target;
  }
  
  
  public Building visits() {
    return visits;
  }
  
  
  public static Pathing pathOrigin(Target from) {
    if (from == null) {
      return null;
    }
    Type t = from.type();
    if (t.isBuilding() && ((Building) from).complete()) {
      return (Building) from;
    }
    if (t.isActor()) {
      Actor a = (Actor) from;
      if (a.inside() != null) return a.inside();
    }
    return from.at();
  }
  
  
  float actionRange() {
    return 0.1f;
  }
  
  
  boolean inContact() {
    return inContact;
  }
  
  
  Pathing pathTarget() {
    Pathing t = null;
    if (t == null && visits != null && visits.complete()) {
      t = visits;
    }
    if (t == null && target != null) {
      t = pathOrigin(target);
    }
    if (t == null && path != null) {
      t = (Pathing) Visit.last(path);
    }
    return t;
  }
  
  
  Pathing nextOnPath() {
    if (Visit.empty(path)) return null;
    int index = Nums.clamp(pathIndex + 1, path.length);
    return path[index];
  }
  
  
  boolean checkPathing(Target target) {
    if (path == null || target == null) return false;
    
    Pathing last = (Pathing) Visit.last(path);
    if (CityMap.distance(last, target) > 1.5f) return false;
    
    int index = Nums.clamp(pathIndex, path.length);
    Pathing current = pathOrigin(actor), step = path[index];
    if (current != step) return false;
    
    for (int i = 0; i < actor.type().sightRange; i++, index++) {
      if (index >= path.length) break;
      Pathing t = path[index];
      if (t.isTile() && actor.map.blocked((Tile) t)) return false;
      if (! (t.onMap() && t.allowsEntry(actor))    ) return false;
    }
    
    return true;
  }
  
  
  Pathing[] updatePathing() {
    boolean report  = actor.reports();
    boolean verbose = false;
    
    CityMap map      = actor.map;
    boolean visiting = visits != null;
    Pathing from     = pathOrigin(actor);
    Pathing heads    = pathTarget();
    
    if (report && verbose) {
      I.say(this+" pathing toward "+(visiting ? visits : target));
    }
    if (from == null || heads == null) {
      if (report) I.say("  Bad endpoints: "+from+" -> "+heads);
      return null;
    }
    
    ActorPathSearch search = new ActorPathSearch(map, from, heads, -1);
    
    //  TODO:  You should have map-settings that toggle whether the
    //  path-cache is used at all.  Default to simpler checks in that
    //  case.
    if ((! visiting) && ! map.pathCache.pathConnects(from, heads, false, false)) {
      search.setProximate(true);
    }
    //search.verbosity = Search.VERBOSE;
    search.doSearch();
    Pathing path[] = search.fullPath(Pathing.class);
    
    if (path == null) {
      if (report) I.say("  Could not find path for "+this);
      return null;
    }
    else if (path.length < (CityMap.distance(from, heads) / 2) - 1) {
      if (report) I.say("  Path is impossible!");
      return null;
    }
    else {
      ///if (report) I.say("  Path is: "+path.length+" tiles long...");
      return path;
    }
  }
  
  
  public static boolean verifyPath(
    Pathing path[], Pathing start, Pathing end, CityMap map
  ) {
    if (Visit.empty(path) || path[0] != start) return false;

    Pathing temp[] = new Pathing[9];
    Pathing last = (Pathing) Visit.last(path);
    if (last != end && CityMap.distance(last, end) > 1.5f) {
      return false;
    }
    
    Pathing prior = path[0];
    for (Pathing p : path) {
      Pathing a[] = p.adjacent(temp, map);
      if (p != prior && ! Visit.arrayIncludes(a, prior)) {
        return false;
      }
      prior = p;
    }
    return true;
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    Object subject = visits == null ? target : visits;
    if (subject == null) return type.name();
    else return type.name()+": "+subject;
  }
  
  
  boolean reports() {
    return actor.reports();
  }
  
  
  String animName() {
    return AnimNames.STAND;
  }
}











