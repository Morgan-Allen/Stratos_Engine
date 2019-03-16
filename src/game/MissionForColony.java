

package game;
import static game.World.*;
import start.*;
import util.*;



public class MissionForColony extends Mission {
  
  
  final public Expedition expedition;
  boolean withPlayer;
  
  
  public MissionForColony(Base belongs, boolean playing) {
    super(Mission.OBJECTIVE_COLONY, belongs);
    expedition = new Expedition();
    withPlayer = playing;
  }
  
  
  public MissionForColony(Session s) throws Exception {
    super(s);
    expedition = (Expedition) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(expedition);
  }
  
  
  
  public boolean allowsFocus(Object focus) {
    World world = homeBase().world;
    Area l = (Area) I.cast(focus, Area.class);
    return world.basesFor(l).empty();
  }
  
  
  Task nextLocalMapBehaviour(Actor actor) {
    return null;
  }
  
  
  void handleOffmapArrival(Area goes, Journey journey) {
    
    World world = homeBase().world;
    WorldScenario scenario = world.scenarioFor(goes);
    
    //  TODO:  Ensure the existing world is used for this purpose, after
    //  scrubbing any connections to the older map...
    
    if (scenario != null && withPlayer) {
      scenario.assignExpedition(expedition);
      scenario.initScenario(MainGame.mainGame());
      MainGame.playScenario(scenario, world);
    }

    //  TODO:  Establish a new base from the assigned expedition.
    
    else {
      Base landing = new Base(world, goes, expedition.faction);
      //landing.initBuildLevels(BASTION, 1);
      world.addBases(landing);
    }
  }
  
  
  void handleOffmapDeparture(Area from, Journey journey) {
  }
  
  
}










