


package game;
import static game.GameConstants.*;



public class TaskTransport extends Task {
  
  
  ActorAsVessel vessel;
  Mission mission;
  boolean returned;
  
  
  TaskTransport(ActorAsVessel actor, Mission mission) {
    super(actor);
    this.vessel  = actor;
    this.mission = mission;
  }
  
  public TaskTransport(Session s) throws Exception {
    super(s);
    vessel   = (ActorAsVessel) active;
    mission  = (Mission) s.loadObject();
    returned = s.loadBool();
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(mission);
    s.saveBool(returned);
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
    Area home = mission.homeBase().area, away = mission.worldFocusArea();
    world.beginJourney(home, away, vessel.type().moveMode, vessel);
  }
  
  
  protected void onTarget(Target target) {
    
    Base home = mission.homeBase();
    Area away = mission.worldFocusArea();

    if (mission.complete() && vessel.map() == home.activeMap()) {
      returned = true;
    }
    
    if (type == JOB.DEPARTING) {
      mission.beginJourney(home.area, away);
      vessel.exitMap(vessel.map());
      return;
    }
    
    if (type == JOB.RETURNING) {
      mission.beginJourney(away, home.area);
      vessel.exitMap(vessel.map());
      return;
    }
    
    if (mission.readyToDepart() && ! returned) {
      AreaTile transit = ActorUtils.findTransitPoint(
        vessel.map(), home.area, away, vessel
      );
      configTask(origin, null, transit, JOB.DEPARTING, 0);
      vessel.doTakeoff(vessel.landsAt());
      return;
    }
    
    if (mission.readyToReturn() && ! returned) {
      AreaTile transit = ActorUtils.findTransitPoint(
        vessel.map(), away, home.area, vessel
      );
      configTask(origin, null, transit, JOB.RETURNING, 0);
      vessel.doTakeoff(vessel.landsAt());
      return;
    }
    
    if (type == JOB.DOCKING || type == JOB.WAITING) {
      if (! vessel.landed()) {
        vessel.doLanding(vessel.landsAt());
      }
      if (! returned) {
        configTask(origin, null, target, JOB.WAITING, 1);
      }
    }
  }
  

  
  boolean doingLanding(Area local) {
    return local.activeMap() != null && active.type().isAirship();
  }
  
  
  protected boolean updateOnArrival(Area goes, World.Journey journey) {
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




