

package game;
import static game.GameConstants.*;
import util.*;



public class ActorAsVessel extends Actor implements Trader, Employer, Pathing {
  
  
  /**  Data fields, construction and save/load methods-
    */
  Tally <Good> prodLevel = new Tally();
  Tally <Good> needLevel = new Tally();
  
  Actor pilot = null;
  List <Actor> passengers = new List();
  List <Actor> inside     = new List();
  
  boolean flying = false;
  boolean landed = false;
  float flyHeight = 0;
  BuildingForDock boundTo, dockedAt;
  AreaTile entrance;
  
  
  
  public ActorAsVessel(ActorType type) {
    super(type);
  }
  
  
  public ActorAsVessel(Session s) throws Exception {
    super(s);
    
    s.loadTally(prodLevel);
    s.loadTally(needLevel);
    
    /*
    proxy.near = Area.loadTarget(map, s);
    s.loadTally(proxy.nearDemand);
    s.loadTally(proxy.nearSupply);
    s.loadTally(proxy.inventory );
    //*/
    
    pilot = (Actor) s.loadObject();
    s.loadObjects(passengers);
    s.loadObjects(inside);
    
    flying    = s.loadBool();
    landed    = s.loadBool();
    flyHeight = s.loadFloat();
    boundTo  = (BuildingForDock) s.loadObject();
    dockedAt = (BuildingForDock) s.loadObject();
    entrance = Area.loadTile(map, s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveTally(prodLevel);
    s.saveTally(needLevel);
    
    /*
    Area.saveTarget(proxy.near, map, s);
    s.saveTally(proxy.nearDemand);
    s.saveTally(proxy.nearSupply);
    s.saveTally(proxy.inventory );
    //*/
    
    s.saveObject(pilot);
    s.saveObjects(passengers);
    s.saveObjects(inside);
    
    s.saveBool  (flying  );
    s.saveBool  (landed  );
    s.saveFloat(flyHeight);
    s.saveObject(boundTo );
    s.saveObject(dockedAt);
    Area.saveTile(entrance, map, s);
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
  
  
  
  /**  Implementing the Employer interface-
    */
  public void setWorker(Actor actor, boolean is) {
    passengers.toggleMember(actor, is);
    actor.setWork(is ? this : null);
  }
  
  
  public Task selectActorBehaviour(Actor actor) {
    if (landed) {
      return TaskDelivery.pickNextDelivery(
        actor, this, this, MAX_TRADER_RANGE, 1, map().world.goodTypes()
      );
    }
    else {
      return null;
    }
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
      AreaTile at = at();
      Type t = type();
      int coords[] = Building.entranceCoords(t.wide, t.high, Building.FACE_EAST);
      return new AreaTile[] { map.tileAt(at.x + coords[0], at.y + coords[1]) };
    }
  }
  
  
  public boolean allowsEntryFrom(Pathing p) {
    return p == dockedAt || p == entrance;
  }
  
  
  public boolean allowsEntry(Actor a) {
    return true;
  }
  
  
  public void setInside(Actor a, boolean is) {
    inside.toggleMember(a, is);
  }
  
  
  public Series <Actor> allInside() {
    return inside;
  }
  
  
  public AreaTile mainEntrance() {
    return entrance;
  }
  
  
  
  /**  Methods related to docking and landing-
    */
  public AreaTile findLandingPoint(Area map) {
    return findLandingPoint(map, null);
  }
  
  
  public AreaTile findLandingPoint(Area map, TaskTrading task) {
    //
    //  First, see where we're supposed to be getting close to:
    
    //  TODO:  Use the cargo-profile for the trading-task to rate the viability
    //  of different landing-sites...
    
    Target from = task.tradeGoes.base().headquarters();
    if (task != null && task.tradeGoes instanceof Building) {
      from = (Building) task.tradeGoes;
    }
    //
    //  Then see if you can find a convenient building to dock at-
    Pick <AreaTile> pick = new Pick();
    for (Building b : map.buildings()) {
      
      if (b.base() != task.tradeGoes.base()) continue;
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
  
  
  void setBound(BuildingForDock boundTo) {
    this.boundTo = boundTo;
  }
  
  
  public void enterMap(Area map, int x, int y, float buildLevel, Base owns) {
    super.enterMap(map, x, y, buildLevel, owns);
    if (type().moveMode == Type.MOVE_AIR) this.flying = true;
  }
  
  
  void doLanding(AreaTile landing) {
    //
    //  Dock with your location-
    if (landing.above != null && landing.above.type().isDockBuilding()) {
      dockedAt = (BuildingForDock) landing.above;
      dockedAt.toggleDocking(this, landing, true);
    }
    else {
      imposeFootprint();
    }
    this.flying = false;
    this.landed = true;
  }
  
  
  void doTakeoff(AreaTile landing) {
    //
    //  Eject any unathorised visitors...
    for (Actor actor : inside) {
      if (actor == pilot || passengers.includes(actor)) continue;
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
    }
    this.dockedAt = null;
    this.landed = false;
    this.flying = true;
  }
  
  
  boolean landed() {
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









