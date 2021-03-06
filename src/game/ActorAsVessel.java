

package game;
import static game.GameConstants.*;
import static game.World.*;
import graphics.common.*;
import util.*;



public class ActorAsVessel extends Actor implements Trader, Employer, Pathing {
  
  static int
    MAX_HEIGHT    = 3,
    DOOR_HEIGHT   = 1,
    DESCENT_RANGE = 4,
    DESCENT_TIME  = 5
  ;
  
  
  /**  Data fields, construction and save/load methods-
    */
  Tally <Good> prodLevel = new Tally();
  Tally <Good> needLevel = new Tally();
  
  private int lastUpdateTime = -1;
  Actor pilot = null;
  List <Actor> crew   = new List();
  List <Actor> inside = new List();
  
  boolean flying = false;
  boolean landed = false;
  float flyHeight = 0;
  AreaTile landsAt, entrance;
  BuildingForDock boundTo, dockedAt;
  
  
  
  public ActorAsVessel(ActorType type) {
    super(type);
  }
  
  
  public ActorAsVessel(Session s) throws Exception {
    super(s);
    
    s.loadTally(prodLevel);
    s.loadTally(needLevel);
    
    lastUpdateTime = s.loadInt();
    pilot = (Actor) s.loadObject();
    s.loadObjects(crew);
    s.loadObjects(inside);
    
    flying    = s.loadBool();
    landed    = s.loadBool();
    flyHeight = s.loadFloat();
    landsAt   = AreaMap.loadTile(map, s);
    entrance  = AreaMap.loadTile(map, s);
    boundTo   = (BuildingForDock) s.loadObject();
    dockedAt  = (BuildingForDock) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveTally(prodLevel);
    s.saveTally(needLevel);
    
    s.saveInt(lastUpdateTime);
    s.saveObject(pilot);
    s.saveObjects(crew);
    s.saveObjects(inside);
    
    s.saveBool(flying);
    s.saveBool(landed);
    s.saveFloat(flyHeight);
    AreaMap.saveTile(landsAt , map, s);
    AreaMap.saveTile(entrance, map, s);
    s.saveObject(boundTo );
    s.saveObject(dockedAt);
  }
  
  
  
  /**  Regular updates and behaviour methods-
    */
  void beginNextBehaviour() {
    assignTask(null, this);
    
    Mission mission = mission();
    Employer work = work();
    
    if (idle() && mission != null && mission.active()) {
      assignTask(mission.selectActorBehaviour(this), this);
    }
    
    if (idle() && work != null && ((Element) work).complete()) {
      assignTask(work.selectActorBehaviour(this), this);
    }
    
    if (idle()) {
      assignTask(TaskResting.nextResting(this, home()), this);
    }
  }
  
  
  
  /**  Implementing the Employer interface-
    */
  public void setWorker(Actor actor, boolean is) {
    crew.toggleMember(actor, is);
    actor.setWork(is ? this : null);
  }
  
  
  public void setPilot(Actor actor, boolean is) {
    this.pilot = is ? actor : null;
    setWorker(actor, is);
  }
  
  
  public Actor pilot() {
    return pilot;
  }
  
  
  public Series <Actor> crew() {
    return crew;
  }
  
  
  public Task selectActorBehaviour(Actor actor) {
    
    if (landed) {
      if (task() instanceof TaskTrading) {
        TaskTrading trading = (TaskTrading) task();
        boolean shouldGo = trading.shouldTakeoff(map());
        
        if (shouldGo) {
          return TaskRetreat.configRetreat(actor, this, Task.PARAMOUNT);
        }
        else {
          Base partner = trading.tradeGoes.base();
          AreaMap map = map();
          /*
          return TaskDelivery.pickNextDelivery(
            actor, this, this, MAX_TRADER_RANGE, 1, map().world.goodTypes()
          );
          //*/
          return BuildingForTrade.selectTraderBehaviour(
            this, actor, partner, false, map
          );
        }
      }
      if (task() instanceof TaskTransport) {
        return TaskWaiting.configWaiting(
          actor, this, TaskWaiting.TYPE_OVERSIGHT, this
        );
      }
    }
    
    return null;
  }
  
  
  public void actorUpdates(Actor actor) {
    return;
  }
  
  
  public void actorTargets(Actor actor, Target other) {
    return;
  }
  
  
  public void actorVisits(Actor actor, Pathing visits) {
    return;
  }
  
  
  void updateOffMap(Area locale) {
    super.updateOffMap(locale);
    updateWorkers(base().world.time());
  }


