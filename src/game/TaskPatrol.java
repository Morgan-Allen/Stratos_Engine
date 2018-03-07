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
  final Element guarded;
  
  private Target onPoint;
  private int numStops = 0;
  private List <Target> patrolled;
  
  
  
  private TaskPatrol(
    Actor actor, Element guarded, List <Target> patrolled, int type
  ) {
    super(actor);
    this.type      = type;
    this.guarded   = guarded;
    this.patrolled = patrolled;
    onPoint = (Target) patrolled.first();
  }
  
  
  public TaskPatrol(Session s) throws Exception {
    super(s);
    type     = s.loadInt();
    guarded  = (Element) s.loadObject();
    onPoint  = (Target) s.loadObject();
    numStops = s.loadInt();
    
    AreaMap map = guarded.map();
    patrolled = new List();
    for (int n = s.loadInt(); n-- > 0;) {
      if (s.loadBool()) {
        patrolled.add(AreaMap.loadTile(map, s));
      }
      else {
        patrolled.add((Target) s.loadObject());
      }
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt   (type    );
    s.saveObject(guarded );
    s.saveObject(onPoint );
    s.saveInt   (numStops);
    
    AreaMap map = guarded.map();
    s.saveInt(patrolled.size());
    for (Target t : patrolled) {
      if (t.isTile()) {
        s.saveBool(true);
        AreaMap.saveTile((Tile) t, map, s);
      }
      else {
        s.saveBool(false);
        s.saveObject(t);
      }
    }
  }
  
  
  
  /**  External factory methods and supplemental evaluation calls-
    */
  public static TaskPatrol protectionFor(
    Actor actor, Element guarded, float priority
  ) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) I.say("\nGetting next perimeter patrol for "+actor);
    
    final AreaMap map = actor.map();
    final List <Target> patrolled = new List();
    
    if (guarded.type().mobile) {
      patrolled.add(guarded);
    }
    else {
      Tile from = actor.at();
      if (actor.indoors()) from = ((Building) actor.inside()).mainEntrance();
      Batch <Tile> around = new Batch();
      
      for (Tile t : guarded.perimeter(map)) {
        if (t == null || ! map.pathCache.pathConnects(from, t)) continue;
        around.include(t);
      }
      
      int divS = around.size() / 4;
      for (int n = 0; n < around.size();) {
        Tile adds = around.atIndex(n);
        patrolled.add(adds);
        n += divS;
      }
    }
    
    if (patrolled.empty()) return null;
    
    TaskPatrol p = new TaskPatrol(actor, guarded, patrolled, TYPE_PROTECTION);
    return (TaskPatrol) p.configTask(null, null, guarded, JOB.PATROLLING, 1);
  }
  
  
  public static TaskPatrol streetPatrol(
    Actor actor, Element init, Element dest, float priority
  ) {
    final AreaMap map = actor.map();
    final Tile
      initT = Tile.nearestOpenTile(init.at(), map, 4),
      destT = Tile.nearestOpenTile(dest.at(), map, 4);
    
    if (! map.pathCache.pathConnects(initT, destT)) return null;
    
    final List <Target> patrolled = new List();
    patrolled.include(initT);
    patrolled.include(destT);
    
    TaskPatrol p = new TaskPatrol(actor, init, patrolled, TYPE_STREET_PATROL);
    return (TaskPatrol) p.configTask(null, null, initT, JOB.PATROLLING, 1);
  }
  
  
  public static TaskPatrol nextGuardPatrol(
    Actor actor, Building origin, float priority
  ) {
    final boolean report = evalVerbose && I.talkAbout == actor;
    if (report) {
      I.say("\nGetting next guard patrol for "+actor);
      I.say("  Base: "+origin.base());
    }
    
    //  Grab a random building nearby and patrol around it.
    final AreaMap map = actor.map();
    final Base base = origin.base();
    final float range = GameConstants.MAX_WANDER_RANGE;
    
    Pick <Building> pick = new Pick();
    for (Building b : map.buildings()) if (b.base() == base) {
      float dist = AreaMap.distance(origin, b);
      if (dist > range) continue;
      if (Task.hasTaskFocus(b, JOB.PATROLLING)) continue;
      pick.compare(b, Rand.num());
    }
    
    Building goes = pick.result();
    if (report) I.say("  Venue picked: "+goes);
    
    if (goes == null) return null;
    return protectionFor(actor, goes, priority);
    //else return streetPatrol(actor, origin, goes, priority);
  }
  
  
  
  /**  Behaviour execution-
    */
  void toggleFocus(boolean activeNow) {
    super.toggleFocus(activeNow);
    guarded.setFocused(active, activeNow);
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
    
    final AreaMap map = actor.map();
    Tile stop = onPoint.at();
    
    if (onPoint.type().isActor()) {
      final Task t = ((Actor) onPoint).task();
      final Target ahead = t == null ? onPoint : t.nextOnPath();
      stop = ActorUtils.pickRandomTile(ahead, 2, map);
      stop = Tile.nearestOpenTile(stop, map);
      if (++numStops >= MAX_STOPS) return;
    }
    
    else {
      Target last = onPoint;
      stop = Tile.nearestOpenTile(stop, map);
      
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
      configTask(null, null, stop, JOB.PATROLLING, 1);
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





