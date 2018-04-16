

package game;
import static game.GameConstants.*;

import game.World.Journey;
import util.*;



public class ActorAsVessel extends Actor implements Trader, Employer, Pathing {
  
  
  /**  Data fields, construction and save/load methods-
    */
  Tally <Good> prodLevel = new Tally();
  Tally <Good> needLevel = new Tally();
  
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
    
    pilot = (Actor) s.loadObject();
    s.loadObjects(crew);
    s.loadObjects(inside);
    
    flying    = s.loadBool();
    landed    = s.loadBool();
    flyHeight = s.loadFloat();
    landsAt   = Area.loadTile(map, s);
    entrance  = Area.loadTile(map, s);
    boundTo   = (BuildingForDock) s.loadObject();
    dockedAt  = (BuildingForDock) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveTally(prodLevel);
    s.saveTally(needLevel);
    
    s.saveObject(pilot);
    s.saveObjects(crew);
    s.saveObjects(inside);
    
    s.saveBool(flying);
    s.saveBool(landed);
    s.saveFloat(flyHeight);
    Area.saveTile(landsAt , map, s);
    Area.saveTile(entrance, map, s);
    s.saveObject(boundTo );
    s.saveObject(dockedAt);
  }
  
  
  
  /**  Regular updates and behaviour methods-
    */
  void beginNextBehaviour() {
    assignTask(null);
    
    if (idle() && work() != null && ((Element) work()).complete()) {
      assignTask(work().selectActorBehaviour(this));
    }
    if (idle()) {
      assignTask(TaskResting.configResting(this, home()));
    }
  }
  
  
  /*
  public void assignTask(Task task) {
    if (type().isAirship() && task == null) {
      I.say("?");
      Task old = task();
      old.checkAndUpdateTask();
    }
    super.assignTask(task);
  }
  //*/
  
  
  
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
          Area map = map();
          Base partner = trading.tradeGoes.base();
          
          /*
          return TaskDelivery.pickNextDelivery(
            actor, this, this, MAX_TRADER_RANGE, 1, map().world.goodTypes()
          );
          //*/
          return BuildingForTrade.selectTraderBehaviour(this, actor, partner, map);
        }
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
  
  
  
  /**  Implementing Pathing interface-
    */
  public Pathing[] adjacent(Pathing[] temp, Area map) {
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
  public AreaTile findLandingPoint(Area map, Base visits, Task task) {
    
    //  TODO:  Use the cargo-profile for the trading-task to rate the viability
    //  of different landing-sites.
    
    //
    //  First, see where we're supposed to be getting close to:
    Target from = visits.headquarters();
    
    if (task instanceof TaskTrading) {
      TaskTrading trading = (TaskTrading) task;
      if (trading.tradeGoes instanceof Target) {
        from = (Target) trading.tradeGoes;
      }
    }
    
    if (from == null) {
      from = map.tileAt(map.size() / 2, map.size() / 2);
    }
    //
    //  Then see if you can find a convenient building to dock at-
    Pick <AreaTile> pick = new Pick();
    for (Building b : map.buildings()) {
      
      if (b.base() != visits) continue;
      if (b.type().category != Type.IS_DOCK_BLD) continue;
      
      BuildingForDock dock = (BuildingForDock) b;
      AreaTile docks = dock.nextFreeDockPoint();
      if (docks == null) continue;
      
      float rating = Area.distancePenalty(from, b);
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
  
  
  public void onArrival(Base goes, Journey journey) {
    
    if (type().isAirship()) {
      //  TODO:  Create a separate Task to allow ships to dock and land,
      //  distinct from the methods in TaskTrading, and assign it here!
      this.landsAt = findLandingPoint(goes.activeMap(), goes, task());
    }
    
    super.onArrival(goes, journey);
    
    if (type().isAirship()) {
      I.say("ENTERED MAP AT TIME: "+map.time()+", AT: "+at());
    }
    
    if (goes.activeMap() != null) {
      AreaTile at = at();
      for (Actor a : inside) {
        a.enterMap(map, at.x, at.y, 1, a.base());
      }
      if (type().moveMode == Type.MOVE_AIR) this.flying = true;
    }
    else {
      //  TODO:  Add these to the roster of off-map visitors...
      for (Actor a : inside) {
        continue;
      }
    }
  }
  
  
  public void setLocation(AreaTile at, Area map) {
    
    if (type().isAirship() && onMap() && at != at()) {
      I.say("SETTING LOCATION: "+at+", TIME: "+map.time());
    }
    
    super.setLocation(at, map);
  }
  
  
  void update() {
    super.update();
    
    Vec3D pos = exactPosition(null);
    for (Actor a : inside) {
      a.setExactLocation(pos, map, false);
    }
  }
  
  
  public void doLanding(AreaTile landing) {
    
    if (landing != this.at()) {
      I.say("\nWARNING: "+this+" LANDING AT INCORRECT LOCATION: "+at());
      I.say("  Should be: "+landing);
      setExactLocation(landing.exactPosition(null), map, false);
    }
    
    if (type().isAirship()) {
      I.say("LANDING AT TIME: "+map.time()+", AT: "+landing);
    }
    
    //
    //  Dock with your location-
    if (landing.above != null && landing.above.type().isDockBuilding()) {
      dockedAt = (BuildingForDock) landing.above;
      dockedAt.toggleDocking(this, landing, true);
      entrance = null;
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
    for (Actor a : crew) if (a.inside() != this) return false;
    return true;
  }
  
  
  public void doTakeoff(AreaTile landing) {
    //
    //  Eject any unathorised visitors...
    for (Actor actor : inside) {
      if (actor == pilot || crew.includes(actor)) continue;
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
    this.dockedAt = null;
    this.landed = false;
    this.flying = true;
  }
  
  
  public void onDeparture(Base goes, World.Journey journey) {
    super.onDeparture(goes, journey);
    
    for (Actor a : inside) {
      if (a.onMap()) {
        a.exitMap(map);
      }
      //  TODO:  Remove these from the roster of off-map visitors...
      else {
        continue;
      }
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
}









