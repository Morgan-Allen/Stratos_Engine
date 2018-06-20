


package game;
import static game.GameConstants.*;



public class TaskTransport extends Task {
  
  
  ActorAsVessel vessel;
  Mission mission;
  
  
  TaskTransport(ActorAsVessel actor, Mission mission) {
    super(actor);
    this.vessel  = actor;
    this.mission = mission;
  }
  
  public TaskTransport(Session s) throws Exception {
    super(s);
    vessel   = (ActorAsVessel) active;
    mission  = (Mission) s.loadObject();
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(mission);
  }
  
  
  
  
  static TaskTransport nextTransport(ActorAsVessel actor, Mission mission) {
    TaskTransport task = new TaskTransport(actor, mission);
    AreaTile goes = actor.landsAt();
    task.configTask(actor, null, goes, JOB.DOCKING, 1);
    if (! task.pathValid()) return null;
    return task;
  }

  
  public void beginFromOffmap(Base from) {
    World world = mission.homeBase.world;
    Base home = mission.homeBase(), away = mission.worldFocus();
    world.beginJourney(home, away, vessel.type().moveMode, vessel);
  }
  
  
  protected void onTarget(Target target) {
    
    Base home = mission.homeBase(), away = mission.worldFocus();
    
    if (type == JOB.DEPARTING) {
      mission.beginJourney(home, away);
      vessel.exitMap(vessel.map());
      return;
    }
    
    if (type == JOB.RETURNING) {
      mission.beginJourney(away, home);
      vessel.exitMap(vessel.map());
      return;
    }

    if (mission.readyToDepart()) {
      AreaTile transit = ActorUtils.findTransitPoint(
        vessel.map(), home, away, vessel
      );
      configTask(origin, null, transit, JOB.DEPARTING, 0);
      vessel.doTakeoff(vessel.landsAt());
      return;
    }
    
    if (mission.readyToReturn()) {
      AreaTile transit = ActorUtils.findTransitPoint(
        vessel.map(), away, home, vessel
      );
      configTask(origin, null, transit, JOB.RETURNING, 0);
      vessel.doTakeoff(vessel.landsAt());
      return;
    }
    
    if (type == JOB.DOCKING || type == JOB.WAITING) {
      if (! vessel.landed()) vessel.doLanding(vessel.landsAt());
      configTask(origin, null, target, JOB.WAITING, 1);
    }
    
  }
  

  
  boolean doingLanding(Base local) {
    return local.activeMap() != null && active.type().isAirship();
  }
  
  
  protected boolean updateOnArrival(Base goes, World.Journey journey) {
    //
    //  If we're arriving at a base with an active map, then we proceed to
    //  whatever landing-site the vessel was able to find...
    if (doingLanding(goes)) {
      AreaTile docks = vessel.landsAt();
      configTask(origin, null, docks, JOB.DOCKING, 1);
      return true;
    }
    //
    //  In the event of arriving at an off-map base, then in principle the
    //  mission itself should handle any extra resolution required...
    return true;
  }
  
  
}




