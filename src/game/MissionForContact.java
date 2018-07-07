

package game;
import static game.GameConstants.*;
import game.BaseCouncil.Role;
import game.World.Journey;
import util.*;



public class MissionForContact extends Mission {
  
  
  public MissionForContact(Base belongs) {
    super(OBJECTIVE_CONTACT, belongs);
  }
  
  
  public MissionForContact(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  void update() {
    super.update();
    //
    //  If you're delivering terms, just check for either acceptance or
    //  rejection-
    if (terms.hasTerms()) {
      if (terms.accepted()) {
        setMissionComplete(true);
      }
      if (terms.rejected()) {
        setMissionComplete(false);
      }
    }
    //
    //  If your focus is an actor, check whether they're converted-
    if (localFocus() instanceof Actor) {
      Actor focus = (Actor) localFocus();
      if (focus.bonds.baseLoyal() == homeBase()) {
        setMissionComplete(true);
      }
      if (focus.health.dead()) {
        setMissionComplete(false);
      }
    }
    //
    //  And if your focus is a building, check whether that's switched-
    if (localFocus() instanceof Building) {
      Building focus = (Building) localFocus();
      if (focus.base() == homeBase()) {
        setMissionComplete(true);
      }
      if (focus.destroyed()) {
        setMissionComplete(false);
      }
    }
  }
  
  
  public void toggleRecruit(Actor a, boolean is) {
    //
    //  We need to ensure that recruits are recognised as guests of the other
    //  base...
    Base goes = worldFocus();
    if (goes != homeBase()) a.bonds.assignGuestBase(is ? goes : null);
    super.toggleRecruit(a, is);
  }


  void handleOffmapArrival(Base goes, World.Journey journey) {
    TaskGifting.performMisionDelivery(this);
    WorldEvents.handleDialog(this, goes, journey);
  }
  
  
  void handleOffmapDeparture(Base from, Journey journey) {
    TaskGifting.performMissionPickup(this);
  }
  
  
  public Task selectActorBehaviour(Actor actor) {
    //
    //  If we haven't embarked on a journey yet, envoys may first have to pick
    //  up a gift for whoever they're meeting.  (Not talking- just the pickup.)
    boolean isEnvoy = envoys.includes(actor);
    if (onWrongMap() && isEnvoy && actor.todo(TaskGifting.class) == null) {
      Actor offersTerms = findTalkSubject(this, actor, true);
      TaskDialog dialog = TaskDialog.contactDialogFor(actor, offersTerms, this);
      TaskGifting gifts = TaskGifting.nextGiftingFor(actor, dialog);
      if (gifts != null) return gifts;
    }
    //
    //  Otherwise, proceed as usual-
    return super.selectActorBehaviour(actor);
  }


  public Task nextLocalMapBehaviour(Actor actor) {
    
    boolean  haveTerms = terms.hasTerms();
    boolean  onAwayMap = ! onHomeMap();
    boolean  isEnvoy   = envoys.includes(actor);
    Pathing  camp      = transitPoint(actor);
    AreaTile stands    = MissionForSecure.standingPointRanks(actor, this, camp);
    
    if (onAwayMap && haveTerms && isEnvoy && ! terms.sent()) {
      Actor offersTerms = findTalkSubject(this, actor, true);
      TaskDialog dialog = TaskDialog.contactDialogFor(actor, offersTerms, this);
      TaskGifting gifts = TaskGifting.nextGiftingFor(actor, dialog);
      if (gifts  != null) return gifts ;
      if (dialog != null) return dialog;
    }
    
    Actor chatsWith = findTalkSubject(this, actor, false);
    Task chatting = TaskDialog.contactDialogFor(actor, chatsWith, this);
    if (chatting != null) return chatting;
    
    
    //  TODO:  Local missions may not have a camp-point.  How does that work
    //  then, exactly?
    
    TaskCombat taskC = (Task.inCombat(actor) || isEnvoy) ? null :
      TaskCombat.nextReaction(actor, stands, this, true, actor.seen())
    ;
    if (taskC != null) return taskC;
    
    Task standT = actor.targetTask(stands, 1, Task.JOB.MILITARY, this);
    if (standT != null) return standT;
    
    return null;
  }
  

  public boolean allowsFocus(Object newFocus) {
    if (newFocus instanceof Building) return true;
    if (newFocus instanceof Actor   ) return true;
    return false;
  }
  
  
  static Task nextGiftPickup(Mission parent, Actor talks, Series <Actor> gets) {
    return null;
  }
  
  
  static Actor findTalkSubject(Mission parent, Actor talks, boolean official) {
    
    Pick <Actor> pick = new Pick();
    Series <Actor> looksAt = null;
    Base focusBase = null;

    if (official) {
      Base focus = parent.worldFocus();
      BaseCouncil council = focus.council;
      Area area = focus.activeMap();
      
      Actor monarch = council.memberWithRole(Role.MONARCH);
      if (monarch != null && monarch.onMap()) pick.compare(monarch, 3);
      
      Actor minister = council.memberWithRole(Role.PRIME_MINISTER);
      if (minister != null && monarch.onMap()) pick.compare(minister, 2);
      
      Actor consort = council.memberWithRole(Role.CONSORT);
      if (consort != null && consort.onMap()) pick.compare(consort, 2);
      
      looksAt = area.actors();
      focusBase = focus;
    }
    else if (parent.localFocus() instanceof Building) {
      Building focus = (Building) parent.localFocus();
      looksAt = focus.workers();
      focusBase = focus.base();
    }
    else if (parent.localFocus() instanceof Actor) {
      Actor focus = (Actor) parent.localFocus();
      looksAt = new Batch(focus);
      focusBase = focus.base();
    }
    
    if (looksAt != null) for (Actor a : looksAt) {
      if (a.base() != focusBase) continue;
      float rating = 1f;
      if (a.type().socialClass == CLASS_COMMON) rating /= 2;
      rating *= Area.distance(parent.transitTile, a);
      rating *= 0.5f + Rand.num();
      rating *= TaskDialog.dialogRating(a, talks, false, false);
      pick.compare(a, rating);
    }
    
    return pick.result();
  }
  
}