  void updateWorkers(int time) {
    
    if (type().crewTypes.empty()) return;
    if (time - lastUpdateTime < AVG_UPDATE_GAP) return;
    lastUpdateTime = time;
    
    for (ActorType w : type().crewTypes.keys()) {
      int maxWorkers = (int) type().crewTypes.valueFor(w);
      int numWorkers = 0;
      for (Actor a : crew) if (a.type() == w) numWorkers += 1;
      
      if (numWorkers < maxWorkers && w.socialClass == CLASS_COMMON) {
        ActorUtils.generateMigrant(w, this, false);
      }
    }
  }
  
  
  
  /**  Implementing Pathing interface-
    */
  public Pathing[] adjacent(Pathing[] temp, AreaMap map) {
    if (dockedAt != null) {
      return new Pathing[] { dockedAt };
    }
    else {
      return new Pathing[] { entrance };
    }
  }
  
  
  public BuildingForDock dockedAt() {
    return dockedAt;
  }
  
  
  public AreaTile[] entrances() {
    if (entrance != null) return new AreaTile[] { entrance };
    else return new AreaTile[0];
  }
  
  
  public AreaTile mainEntrance() {
    if (dockedAt != null) return null;
    return entrance;
  }
  
  
  public boolean allowsEntryFrom(Pathing p) {
    if (dockedAt != null) return p == dockedAt;
    else return p == entrance;
  }
  
  
  public boolean allowsEntry(Actor a) {
    return landed;
  }
  
  
  public boolean allowsExit(Actor a) {
    return landed;
  }
  
  
  public void setInside(Actor a, boolean is) {
    inside.toggleMember(a, is);
  }
  
  
  public Series <Actor> allInside() {
    return inside;
  }
  
  
  
