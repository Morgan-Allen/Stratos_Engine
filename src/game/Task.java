

package game;
import graphics.common.*;
import util.*;
import static game.Area.*;
import static game.GameConstants.*;



public class Task implements Session.Saveable {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public static enum JOB {
    NONE      ,
    FLINCH    ,
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
    HEALING   ,
    DIALOG    ,
    CASTING   ,
  };
  final public static float
    NO_PRIORITY = -100f,
    IDLE        =  1.0f,
    CASUAL      =  2.5f,
    ROUTINE     =  5.0f,
    URGENT      =  7.5f,
    PARAMOUNT   = 10.0f,
    SWITCH_DIFF =  5.0f,
    PRIORITY_PER_100_CASH = 1.0f
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
    PROG_COMPLETE  =  2,
    RESUME_NO      = -1,
    RESUME_WAIT    =  0,
    RESUME_YES     =  1
  ;
  
  final Active active;
  Employer origin;
  
  JOB   type       = JOB.NONE;
  float lastTicks  = 0;
  float ticksSpent = 0;
  int   maxTime    = 20;
  
  Pathing path[] = null;
  int pathIndex = -1;
  boolean offMap;
  Target  target;
  Pathing visits;
  
  private float priorityEval = NO_PRIORITY;
  private int contactState = PROG_CLOSING;
  
  
  
