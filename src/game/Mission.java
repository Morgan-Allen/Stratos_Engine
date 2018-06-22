

package game;
import static game.ActorUtils.*;
import static game.GameConstants.*;
import static game.Task.*;
import gameUI.play.*;
import graphics.common.*;
import graphics.widgets.*;
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
  final public MissionTerms   terms   = new MissionTerms  (this);
  
  boolean active;
  boolean complete;
  boolean success;
  boolean failure;
  
  Base localBase;
  AreaTile transitTile;
  ActorAsVessel transport;
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
    rewards.loadState(s);
    terms.loadState(s);
    
    active   = s.loadBool();
    complete = s.loadBool();
    success  = s.loadBool();
    failure  = s.loadBool();
    
    localBase   = (Base) s.loadObject();
    transitTile = Area.loadTile(map, s);
    transport = (ActorAsVessel) s.loadObject();
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
    rewards.saveState(s);
    terms.saveState(s);
    
    s.saveBool(active  );
    s.saveBool(complete);
    s.saveBool(success );
    s.saveBool(failure );
    
    s.saveObject(localBase);
    Area.saveTile(transitTile, map, s);
    s.saveObject(transport);
    s.saveInt(arriveTime);
  }
  
  
  
  /**  Setting up recruits, envoys, transit and objectives (foci)-
    */
  public void toggleRecruit(Actor a, boolean is) {
    this.recruits.toggleMember(a, is);
    a.setMission(is ? this : null);
  }
  
  
  public void toggleEnvoy(Actor a, boolean is) {
    this.envoys.toggleMember(a, is);
    toggleRecruit(a, is);
  }
  
  
  public void setWorker(Actor a, boolean is) {
    toggleRecruit(a, is);
  }
  
  
  public void assignTransport(ActorAsVessel transport) {
    if (this.transport != null) toggleRecruit(this.transport, false);
    this.transport = transport;
    if (transport != null) toggleRecruit(transport, true);
  }
  
  
  public void setLocalFocus(Target focus) {
    this.localFocus = focus;
  }
  
  
  public void setWorldFocus(Base focus) {
    this.worldFocus = focus;
  }
  
  
  
  /**  Regular updates and internal events-
    */
  //  TODO:  The local-base has to be assigned along with the focus, I think-
  //  because actors might be assessing the mission before it's begun.
  
  public void beginMission(Base localBase) {
    homeBase.missions.toggleMember(this, true);
    this.active    = true;
    this.localBase = localBase;
  }
  
  
  void update() {
    
    Area    map      = localMap();
    Base    offmap   = offmapBase();
    boolean valid    = recruits.size() > 0 && active;
    boolean complete = valid && complete();
    boolean moveSelf = transport == null || ! transport.onMap();
    
    if (transitTile == null && map != null && offmap != null) {
      transitTile = findTransitPoint(map, localBase, offmap, Type.MOVE_LAND, 1);
    }
    if ((! complete) && moveSelf && readyToDepart()) {
      beginJourney(localBase, worldFocus());
    }
    if (complete && moveSelf && readyToReturn()) {
      beginJourney(localBase, homeBase());
    }
    if (complete && recruitsAllHome()) {
      disbandMission();
    }
  }
  
  
  boolean readyToDepart() {
    for (Actor a : recruits) {
      if (a == transport) continue;
      if (! readyForTransit(a, JOB.DEPARTING)) return false;
    }
    return true;
  }
  
  
  boolean readyToReturn() {
    for (Actor a : recruits) {
      if (a == transport) continue;
      if (! readyForTransit(a, JOB.RETURNING)) return false;
    }
    return true;
  }
  
  
  boolean recruitsAllHome() {
    for (Actor a : recruits) {
      if (a.map() != homeBase.activeMap() && a.offmapBase() != homeBase) {
        return false;
      }
    }
    return true;
  }
  
  
  void beginJourney(Base from, Base goes) {
    
    World world = homeBase.world;
    List <Journeys> going = new List();
    World.Journey journey;
    int moveMode = Type.MOVE_LAND;
    
    if (transport != null) {
      going.add(transport);
      moveMode = transport.type().moveMode;
    }
    else {
      Visit.appendTo(going, recruits);
    }
    going.add(this);
    journey = world.beginJourney(from, goes, moveMode, going);
    
    //I.say(this+" beginning journey from "+from+" to "+goes);
    //I.say("  ETA: "+world.arriveTime(this));
    
    if (from.activeMap() == null) {
      if (from == homeBase) {
        WorldEvents.handleDeparture(this, from, goes);
      }
      else {
        handleOffmapDeparture(from, journey);
      }
    }
    
    this.localBase   = null;
    this.localFocus  = null;
    this.transitTile = null;
  }
  
  
  public void onDeparture(Base goes, World.Journey journey) {
    return;
  }
  
  
  public void onArrival(Base goes, World.Journey journey) {
    
    this.localBase  = goes;
    this.arriveTime = goes.world.time();
    
    if (goes.activeMap() == null) {
      if (goes == homeBase) {
        WorldEvents.handleReturn(this, goes, journey);
      }
      else {
        handleOffmapArrival(goes, journey);
      }
    }
  }
  
  
  public boolean isActor() {
    return false;
  }
  
  
  public Task selectActorBehaviour(Actor actor) {
    
    if (actor == transport) {
      return TaskTransport.nextTransport(transport, this);
    }
    
    Pathing exits = transitPoint(actor);
    
    if (complete()) {
      if (exits != null && ! onHomeMap()) {
        if (exits == transport) {
          return actor.visitTask(exits, 0, JOB.RETURNING, this);
        }
        else {
          return actor.targetTask(exits, 0, JOB.RETURNING, this);
        }
      }
    }
    else {
      if (onWrongMap() && exits != null) {
        if (exits == transport) {
          return actor.visitTask(exits, 0, JOB.DEPARTING, this);
        }
        else {
          return actor.targetTask(exits, 0, JOB.DEPARTING, this);
        }
      }
      else {
        return nextLocalMapBehaviour(actor);
      }
    }
    
    return null;
  }
  
  
  public void actorTargets(Actor actor, Target other) {
    boolean leaves = actor == transport || ! haveTransport();
    
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
    if (transport != null) return transport;
    if (transitTile != null) return transitTile;
    return null;
  }
  
  
  public boolean haveTransport() {
    return transport != null;
  }
  
  
  public boolean readyForTransit(Actor a, JOB type) {
    if (localBase  == null) return false;
    if (localMap() == null) return true;
    if (a.jobType() != type) return false;
    if (haveTransport()) return a.inside() == transport;
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
  
  
  public void actorVisits(Actor actor, Pathing visits) {
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
  
  
  private Type flagMedia() {
    String key = "";
    if (objective == OBJECTIVE_STRIKE ) key = World.KEY_ATTACK_FLAG ;
    if (objective == OBJECTIVE_SECURE ) key = World.KEY_DEFEND_FLAG ;
    if (objective == OBJECTIVE_RECON  ) key = World.KEY_EXPLORE_FLAG;
    if (objective == OBJECTIVE_CONTACT) key = World.KEY_CONTACT_FLAG;
    return homeBase.world.mediaTypeWithKey(key);
  }
  
  
  public Composite portrait() {
    final String key = "mission_ob"+objective+"_"+localFocus.hashCode();
    final Composite cached = Composite.fromCache(key);
    if (cached != null) return cached;
    
    //final int size = SelectionPane.PORTRAIT_SIZE;
    final int size = 40;
    
    Type type = flagMedia();
    if (type == null) return Composite.withSize(size, size, key);
    
    return Composite.withImage(type.icon, key);
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
      
      Type type = flagMedia();
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
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    return;
  }
  
  
  public boolean setSelected(PlayUI UI) {
    UI.setDetailPane(new PaneMission(UI, this));
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



