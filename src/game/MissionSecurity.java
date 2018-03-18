

package game;
import static game.GameConstants.*;



public class MissionSecurity extends Mission {
  
  
  public MissionSecurity(Base belongs, boolean activeAI) {
    super(OBJECTIVE_GARRISON, belongs, activeAI);
  }
  
  
  public MissionSecurity(Session s) throws Exception {
    super(s);
  }
  
  
  public Task selectActorBehaviour(Actor actor) {
    
    AreaTile stands = standLocation(actor);
    
    TaskCombat taskC = Task.inCombat(actor) ? null :
      TaskCombat.nextReaction(actor, stands, this, AVG_FILE)
    ;
    if (taskC != null) {
      return taskC;
    }
    
    Task standT = actor.targetTask(stands, 1, Task.JOB.MILITARY, this);
    if (standT != null) {
      return standT;
    }
    
    return null;
  }
  

  public boolean allowsFocus(Object newFocus) {
    //  TODO:  Fill this in...
    return false;
  }
  
  
  boolean objectiveComplete() {
    //  TODO:  In this case, you should arrange shifts and pay off at
    //  regular intervals.
    //int sinceStart = CityMap.timeSince(timeBegun, map.time);
    //if (sinceStart > MONTH_LENGTH * 2) return true;
    return false;
  }
  
  
  
  void handleArrival(Base goes, World.Journey journey) {
    BaseEvents.handleGarrison(this, goes, journey);
  }
  
}



