

package game;
import graphics.common.*;
import util.*;
import static game.Area.*;
import static game.GameConstants.*;

import game.GameConstants.Pathing;



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
    DOCKING   ,
    WAITING   ,
    VISITING  ,
    PLANTING  ,
    HARVEST   ,
    CRAFTING  ,
    BUILDING  ,
    SALVAGE   ,
    MILITARY  ,
    PATROLLING,
    FORAGING  ,
    HUNTING   ,
    LOOTING   ,
    EXPLORING ,
    COMBAT    ,
    RETREAT   ,
    DIALOG    ,
    CASTING   ,
  };
  final public static float
    NO_PRIORITY = -100f,
    IDLE        =  1.0f,
    CASUAL      =  2.5f,
    ROUTINE     =  5.0f,
    URGENT      =  7.5f,
    PARAMOUNT   =  10.0f,
    SWITCH_DIFF =  5.0f,
    PRIORITY_PER_100_CASH = 0.5f
  ;
  final public static float
    HARM_NULL   = -1.0f,
    EXTRA_HARM  =  1.5f,
    FULL_HARM   =  1.0f,
    MILD_HARM   =  0.5f,
    NO_HARM     =  0.0f,
    MILD_HELP   = -0.5f,
    FULL_HELP   = -1.0f,
    EXTRA_HELP  = -1.5f
  ;
  final static int
    PROG_CLOSING   = -1,
    PROG_CONTACT   =  0,
    PROG_ACTION    =  1,
    PROG_FINISHING =  2,
    PROG_COMPLETE  =  3
  ;
  
  final Active active;
  Employer origin;
  
  JOB type       = JOB.NONE;
  int ticksSpent = 0;
  int maxTime    = 20;
  
  Pathing path[] = null;
  int pathIndex = -1;
  boolean offMap;
  Target  target;
  Pathing visits;
  
  private float priorityEval = NO_PRIORITY;
  private boolean inContact = false;
  
  
  
  Task(Active actor) {
    this.active = actor;
  }
  
  
  public Task(Session s) throws Exception {
    s.cacheInstance(this);
    
    active     = (Actor   ) s.loadObject();
    origin     = (Employer) s.loadObject();
    type       = JOB.values()[s.loadInt()];
    ticksSpent = s.loadInt();
    maxTime    = s.loadInt();
    
    int PL = s.loadInt();
    if (PL == -1) {
      path = null;
    }
    else {
      path = new Pathing[PL];
      for (int i = 0; i < PL; i++) {
        if (s.loadBool()) path[i] = loadTile(active.map(), s);
        else              path[i] = (Pathing) s.loadObject();
      }
    }
    pathIndex = s.loadInt();
    offMap    = s.loadBool();
    target    = loadTarget(active.map(), s);
    visits    = (Building) s.loadObject();
    
    priorityEval = s.loadFloat();
    inContact = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(active );
    s.saveObject(origin);
    s.saveInt(type.ordinal());
    s.saveInt(ticksSpent);
    s.saveInt(maxTime);
    
    if (path == null) s.saveInt(-1);
    else {
      s.saveInt(path.length);
      for (Pathing t : path) {
        if (t.isTile()) {
          s.saveBool(true);
          saveTile((AreaTile) t, active.map(), s);
        }
        else {
          s.saveBool(false);
          s.saveObject(t);
        }
      }
    }
    s.saveInt(pathIndex);
    s.saveBool(offMap);
    saveTarget(target, active.map(), s);
    s.saveObject(visits);
    
    s.saveFloat(priorityEval);
    s.saveBool(inContact);
  }
  
  
  
  /**  Supplemental setup methods-
    */
  Task configTask(
    Employer origin, Pathing visits, Target target, JOB jobType, int maxTime
  ) {
    //  Note- the un/assign task calls are needed to ensure that current focus
    //  for the actor is updated correctly.
    boolean activeNow = active.task() == this;
    
    //  TODO:  This should probably *not* be done this way, now that multiple
    //  tasks can be configured for assessment at the same time.
    if (activeNow) {
      this.toggleFocus(false);
    }
    
    //  TODO:  Don't actually generate the path now!  Just check that pathing
    //  is possible.
    
    this.origin     = origin ;
    this.type       = jobType;
    this.ticksSpent = 0      ;
    this.maxTime    = maxTime;
    this.visits     = visits ;
    this.target     = target ;
    
    if (maxTime == -1) this.maxTime = AVG_VISIT_TIME;
    
    updatePathing();
    
    if (Visit.empty(path)) {
      return null;
    }
    if (activeNow) {
      this.toggleFocus(true);
    }
    return this;
  }
  
  
  void toggleFocus(boolean activeNow) {
    Target t = mainFocus();
    if (t != null) t.setFocused(active, activeNow);
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
  
  
  protected float rewardPriority() {
    if (origin instanceof Mission) {
      Mission mission = (Mission) origin;
      if (mission.rewards.isBounty()) {
        float reward = 0;
        reward += mission.rewards.cashReward();
        return PRIORITY_PER_100_CASH * reward / 100f;
      }
      else {
        return ROUTINE;
      }
    }
    return 0;
  }
  
  
  public float priority() {
    if (priorityEval == NO_PRIORITY) {
      float
        chance  = successChance(),
        success = successPriority(),
        failure = failCostPriority(),
        reward  = rewardPriority()
      ;
      priorityEval = (chance * (success + reward)) - ((1 - chance) * failure);
    }
    return Nums.max(0, priorityEval);
  }
  
  
  public float harmLevel() {
    return NO_HARM;
  }
  
  
  
  /**  Regular updates:
    */
  boolean checkAndUpdateTask() {
    
    Target focusT = mainFocus();
    if (focusT == null || ! focusT.onMap()) {
      return false;
    }
    Target pathT = pathTarget();
    if (pathT == null || ! pathT.onMap()) {
      return false;
    }
    
    if (active.isActor() && ! checkPathing(pathTarget())) {
      updatePathing();
      if (! checkPathing(pathTarget())) {
        return false;
      }
    }
    
    Area    map     = active.map();
    boolean isActor = active.isActor();
    Actor   asActor = isActor ? (Actor) active : null;
    
    final float BASE_TILES_PER_SECOND = 1.5f;
    float motion = BASE_TILES_PER_SECOND / map.ticksPS;
    motion *= isActor ? asActor.moveSpeed() : 0;
    
    
    while (motion > 0 || ! isActor) {
      
      Pathing path[]   = this.path;
      boolean contacts = checkContact(path);
      Pathing visits   = this.visits;
      Target  target   = this.target;
      
      priorityEval = NO_PRIORITY;
      inContact = false;
      
      if (visits != null && asActor.inside() == visits) {
        ticksSpent += 1;
        int progress = checkActionProgress();
        
        if (progress == PROG_COMPLETE) {
          return false;
        }
        else if (progress == PROG_CONTACT) {
          inContact = true;
          return true;
        }
        else {
          if (isActor) {
            onVisit(visits);
            asActor.onVisit(visits);
            if (origin != null) origin.actorVisits(asActor, visits);
          }
          if (progress == PROG_FINISHING) {
            onVisitEnds(visits);
          }
          inContact = true;
          return true;
        }
      }
      else if (contacts) {
        ticksSpent += 1;
        int progress = checkActionProgress();
        
        if (progress == PROG_COMPLETE) {
          return false;
        }
        else if (progress == PROG_CONTACT) {
          inContact = true;
          return true;
        }
        else {
          onTarget(target);
          target.targetedBy(active);
          if (isActor) {
            asActor.onTarget(target);
            if (origin != null) origin.actorTargets(asActor, target);
          }
          if (progress == PROG_FINISHING) {
            onTargetEnds(target);
          }
          inContact = true;
          return true;
        }
      }
      else if (isActor) {

        Pathing from     = Task.pathOrigin(asActor);
        Pathing inside   = asActor.inside();
        Pathing ahead    = nextOnPath();
        Vec3D   actorPos = asActor .exactPosition(null);
        Vec3D   aheadPos = ahead   .exactPosition(null);
        Vec3D   diff     = aheadPos.sub(actorPos, null);
        float   dist     = diff.length();
        boolean jump     = from.isTile() != ahead.isTile();
        
        if (jump) {
          ///I.say(active+" jumping from "+from+" to "+ahead);
          asActor.setExactLocation(aheadPos, map);
          if (! from .isTile()) asActor.setInside(inside, false);
          if (! ahead.isTile()) asActor.setInside(ahead , true );
        }
        
        else if (dist > 0) {
          diff.normalise();
          float distMoved = Nums.min(dist, motion);
          actorPos.x += distMoved * diff.x;
          actorPos.y += distMoved * diff.y;
          
          motion -= distMoved;
          asActor.setExactLocation(actorPos, map);
        }
        
        if (asActor.at().above == ahead || asActor.at() == ahead) {
          pathIndex = Nums.clamp(pathIndex + 1, path.length);
        }
        
        if (dist == 0 || motion == 0) return true;
        continue;
      }
      else {
        return false;
      }
    }
    
    return false;
  }
  
  
  
  /**  Activity calls-
    */
  protected void onVisit(Pathing visits) {
    return;
  }
  
  
  protected void onVisitEnds(Pathing visits) {
    return;
  }
  
  
  protected void onTarget(Target target) {
    return;
  }
  
  
  protected void onTargetEnds(Target target) {
    return;
  }
  
  
  protected void onArrival(Base goes, World.Journey journey) {
    return;
  }
  
  
  protected void onCancel() {
    return;
  }
  
  
  
  /**  Pathing and focus-related methods:
    */
  public static boolean hasTaskFocus(Target t, JOB type, Active except) {
    if (t.focused().empty()) return false;
    for (Active a : t.focused()) {
      if (a != except && a.jobType() == type) return true;
    }
    return false;
  }
  
  
  public static boolean hasTaskFocus(Target t, JOB type) {
    return hasTaskFocus(t, type, null);
  }
  
  
  public static boolean inCombat(Element f) {
    if (f == null || ! (f instanceof Active)) return false;
    JOB type = ((Active) f).jobType();
    return type == JOB.COMBAT || type == JOB.HUNTING;
  }
  
  
  public static Target mainTaskFocus(Element other) {
    if (! (other instanceof Active)) return null;
    Task t = ((Active) other).task();
    return t == null ? null : t.mainFocus();
  }
  
  
  public Target mainFocus() {
    if (target != null) return target;
    if (visits != null) return visits;
    if (path   != null) return (Pathing) Visit.last(path);
    return null;
  }
  
  
  public Target target() {
    return target;
  }
  
  
  public Pathing visits() {
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
  
  
  boolean checkContact(Pathing path[]) {
    Pathing from = (Pathing) Visit.last(path);
    return Area.distance(active, from) < actionRange();
  }
  
  
  int checkActionProgress() {
    Area map = active.map();
    int maxTicks = map.ticksPS * Nums.max(1, maxTime);
    boolean exactInterval = ticksSpent % map.ticksPS == 0;

    if (ticksSpent >  maxTicks) return PROG_COMPLETE;
    if (ticksSpent <= 0       ) return PROG_CLOSING;
    if (ticksSpent == maxTicks) return PROG_FINISHING;
    if (exactInterval         ) return PROG_ACTION;
    return PROG_CONTACT;
  }
  
  
  float actionRange() {
    return 0.1f;
  }
  
  
  boolean inContact() {
    int progress = checkActionProgress();
    return progress != PROG_CLOSING && progress != PROG_COMPLETE;
  }
  
  
  int motionMode() {
    return Actor.MOVE_NORMAL;
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
    Actor actor = (Actor) this.active;
    Area map = actor.map();
    if (Area.distance(last, target) > 1.5f) return false;
    
    int index = Nums.clamp(pathIndex, path.length);
    Pathing current = pathOrigin(actor), step = path[index];
    if (current != step) return false;
    
    for (int i = 0; i < actor.type().sightRange; i++, index++) {
      if (index >= path.length) break;
      Pathing t = path[index];
      if (t.isTile() && map.blocked((AreaTile) t)) return false;
      if (! (t.onMap() && t.allowsEntry(actor))) return false;
    }
    
    return true;
  }
  
  
  boolean updatePathing() {
    boolean report  = reports();
    boolean verbose = false;
    
    Area map      = active.map();
    boolean visiting = visits != null;
    Pathing from     = pathOrigin(active);
    Pathing heads    = pathTarget();
    
    if (report && verbose) {
      I.say(this+" pathing toward "+(visiting ? visits : target));
    }
    if (from == null || heads == null) {
      if (report) I.say("  Bad endpoints: "+from+" -> "+heads);
      return false;
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
    this.path = search.fullPath(Pathing.class);
    this.pathIndex = 0;
    
    if (path == null) {
      if (report) I.say("  Could not find path for "+this);
      return false;
    }
    else if (path.length < (Area.distance(from, heads) / 2) - 1) {
      if (report) I.say("  Path is impossible!");
      this.path = null;
      return false;
    }
    else {
      return true;
    }
  }
  
  
  public static boolean verifyPath(
    Pathing path[], Pathing start, Pathing end, Area map
  ) {
    if (Visit.empty(path) || path[0] != start) {
      return false;
    }
    
    Pathing temp[] = new Pathing[9];
    Pathing last = (Pathing) Visit.last(path);
    if (last != end && Area.distance(last, end) > 1.5f) {
      return false;
    }
    
    Pathing prior = path[0];
    for (Pathing p : path) {
      Pathing a[] = prior.adjacent(temp, map);
      if (p != prior && ! Visit.arrayIncludes(a, p)) {
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
    return ((Element) active).reports();
  }
  
  
  String animName() {
    return AnimNames.STAND;
  }
  
  
  Target faceTarget() {
    return visits == null ? target : visits;
  }
}