  protected Task(Active actor) {
    this.active = actor;
  }
  
  
  public Task(Session s) throws Exception {
    s.cacheInstance(this);
    
    active     = (Actor   ) s.loadObject();
    origin     = (Employer) s.loadObject();
    type       = JOB.values()[s.loadInt()];
    ticksSpent = s.loadFloat();
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
    contactState = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(active);
    s.saveObject(origin);
    s.saveInt(type.ordinal());
    s.saveFloat(ticksSpent);
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
    s.saveInt(contactState);
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
    this.lastTicks  = 0      ;
    this.ticksSpent = 0      ;
    this.maxTime    = maxTime;
    this.visits     = visits ;
    this.target     = target ;
    
    if (maxTime == -1) this.maxTime = AVG_VISIT_TIME;
    
    updatePathing();
    
    if (! pathValid()) {
      return null;
    }
    if (activeNow) {
      this.toggleFocus(true);
    }
    return this;
  }
  
  
  boolean pathValid() {
    if (visits != null && active.mobile()) {
      if (((Actor) active).inside() == visits) return true;
    }
    if (target != null && checkTargetContact(target)) {
      return true;
    }
    return path != null;
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
      Actor actor = (Actor) active;
      Mission mission = (Mission) origin;
      
      if (mission.rewards.isBounty()) {
        //  ...Something of a simplistic hack here?  Examine again later.
        float greed = (1 - actor.traits.levelOf(TRAIT_EMPATHY)) / 2;
        float reward = 0, priority = PRIORITY_PER_100_CASH;
        reward += mission.rewards.cashReward();
        priority *= greed < 0.5f ? (greed + 0.5f) : (greed * 2);
        return priority * reward / 100f;
      }
      else {
        return mission.rewards.basePriority();
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
      if (active.mobile()) {
        Actor actor = (Actor) active;
        float bravery = (actor.traits.levelOf(TRAIT_BRAVERY) + 1) / 2;
        failure *= (1.5f - bravery);
        chance = Nums.clamp(chance + ((bravery - 0.5f) / 2), 0, 1);
      }
      priorityEval = (chance * (success + reward)) - ((1 - chance) * failure);
    }
    return Nums.max(0, priorityEval);
  }
  
  
  public float harmLevel() {
    return NO_HARM;
  }
  
  
  public boolean emergency() {
    return false;
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
    
    if (active.mobile() && ! checkPathing(pathTarget())) {
      updatePathing();
      if (! checkPathing(pathTarget())) {
        return false;
      }
    }
    
    Area    map     = active.map();
    boolean mobile  = active.mobile();
    Actor   asActor = mobile ? (Actor) active : null;
    
    float motion = AVG_MOVE_UNIT / (100f * map.ticksPS);
    float tickProgress = 1.0f;
    motion *= mobile ? asActor.moveSpeed() : 0;
    tickProgress *= mobile ? asActor.actSpeed() : 1;
    
    
    while (motion > 0 || ! mobile) {
      
      Pathing path[]   = this.path;
      Pathing visits   = this.visits;
      Target  target   = this.target;
      
      priorityEval = NO_PRIORITY;
      
      if (visits != null && mobile && asActor.inside() == visits) {
        int progress = checkActionProgress(tickProgress);
        
        if (progress == PROG_COMPLETE) {
          onVisitEnds(visits);
          return false;
        }
        else if (progress == PROG_CONTACT) {
          return true;
        }
        else {
          if (mobile) {
            onVisit(visits);
            asActor.onVisit(visits);
            if (origin != null) origin.actorVisits(asActor, visits);
          }
          return true;
        }
      }
      else if (target != null && checkTargetContact(target)) {
        int progress = checkActionProgress(tickProgress);
        
        if (progress == PROG_COMPLETE) {
          onTargetEnds(target);
          return false;
        }
        else if (progress == PROG_CONTACT) {
          return true;
        }
        else {
          onTarget(target);
          target.targetedBy(active);
          if (mobile) {
            asActor.onTarget(target);
            if (origin != null) origin.actorTargets(asActor, target);
          }
          return true;
        }
      }
      else if (mobile) {
        this.contactState = PROG_CLOSING;
        
        Pathing from     = Task.pathOrigin(asActor);
        Pathing inside   = asActor.inside();
        Pathing ahead    = nextOnPath();
        Vec3D   actorPos = asActor .exactPosition(null);
        Vec3D   aheadPos = ahead   .exactPosition(null);
        Vec3D   diff     = aheadPos.sub(actorPos, null); diff.z = 0;
        float   dist     = diff.length();
        boolean jump     = ! (from.isTile() && ahead.isTile());
        
        //I.say("Updating motion...");
        
        if (jump) {
          //I.say("  Jumped ahead to "+aheadPos);
          
          asActor.setExactLocation(aheadPos, map, true);
          if (! from .isTile()) asActor.setInside(inside, false);
          if (! ahead.isTile()) asActor.setInside(ahead , true );
        }
        
        else if (dist > 0) {
          diff.normalise();
          float distMoved = Nums.min(dist, motion);
          actorPos.x += distMoved * diff.x;
          actorPos.y += distMoved * diff.y;
          
          //I.say("  -> "+distMoved+", diff: "+diff);
          
          motion -= distMoved;
          asActor.setExactLocation(actorPos, map, false);
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
  
  
  protected Task reaction() {
    return null;
  }
  
  
  protected int checkResume() {
    if (complete() || priority() <= 0) return RESUME_NO;
    return RESUME_YES;
  }
  
  
  protected boolean updateOnArrival(Base goes, World.Journey journey) {
    return false;
  }
  
  
  protected void onMapExit() {
    toggleFocus(false);
    path      = null;
    pathIndex = -1  ;
    target    = null;
    visits    = null;
  }
  
  
  protected void onCancel() {
    return;
  }
  
  
  
  /**  Pathing and focus-related methods:
    */
  public static boolean hasTaskFocus(Target t, JOB type, Active except) {
    if (t == null || t.focused().empty()) return false;
    for (Active a : t.focused()) {
      if (a != except && a.jobType() == type) return true;
    }
    return false;
  }
  
  
  public static boolean hasTaskFocus(Target t, JOB type) {
    return hasTaskFocus(t, type, null);
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
  
  
  public static boolean inCombat(Element f, Target with) {
    if (f == null || ! (f instanceof Active)) return false;
    if (with != null && mainTaskFocus(f) != with) return false;
    JOB type = ((Active) f).jobType();
    return type == JOB.COMBAT || type == JOB.HUNTING;
  }
  
  
  public static boolean inCombat(Element f) {
    return inCombat(f, null);
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
  
  
  private int checkActionProgress(float tickProgress) {
    Area map = active.map();
    if (map == null) return PROG_CLOSING;
    
    final float oldProgress = ticksSpent;
    int maxTicks = map.ticksPS * Nums.max(1, maxTime);
    int contact  = maxTicks / 2;
    
    lastTicks   = ticksSpent;
    ticksSpent += tickProgress;
    
    if (oldProgress <= contact && ticksSpent > contact) {
      return contactState = PROG_ACTION;
    }
    else if (ticksSpent <= 0) {
      return contactState = PROG_CLOSING;
    }
    else if (ticksSpent >= maxTicks) {
      return contactState = PROG_COMPLETE;
    }
    else {
      return contactState = PROG_CONTACT;
    }
  }
  
  
  boolean checkTargetContact(Target from) {
    Pathing last = (Pathing) Visit.last(path);
    if (from != last && active.at() == last) return true;
    float dist = Area.distance(active, from), range = actionRange();
    return dist < range;
  }
  
  
  float actionRange() {
    return 0.1f;
  }
  
  
  boolean inContact() {
    return contactState != PROG_CLOSING && contactState != PROG_COMPLETE;
  }
  
  
  boolean complete() {
    return contactState >= PROG_COMPLETE;
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
    boolean flight = actor.type().moveMode == Type.MOVE_AIR;
    
    if (Area.distance(last, target) > 1.5f) return false;
    
    int index = Nums.clamp(pathIndex, path.length);
    Pathing current = pathOrigin(actor), step = path[index];
    if (current != step) return false;
    
    for (int i = 0; i < actor.type().sightRange; i++, index++) {
      if (index >= path.length) break;
      Pathing t = path[index];
      
      //  TODO:  Reference an 'isBlocked' check in the ActorPathSearch class
      //  here!
      
      if (
        t.isTile() && (! flight) &&
        map.blocked((AreaTile) t) &&
        map.above((AreaTile) t) != actor
      ) {
        return false;
      }
      if (! (t.onMap() && t.allowsEntry(actor))) {
        return false;
      }
    }
    
    return true;
  }
  
  
  boolean updatePathing() {
    if (! active.mobile()) return false;
    
    boolean report  = reports();
    boolean verbose = false;
    
    Actor   actor    = (Actor) active;
    Area    map      = active.map();
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
    
    //  TODO:  You should have map-settings that toggle whether the
    //  path-cache is used at all.  Default to simpler checks in that
    //  case.
    
    ActorPathSearch search = new ActorPathSearch(actor, from, heads);
    if (
      (! visiting) && (! search.flight) &&
      ! map.pathCache.pathConnects(from, heads, false, false)
    ) {
      search.setProximate(true);
    }
    search.doSearch();
    this.path = search.fullPath(Pathing.class);
    this.pathIndex = 0;
    
    if (path == null) {
      if (report) I.say("  Could not find path for "+this);
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
  
  
  float animProgress(float alpha) {
    Area map = active.map();
    if (map == null) return -1;
    int maxTicks = map.ticksPS * Nums.max(1, maxTime);
    
    float prog = (
      (lastTicks  * (1 - alpha)) +
      (ticksSpent *      alpha )
    ) / maxTicks;
    return prog;
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











