/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package game;
import static game.GameConstants.*;
import util.*;



public class TaskPatrol extends Task implements TileConstants {
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static int
    TYPE_PROTECTION    = 0,
    TYPE_STREET_PATROL = 1,
    TYPE_SENTRY_DUTY   = 2;
  final static int
    WATCH_TIME = 10;
  
  
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  final int type;
  final Element guarded;
  
  private Target onPoint;
  private float postTime = -1;
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
    postTime = s.loadFloat();
    s.loadObjects(patrolled = new List());
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt   (type    );
    s.saveObject(guarded );
    s.saveObject(onPoint );
    s.saveFloat (postTime);
    s.saveObjects(patrolled);
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
      final float range = Nums.max(
        guarded.radius() * 2,
        actor.sightRange() / 2
      );
      Tile at = guarded.centre();
      if (report) I.say("  Range is: "+range+", centre: "+at);
      
      for (int n : T_ADJACENT) {
        Tile point = map.tileAt(
          Nums.clamp(at.x + (T_X[n] * range), 0, map.size - 1),
          Nums.clamp(at.y + (T_Y[n] * range), 0, map.size - 1)
        );
        if (point != null) {
          if (report) I.say("  Patrol point: "+point);
          patrolled.include(point);
        }
      }
    }
    
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
    patrolled.add(initT);
    patrolled.add(destT);
    
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
      pick.compare(b, Rand.num());
    }
    
    Building goes = pick.result();
    if (report) I.say("  Venue picked: "+goes);
    
    if (goes == null) return null;
    return TaskPatrol.streetPatrol(actor, goes, goes, priority);
  }
  
  
  /**  Behaviour execution-
    */
  protected void onTarget(Target target) {
    final Actor actor = (Actor) active;
    if (onPoint == null) return;
    
    final boolean report = I.talkAbout == actor && stepsVerbose;
    if (report) {
      I.say("\nGetting next patrol step for "+actor);
      I.say("  Going to: "+onPoint+", post time: "+postTime);
    }
    
    final AreaMap map = actor.map();
    Target stop = onPoint;
    
    if (onPoint.type().isActor()) {
      final Task t = ((Actor) onPoint).task();
      final Target ahead = t == null ? onPoint : t.nextOnPath();
      Tile open = ActorUtils.pickRandomTile(ahead, 2, map);
      open = Tile.nearestOpenTile(open, map);
      if (open == null) return;
      else stop = open;
    }
    
    else {
      Tile open = onPoint.at();
      open = Tile.nearestOpenTile(open, map);
      if (open == null) return;
      else stop = open;

      final int index = patrolled.indexOf(onPoint) + 1;
      if (index < patrolled.size()) {
        onPoint = (Target) patrolled.atIndex(index);
      }
      else {
        onPoint = null;
      }
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
}






//  TODO:  Return to this later...
/*
public static TaskPatrol sentryDuty(
  Actor actor, ShieldWall start, Venue barracks, float priority
) {
  if (start == null || start.base() != barracks.base()) {
    return null;
  }
  
  final Vec3D between = Spacing.between(barracks, start);
  final Vec2D prefHeading = new Vec2D(between).perp();
  final float maxDist = Stage.ZONE_SIZE;
  
  final List <Target> patrolled = new List();
  final Batch <Target> flagged = new Batch();
  ShieldWall doors = null;
  ShieldWall next = start;
  float sumDist = 0; Vec3D temp = new Vec3D();
  
  while (next != null) {
    if (next.isTower()) patrolled.include(next);
    if (next.isGate() && doors == null) doors = next;
    sumDist += next.radius() * 2;
    
    if (sumDist > maxDist) break;
    next.flagWith(flagged);
    flagged.add(next);
    
    final Pick <Boarding> pick = new Pick();
    for (Boarding b : next.canBoard()) {
      if (! (b instanceof ShieldWall)) continue;
      if (b.flaggedWith() != null) continue;
      b.position(temp);
      pick.compare(b, prefHeading.dot(temp.x, temp.y));
    }
    next = (ShieldWall) pick.result();
  }
  
  for (Target t : flagged) t.flagWith(null);
  if (doors == null) {
    return null;
  }
  
  TaskPatrol p = new TaskPatrol(actor, doors, patrolled, TYPE_SENTRY_DUTY);
  return (TaskPatrol) p.addMotives(Plan.NO_PROPERTIES, priority);
}
//*/



/*
  
  final static Trait BASE_TRAITS[] = { FEARLESS, PERSISTENT, SOLITARY };
  
  protected float getPriority() {
    if (onPoint == null || patrolled.size() == 0) return 0;
    
    float urgency, avgDanger = 0, modifier;
    if (actor.base() != null) for (Target t : patrolled) {
      avgDanger += actor.base().dangerMap.sampleAround(t, Stage.ZONE_SIZE);
    }
    avgDanger = Nums.clamp(avgDanger / patrolled.size(), 0, 2);
    urgency   = avgDanger;
    modifier  = 0 - actor.senses.fearLevel();
    
    int teamSize = hasMotives(MOTIVE_MISSION) ? Mission.AVG_PARTY_LIMIT : 1;
    setCompetence(rateCompetence(actor, guarded, teamSize));
    
    toggleMotives(MOTIVE_EMERGENCY, PlanUtils.underAttack(guarded));
    final float priority = PlanUtils.jobPlanPriority(
      actor, this, urgency + modifier, competence(),
      -1, Plan.REAL_FAIL_RISK * avgDanger, BASE_TRAITS
    );
    return priority;
  }
  
  
  public static void addFormalPatrols(
    Actor actor, Venue origin, Choice choice
  ) {
    ShieldWall wall = (ShieldWall) origin.world().presences.randomMatchNear(
      ShieldWall.class, origin, Stage.ZONE_SIZE
    );
    choice.add(TaskPatrol.sentryDuty(actor, wall, origin, Plan.ROUTINE));
    choice.add(TaskPatrol.nextGuardPatrol(actor, origin, Plan.CASUAL));
  }
  
  
  public static ShieldWall turretIsAboard(Target t) {
    if (! (t instanceof Mobile)) return null;
    final Boarding aboard = ((Mobile) t).aboard();
    if (aboard instanceof ShieldWall) return (ShieldWall) aboard;
    else return null;
  }
  
  
  public static float rateCompetence(
    Actor actor, Target guarded, int teamSize
  ) {
    //  TODO:  Include bonus from first aid or assembly skills, depending on the
    //  target and damage done?
    
    if (! PlanUtils.isArmed(actor)) return 0;
    final Tile under = actor.world().tileAt(guarded);
    return PlanUtils.combatWinChance(actor, under, teamSize);
  }
  
  
  public boolean finished() {
    if (onPoint == null) return true;
    return super.finished();
  }


public int motionType(Actor actor) {
  
  //
  //  TODO:  Revisit this later...
  
  if (actor.senses.isEmergency()) {
    return MOTION_FAST;
  }
  
  if (guarded.isMobile()) {
    final Mobile m = (Mobile) guarded;
    final float dist = Spacing.distance(actor, m.aboard());
    if (dist >= actor.health.sightRange() / 2) return MOTION_FAST;
  }
  
  //  TODO:  Replace this with a general 'harm intended' clause?
  final Activities a = actor.world().activities;
  if (a.includesActivePlan(guarded, Combat.class)) return MOTION_FAST;
  
  return super.motionType(actor);
}
//*/








