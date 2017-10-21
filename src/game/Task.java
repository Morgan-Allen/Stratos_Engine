

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;

import game.GameConstants.Target;



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
  
  Tile path[] = null;
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
      path = new Tile[PL];
      for (int i = 0; i < PL; i++) path[i] = loadTile(actor.map, s);
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
      for (Tile t : path) saveTile(t, actor.map, s);
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
    this.origin    = origin ;
    this.type      = jobType;
    this.timeSpent = 0      ;
    this.maxTime   = maxTime;
    this.visits    = visits ;
    this.target    = target ;
    
    if (maxTime == -1) this.maxTime = AVG_VISIT_TIME;
    this.pathIndex = -1;
    path = updatePathing();
    
    if (! Visit.empty(path)) {
      if (actor.reports()) I.say("  Path is: "+path.length+" tiles long...");
      return this;
    }
    else {
      if (actor.reports()) I.say("  Could not find path!");
      return null;
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
  
  
  
  /**  Pathing-related methods:
    */
  boolean checkAndUpdatePathing() {
    if (checkPathing()) return true;
    
    path = updatePathing();
    if (path != null) return true;
    
    return false;
  }
  
  
  boolean checkPathing() {
    if (path == null || Visit.last(path) != pathTarget()) return false;
    
    for (int i = 0; i < actor.type.sightRange; i++) {
      if (i >= path.length) break;
      Tile t = path[i];
      if (actor.map.blocked(t.x, t.y)) return false;
    }
    
    return true;
  }
  
  
  Tile pathTarget() {
    Tile t = null;
    if (t == null && visits != null) t = visits.entrance;
    if (t == null && target != null) t = target.at();
    if (t == null && path   != null) t = (Tile) Visit.last(path);
    return t;
  }
  
  
  Tile[] updatePathing() {
    CityMap map = actor.map;
    Building inside = actor.inside;
    boolean visiting = visits != null;
    
    if (actor.reports()) {
      I.say(this+" pathing toward "+(visiting ? visits : target));
    }
    
    Tile from  = (inside == null) ? actor.at : inside.entrance;
    Tile heads = pathTarget();
    
    if (from == null || heads == null) return null;
    //heads.setFocused(actor, true);
    
    ActorPathSearch search = new ActorPathSearch(map, from, heads, -1);
    search.setPaveOnly(visiting && map.paved(from.x, from.y));
    search.doSearch();
    return search.fullPath(Tile.class);
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