  /**  General on-map life-cycle methods:
    */
  public AreaTile findLandingPoint(AreaMap map, Task task) {
    
    //  TODO:  Use the cargo-profile for the trading-task to rate the viability
    //  of different landing-sites?
    
    //
    //  First, see where we're supposed to be getting close to:
    Target from = null;
    Base visits = null;
    
    if (task instanceof TaskTrading) {
      TaskTrading trading = (TaskTrading) task;
      if (trading.tradeGoes instanceof Building) {
        from = (Building) trading.tradeGoes;
        visits = ((Building) trading.tradeGoes).base();
      }
    }
    
    if (task instanceof TaskTransport) {
      TaskTransport transport = (TaskTransport) task;
      if (transport.mission.localFocus() != null) {
        from = transport.mission.localFocus();
        visits = transport.mission.worldFocusBase();
      }
    }
    
    if (from == null) {
      from = map.tileAt(map.size() / 2, map.size() / 2);
    }
    //
    //  Then see if you can find a convenient building to dock at-
    Pick <AreaTile> pick = new Pick();
    for (Building b : map.buildings()) {
      
      if (visits != null && b.base() != visits) continue;
      if (b.type().category != Type.IS_DOCK_BLD) continue;
      
      BuildingForDock dock = (BuildingForDock) b;
      AreaTile docks = dock.nextFreeDockPoint();
      if (docks == null) continue;
      
      float rating = AreaMap.distancePenalty(from, b);
      pick.compare(docks, rating);
    }
    //
    //  And failing that, find an empty nearby space to park-
    if (pick.empty()) {
      int maxRange = MAX_TRADER_RANGE / 2;
      AreaTile free = ActorUtils.findEntryPoint(this, map, from, maxRange);
      return free;
    }
    else {
      return pick.result();
    }
  }
  
  
  public void setLandPoint(AreaTile at) {
    this.landsAt = at;
  }
  
  
  public void onArrival(Area goes, Journey journey) {
    //
    //  PLEASE NOTE:  The super.onArrival call may itself trigger a new journey
    //  when visiting off-map bases, and most of the on-map calls require that
    //  a landing/entry point has been found, so the order of operations here
    //  is somewhat delicate.  Tinker with care.
    boolean onMap = goes.activeMap() != null;

    if (! onMap) {
      for (Actor a : allInside()) {
        a.setInside(this, false);
        goes.toggleVisitor(a, true);
      }
      if (base().area == goes) {
        transferCash(false, base());
      }
    }
    
    else if (type().isAirship()) {
      setLandPoint(findLandingPoint(goes.activeMap(), task()));
      this.flying    = true;
      this.flyHeight = MAX_HEIGHT;
    }
    
    super.onArrival(goes, journey);
    
    if (onMap) {
      AreaTile at = at();
      for (Actor a : inside) {
        a.enterMap(map, at.x, at.y, 1, a.base());
      }
    }
  }
  
  
  public void onDeparture(Area from, World.Journey journey) {
    //
    //  If you're departing from a foreign base, scoop up your crew and any
    //  passengers or mission teammates-
    Area offmap = offmap();
    Mission mission = mission();
    if (offmap != null && ! onMap()) {
      for (Actor a : crew) if (a.offmap() == offmap) {
        a.setInside(this, true);
      }
      if (mission != null) for (Actor a : mission.recruits()) {
        if (a != this) a.setInside(this, true);
      }
      for (Actor a : allInside()) if (a.offmap() == offmap) {
        offmap.toggleVisitor(a, false);
      }
    }
    super.onDeparture(from, journey);
    //
    //  If you're departing from an active map, ensure that any passengers
    //  exit as well.  NOTE:  Actors exiting the map remove themselves from the
    //  vehicle, so we need to stick them back in.  (TODO:  This is a bit of a
    //  hack and may need a cleaner solution.)
    Batch <Actor> going = new Batch();
    Visit.appendTo(going, inside);
    
    for (Actor a : going) {
      if (a.onMap()) a.exitMap(map);
      a.setInside(this, true);
    }
  }
  
  
  void update() {
    super.update();
    
    if (onMap()) {
      updateWorkers(map().time());
      updateFlight();
      
      Vec3D pos = exactPosition(null);
      for (Actor a : inside) {
        a.setExactLocation(pos, map, false);
      }
      
      if (type().isAirship() && task() instanceof TaskTrading) {
        TaskTrading trading = (TaskTrading) task();
        needLevel.clear();
        needLevel.add(trading.taken);
      }
    }
  }
  
  
  public void doLanding(AreaTile landing) {
    
    if (landing != this.at()) {
      I.say("\nWARNING: "+this+" LANDING AT INCORRECT LOCATION: "+at());
      I.say("  Should be: "+landing);
      setExactLocation(landing.exactPosition(null), map, false);
    }
    
    //
    //  Dock with your location-
    if (landing.above != null && landing.above.type().isDockBuilding()) {
      dockedAt = (BuildingForDock) landing.above;
      dockedAt.toggleDocking(this, landing, true);
      entrance = null;
      transferCash(true, dockedAt.base());
    }
    else {
      imposeFootprint();
      AreaTile at = at();
      Type t = type();
      int coords[] = Building.entranceCoords(t.wide, t.high, Building.FACE_EAST);
      entrance = map.tileAt(at.x + coords[0], at.y + coords[1]);
    }
    
    this.flying = false;
    this.landed = true;
    this.landsAt = landing;
  }
  
  
  public boolean readyForTakeoff() {
    boolean hasCrew = false;
    for (Actor a : crew) {
      if (a.inside() != this) return false;
      else hasCrew = true;
    }
    return hasCrew;
  }
  
  
  public void doTakeoff(AreaTile landing) {
    //
    //  Eject any unathorised visitors...
    for (Actor actor : inside) {
      if (actor == pilot || crew.includes(actor)) continue;
      if (actor.mission() == mission()) continue;
      actor.setInside(this, false);
      actor.setLocation(entrance, map);
    }
    //
    //  And undock from your location-
    if (dockedAt != null) {
      dockedAt.toggleDocking(this, landing, false);
    }
    else {
      removeFootprint();
      entrance = null;
    }
    this.landsAt  = null;
    this.dockedAt = null;
    this.landed   = false;
    this.flying   = true;
  }
  
  
  void transferCash(boolean onMap, Base visits) {
    if (onMap && dockedAt != null && dockedAt == work()) {
      float cash = outfit.carried(CASH);
      outfit.setCarried(CASH, 0);
      dockedAt.addInventory(cash, CASH);
    }
    if (visits == base() && ! onMap) {
      float cash = outfit.carried(CASH);
      outfit.setCarried(CASH, 0);
      visits.incFunds((int) cash);
    }
  }
  


