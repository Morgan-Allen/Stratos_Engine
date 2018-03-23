

package game;
import static game.ActorUtils.*;
import static game.GameConstants.*;
import game.Task.JOB;
import gameUI.play.*;
import graphics.common.*;
import util.*;



public abstract class Mission implements
  Session.Saveable, Journeys, TileConstants, Employer, Selection.Focus
{
  
  /**  Data fields, construction and save/load methods-
    */
  final public static int
    OBJECTIVE_STANDBY = 0,
    OBJECTIVE_STRIKE  = 1,
    OBJECTIVE_SECURE  = 2,
    OBJECTIVE_RECON   = 3,
    OBJECTIVE_CONTACT = 4
  ;
  
  final public int objective;
  final Base homeBase;
  
  private Base worldFocus;
  private Target localFocus;
  
  List <Actor> recruits = new List();
  List <Actor> envoys   = new List();
  final public MissionRewards rewards = new MissionRewards(this);
  final public MissionTerms terms = new MissionTerms(this);
  
  boolean active;
  boolean complete;
  boolean success;
  boolean failure;
  
  Base localBase;
  AreaTile transitTile;
  ActorAsVessel transitShip;
  int arriveTime = -1;
  
  
  final static String OBJECTIVE_NAMES[] = {
    "Standby", "Strike", "Secure", "Recon", "Contact"
  };
  
  Sprite flag;
  boolean noFlag = false;
  
  
  
  public Mission(int objective, Base belongs) {
    this.objective = objective;
    this.homeBase  = belongs;
  }
  
  
  public Mission(Session s) throws Exception {
    s.cacheInstance(this);
    
    objective = s.loadInt();
    homeBase = (Base) s.loadObject();
    
    Area map = (Area) s.loadObject();
    worldFocus = (Base) s.loadObject();
    localFocus = Area.loadTarget(map, s);
    
    s.loadObjects(recruits);
    s.loadObjects(envoys);
    terms.loadState(s);
    
    active   = s.loadBool();
    complete = s.loadBool();
    success  = s.loadBool();
    failure  = s.loadBool();
    
    localBase   = (Base) s.loadObject();
    transitTile = Area.loadTile(map, s);
    transitShip = (ActorAsVessel) s.loadObject();
    arriveTime  = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveInt(objective);
    s.saveObject(homeBase);
    
    Area map = localMap();
    s.saveObject(map);
    s.saveObject(worldFocus);
    Area.saveTarget(localFocus, map, s);
    
    s.saveObjects(recruits);
    s.saveObjects(envoys);
    terms.saveState(s);
    
    s.saveBool(active  );
    s.saveBool(complete);
    s.saveBool(success );
    s.saveBool(failure );
    
    s.saveObject(localBase);
    Area.saveTile(transitTile, map, s);
    s.saveObject(transitShip);
    s.saveInt(arriveTime);
  }
  
  
  
  /**  Regular updates and internal events-
    */
  public void toggleRecruit(Actor a, boolean is) {
    this.recruits.toggleMember(a, is);
    a.setMission(is ? this : null);
  }
  
  
  public void toggleEnvoy(Actor a, boolean is) {
    this.envoys.toggleMember(a, is);
    toggleRecruit(a, is);
  }
  
  
  public void setLocalFocus(Target focus) {
    this.localFocus = focus;
  }
  
  
  public void setWorldFocus(Base focus) {
    this.worldFocus = focus;
  }
  
  
  //  TODO:  The local-base has to be assigned along with the focus, I think-
  //  because actors might be assessing the mission before it's begun.
  
  public void beginMission(Base localBase) {
    homeBase.missions.toggleMember(this, true);
    this.active    = true;
    this.localBase = localBase;
  }
  
  
  void update() {
    
    World world  = homeBase.world;
    Area  map    = localMap();
    Base  offmap = offmapBase();
    
    boolean valid = recruits.size() > 0 && active, allHome = true;
    boolean allDeparting = valid, allReturning = valid;
    
    for (Actor a : recruits) {
      if (! readyForTransit(a, JOB.DEPARTING)) allDeparting = false;
      if (! readyForTransit(a, JOB.RETURNING)) allReturning = false;
      if (a.map() != homeBase.activeMap()) allHome = false;
      if (world.onJourney(a)) allHome = false;
    }
    
    if (transitTile == null && map != null && offmap != null) {
      transitTile = findTransitPoint(map, localBase, offmap);
    }
    if (allDeparting && ! complete()) {
      beginJourney(localBase, worldFocus());
    }
    if (allReturning && complete()) {
      beginJourney(localBase, homeBase());
    }
    if (allHome && complete()) {
      disbandMission();
    }
  }
  
  
  void beginJourney(Base from, Base goes) {
    
    //  TODO:  Consider carefully who should be embarking- maybe only the
    //  journey, or maybe only the ship- and remember that actors who go on a
    //  journey handle their entry independently.
    
    World world = homeBase.world;
    List <Journeys> going = new List();
    Visit.appendTo(going, recruits);
    going.add(this);
    
    World.Journey journey;
    journey = world.beginJourney(from, goes, going.toArray(Journeys.class));
    
    //I.say(this+" beginning journey from "+from+" to "+goes);
    //I.say("  ETA: "+world.arriveTime(this));
    
    if (from.activeMap() == null) {
      if (goes == homeBase) {
        BaseEvents.handleDeparture(this, from, goes);
      }
      else {
        handleOffmapDeparture(from, journey);
      }
    }
    
    this.localBase   = null;
    this.localFocus  = null;
    this.transitTile = null;
  }
  
  
  public void onArrival(Base goes, World.Journey journey) {
    this.localBase  = goes;
    this.arriveTime = goes.world.time();
    
    //I.say(this+" arrived at "+goes);
    
    if (goes.activeMap() == null) {
      if (goes == homeBase) {
        BaseEvents.handleReturn(this, goes, journey);
      }
      else {
        handleOffmapArrival(goes, journey);
      }
    }
  }
  
  
  public Task selectActorBehaviour(Actor actor) {
    Pathing exits = transitPoint(actor);
    
    //  TODO:  Ponder how to handle the ship later...
    /*
    if (actor == transitShip) {
      return actor.targetTask(transitShip.landing(), 5, JOB.WAITING, this);
    }
    //*/
    
    if (complete()) {
      if (exits != null && ! onHomeMap()) {
        return actor.targetTask(exits, 5, Task.JOB.RETURNING, this);
      }
    }
    else {
      if (onWrongMap() && exits != null) {
        return actor.targetTask(exits, 5, Task.JOB.DEPARTING, this);
      }
      else {
        return nextLocalMapBehaviour(actor);
      }
    }
    
    return null;
  }
  
  
  public void actorTargets(Actor actor, Target other) {
    boolean leaves = actor == transitShip || ! haveTransport();
    
    if (actor.jobType() == JOB.RETURNING) {
      if (leaves) actor.exitMap(actor.map());
    }
    if (actor.jobType() == JOB.DEPARTING) {
      if (leaves) actor.exitMap(actor.map());
    }
  }
  
  
  public void setMissionComplete(boolean success) {
    this.complete = true;
    if (success) this.success = true;
    else this.failure = true;
  }
  
  
  public void disbandMission() {
    rewards.dispenseRewards();
    homeBase.missions.toggleMember(this, false);
    this.active = false;
    for (Actor r : recruits) toggleRecruit(r, false);
  }
  
  
  
  /**  Internal utility methods-
    */
  public boolean onWrongMap() {
    if (worldFocus == null) return false;
    if (localMap() == null) return true;
    return worldFocus.activeMap() != localBase.activeMap();
  }
  
  
  public boolean onHomeMap() {
    if (localBase == null) return false;
    return localBase.activeMap() == homeBase.activeMap();
  }
  
  
  public Base offmapBase() {
    if (worldFocus == null) return null;
    return onWrongMap() ? worldFocus : homeBase;
  }
  
  
  public Area localMap() {
    if (localBase == null) return null;
    return localBase.activeMap();
  }
  
  
  public Base homeBase() {
    return homeBase;
  }
  
  
  public Base worldFocus() {
    return worldFocus;
  }
  
  
  public Target localFocus() {
    return localFocus;
  }
  
  
  public Pathing transitPoint(Actor actor) {
    if (actor == transitShip) return transitTile;
    if (transitTile != null) return transitTile;
    if (transitShip != null) return transitShip;
    return null;
  }
  
  
  public boolean haveTransport() {
    return transitShip != null;
  }
  
  
  public boolean readyForTransit(Actor a, JOB type) {
    if (localBase  == null) return false;
    if (localMap() == null) return true;
    if (a.jobType() != type) return false;
    if (haveTransport()) return a.inside() == transitShip;
    return ! a.onMap();
  }
  
  
  public Series <Actor> recruits() {
    return recruits;
  }
  
  
  public Series <Actor> envoys() {
    return envoys;
  }
  
  
  public boolean active() {
    return active;
  }
  
  
  public boolean complete() {
    return complete;
  }
  
  
  public Base base() {
    return homeBase;
  }
  
  
  
  /**  Other terms of the Employer interface...
    */
  public void actorUpdates(Actor actor) {
    return;
  }
  
  
  public void actorPasses(Actor actor, Building other) {
    return;
  }
  
  
  public void actorVisits(Actor actor, Building visits) {
    return;
  }
  
  
  
  /**  Delegates to sub-classes:
    */
  public abstract boolean allowsFocus(Object focus);
  
  abstract Task nextLocalMapBehaviour(Actor actor);
  
  abstract void handleOffmapArrival  (Base goes, World.Journey journey);
  abstract void handleOffmapDeparture(Base from, World.Journey journey);
  
  
  
  /**  Graphical, debug and interface methods-
    */
  boolean reports() {
    return false;
  }
  
  
  public String toString() {
    return fullName();
  }
  
  
  public String fullName() {
    Object focus = localFocus == null ? worldFocus : localFocus;
    return "Mission: "+OBJECTIVE_NAMES[objective]+": "+focus;
  }
  
  
  public void whenClicked(Object context) {
    PlayUI.pushSelection(this);
  }
  
  
  public boolean canRender(Base base, Viewport view) {
    if (noFlag) {
      return false;
    }
    if (localFocus.isTile()) {
      return true;
    }
    else {
      return ((Element) localFocus).canRender(base, view);
    }
  }
  
  
  public void renderFlag(Rendering rendering) {
    if (flag == null) {
      String key = "";
      if (objective == OBJECTIVE_STRIKE ) key = World.KEY_ATTACK_FLAG ;
      if (objective == OBJECTIVE_SECURE ) key = World.KEY_DEFEND_FLAG ;
      if (objective == OBJECTIVE_RECON  ) key = World.KEY_EXPLORE_FLAG;
      if (objective == OBJECTIVE_CONTACT) key = World.KEY_CONTACT_FLAG;
      Type type = homeBase.world.mediaTypeWithKey(key);
      if (type == null || type.model == null) {
        flag   = null;
        noFlag = true;
        return;
      }
      else {
        flag = type.model.makeSprite();
      }
    }
    
    if (localFocus.isTile()) {
      AreaTile t = (AreaTile) localFocus;
      flag.position.setTo(t.trackPosition());
      flag.position.z += 1;
    }
    else {
      Element e = (Element) localFocus;
      flag.position.setTo(e.trackPosition());
      flag.position.z += e.type().deep;
    }
    
    //  TODO:  Tint the flag if it's been highlit.
    
    flag.readyFor(rendering);
  }
  
  
  public boolean testSelection(PlayUI UI, Base base, Viewport view) {
    if (flag == null || ! canRender(base, view)) return false;
    
    final float selRad = 0.5f;
    final Vec3D selPos = new Vec3D(flag.position);
    selPos.z += 0.5f;
    if (! view.mouseIntersects(selPos, selRad, UI)) return false;
    
    return true;
  }
  
  
  public boolean setSelected(PlayUI UI) {
    UI.setDetailPane(new MissionPane(UI, this));
    return true;
  }
  
  
  public boolean trackSelection() {
    if (localFocus instanceof Selection.Focus) {
      return true;
    }
    else {
      return false;
    }
  }
  
  
  public Vec3D trackPosition() {
    if (localFocus instanceof Selection.Focus) {
      return ((Selection.Focus) localFocus).trackPosition();
    }
    else {
      return new Vec3D();
    }
  }
  
}



