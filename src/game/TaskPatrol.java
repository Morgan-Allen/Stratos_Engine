/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package game;
import static game.GameConstants.*;
import graphics.common.*;
import util.*;



public class TaskPatrol extends Task implements TileConstants {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static int
    TYPE_PROTECTION    = 0,
    TYPE_STREET_PATROL = 1,
    TYPE_SENTRY_DUTY   = 2;
  final static int
    WATCH_TIME = 10,
    MAX_STOPS  = 4;
  
  
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  final int type;
  final Target guarded;
  
  private int numStops = 0;
  private Target onPoint;
  private List <Target> patrolled;
  
  
  
  private TaskPatrol(
    Actor actor, Target guarded, List <Target> patrolled, int type
  ) {
    super(actor);
    this.type      = type;
    this.guarded   = guarded;
    this.patrolled = patrolled;
    onPoint = (Target) patrolled.first();
  }
  
  
  public TaskPatrol(Session s) throws Exception {
    super(s);
    Area map = active.map();
    
    type     = s.loadInt();
    guarded  = Area.loadTarget(map, s);
    numStops = s.loadInt();
    onPoint  = Area.loadTarget(map, s);
    
    patrolled = new List();
    for (int n = s.loadInt(); n-- > 0;) {
      patrolled.add(Area.loadTarget(map, s));
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    Area map = active.map();
    
    s.saveInt(type);
    Area.saveTarget(guarded, map, s);
    s.saveInt(numStops);
    Area.saveTarget(onPoint, map, s);
    
    s.saveInt(patrolled.size());
    for (Target t : patrolled) {
      Area.saveTarget(t, map, s);
    }
  }
  
  
  
  /**  External factory methods and supplemental evaluation calls-
    */
  public static TaskPatrol protectionFor(
    Actor actor, Target guarded, Employer origin
  ) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next perimeter patrol for "+actor);
    
    final Area map = actor.map();
    final List <Target> patrolled = new List();
    
    if (! guarded.type().isBuilding()) {
      patrolled.add(guarded);
    }
    else {
      Building building = (Building) guarded;
      
      AreaTile from = actor.at();
      if (actor.indoors()) from = ((Building) actor.inside()).mainEntrance();
      Batch <AreaTile> around = new Batch();
      
      for (AreaTile t : building.perimeter(map)) {
        if (t == null || ! map.pathCache.pathConnects(from, t)) continue;
        around.include(t);
      }
      
      int divS = around.size() / 4;
      for (int n = 0; n < around.size();) {
        AreaTile adds = around.atIndex(n);
        patrolled.add(adds);
        n += divS;
      }
    }
    
    if (patrolled.empty()) return null;
    
    TaskPatrol p = new TaskPatrol(actor, guarded, patrolled, TYPE_PROTECTION);
    return (TaskPatrol) p.configTask(origin, null, guarded, JOB.PATROLLING, 1);
  }
  
  
  public static TaskPatrol sentryDutyFor(
    Actor actor, AreaTile point, Employer origin
  ) {
    List <Target> points = new List();
    points.add(point);
    TaskPatrol p = new TaskPatrol(actor, point, points, TYPE_SENTRY_DUTY);
    return (TaskPatrol) p.configTask(origin, null, point, JOB.PATROLLING, 5);
  }
  
  
  public static TaskPatrol nextGuardPatrol(
    Actor actor, Building origin
  ) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) {
      I.say("\nGetting next guard patrol for "+actor);
      I.say("  Base: "+origin.base());
    }
    
    //  Grab a random building nearby and patrol around it.
    final Area map = actor.map();
    final Base base = origin.base();
    final float range = GameConstants.MAX_WANDER_RANGE;
    
    Pick <Building> pick = new Pick();
    for (Building b : map.buildings()) if (b.base() == base) {
      float dist = Area.distance(origin, b);
      if (dist > range) continue;
      if (Task.hasTaskFocus(b, JOB.PATROLLING)) continue;
      pick.compare(b, Rand.num());
    }
    
    Building goes = pick.result();
    if (report) I.say("  Venue picked: "+goes);
    
    if (goes == null) return null;
    return protectionFor(actor, goes, origin);
  }
  
  
  
  /**  Behaviour execution-
    */
  void toggleFocus(boolean activeNow) {
    super.toggleFocus(activeNow);
    guarded.setFocused(active, activeNow);
  }
  
  
  protected Task reaction() {
    final Actor actor = (Actor) active;
    return Task.inCombat(actor) ? null :
      TaskCombat.nextReaction(actor, guarded, origin, actor.seen())
    ;
  }


  protected void onTarget(Target target) {
    final Actor actor = (Actor) active;
    if (onPoint == null) return;
    
    final boolean report = I.talkAbout == actor;
    if (report) {
      I.say("\nGetting next patrol step for "+actor);
      I.say("  Going to: "+onPoint+", num stops: "+numStops);
      I.say("  All patrol points: "+patrolled);
    }
    
    final Area map = actor.map();
    AreaTile stop = onPoint.at();
    
    if (onPoint.type().isActor()) {
      final Task t = ((Actor) onPoint).task();
      final Target ahead = t == null ? onPoint : t.nextOnPath();
      stop = ActorUtils.pickRandomTile(ahead, 2, map);
      stop = AreaTile.nearestOpenTile(stop, map);
      if (++numStops >= MAX_STOPS) return;
    }
    
    else {
      Target last = onPoint;
      stop = AreaTile.nearestOpenTile(stop, map);
      
      final int index = patrolled.indexOf(onPoint) + 1;
      if (index < patrolled.size() && last != patrolled.atIndex(index)) {
        onPoint = (Target) patrolled.atIndex(index);
      }
      else {
        onPoint = null;
      }
    }
    
    if (report) {
      I.say("  Next stop: "+stop);
    }
    
    if (stop != null) {
      configTask(origin, null, stop, JOB.PATROLLING, 1);
    }
  }


  /**  Rendering and interface methods-
    */
  public String toString() {
    StringBuffer d = new StringBuffer();
    if (type == TYPE_PROTECTION) {
      d.append("Guarding ");
      d.append(guarded);
    }
    if (type == TYPE_STREET_PATROL || type == TYPE_SENTRY_DUTY) {
      if (patrolled.size() == 1 || guarded == patrolled.last()) {
        d.append("Patrolling around ");
        d.append(guarded);
      }
      else {
        d.append("Patrolling between ");
        d.append(guarded);
        d.append(" and ");
        d.append(patrolled.last());
      }
    }
    return d.toString();
  }
  
  
  String animName() {
    return AnimNames.LOOK;
  }
}