  /**  Other methods related to docking, pathing and landing-
    */
  void setBound(BuildingForDock boundTo) {
    this.boundTo = boundTo;
  }
  
  
  boolean checkPathingEscape() {
    //
    //  When you're landed, you don't collide with yourself, and when you're
    //  flying, you don't collide with ground-buildings.
    //  TODO:  Use a more general-purpose 'checkCollision' function here
    return false;
  }
  
  
  public boolean mobile() {
    if (landed) return false;
    return super.mobile();
  }
  
  
  public AreaTile landsAt() {
    return landsAt;
  }
  
  
  public boolean landed() {
    return landed;
  }
  
  
  
  /**  Implementing Trader interface-
    */
  public Tally <Good> needLevels() {
    return needLevel;
  }
  
  public Tally <Good> prodLevels() {
    return prodLevel;
  }
  
  public float importPrice(Good g, Base sells) {
    return base().importPrice(g, sells);
  }
  
  public float exportPrice(Good g, Base buys) {
    return base().exportPrice(g, buys);
  }
  
  public boolean allowExport(Good g, Trader buys) {
    return true;
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  void updateFlight() {
    if (! type().isAirship()) return;
    
    float landsDist = AreaMap.distance(this, landsAt);
    float targetHeight = MAX_HEIGHT;
    if (jobFocus() == landsAt && landsDist < DESCENT_RANGE) {
      targetHeight = 0;
    }
    
    float descTime  = DESCENT_TIME * 1f / moveSpeed();
    float maxChange = MAX_HEIGHT * 1f / (map.ticksPS * descTime);
    float diff = targetHeight - flyHeight;
    diff = Nums.clamp(diff, 0 - maxChange, maxChange);
    
    flyHeight = Nums.clamp(flyHeight + diff, 0, MAX_HEIGHT);
    
    Vec3D pos = this.exactPosition(null);
    this.setExactLocation(pos, map, false);
  }
  
  
  protected float moveHeight() {
    return flyHeight;
  }
  
  
  protected void updateSprite(
    Sprite s, String animName, Vec2D angleVec, float animProg, boolean loop
  ) {
    if (type().isAirship()) {
      
      angleVec.scale(-1);
      if (angleVec.length() > 0) s.rotation = angleVec.toAngle();
      
      float prog = 1f - (flyHeight / DOOR_HEIGHT);
      prog = Nums.clamp(prog, 0, 1);
      s.setAnimation("descend", prog, loop);
    }
    else {
      super.updateSprite(s, animName, angleVec, animProg, loop);
    }
  }
  
  
}







