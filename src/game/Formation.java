

package game;
import util.*;
import static game.CityMap.*;
import static game.City.*;
import static game.CityBorders.*;
import static game.GameConstants.*;



public class Formation implements
  Session.Saveable, Journeys, TileConstants, Employer
{
  
  /**  Data fields, setup and save/load methods-
    */
  final static int
    OBJECTIVE_STANDBY  = 0,
    OBJECTIVE_CONQUER  = 1,
    OBJECTIVE_GARRISON = 2,
    OBJECTIVE_DIALOG   = 3
  ;
  
  int objective = OBJECTIVE_STANDBY;
  boolean activeAI = false;
  
  boolean termsGiven    = false;
  boolean termsAccepted = false;
  boolean termsRefused  = false;
  boolean victorious    = false;
  boolean defeated      = false;
  boolean complete      = false;
  
  City.POSTURE postureDemand  = null;
  Formation    actionDemand   = null;
  Actor        marriageDemand = null;
  Tally <Good> tributeDemand  = new Tally();
  
  List <Actor> escorted = new List();
  List <Actor> recruits = new List();
  
  boolean away   = false;
  boolean active = false;
  City    homeCity    ;
  City    secureCity  ;
  CityMap map         ;
  Tile    transitPoint;
  Tile    standPoint  ;
  Object  secureFocus ;
  int     facing      ;
  
  List <Tile> guardPoints = new List();
  int lastUpdateTime = -1;
  
  
  Formation(int objective, City belongs, boolean activeAI) {
    this.objective = objective;
    this.activeAI  = activeAI;
    this.homeCity  = belongs;
    this.map       = belongs.map;
  }
  
  
  public Formation(Session s) throws Exception {
    s.cacheInstance(this);
    
    objective     = s.loadInt ();
    termsGiven    = s.loadBool();
    termsAccepted = s.loadBool();
    termsRefused  = s.loadBool();
    victorious    = s.loadBool();
    defeated      = s.loadBool();
    complete      = s.loadBool();
    
    postureDemand  = (City.POSTURE) s.loadEnum(City.POSTURE.values());
    actionDemand   = (Formation   ) s.loadObject();
    marriageDemand = (Actor       ) s.loadObject();
    s.loadTally(tributeDemand);
    
    s.loadObjects(escorted);
    s.loadObjects(recruits);
    
    away         = s.loadBool();
    homeCity     = (City   ) s.loadObject();
    secureCity   = (City   ) s.loadObject();
    map          = (CityMap) s.loadObject();
    
    active       = s.loadBool();
    transitPoint = loadTile(map, s);
    standPoint   = loadTile(map, s);
    secureFocus  = s.loadObject();
    facing       = s.loadInt();
    
    for (int n = s.loadInt(); n-- > 0;) {
      Tile point = CityMap.loadTile(map, s);
      guardPoints.add(point);
    }
    lastUpdateTime = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveInt (objective    );
    s.saveBool(termsGiven   );
    s.saveBool(termsAccepted);
    s.saveBool(termsRefused );
    s.saveBool(victorious   );
    s.saveBool(defeated     );
    s.saveBool(complete     );
    
    s.saveEnum  (postureDemand );
    s.saveObject(actionDemand  );
    s.saveObject(marriageDemand);
    s.saveTally (tributeDemand );
    
    s.saveObjects(escorted);
    s.saveObjects(recruits);
    
    s.saveBool  (away      );
    s.saveObject(homeCity  );
    s.saveObject(secureCity);
    s.saveObject(map       );
    
    s.saveBool(active);
    saveTile(transitPoint, map, s);
    saveTile(standPoint  , map, s);
    s.saveObject(secureFocus);
    s.saveInt(facing);
    
    s.saveInt(guardPoints.size());
    for (Tile t : guardPoints) CityMap.saveTile(t, map, s);
    s.saveInt(lastUpdateTime);
  }
  
  
  
  /**  Supplemental utility methods for setting objectives, demands and
    *  marching orders:
    */
  void assignTerms(
    City.POSTURE posture,
    Formation actionTaken,
    Actor toMarry,
    Tally <Good> tribute
  ) {
    this.postureDemand  = posture;
    this.actionDemand   = actionTaken;
    this.marriageDemand = toMarry;
    this.tributeDemand  = tribute == null ? new Tally() : tribute;
  }
  
  
  void setTermsAccepted(boolean accepted) {
    if (accepted) termsAccepted = true;
    else          termsRefused  = true;
  }
  
  
  void setMissionComplete(boolean victorious) {
    if (victorious) this.victorious = true;
    else this.defeated = true;
    this.complete = true;
  }
  
  
  void toggleRecruit(Actor a, boolean is) {
    this.recruits.toggleMember(a, is);
    if (is) a.formation = this;
    else    a.formation = null;
  }
  
  
  void toggleEscorted(Actor a, boolean is) {
    this.escorted.toggleMember(a, is);
    if (is) a.formation = this;
    else    a.formation = null;
  }
  
  
  void beginSecuring(City city) {
    homeCity.formations.toggleMember(this, true);
    
    this.secureCity = city;
    this.active     = true;
    
    if (this.map != null) {
      this.transitPoint = findTransitPoint(map, city);
      beginSecuring(city, 0, map); //  Make this face the border?
    }
    else {
      beginJourney(homeCity, secureCity);
    }
  }
  
  
  void beginSecuring(Object focus, int facing, CityMap map) {
    homeCity.formations.toggleMember(this, true);
    
    this.facing      = facing;
    this.secureFocus = focus ;
    this.active      = true  ;
    this.map         = map   ;
    
    this.standPoint = standPointFrom(secureFocus);
  }
  
  
  Tile standPointFrom(Object focus) {
    
    //  TODO:  Get the nearest open tile, just for good measure?
    
    if (focus instanceof City) {
      return this.transitPoint;
    }
    if (focus instanceof Formation) {
      Pathing from = ((Formation) focus).pathFrom();
      return standPointFrom(from);
    }
    if (focus instanceof Element) {
      return ((Element) focus).centre();
    }
    if (focus instanceof Target) {
      return ((Target) focus).at();
    }
    return null;
  }
  
  
  Pathing pathFrom() {
    
    //  TODO:  Use the position of the member closest to average?
    
    Actor a = recruits.atIndex(recruits.size() / 2);
    return Task.pathOrigin(a);
  }
  
  
  Target pathGoes() {
    if (secureFocus instanceof City) {
      return transitPoint;
    }
    if (secureFocus instanceof Formation) {
      return ((Formation) secureFocus).pathFrom();
    }
    if (secureFocus instanceof Target) {
      return (Target) secureFocus;
    }
    return null;
  }
  
  
  void disbandFormation() {
    homeCity.formations.toggleMember(this, false);
    
    this.secureCity  = null  ;
    this.secureFocus = null  ;
    this.facing      = CENTRE;
    this.active      = false ;
    
    for (Actor r : recruits) toggleRecruit (r, false);
    for (Actor e : escorted) toggleEscorted(e, false);
  }
  
  
  
  /**  Regular updates and journey-related methods:
    */
  void update() {
    
    //  Cull any casualties...
    for (Actor a : recruits) if (a.dead()) {
      recruits.remove(a);
    }
    if (active && recruits.empty()) {
      disbandFormation();
      return;
    }
    
    //
    //  Update your stand-point...
    this.standPoint = standPointFrom(secureFocus);
    
    //  This is a hacky way of saying "I want to go to a different city, is
    //  everyone ready yet?"
    if (
      map != null && secureCity != null &&
      secureCity != map.city && formationReady()
    ) {
      beginJourney(map.city, secureCity);
    }
  }
  
  
  private World.Journey beginJourney(City from, City goes) {
    if (reports()) I.say("\nREADY TO BEGIN FOREIGN MISSION!");
    for (Actor w : recruits) if (w.onMap(map)) {
      w.exitMap(map);
    }
    away         = true;
    map          = null;
    secureFocus  = null;
    transitPoint = null;
    return goes.world.beginJourney(from, goes, this);
  }
  
  
  public void onArrival(City goes, World.Journey journey) {
    //
    //  There are 4 cases here: arriving home or away, and arriving on a map or
    //  not.
    boolean home  = goes == homeCity;
    boolean onMap = goes.map != null;
    //
    //  In the event that a formation arrives on the map, either it's a foreign
    //  invader threatening the city, or one of the player's legions returning
    //  home.
    if (onMap) {
      if (reports()) I.say("\nARRIVED ON MAP: "+goes+" FROM "+journey.from);
      
      Tile transits     = findTransitPoint(goes.map, journey.from);
      this.map          = goes.map;
      this.transitPoint = transits;
      
      for (Actor w : recruits) {
        w.enterMap(map, transits.x, transits.y, 1);
      }
      beginSecuring(transitPoint, N, map);
      
      if (home) {
        this.away = false;
        disbandFormation();
      }
      else {
        this.away = true;
        if (activeAI) FormationUtils.updateTacticalTarget(this);
      }
    }
    //
    //  In the event that this happens off-map, either an army has returned
    //  home to a foreign city, or an army has assaulted a foreign city:
    else {
      guardPoints.clear();
      lastUpdateTime = -1;
      
      if (home) {
        if (reports()) I.say("\nARRIVED HOME: "+goes+" FROM "+journey.from);
        this.away = false;
        CityEvents.handleReturn(this, journey.from, journey);
      }
      else {
        if (reports()) I.say("\nARRIVED AT: "+goes+" FROM "+journey.from);
        this.away = true;
        CityEvents.handleInvasion(this, goes, journey);
      }
    }
  }
  
  
  
  /**  Organising actors and other basic query methods-
    */
  public void selectActorBehaviour(Actor actor) {
    
    boolean haveTerms = false;
    haveTerms |= marriageDemand != null;
    haveTerms |= actionDemand   != null;
    haveTerms |= postureDemand  != null;
    haveTerms |= ! tributeDemand.empty();
    
    boolean isEnvoy    = escorted.includes(actor);
    boolean diplomatic = objective == OBJECTIVE_DIALOG;
    
    if (haveTerms && isEnvoy && ! termsGiven) {
      Actor offersTerms = FormationUtils.findOfferRecipient(this);
      actor.embarkOnTarget(offersTerms, 1, Task.JOB.DIALOG, this);
      if (actor.idle()) termsGiven = true;
      else return;
    }
    
    if (activeAI) {
      FormationUtils.updateTacticalTarget(this);
    }
    
    if (defeated || victorious || (diplomatic && termsRefused)) {
      Tile exits = standLocation(actor);
      if (exits != null) {
        actor.embarkOnTarget(exits, 10, Task.JOB.RETURNING, this);
        return;
      }
    }
    
    TaskCombat taskC = (actor.inCombat() || isEnvoy) ? null :
      TaskCombat.nextReaction(actor, this)
    ;
    if (taskC != null) {
      actor.assignTask(taskC);
      return;
    }
    
    TaskCombat taskS = (actor.inCombat() || isEnvoy) ? null :
      TaskCombat.nextSieging(actor, this)
    ;
    if (taskS != null) {
      actor.assignTask(taskS);
      return;
    }
    
    Tile stands = standLocation(actor);
    if (stands != null) {
      actor.embarkOnTarget(stands, 10, Task.JOB.MILITARY, this);
      return;
    }
  }
  
  
  public void actorTargets(Actor actor, Target other) {
    //objective.actorTargets(actor, other, this);
    
    if (actor.jobType() == Task.JOB.DIALOG) {
      map.city.council.receiveTerms(this);
    }
    if (actor.jobType() == Task.JOB.MILITARY) {
    }
    if (actor.jobType() == Task.JOB.RETURNING) {
    }
    if (actor.jobType() == Task.JOB.RETREAT) {
      actor.exitMap(map);
    }
  }
  
  
  public void actorUpdates(Actor actor) {
    //objective.actorUpdates(actor, this);
  }
  
  
  public void actorPasses(Actor actor, Building other) {
    return;
  }
  
  
  public void actorVisits(Actor actor, Building visits) {
    return;
  }
  
  
  public City homeCity() {
    return homeCity;
  }
  
  
  Tile standLocation(Actor actor) {
    boolean doPatrol =
      (secureFocus instanceof Target) &&
      ((Target) secureFocus).type().isWall
    ;
    if (doPatrol) {
      return FormationUtils.standingPointPatrol(actor, this);
    }
    else {
      return FormationUtils.standingPointRanks(actor, this);
    }
  }
  
  
  boolean formationReady() {
    if (map == null) return true;
    if (secureFocus == null || ! active) return false;
    
    //  TODO:  Consider this more carefully?
    for (Actor a : recruits) {
      if (standLocation(a) != a.at()) {
        return false;
      }
    }
    
    return true;
  }
  
  
  int powerSum() {
    return powerSum(recruits, null);
  }
  
  
  static int powerSum(Series <Actor> recruits, CityMap mapOnly) {
    float sumStats = 0;
    for (Actor a : recruits) {
      if (mapOnly != null && a.map != mapOnly) continue;
      sumStats += TaskCombat.attackPower(a);
    }
    return (int) (sumStats * POP_PER_CITIZEN);
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  boolean reports() {
    return false;
  }
  
  
  public String toString() {
    return "Formation ("+homeCity+")";
  }
}










