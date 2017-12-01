

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class Task implements Session.Saveable {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public static enum JOB {
    NONE     ,
    RETURNING,
    RESTING  ,
    WANDERING,
    DELIVER  ,
    SHOPPING ,
    TRADING  ,
    VISITING ,
    PLANTING ,
    HARVEST  ,
    CRAFTING ,
    BUILDING ,
    SALVAGE  ,
    MILITARY ,
    FORAGING ,
    HUNTING  ,
    EXPLORING,
    COMBAT   ,
  };
  
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
    target    = (Target  ) s.loadObject();
    visits    = (Building) s.loadObject();
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
    s.saveObject(target);
    s.saveObject(visits);
  }
  
  
  
  /**  Supplemental setup methods-
    */
  Task configTask(
    Employer origin, Building visits, Target target, JOB jobType, int maxTime
  ) {
    //  Note- the un/assign task calls are needed to ensure that current focus
    //  for the actor is updated correctly.
    boolean active = actor.task == this;
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
  static Target focusTarget(Task t) {
    if (t        == null) return null;
    if (t.target != null) return t.target;
    if (t.visits != null) return t.visits;
    if (t.path   != null) return (Pathing) Visit.last(t.path);
    return null;
  }
  
  
  static boolean hasTaskFocus(Target t, JOB type) {
    if (t.focused().empty()) return false;
    for (Actor a : t.focused()) if (a.jobType() == type) return true;
    return false;
  }
  
  
  Pathing pathTarget() {
    Pathing t = null;
    if (t == null && visits != null && visits.complete()) {
      t = visits;
    }
    if (t == null && target != null) {
      t = target.at();
    }
    if (t == null && path != null) {
      t = (Pathing) Visit.last(path);
    }
    return t;
  }
  
  
  boolean checkAndUpdatePathing() {
    Target focus = focusTarget(this);
    if (focus == null || ! focus.onMap()) {
      return false;
    }
    Target target = pathTarget();
    if (target == null || ! target.onMap()) {
      return false;
    }
    if (checkPathing(target)) {
      return true;
    }
    path = updatePathing();
    if (path == null) {
      return false;
    }
    return true;
  }
  
  
  boolean checkPathing(Target target) {
    if (path == null || Visit.last(path) != target) return false;
    
    for (int i = 0; i < actor.type.sightRange; i++) {
      if (i >= path.length) break;
      Pathing t = path[i];
      if (t.isTile() && actor.map.blocked((Tile) t)) return false;
      if (! t.allowsEntry(actor)) return false;
    }
    
    return true;
  }
  
  
  Pathing[] updatePathing() {
    
    CityMap map      = actor.map;
    Pathing inside   = actor.inside;
    boolean visiting = visits != null;
    
    boolean report  = actor.reports();
    boolean verbose = false;
    if (report && verbose) {
      I.say(this+" pathing toward "+(visiting ? visits : target));
    }
    
    Pathing from  = inside == null ? actor.at() : inside;
    Pathing heads = pathTarget();
    
    if (from == null || heads == null) {
      if (report) I.say("  Bad endpoints: "+from+" -> "+heads);
      return null;
    }
    
    ActorPathSearch search = new ActorPathSearch(map, from, heads, -1);
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
      if (report) I.say("  Path is: "+path.length+" tiles long...");
      return path;
    }
  }
  
  
  static boolean verifyPath(
    Pathing path[], Pathing start, Pathing end, CityMap map
  ) {
    if (Visit.empty(path) || path[0] != start) return false;

    Pathing temp[] = new Pathing[8];
    Pathing last = (Pathing) Visit.last(path);
    if (last != end) {
      if (! Visit.arrayIncludes(end.adjacent(temp, map), last)) {
        return false;
      }
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
}











