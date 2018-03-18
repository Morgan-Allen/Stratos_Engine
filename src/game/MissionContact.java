

package game;
import static game.GameConstants.*;

import game.GameConstants.Target;



public class MissionContact extends Mission {
  
  
  public MissionContact(Base belongs, boolean activeAI) {
    super(OBJECTIVE_RECON, belongs, activeAI);
  }
  
  
  public MissionContact(Session s) throws Exception {
    super(s);
  }
  
  
  public Task selectActorBehaviour(Actor actor) {
    
    boolean haveTerms  = hasTerms();
    boolean onAwayMap  = onAwayMap();
    boolean isEnvoy    = escorted.includes(actor);
    
    if (onAwayMap && haveTerms && isEnvoy && timeTermsSent == -1) {
      Actor offersTerms = MissionUtils.findOfferRecipient(this);
      Task t = actor.targetTask(offersTerms, 1, Task.JOB.DIALOG, this);
      if (t != null) return t;
    }
    
    /*
    //  TODO:  Merge with 'shouldLeave' criteria above?
    if (complete || termsAccepted || termsRefused) {
      AreaTile exits = standLocation(actor);
      if (exits != null) {
        return actor.targetTask(exits, 10, Task.JOB.RETURNING, this);
      }
    }
    //*/
    
    AreaTile stands = standLocation(actor);
    
    TaskCombat taskC = (Task.inCombat(actor) || isEnvoy) ? null :
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
  
  
  public void actorTargets(Actor actor, Target other) {
    if (actor.jobType() == Task.JOB.DIALOG) {
      dispatchTerms(awayCity());
    }
  }
  

  public boolean allowsFocus(Object newFocus) {
    //  TODO:  Fill this in...
    return false;
  }
  
  
  boolean objectiveComplete() {
    return termsAccepted || termsRefused;
  }
  
  
  
  void handleArrival(Base goes, World.Journey journey) {
    BaseEvents.handleDialog(this, goes, journey);
  }
  
}




