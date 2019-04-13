

package game;
import static game.GameConstants.*;
import game.BaseCouncil.*;
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
    if (! complete()) {
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
  }
  
  
  public void beginMission(Area locale) {
    super.beginMission(locale);
    Base goes = (Base) worldFocus();
    for (Actor a : recruits()) a.bonds.assignGuestBase(goes);
  }
  
  
  public void disbandMission() {
    for (Actor a : recruits()) a.bonds.assignGuestBase(null);
    super.disbandMission();
  }
  
  
  public Task selectActorBehaviour(Actor actor) {
    //
    //  If we haven't embarked on a journey yet, envoys may first have to pick
    //  up a gift for whoever they're meeting.  (Not talking- just the pickup.)
    if (
      goesOffmap() && ! departed() && isEnvoy(actor) &&
      actor.todo(TaskGifting.class) == null
    ) {
      Actor offersTerms = findTalkSubject(this, actor, true, false);
      TaskGifting gifts = TaskGifting.nextGiftingFor(actor, offersTerms, this);
      if (gifts != null) return gifts;
    }
    //
    //  Otherwise, proceed as usual-
    return super.selectActorBehaviour(actor);
  }


  public Task nextLocalMapBehaviour(Actor actor) {
    
    //boolean  haveTerms = terms.hasTerms();
    //boolean  onAwayMap = ! onHomeMap();
    boolean  isEnvoy   = isEnvoy(actor);
    Pathing  camp      = transitTile();
    AreaTile stands    = MissionForSecure.standingPointRanks(actor, this, camp);
    
    //if (onAwayMap && haveTerms && isEnvoy && ! terms.sent()) {
    if (isEnvoy) {
      Actor offersTerms = findTalkSubject(this, actor, true, true);
      TaskDialog  dialog = TaskDialog .contactDialogFor(actor, offersTerms, this);
      TaskGifting gifts  = TaskGifting.nextGiftingFor  (actor, offersTerms, this);
      if (gifts  != null) return gifts ;
      if (dialog != null) return dialog;
    }
    
    Actor chatsWith = findTalkSubject(this, actor, false, true);
    Task chatting = TaskDialog.contactDialogFor(actor, chatsWith, this);
    if (chatting != null) return chatting;
    
    
    //  TODO:  Local missions may not have a camp-point.  How does that work
    //  then, exactly?
    
    //  TODO:  You may need to find another way to keep the envoys busy while
    //  talking is not possible.
    
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
  
  
  static Actor findTalkSubject(
    final Mission parent, final Actor talks,
    final boolean official, final boolean localOnly
  ) {
    
    final Pick <Actor> pick = new Pick <Actor> () {
      public void compare(Actor a, float rating) {
        if (a == null) return;
        if (a.map() != parent.localMap() && localOnly) return;
        if (a.type().socialClass == CLASS_COMMON) rating /= 2;
        rating *= 0.5f + Rand.num();
        rating *= TaskDialog.dialogRating(a, talks, TaskDialog.MODE_CONTACT, false);
        rating /= Task.ROUTINE;
        super.compare(a, rating);
      }
    };
    
    Series <Actor> looksAt = null;
    Base focusBase = null;
    
    if (official && parent.worldFocus() != null) {
      Base focus = (Base) parent.worldFocus();
      BaseCouncil council = focus.council;
      AreaMap area = focus.activeMap();
      
      Actor monarch = council.memberWithRole(Role.MONARCH);
      pick.compare(monarch, 3);
      
      Actor minister = council.memberWithRole(Role.PRIME_MINISTER);
      pick.compare(minister, 2);
      
      Actor consort = council.memberWithRole(Role.CONSORT);
      pick.compare(consort, 2);
      
      looksAt = area == null ? null : area.actors();
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
      float rating = AreaMap.distancePenalty(parent.transitPoint(), a);
      pick.compare(a, rating);
    }
    
    return pick.result();
  }
  
  
  
  /**  Utility methods for faction-level decision-making-
    */
  void handleOffmapDeparture(Area from, Journey journey) {
    
    ///I.say("Contact departing: "+this.hashCode()+", from: "+from);
    
    if (from == homeBase().area) {
      TaskGifting.performMissionPickup(this);
    }
  }
  
  
  void handleOffmapArrival(Area goes, World.Journey journey) {
    
    ///I.say("Contact arriving: "+this.hashCode()+", goes: "+goes);
    
    if (goes == worldFocusArea()) {
      TaskGifting.performMisionDelivery(this);
      Base focus = worldFocusBase();
      MissionUtils.handleDialog(this, focus, journey);
    }
  }
  
  
}







