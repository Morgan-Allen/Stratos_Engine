

package game;
import gameUI.play.*;
import graphics.common.*;
import util.*;
import static game.Area.*;
import static game.Base.*;
import static game.ActorUtils.*;
import static game.GameConstants.*;



public abstract class Mission implements
  Session.Saveable, Journeys, TileConstants, Employer, Selection.Focus
{
  
  /**  Data fields, setup and save/load methods-
    */
  final public static int
    OBJECTIVE_STANDBY  = 0,
    OBJECTIVE_CONQUER  = 1,
    OBJECTIVE_GARRISON = 2,
    OBJECTIVE_RECON    = 3,
    OBJECTIVE_DIALOG   = 4
  ;
  
  final static String OBJECTIVE_NAMES[] = {
    "Standby", "Conquer", "Garrison", "Recon", "Dialog"
  };
  
  final public int objective;
  boolean tacticalAI = false;
  
  int     timeArrived   = -1;
  int     timeTermsSent = -1;
  boolean termsAccepted = false;
  boolean termsRefused  = false;
  boolean victorious    = false;
  boolean defeated      = false;
  boolean complete      = false;
  
  boolean isBounty = false;
  int cashReward = -1;
  
  Base.POSTURE postureDemand  = null;
  Mission      actionDemand   = null;
  Actor        marriageDemand = null;
  Tally <Good> tributeDemand  = new Tally();
  
  List <Actor> escorted   = new List();
  List <Actor> recruits   = new List();
  List <Actor> casualties = new List();
  
  private boolean  away   = false;
  private boolean  active = false;
  private Base     homeBase    ;
  private Base     awayBase    ;
  private Area     map         ;
  private AreaTile transitPoint;
  private AreaTile standPoint  ;
  private Object   focus       ;
  private int      facing      ;
  
  float exploreRange = AVG_EXPLORE_DIST;
  
  List <AreaTile> guardPoints = new List();
  int lastUpdateTime = -1;
  
  Sprite flag;
  boolean noFlag = false;
  
  
  
  public Mission(int objective, Base belongs, boolean activeAI) {
    this.objective  = objective;
    this.tacticalAI = activeAI;
    this.homeBase   = belongs;
    this.map        = belongs.activeMap();
  }
  
  
  public Mission(Session s) throws Exception {
    s.cacheInstance(this);
    
    objective     = s.loadInt ();
    timeArrived   = s.loadInt ();
    timeTermsSent = s.loadInt ();
    termsAccepted = s.loadBool();
    termsRefused  = s.loadBool();
    victorious    = s.loadBool();
    defeated      = s.loadBool();
    complete      = s.loadBool();
    
    isBounty      = s.loadBool();
    cashReward    = s.loadInt ();
    
    postureDemand  = (Base.POSTURE) s.loadEnum(Base.POSTURE.values());
    actionDemand   = (Mission     ) s.loadObject();
    marriageDemand = (Actor       ) s.loadObject();
    s.loadTally(tributeDemand);
    
    s.loadObjects(escorted  );
    s.loadObjects(recruits  );
    s.loadObjects(casualties);
    
    away         = s.loadBool();
    homeBase     = (Base   ) s.loadObject();
    awayBase     = (Base   ) s.loadObject();
    map          = (Area) s.loadObject();
    
    active = s.loadBool();
    if (s.loadBool()) {
      focus = loadTile(map, s);
    }
    else {
      focus = s.loadObject();
    }
    
    transitPoint = loadTile(map, s);
    standPoint   = loadTile(map, s);
    facing       = s.loadInt();
    
    exploreRange = s.loadFloat();
    
    for (int n = s.loadInt(); n-- > 0;) {
      AreaTile point = Area.loadTile(map, s);
      guardPoints.add(point);
    }
    lastUpdateTime = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveInt (objective    );
    s.saveInt (timeArrived  );
    s.saveInt (timeTermsSent);
    s.saveBool(termsAccepted);
    s.saveBool(termsRefused );
    s.saveBool(victorious   );
    s.saveBool(defeated     );
    s.saveBool(complete     );
    
    s.saveBool(isBounty  );
    s.saveInt (cashReward);
    
    s.saveEnum  (postureDemand );
    s.saveObject(actionDemand  );
    s.saveObject(marriageDemand);
    s.saveTally (tributeDemand );
    
    s.saveObjects(escorted  );
    s.saveObjects(recruits  );
    s.saveObjects(casualties);
    
    s.saveBool  (away    );
    s.saveObject(homeBase);
    s.saveObject(awayBase);
    s.saveObject(map     );
    
    s.saveBool(active);
    if (focus instanceof AreaTile) {
      s.saveBool(true);
      saveTile((AreaTile) focus, map, s);
    }
    else {
      s.saveBool(false);
      s.saveObject(focus);
    }
    
    saveTile(transitPoint, map, s);
    saveTile(standPoint  , map, s);
    s.saveInt(facing);
    
    s.saveFloat(exploreRange);
    
    s.saveInt(guardPoints.size());
    for (AreaTile t : guardPoints) Area.saveTile(t, map, s);
    s.saveInt(lastUpdateTime);
  }
  
  
  
  /**  Supplemental utility methods for setting objectives, demands and
    *  marching orders:
    */
  public void setAsBounty(int cashReward) {
    this.isBounty = true;
    this.cashReward = 0;
    incReward(cashReward);
  }
  
  
  public boolean incReward(int inc) {
    if (! isBounty) return false;
    
    if (inc > homeBase.funds()) return false;
    if (inc < 0 - cashReward) inc = 0 - cashReward;
    
    homeBase.incFunds(0 - inc);
    this.cashReward += inc;
    return true;
  }
  
  
  public boolean isBounty() {
    return isBounty;
  }
  
  
  public int cashReward() {
    return this.cashReward;
  }
  
  
  public void assignTerms(
    Base.POSTURE posture,
    Mission actionTaken,
    Actor toMarry,
    Tally <Good> tribute
  ) {
    this.postureDemand  = posture;
    this.actionDemand   = actionTaken;
    this.marriageDemand = toMarry;
    this.tributeDemand  = tribute == null ? new Tally() : tribute;
  }
  
  
  public boolean hasTerms() {
    boolean haveTerms = false;
    haveTerms |= marriageDemand != null;
    haveTerms |= actionDemand   != null;
    haveTerms |= postureDemand  != null;
    haveTerms |= tributeDemand  != null && ! tributeDemand.empty();
    return haveTerms;
  }
  
  
  public POSTURE      postureDemand () { return postureDemand ; }
  public Mission      actionDemand  () { return actionDemand  ; }
  public Actor        marriageDemand() { return marriageDemand; }
  public Tally <Good> tributeDemand () { return tributeDemand ; }
  
  
  public void setTermsAccepted(boolean accepted) {
    if (accepted) {
      termsAccepted = true;
      BaseEvents.imposeTerms(awayBase, homeBase, this);
      setMissionComplete(true);
    }
    else {
      termsRefused = true;
    }
  }
  
  
  public boolean termsAnswered() {
    return termsAccepted || termsRefused;
  }
  
  
  public void setExploreRange(float range) {
    this.exploreRange = range;
  }
  
  
  public float exploreRange() {
    return exploreRange;
  }
  
  
  public void setMissionComplete(boolean victorious) {
    if (victorious) this.victorious = true;
    else this.defeated = true;
    this.complete = true;
  }
  
  
  public boolean complete() {
    return complete;
  }
  
  
  public void toggleRecruit(Actor a, boolean is) {
    this.recruits.toggleMember(a, is);
    a.setMission(is ? this : null);
  }
  
  
  public void toggleEscorted(Actor a, boolean is) {
    this.escorted.toggleMember(a, is);
    a.setMission(is ? this : null);
  }
  
  
  public Series <Actor> recruits() {
    return recruits;
  }
  
  
  public Series <Actor> escorted() {
    return escorted;
  }
  
  
  public void setFocus(Base city) {
    
    homeBase.missions.toggleMember(this, true);
    this.active = true;
    
    Base from, goes;
    if (city != homeBase) {
      this.awayBase = city;
      from = homeBase;
      goes = city;
    }
    else {
      from = awayBase;
      goes = homeBase;
    }
    
    if (onMap()) {
      this.transitPoint = findTransitPoint(map, from, goes);
      setFocus(city, 0, map);
    }
    else {
      this.focus = city;
      beginJourney(from, goes);
    }
  }
  
  
  public void setFocus(Object focus, int facing, Area map) {
    homeBase.missions.toggleMember(this, true);
    
    this.facing = facing;
    this.focus  = focus ;
    this.active = true  ;
    this.map    = map   ;
    
    this.standPoint = standPointFrom(focus);
  }
  
  
  public void disbandFormation() {
    
    if (isBounty && ! recruits.empty()) {
      int split = this.cashReward / recruits.size();
      int rem   = this.cashReward % recruits.size();
      int index = 0;
      for (Actor r : recruits) {
        r.incCarried(CASH, split + (index++ < rem ? 1 : 0));
      }
    }
    
    homeBase.missions.toggleMember(this, false);
    
    this.awayBase = null  ;
    this.focus    = null  ;
    this.facing   = CENTRE;
    this.active   = false ;
    
    for (Actor r : recruits) toggleRecruit (r, false);
    for (Actor e : escorted) toggleEscorted(e, false);
  }
  
  
  public boolean active() {
    return active;
  }
  
  
  
  /**  Pathing and stand-point related methods-
    */
  AreaTile standPointFrom(Object focus) {
    
    //  TODO:  Get the nearest open tile, just for good measure?
    
    if (focus instanceof Base) {
      return this.transitPoint;
    }
    if (focus instanceof Mission) {
      Pathing from = ((Mission) focus).pathFrom();
      return standPointFrom(from);
    }
    if (focus instanceof Element) {
      return ((Element) focus).centre();
    }
    if (focus instanceof Target) {
      return ((Target) focus).at();
    }
    return null;
  }
  
  
  Area map() {
    return map;
  }
  
  
  boolean onAwayMap() {
    return awayBase != null && map == awayBase.activeMap();
  }
  
  
  int facing() {
    return facing;
  }
  
  
  AreaTile standPoint() {
    return standPoint;
  }
  
  
  Pathing pathFrom() {
    //  TODO:  Use the position of the member closest to average?
    Actor a = recruits.atIndex(recruits.size() / 2);
    return Task.pathOrigin(a);
  }
  
  
  Target pathGoes() {
    if (focus instanceof Base) {
      return transitPoint;
    }
    if (focus instanceof Mission) {
      return ((Mission) focus).pathFrom();
    }
    if (focus instanceof Target) {
      return (Target) focus;
    }
    return null;
  }
  
  
  public Object focus() {
    return focus;
  }
  
  
  public boolean onMap() {
    return map != null;
  }
  
  
  public boolean onMap(Area map) {
    return this.map != null && this.map == map;
  }
  
  
  public boolean away() {
    return away;
  }
  
  
  public Base awayCity() {
    return awayBase;
  }
  
  
  
  /**  Regular updates and journey-related methods:
    */
  void update() {
    //
    //  Cull any casualties...
    for (Actor a : recruits) if (a.dead()) {
      recruits.remove(a);
      casualties.add(a);
    }
    for (Actor a : escorted) if (a.dead()) {
      escorted.remove(a);
      casualties.add(a);
    }
    //
    //  Bounties just stay open until their objective is completed, as do non-
    //  autopilot missions.
    if (isBounty || ! tacticalAI) {
      if (active && objectiveComplete()) {
        setMissionComplete(true);
        disbandFormation();
        return;
      }
    }
    //
    //  Other formations auto-disband if all applicants are dead or un-toggled.
    else {
      if (active && recruits.empty() && escorted.empty()) {
        disbandFormation();
        return;
      }
    }
    //
    //  Check to see if an offer has expired-
    Base local   = away ? awayBase : homeBase;
    Base distant = away ? homeBase : awayBase;
    int sinceArrive = Area.timeSince(timeArrived  , homeBase.world.time);
    int sinceTerms  = Area.timeSince(timeTermsSent, homeBase.world.time);
    boolean hasEnvoy = escorted.size() > 0;
    
    if (hasEnvoy && (
      sinceTerms > DAY_LENGTH ||
      (timeTermsSent == -1 && sinceArrive > DAY_LENGTH * 2)
    )) {
      timeTermsSent = homeBase.world.time;
      termsRefused  = true;
    }
    //
    //  Update your target and stand-point while you're on the map...
    if (map != null) {
      if (tacticalAI) {
        MissionUtils.updateTacticalTarget(this);
      }
      this.standPoint = standPointFrom(focus);
      //
      //  This is a hacky way of saying "I want to go to a different city, is
      //  everyone ready yet?"
      boolean ready = formationReady();
      if (focus == distant && distant.activeMap() != map && ready) {
        beginJourney(local, distant);
      }
    }
    //
    //  And see if it's time to leave otherwise-
    else if (focus == awayBase) {
      boolean shouldLeave = false;
      shouldLeave |= termsAccepted;
      shouldLeave |= termsRefused && objective == OBJECTIVE_DIALOG;
      shouldLeave |= complete;
      if (shouldLeave) {
        setFocus(homeBase);
      }
    }
  }
  
  
  private World.Journey beginJourney(Base from, Base goes) {
    if (reports()) I.say("\nREADY TO BEGIN FOREIGN MISSION!");
    for (Actor r : recruits) if (r.onMap(map)) {
      r.exitMap(map);
    }
    for (Actor e : escorted) if (e.onMap(map)) {
      e.exitMap(map);
      e.assignGuestBase(null);
    }
    away         = true;
    map          = null;
    transitPoint = null;
    return goes.world.beginJourney(from, goes, this);
  }
  
  
  public void onArrival(Base goes, World.Journey journey) {
    //
    //  There are 4 cases here: arriving home or away, and arriving on a map or
    //  not.
    boolean home  = goes == homeBase;
    boolean onMap = goes.activeMap() != null;
    //
    //  In the event that a formation arrives on the map, either it's a foreign
    //  invader threatening the city, or one of the player's legions returning
    //  home.
    if (onMap) {
      if (reports()) I.say("\nARRIVED ON MAP: "+goes+" FROM "+journey.from);
      
      AreaTile transits = findTransitPoint(goes.activeMap(), goes, journey.from);
      this.map          = goes.activeMap();
      this.transitPoint = transits;
      
      for (Actor r : recruits) {
        r.enterMap(map, transits.x, transits.y, 1, r.base());
      }
      for (Actor e : escorted) {
        e.enterMap(map, transits.x, transits.y, 1, e.base());
        e.assignGuestBase(goes);
      }
      setFocus(transitPoint, N, map);
      
      if (home) {
        this.away = false;
        disbandFormation();
      }
      else {
        this.away = true;
        this.timeArrived = map.time;
        if (tacticalAI) MissionUtils.updateTacticalTarget(this);
      }
    }
    //
    //  In the event that this happens off-map, either an army has returned
    //  home to a foreign city, or an army has assaulted a foreign city:
    else {
      guardPoints.clear();
      lastUpdateTime = -1;
      
      if (home) {
        if (reports()) I.say("\nARRIVED HOME: "+goes+" FROM "+journey.from);
        this.away = false;
        BaseEvents.handleReturn(this, journey.from, journey);
      }
      else {
        if (reports()) I.say("\nARRIVED AT: "+goes+" FROM "+journey.from);
        this.away = true;
        handleArrival(goes, journey);
      }
    }
  }
  
  
  
  /**  Delegate functions for subclasses to flesh out-
    */
  public abstract boolean allowsFocus(Object newFocus);
  
  abstract boolean objectiveComplete();
  
  abstract void handleArrival(Base goes, World.Journey journey);
  
  
  
  /**  Organising actors and other basic query methods-
    */
  public void actorTargets(Actor actor, Target other) {
    return;
  }
  
  
  public void actorUpdates(Actor actor) {
    return;
  }
  
  
  public void actorPasses(Actor actor, Building other) {
    return;
  }
  
  
  public void actorVisits(Actor actor, Building visits) {
    return;
  }
  
  
  public Base base() {
    return homeBase;
  }
  
  
  void dispatchTerms(Base goes) {
    goes.council.receiveTerms(this);
    timeTermsSent = goes.world.time;
  }
  
  
  public AreaTile standLocation(Actor actor) {
    boolean doPatrol =
      (focus instanceof Target) &&
      ((Target) focus).type().isWall
    ;
    if (doPatrol) {
      return MissionUtils.standingPointPatrol(actor, this);
    }
    else {
      return MissionUtils.standingPointRanks(actor, this);
    }
  }
  
  
  public boolean formationReady() {
    if (map == null) return true;
    if (focus == null || ! active) return false;
    
    for (Actor a : recruits) {
      if (standLocation(a) != a.at()) {
        return false;
      }
    }
    for (Actor a : escorted) {
      if (standLocation(a) != a.at()) {
        return false;
      }
    }
    return true;
  }
  
  
  public int powerSum() {
    return powerSum(recruits, null);
  }
  
  
  public float casualtyLevel() {
    float live = recruits.size(), dead = casualties.size();
    return dead / (dead + live);
  }
  
  
  static int powerSum(Series <Actor> recruits, Area mapOnly) {
    float sumStats = 0;
    for (Actor a : recruits) {
      if (mapOnly != null && a.map != mapOnly) continue;
      sumStats += TaskCombat.attackPower(a);
    }
    return (int) (sumStats * POP_PER_CITIZEN);
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  boolean reports() {
    return false;
  }
  
  
  public String toString() {
    return "Mission ("+homeBase+")";
  }
  
  
  public String fullName() {
    return "Mission: "+OBJECTIVE_NAMES[objective]+": "+focus;
  }
  
  
  public void whenClicked(Object context) {
    PlayUI.pushSelection(this);
  }
  
  
  public boolean canRender(Base base, Viewport view) {
    if (noFlag) {
      return false;
    }
    if (focus instanceof Element) {
      return ((Element) focus).canRender(base, view);
    }
    if (focus instanceof AreaTile) {
      return true;
    }
    else return false;
  }
  
  
  public void renderFlag(Rendering rendering) {
    if (flag == null) {
      String key = "";
      if (objective == OBJECTIVE_CONQUER ) key = World.KEY_ATTACK_FLAG ;
      if (objective == OBJECTIVE_GARRISON) key = World.KEY_DEFEND_FLAG ;
      if (objective == OBJECTIVE_RECON   ) key = World.KEY_EXPLORE_FLAG;
      if (objective == OBJECTIVE_DIALOG  ) key = World.KEY_CONTACT_FLAG;
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
    if (focus instanceof Element) {
      Element e = (Element) focus;
      flag.position.setTo(e.trackPosition());
      flag.position.z += e.type().deep;
    }
    if (focus instanceof AreaTile) {
      AreaTile t = (AreaTile) focus;
      flag.position.setTo(t.trackPosition());
      flag.position.z += 1;
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
    if (focus instanceof Selection.Focus) {
      return true;
    }
    else {
      return false;
    }
  }
  
  
  public Vec3D trackPosition() {
    if (focus instanceof Selection.Focus) {
      return ((Selection.Focus) focus).trackPosition();
    }
    else {
      return new Vec3D();
    }
  }
  
}





