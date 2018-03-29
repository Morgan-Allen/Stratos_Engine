

package game;
import static game.GameConstants.*;
import util.*;



public class ActorAsVessel extends Actor implements Trader, Employer, Pathing {
  
  
  Actor pilot = null;
  List <Actor> passengers = new List();
  
  boolean flying = false;
  boolean landed = false;
  float flyHeight = 0;
  
  BuildingForDock dockedAt;
  AreaTile entrance;
  
  
  
  public ActorAsVessel(ActorType type) {
    super(type);
  }
  
  
  public ActorAsVessel(Session s) throws Exception {
    super(s);
    pilot = (Actor) s.loadObject();
    s.loadObjects(passengers);
    flying    = s.loadBool();
    landed    = s.loadBool();
    flyHeight = s.loadFloat();
    dockedAt = (BuildingForDock) s.loadObject();
    entrance = Area.loadTile(map, s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(pilot);
    s.saveObjects(passengers);
    s.saveBool  (flying  );
    s.saveBool  (landed  );
    s.saveFloat(flyHeight);
    s.saveObject(dockedAt);
    Area.saveTile(entrance, map, s);
  }
  
  
  
  /**  Regular updates and behaviour methods-
    */
  void beginNextBehaviour() {
    assignTask(null);
    
    if (idle() && work() != null && work().complete()) {
      assignTask(work().selectActorBehaviour(this));
    }
    if (idle()) {
      assignTask(TaskResting.configResting(this, home()));
    }
  }
  


  /**  Implementing Employer interface-
    */
  public Task selectActorBehaviour(Actor actor) {
    return null;
  }
  
  
  public void actorUpdates(Actor actor) {
    return;
  }
  
  
  public void actorPasses(Actor actor, Building other) {
    return;
  }
  
  
  public void actorTargets(Actor actor, Target other) {
    return;
  }
  
  
  public void actorVisits(Actor actor, Building visits) {
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
    passengers.toggleMember(a, is);
  }
  
  
  public Series <Actor> allInside() {
    return passengers;
  }
  
  
  
  /**  Methods related to docking and landing-
    */
  AreaTile findLandingPoint(Area map, TaskTrading task) {
    //
    //  First, see where we're supposed to be getting close to:
    Target from = task.tradeGoes.base().headquarters();
    if (task.tradeGoes instanceof Building) from = (Building) task.tradeGoes;
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
  
  
  public void enterMap(Area map, int x, int y, float buildLevel, Base owns) {
    super.enterMap(map, x, y, buildLevel, owns);
    if (type().moveMode == Type.MOVE_AIR) this.flying = true;
  }
  
  
  void doLanding(AreaTile landing) {
    
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
  //  TODO:  Sew these up properly.
  
  public Tally <Good> needLevels() {
    return null;
  }
  
  public Tally <Good> prodLevels() {
    return null;
  }
  
  public float importPrice(Good g, Base sells) {
    return 0;
  }
  
  public float exportPrice(Good g, Base buys) {
    return 0;
  }
  
  public boolean allowExport(Good g, Trader buys) {
    return true;
  }
}



