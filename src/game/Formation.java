

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
  Objective    objective     = null;
  City.POSTURE postureDemand = null;
  Formation    actionDemand  = null;
  Tally <Good> tributeDemand = new Tally();
  
  List <Actor> escorted = new List();
  List <Actor> recruits = new List();
  
  boolean away   = false;
  City    homeCity   ;
  City    secureCity ;
  CityMap map        ;
  
  boolean active = false;
  Tile    entryPoint ;
  Tile    exitsPoint ;
  Tile    securePoint;
  Object  secureFocus;
  int     facing     ;
  
  
  Formation(Objective objective, City belongs) {
    this.objective = objective;
    this.homeCity  = belongs;
    this.map       = belongs.map;
  }
  
  
  public Formation(Session s) throws Exception {
    s.cacheInstance(this);
    
    objective     = (Objective   ) s.loadObject();
    postureDemand = (City.POSTURE) s.loadEnum(City.POSTURE.values());
    actionDemand  = (Formation   ) s.loadObject();
    s.loadTally(tributeDemand);
    
    s.loadObjects(escorted);
    s.loadObjects(recruits);
    
    away        = s.loadBool();
    homeCity    = (City   ) s.loadObject();
    secureCity  = (City   ) s.loadObject();
    map         = (CityMap) s.loadObject();
    
    active      = s.loadBool();
    entryPoint  = loadTile(map, s);
    exitsPoint  = loadTile(map, s);
    securePoint = loadTile(map, s);
    secureFocus = s.loadObject();
    facing      = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(objective    );
    s.saveEnum  (postureDemand);
    s.saveObject(actionDemand );
    s.saveTally (tributeDemand);
    
    s.saveObjects(escorted);
    s.saveObjects(recruits);
    
    s.saveBool  (away      );
    s.saveObject(homeCity  );
    s.saveObject(secureCity);
    s.saveObject(map       );
    
    s.saveBool(active);
    saveTile(entryPoint , map, s);
    saveTile(exitsPoint , map, s);
    saveTile(securePoint, map, s);
    s.saveObject(secureFocus);
    s.saveInt(facing);
  }
  
  
  
  /**  Used to customise behaviour specific to conquest, defence, trade,
    *  diplomacy, et cetera-
    */
  public abstract static class Objective implements Session.Saveable {
    
    Objective() {
      return;
    }
    
    public Objective(Session s) throws Exception {
      s.cacheInstance(this);
    }
    
    public void saveState(Session s) throws Exception {
      return;
    }
    
    abstract boolean updateTacticalTarget(Formation parent);
    abstract Tile standLocation(Actor a, Formation parent);
    
    abstract void selectActorBehaviour(Actor a, Formation parent);
    abstract void actorUpdates(Actor a, Formation parent);
    abstract void actorTargets(Actor a, Target other, Formation parent);
  }
  
  
  
  /**  Supplemental utility methods for setting objectives, demands and
    *  marching orders:
    */
  void assignDemands(
    City.POSTURE posture,
    Formation actionTaken,
    Tally <Good> tribute
  ) {
    this.postureDemand = posture;
    this.actionDemand  = actionTaken;
    this.tributeDemand = tribute == null ? new Tally() : tribute;
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
      this.exitsPoint = findTransitPoint(map, city);
      beginSecuring(exitsPoint, 0, city, map); //  Make this face the border!
    }
    else {
      beginJourney(homeCity, secureCity);
    }
  }
  
  
  void beginSecuring(Tile point, int facing, Object focus, CityMap map) {
    homeCity.formations.toggleMember(this, true);
    
    this.securePoint = point ;
    this.facing      = facing;
    this.secureFocus = focus ;
    this.active      = true  ;
    this.map         = map   ;
  }
  
  
  void disbandFormation() {
    homeCity.formations.toggleMember(this, false);
    
    this.secureCity  = null  ;
    this.securePoint = null  ;
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
    
    //  Update any tactical targets-
    if (away && map != null) {
      objective.updateTacticalTarget(this);
    }
    
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
    away        = true;
    map         = null;
    securePoint = null;
    secureFocus = null;
    entryPoint  = null;
    exitsPoint  = null;
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
      
      this.map = goes.map;
      this.entryPoint = findTransitPoint(map, journey.from);
      
      for (Actor w : recruits) {
        w.enterMap(map, entryPoint.x, entryPoint.y, 1);
      }
      beginSecuring(entryPoint, N, null, map);
      
      if (home) {
        this.away = false;
        disbandFormation();
      }
      else {
        this.away = true;
        objective.updateTacticalTarget(this);
      }
    }
    //
    //  In the event that this happens off-map, either an army has returned
    //  home to a foreign city, or an army has assaulted a foreign city:
    else {
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
  
  
  
  /**  Other utility methods:
    */
  boolean formationReady() {
    if (map == null) return true;
    if (securePoint == null || ! active) return false;
    for (Actor a : recruits) {
      if (objective.standLocation(a, this) != a.at()) {
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
  
  
  
  /**  Organising actors and other basic query methods-
    */
  public void selectActorBehaviour(Actor actor) {
    objective.selectActorBehaviour(actor, this);
  }
  
  
  public void actorUpdates(Actor actor) {
    objective.actorUpdates(actor, this);
  }
  
  
  public void actorTargets(Actor actor, Target other) {
    objective.actorTargets(actor, other, this);
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
  
  
  
  /**  Graphical, debug and interface methods-
    */
  boolean reports() {
    return false;
  }
  
  
  public String toString() {
    return "Formation ("+homeCity+")";
  }
}










