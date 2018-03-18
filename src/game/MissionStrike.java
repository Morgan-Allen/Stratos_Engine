

package game;
import static game.GameConstants.*;



public class MissionStrike extends Mission {
  
  
  public MissionStrike(Base belongs, boolean activeAI) {
    super(OBJECTIVE_CONQUER, belongs, activeAI);
  }
  
  
  public MissionStrike(Session s) throws Exception {
    super(s);
  }
  
  
  public Task selectActorBehaviour(Actor actor) {
    //return super.selectActorBehaviour(actor);
    
    boolean haveTerms = hasTerms();
    boolean onAwayMap = onAwayMap();
    boolean isEnvoy   = escorted.includes(actor);
    
    if (onAwayMap && haveTerms && isEnvoy && timeTermsSent == -1) {
      Actor offersTerms = MissionUtils.findOfferRecipient(this);
      Task t = actor.targetTask(offersTerms, 1, Task.JOB.DIALOG, this);
      if (t != null) return t;
    }
    
    //  TODO:  Merge with 'shouldLeave' criteria above.
    if (complete || termsAccepted) {
      AreaTile exits = standLocation(actor);
      if (exits != null) {
        return actor.targetTask(exits, 10, Task.JOB.RETURNING, this);
      }
    }
    
    AreaTile stands = standLocation(actor);
    
    TaskCombat taskC = (Task.inCombat(actor) || isEnvoy) ? null :
      TaskCombat.nextReaction(actor, stands, this, AVG_FILE)
    ;
    if (taskC != null) {
      return taskC;
    }
    
    TaskCombat taskS = (Task.inCombat(actor) || isEnvoy) ? null :
      TaskCombat.nextSieging(actor, this)
    ;
    if (taskS != null) {
      return taskS;
    }
    
    Task standT = actor.targetTask(stands, 1, Task.JOB.MILITARY, this);
    if (standT != null) {
      return standT;
    }
    
    return null;
  }
  
  
  
  public boolean allowsFocus(Object newFocus) {
    if (newFocus instanceof Building) return true;
    if (newFocus instanceof Actor   ) return true;
    return false;
  }
  
  
  boolean objectiveComplete() {
    if (focus() instanceof Element) {
      Element e = (Element) focus();
      return e.destroyed();
    }
    //  TODO:  Use this-
    /*
    if (! FormationUtils.tacticalFocusValid(this, secureFocus)) {
      return true;
    }
    //*/
    return false;
  }
  
  
  
  void handleArrival(Base goes, World.Journey journey) {
    BaseEvents.handleInvasion(this, goes, journey);
  }
  
}








