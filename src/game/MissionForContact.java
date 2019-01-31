

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
  
  
  public void beginMission(Base localBase) {
    super.beginMission(localBase);
    Base goes = worldFocus();
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
    Pathing  camp      = transitPoint(actor);
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
        rating *= TaskDialog.dialogRating(a, talks, false, false) / Task.ROUTINE;
        super.compare(a, rating);
      }
    };
    
    Series <Actor> looksAt = null;
    Base focusBase = null;
    
    if (official && parent.worldFocus() != null) {
      Base focus = parent.worldFocus();
      BaseCouncil council = focus.council;
      Area area = focus.activeMap();
      
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
      float rating = Area.distancePenalty(parent.transitTile, a);
      pick.compare(a, rating);
    }
    
    return pick.result();
  }

  
  
  
  /**  Utility methods for faction-level decision-making-
    */
  void handleOffmapDeparture(Base from, Journey journey) {
    if (from == homeBase()) {
      TaskGifting.performMissionPickup(this);
    }
  }
  
  
  void handleOffmapArrival(Base goes, World.Journey journey) {
    if (goes == worldFocus()) {
      TaskGifting.performMisionDelivery(this);
      MissionUtils.handleDialog(this, goes, journey);
    }
  }
  
  
  /*
  static Mission configMissionForAI(
    Faction from, Base launches, Base target, World world
  ) {
    
    MissionForContact mission = new MissionForContact(launches);
    mission.setWorldFocus(target);
    
    
    
    /*
    MissionAssessment MA = new MissionAssessment();
    
    MA.objective = Mission.OBJECTIVE_CONTACT;
    MA.fromC = from;
    MA.goesC = goes;
    
    if (typeAI == AI_WARLIKE) {
      MA.evaluatedAppeal = -1;
      return MA;
    }
    
    //  See if it's possible to arrange a marriage as well.
    
    Actor monarch = goes.council().memberWithRole(BaseCouncil.Role.MONARCH);
    Pick <Actor> pickM = new Pick();
    
    for (Actor a : from.council().allMembersWithRole(BaseCouncil.Role.HEIR)) {
      Actor spouse = a.bonds.allBondedWith(BOND_MARRIED).first();
      if (spouse != null) continue;
      if (monarch.health.man() == a.health.man()) continue;
      
      float rating = 1.0f;
      if (random) rating += Rand.num() - 0.5f;
      pickM.compare(a, rating);
    }
    Actor marries = pickM.result();
    
    MA.postureDemand  = POSTURE.ALLY;
    MA.marriageDemand = marries;
    
    //  Appeal of alliance depends on whether you have a good existing
    //  relationship and/or share similar enemies.
    
    //I.say("\nGetting synergy between "+from+" and "+goes);
    float synergyVal = 0, dot = 0, count = 0;
    for (Faction c : goes.relations.relationsWith()) {
      float valueF = from.relations.loyalty(c);
      float valueG = goes.relations.loyalty(c);
      //I.say("  "+c+": "+valueF+" * "+valueG);
      if (valueF == -100 || valueG == -100) continue;
      synergyVal += dot = valueF * valueG;
      count += (Nums.abs(dot) + 1) / 2;
    }
    synergyVal /= Nums.max(1, count);
    //I.say("  Total value: "+synergyVal);
    
    float tradeVal = 0;
    for (Good g : world.goodTypes) {
      float exports = from.trading.prodLevel(g);
      if (exports > 0) {
        tradeVal += tributeValue(g, exports, goes);
      }
      float imports = goes.trading.needLevel(g);
      if (imports > 0) {
        tradeVal += tributeValue(g, exports, from);
      }
    }
    tradeVal *= AVG_ALLIANCE_YEARS * 1f / AVG_TRIBUTE_YEARS;
    
    //I.say("  Trade value: "+tradeVal);
    
    float powerVal = from.idealArmyPower() / POP_PER_CITIZEN;
    powerVal *= casualtyValue(goes);
    powerVal *= AVG_ALLIANCE_YEARS * 1f / AVG_RETIREMENT;
    
    float marriageCost = 0;
    if (marries != null) {
      marriageCost = casualtyValue(from) * MARRIAGE_VALUE_MULT;
      marriageCost *= (1 + from.council().membersBondAvg(marries)) / 2;
    }
    
    float relationsVal = (
      goes.relations.loyalty(from.faction()) +
      from.relations.loyalty(goes.faction())
    ) / 2;
    
    MA.benefits += tradeVal;
    MA.benefits += powerVal;
    MA.benefits *= 1 + synergyVal + relationsVal;
    MA.costs    += marriageCost;
    
    float appeal = 0;
    appeal += (random ? Rand.avgNums(2) : 0.5f) * MA.benefits;
    appeal -= (random ? Rand.avgNums(2) : 0.5f) * MA.costs;
    //appeal /= (MA.benefits + MA.costs) / 2;
    appeal *= ActorUtils.distanceRating(MA.rulesC, MA.goesC);
    MA.evaluatedAppeal = appeal;
    
    return MA;
    //*/
  //}
  
}





