

package game;
import static game.GameConstants.*;
import game.BaseCouncil.Role;
import game.GameConstants.Target;
import game.World.Journey;
import util.*;



public class MissionContact extends Mission {
  
  
  public MissionContact(Base belongs) {
    super(OBJECTIVE_CONTACT, belongs);
  }
  
  
  public MissionContact(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  void update() {
    super.update();
    //
    //  Update current victory-conditions...
    if (terms.accepted()) {
      setMissionComplete(true);
    }
    if (terms.rejected()) {
      setMissionComplete(false);
    }
  }
  
  
  void beginJourney(Base from, Base goes) {
    super.beginJourney(from, goes);
    for (Actor a : recruits) a.assignGuestBase(null);
  }
  

  public void onArrival(Base goes, World.Journey journey) {
    if (goes != homeBase()) for (Actor a : recruits) a.assignGuestBase(goes);
    super.onArrival(goes, journey);
  }
  
  
  void handleOffmapArrival(Base goes, World.Journey journey) {
    BaseEvents.handleDialog(this, goes, journey);
  }
  
  
  void handleOffmapDeparture(Base from, Journey journey) {
    return;
  }


  public Task nextLocalMapBehaviour(Actor actor) {
    
    boolean  haveTerms = terms.hasTerms();
    boolean  onAwayMap = ! onHomeMap();
    boolean  isEnvoy   = envoys.includes(actor);
    Pathing  camp      = transitPoint(actor);
    AreaTile stands    = MissionSecure.standingPointRanks(actor, this, camp);
    
    if (onAwayMap && haveTerms && isEnvoy && ! terms.sent()) {
      Actor offersTerms = findOfferRecipient(this);
      Task t = actor.targetTask(offersTerms, 1, Task.JOB.DIALOG, this);
      if (t != null) return t;
    }
    
    TaskCombat taskC = (Task.inCombat(actor) || isEnvoy) ? null :
      TaskCombat.nextReaction(actor, stands, this, actor.seen())
    ;
    if (taskC != null) return taskC;
    
    Task standT = actor.targetTask(stands, 1, Task.JOB.MILITARY, this);
    if (standT != null) return standT;
    
    return null;
  }
  
  
  public void actorTargets(Actor actor, Target other) {
    if (actor.jobType() == Task.JOB.DIALOG) {
      Base focus = ((Element) other).base();
      terms.sendTerms(focus);
    }
    super.actorTargets(actor, other);
  }
  

  public boolean allowsFocus(Object newFocus) {
    //  TODO:  Fill this in...
    return false;
  }
  
  
  static Actor findOfferRecipient(Mission parent) {
    
    Base focus = parent.worldFocus();
    if (focus == null) focus = ((Element) parent.localFocus()).base();
    if (focus == null) return null;
    
    BaseCouncil council = focus.council;
    
    Actor monarch = council.memberWithRole(Role.MONARCH);
    if (monarch != null && monarch.onMap()) return monarch;
    
    Actor minister = council.memberWithRole(Role.PRIME_MINISTER);
    if (minister != null && monarch.onMap()) return monarch;
    
    Actor consort = council.memberWithRole(Role.CONSORT);
    if (consort != null && consort.onMap()) return consort;
    
    return null;
  }
  
}









