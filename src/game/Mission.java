

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
    OBJECTIVE_CONTACT = 4,
    OBJECTIVE_COLONY  = 5
  ;
  final public static int
    MODE_OPEN   =  0,
    MODE_SCREEN =  1,
    MODE_CLOSED =  2
  ;
  final static String OBJECTIVE_NAMES[] = {
    "Standby", "Strike", "Secure", "Recon", "Contact", "Colonise"
  };
  
  final static int
    STAGE_INIT     = -1,
    STAGE_BEGUN    =  0,
    STAGE_DEPARTED =  1,
    STAGE_ARRIVED  =  2,
    STAGE_RETURNED =  4,
    STAGE_DISBAND  =  5
  ;
  
  final public int objective;
  final Base homeBase;
  
  int hireMode = MODE_CLOSED;
  float evalPriority = -1;
  float evalChance   = -1;
  float evalForce    = -1;
  boolean active = false;
  
  private Object worldFocus;
  private Target localFocus;
  
  private int stage = STAGE_INIT;
  boolean complete = false;
  boolean success  = false;
  boolean failure  = false;
  
  List <Actor> recruits = new List();
  List <Actor> rejects  = new List();
  List <Actor> captives = new List();
  List <Actor> envoys   = new List();
  final public MissionRewards rewards = new MissionRewards(this);
  final public MissionTerms   terms   = new MissionTerms  (this);
  
  Area localLocale;
  AreaTile transitTile;
  ActorAsVessel transport;
  int arriveTime = -1;
  
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
    
    hireMode     = s.loadInt();
    evalPriority = s.loadFloat();
    evalChance   = s.loadFloat();
    evalForce    = s.loadFloat();
    active       = s.loadBool();
    
    AreaMap map = (AreaMap) s.loadObject();
    worldFocus = s.loadObject();
    localFocus = AreaMap.loadTarget(map, s);
    stage    = s.loadInt();
    complete = s.loadBool();
    success  = s.loadBool();
    failure  = s.loadBool();
    
    s.loadObjects(recruits);
    s.loadObjects(rejects );
    s.loadObjects(captives);
    s.loadObjects(envoys  );
    rewards.loadState(s);
    terms.loadState(s);
    
    localLocale = (Area) s.loadObject();
    transitTile = AreaMap.loadTile(map, s);
    transport   = (ActorAsVessel) s.loadObject();
    arriveTime  = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveInt(objective);
    s.saveObject(homeBase);
    
    s.saveInt  (hireMode    );
    s.saveFloat(evalPriority);
    s.saveFloat(evalChance  );
    s.saveFloat(evalForce   );
    s.saveBool (active      );
    
    AreaMap map = localMap();
    s.saveObject(map);
    s.saveObject(worldFocus);
    AreaMap.saveTarget(localFocus, map, s);
    s.saveInt(stage);
    s.saveBool(complete);
    s.saveBool(success);
    s.saveBool(failure);
    
    s.saveObjects(recruits);
    s.saveObjects(rejects );
    s.saveObjects(captives);
    s.saveObjects(envoys  );
    rewards.saveState(s);
    terms.saveState(s);
    
    s.saveObject(localLocale);
    AreaMap.saveTile(transitTile, map, s);
    s.saveObject(transport);
    s.saveInt(arriveTime);
  }
  
  
  
  /**  Setting up recruits, envoys, transit and objectives (foci)-
    */
  public void setHireMode(int mode) {
    this.hireMode = mode;
  }
  
  
  public boolean canRecruit(Actor applies) {
    if (! rewards.isBounty()) return false;
    
    if (hireMode == MODE_OPEN) {
      return true;
    }
    if (hireMode == MODE_SCREEN) {
      if (rejects.includes(applies)) return false;
      if (active) return false;
    }
    if (hireMode == MODE_CLOSED) {
      return false;
    }
    
    return false;
  }
  
  
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
  
  public void setWorldFocus(Area focus) {
    this.worldFocus = focus;
  }
  
  
  
  /**  Regular updates and internal events-
    */
  public void beginMission() {
    beginMission(homeBase.area);
  }
  
  
  public void beginMission(Area locale) {
    homeBase.missions.toggleMember(this, true);
    this.stage       = STAGE_BEGUN;
    this.active      = true;
    this.localLocale = locale;
    homeBase.world.events.recordEvent("Began mission", this);
  }
  
  
  void update() {
    if (! active()) return;
    
    if (hireMode == MODE_OPEN && worldFocus != null) {
      I.complain("OFF-MAP MISSIONS MUST CONFIRM RECRUITS BEFORE STARTING!");
      return;
    }
    
    transitPoint();
    
    boolean valid     = recruits.size() > 0;
    boolean departing = valid && ! departed();
    boolean returning = valid && complete();
    boolean moveSelf  = transport == null || ! transport.onMap();
    
    if (departing && moveSelf && readyToDepart()) {
      beginJourney(localLocale, worldFocusArea());
    }
    if (returning && moveSelf && readyToReturn()) {
      beginJourney(localLocale, homeBase().area);
    }
    if (returning && recruitsAllHome()) {
      disbandMission();
    }
    if (shouldDisband()) {
      disbandMission();
    }
  }
  
  
  boolean shouldDisband() {
    if (hireMode == MODE_OPEN) {
      return false;
    }
    if (hireMode == MODE_SCREEN || hireMode == MODE_CLOSED) {
      return recruits.empty();
    }
    return false;
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
      
      boolean isHome = false;
      if (homeBase.activeMap() != null) {
        isHome = a.map() == homeBase.activeMap();
      }
      else {
        isHome = a.offmap() == homeBase.area;
      }
      if (! isHome) return false;
      
      /*
      if (a.map() != homeBase.activeMap() && a.offmapBase() != homeBase) {
        return false;
      }
      //*/
    }
    return true;
  }
  
  
  void beginJourney(Area from, Area goes) {
    
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
    
    if (from == homeBase.area) stage = STAGE_DEPARTED;
    if (goes == homeBase.area) stage = STAGE_RETURNED;
    
    if (from.isOffmap()) {
      if (from == homeBase.area) MissionUtils.handleDeparture(this, homeBase, goes);
      handleOffmapDeparture(from, journey);
    }
    
    this.localLocale = null;
    this.localFocus  = null;
    this.transitTile = null;
  }
  
  
  public void onDeparture(Area from, World.Journey journey) {
    
    ///homeBase.world.events.recordEvent("  Departed", this, from);
    
    return;
  }
  
  
  public void onArrival(Area goes, World.Journey journey) {
    
    ///homeBase.world.events.recordEvent("  Arrived", this, goes);
    
    this.localLocale = goes;
    this.arriveTime  = homeBase().world.time;
    
    if (goes == worldFocus()) {
      stage = STAGE_ARRIVED;
    }
    if (goes.isOffmap()) {
      handleOffmapArrival(goes, journey);
      if (goes == homeBase.area) MissionUtils.handleReturn(this, homeBase, journey);
    }
  }
  
  
  public boolean isActor() {
    return false;
  }
  
  
  public Task selectActorBehaviour(Actor actor) {
    
    if (actor == transport) {
      return TaskTransport.nextTransport(transport, this);
    }
    
    Pathing exits = transitPoint();
    
    //I.say("");
    
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
    if (success) {
      this.success = true;
      homeBase.world.events.recordEvent("  Succeeded", this);
    }
    else {
      this.failure = true;
      homeBase.world.events.recordEvent("  Failed", this);
    }
  }
  
  
  public void disbandMission() {
    
    rewards.dispenseRewards();
    homeBase.missions.toggleMember(this, false);
    this.stage = STAGE_DISBAND;
    for (Actor r : recruits) toggleRecruit(r, false);
    
    homeBase.world.events.recordEvent("  Disbanded", this);
  }
  
  
  
  /**  Internal utility methods-
    */
  public boolean onWrongMap() {
    Area locale = worldFocusArea();
    if (locale == null     ) return false;
    if (locale.isOffmap()  ) return true;
    if (localLocale == null) return true;
    return locale.activeMap() != localLocale.activeMap();
  }
  
  
  public boolean onHomeMap() {
    if (localLocale == null) return false;
    return localLocale.activeMap() == homeBase.activeMap();
  }
  
  
  public Object worldFocus() {
    return worldFocus;
  }
  
  
  public Area worldFocusArea() {
    if (worldFocus instanceof Base) {
      return ((Base) worldFocus).area;
    }
    if (worldFocus instanceof Area) {
      return (Area) worldFocus;
    }
    return null;
  }
  
  
  public Base worldFocusBase() {
    if (worldFocus instanceof Base) {
      return (Base) worldFocus;
    }
    return null;
  }
  
  
  public Area offmapLocale() {
    return onWrongMap() ? worldFocusArea() : homeBase.area;
  }
  
  
  public AreaMap localMap() {
    if (localLocale == null) return null;
    return localLocale.activeMap();
  }
  
  
  public Base homeBase() {
    return homeBase;
  }
  
  
  public Target localFocus() {
    return localFocus;
  }
  
  
  public Pathing transitPoint() {
    
    if (transitTile == null) {
      AreaMap map = localMap();
      Area offmap = offmapLocale();
      if (map != null && offmap != null) {
        transitTile = findTransitPoint(map, localLocale, offmap, Type.MOVE_LAND, 1);
      }
    }
    
    if (transport != null) return transport;
    if (transitTile != null) return transitTile;
    return null;
  }
  
  
  public AreaTile transitTile() {
    transitPoint();
    return transitTile;
  }
  
  
  public boolean haveTransport() {
    return transport != null;
  }
  
  
  public boolean readyForTransit(Actor a, JOB type) {
    if (localLocale == null) return false;
    if (localMap()  == null) return true;
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
  
  
  public boolean isRecruit(Actor a) {
    return recruits.includes(a);
  }
  
  
  public boolean isEnvoy(Actor a) {
    return envoys.includes(a);
  }
  
  
  public boolean active() {
    return stage >= STAGE_BEGUN && stage < STAGE_DISBAND;
  }
  
  
  public boolean departed() {
    return stage >= STAGE_DEPARTED;
  }
  
  
  public boolean arrived() {
    return stage >= STAGE_ARRIVED;
  }
  
  
  public boolean returned() {
    return stage >= STAGE_RETURNED;
  }
  
  
  public boolean disbanded() {
    return stage >= STAGE_DISBAND;
  }
  
  
  public boolean complete() {
    return complete;
  }
  
  
  public boolean success() {
    return success;
  }
  
  
  public Base base() {
    return homeBase;
  }
  
  
  public boolean goesOffmap() {
    Area focus = worldFocusArea();
    return focus != null && focus.isOffmap();
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
  
  abstract void handleOffmapArrival  (Area goes, World.Journey journey);
  abstract void handleOffmapDeparture(Area from, World.Journey journey);
  
  
  
  /**  Priority-evaluation-
    */
  public float evalPriority() {
    return evalPriority;
  }
  
  public float evalChance() {
    return evalChance;
  }
  
  public void setEvalParams(float priority, float chance) {
    this.evalPriority = priority;
    this.evalChance   = chance;
  }
  
  public void setEvalForce(float force) {
    this.evalForce = force;
  }
  
  public float evalForce() {
    return evalForce;
  }
  
  
  
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
    return OBJECTIVE_NAMES[objective]+" -> "+focus;
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



