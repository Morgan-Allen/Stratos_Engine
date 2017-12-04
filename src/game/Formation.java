

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
  public static enum OBJECTIVE {
    RAZE_ENEMY   ,
    BATTLE_ENEMY ,
    CAPTURE_ENEMY,  // war-ish objectives
    ESCORT_ENVOY ,
    ESCORT_TRADER,
    PATROL_AREA  ,  // peace-ish objectives
  };
  
  Type type;  //  TODO:  This might not be required?
  OBJECTIVE object = OBJECTIVE.BATTLE_ENEMY;
  City.POSTURE postureDemand = null;
  Formation    actionDemand  = null;
  Tally <Good> tributeDemand = new Tally();
  
  List <Actor> escorted = new List();
  List <Actor> recruits = new List();
  
  boolean away   = false;
  City    homeCity    ;
  City    securedCity ;
  CityMap map         ;

  boolean active = false;
  Tile    securedPoint;
  Object  objective   ;
  int     facing      ;
  
  
  Formation() {
    return;
  }
  
  
  public Formation(Session s) throws Exception {
    s.cacheInstance(this);
    
    type          = (Type        ) s.loadObject();
    object        = (OBJECTIVE   ) s.loadEnum(OBJECTIVE   .values());
    postureDemand = (City.POSTURE) s.loadEnum(City.POSTURE.values());
    actionDemand  = (Formation   ) s.loadObject();
    s.loadTally(tributeDemand);
    
    s.loadObjects(escorted);
    s.loadObjects(recruits);
    
    away         = s.loadBool();
    homeCity     = (City   ) s.loadObject();
    securedCity  = (City   ) s.loadObject();
    map          = (CityMap) s.loadObject();
    
    active       = s.loadBool();
    securedPoint = loadTile(map, s);
    objective    = s.loadObject();
    facing       = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(type         );
    s.saveEnum  (object       );
    s.saveEnum  (postureDemand);
    s.saveObject(actionDemand );
    s.saveTally (tributeDemand);
    
    s.saveObjects(escorted);
    s.saveObjects(recruits);
    
    s.saveBool  (away       );
    s.saveObject(homeCity   );
    s.saveObject(securedCity);
    s.saveObject(map        );
    
    s.saveBool(active);
    saveTile(securedPoint, map, s);
    s.saveObject(objective);
    s.saveInt(facing);
  }
  
  
  
  /**  Issuing specific marching orders on the current map-
    */
  void setupFormation(Type type, City belongs) {
    this.type    = type;
    this.homeCity = belongs;
    this.map     = belongs.map;
    belongs.formations.add(this);
  }
  
  
  void disband() {
    for (Actor r : recruits) toggleRecruit(r, false);
  }
  
  
  void toggleRecruit(Actor s, boolean is) {
    this.recruits.toggleMember(s, is);
    if (is) s.formation = this;
    else    s.formation = null;
  }
  
  
  void beginSecuring(Tile point, int facing, Object objective) {
    this.securedPoint = point    ;
    this.facing       = facing   ;
    this.objective    = objective;
    this.active       = true     ;
  }
  
  
  void stopSecuringPoint() {
    this.securedCity  = null  ;
    this.securedPoint = null  ;
    this.active       = false ;
    this.facing       = CENTRE;
  }
  
  
  void update() {
    if (away && map != null) {
      updateTacticalTarget();
    }
    //  This is a hacky way of saying "I want to go to a different city, is
    //  everyone ready yet?"
    if (
      map != null && securedCity != null &&
      securedCity != map.city && formationReady()
    ) {
      beginJourney(map.city, securedCity);
    }
  }
  
  
  
  /**  Methods for handling treatment of foreign cities-
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
  
  
  void beginSecuring(City city) {
    this.securedCity = city;
    
    if (this.map != null) {
      Tile exits = findTransitPoint(map, city);
      beginSecuring(exits, 0, city); //  Make this face the border!
    }
    else {
      beginJourney(homeCity, securedCity);
    }
  }
  
  
  World.Journey beginJourney(City from, City goes) {
    if (reports()) I.say("\nREADY TO BEGIN FOREIGN MISSION!");
    for (Actor w : recruits) if (w.onMap(map)) {
      w.exitMap(map);
    }
    away = true;
    map  = null;
    securedPoint = null;
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
      Tile entry = findTransitPoint(map, journey.from);
      for (Actor w : recruits) {
        w.enterMap(map, entry.x, entry.y, 1);
      }
      beginSecuring(entry, N, null);
      
      if (home) {
        this.away = false;
        stopSecuringPoint();
      }
      else {
        this.away = true;
        updateTacticalTarget();
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
  
  
  public City homeCity() {
    return homeCity;
  }
  
  
  
  /**  Other utility methods:
    */
  Tile standLocation(Actor member) {
    
    Tile c = this.securedPoint;
    if (c == null) return null;
    
    int index = recruits.indexOf(member);
    if (index == -1) return null;
    
    int ranks = type.numRanks;
    int file  = type.numFile ;
    int x     = index % file ;
    int y     = index / file ;
    x += c.x - (file  / 2);
    y += c.y + (ranks / 2);
    
    x = Nums.clamp(x, map.size);
    y = Nums.clamp(y, map.size);
    
    return map.tileAt(x, y);
  }
  
  
  boolean hostile(Actor a, Actor b) {
    City CA = a.homeCity, CB = b.homeCity;
    if (CA == null) CA = map.city;
    if (CB == null) CB = map.city;
    if (CA == CB  ) return false;
    POSTURE r = CA.posture(CB);
    if (r == POSTURE.ENEMY) return true;
    return false;
  }
  
  
  boolean formationReady() {
    if (map == null) return true;
    if (securedPoint == null || ! active) return false;
    for (Actor w : recruits) {
      if (standLocation(w) != w.at()) return false;
    }
    return true;
  }
  
  
  int formationPower() {
    float sumStats = 0;
    for (Actor w : recruits) {
      if (w.state >= Actor.STATE_DEAD) continue;
      float stats = w.type.attackScore + w.type.defendScore;
      stats /= AVG_ATTACK + AVG_DEFEND;
      stats *= 1 - ((w.injury + w.hunger) / w.type.maxHealth);
      sumStats += stats;
    }
    return (int) (sumStats * POP_PER_CITIZEN);
  }
  
  
  Actor findCombatTarget(Actor member) {
    if (map == null) return null;
    Pick <Actor> pick = new Pick();
    
    float seeBonus = type.numFile;
    float range = member.type.sightRange + seeBonus;
    
    Series <Actor> others = map.actors;
    if (others.size() > 100 || true) {
      others = map.actorsInRange(member.at(), range);
    }
    
    for (Actor other : others) if (hostile(other, member)) {
      float distW = CityMap.distance(other.at(), member.at() );
      float distF = CityMap.distance(other.at(), securedPoint);
      if (distF > range + 1) continue;
      if (distW > range + 1) continue;
      pick.compare(other, 0 - distW);
    }
    
    return pick.result();
  }
  
  
  Tile findSiegeTarget(Actor member) {
    if (securedPoint == null || ! (securedPoint.above instanceof Building)) {
      return null;
    }
    
    Building sieged = (Building) securedPoint.above;
    Tile c = sieged.at();
    Pick <Tile> pick = new Pick();
    
    for (Coord p : Visit.perimeter(
      c.x, c.y, sieged.type.wide, sieged.type.high
    )) {
      if (map.blocked(p.x, p.y)) continue;
      Tile best = null;
      
      for (int dir : T_ADJACENT) {
        Tile tile = map.tileAt(p.x + T_X[dir], p.y + T_Y[dir]);
        if (tile == null || tile.above != sieged) continue;
        if (Task.hasTaskFocus(tile, Task.JOB.COMBAT)) continue;
        
        best = tile;
        break;
      }
      
      float dist = CityMap.distance(member.at(), best);
      pick.compare(best, 0 - dist);
    }
    
    Tile point = pick.result();
    if (point == null) return null;
    
    if (! (point.above instanceof Building)) {
      I.complain("PROBLEMMMM");
    }
    return point;
  }
  
  
  
  /**  Organising walkers-
    */
  boolean updateTacticalTarget() {
    
    if (objective instanceof Formation) {
      Formation opposed = (Formation) objective;
      Tile secures = opposed.securedPoint;
      int  power   = opposed.formationPower();
      if (secures == this.securedPoint && power > 0) return false;
    }
    
    if (objective instanceof Building) {
      Building sieged = (Building) objective;
      if (! sieged.destroyed()) return false;
    }
    
    if (objective instanceof City) {
      return false;
    }
    
    //
    //  If you're beaten, turn around and go home:
    //  TODO:  Allow for retreat at partial strength!
    if (formationPower() == 0) {
      City sieges = securedCity;
      CityEvents.enterHostility(sieges, homeCity, false, 1);
      CityEvents.signalVictory(sieges, homeCity, this);
      beginSecuring(homeCity);
      return true;
    }
    
    class Option { Object target; Tile secures; }
    Pick <Option> pick = new Pick();
    
    for (Formation f : map.city.formations) {
      if (f.away || f.securedPoint == null) continue;
      
      Option o = new Option();
      o.secures = f.securedPoint;
      o.target  = f;
      float dist = CityMap.distance(o.secures, securedPoint);
      pick.compare(o, 0 - dist);
    }
    
    for (Building b : map.buildings) {
      if (b.type.category != Type.IS_ARMY_BLD) continue;
      
      Option o = new Option();
      o.secures = b.centre();
      o.target  = b;
      float dist = CityMap.distance(o.secures, securedPoint);
      pick.compare(o, 0 - dist);
    }
    
    if (! pick.empty()) {
      Option o = pick.result();
      beginSecuring(o.secures, facing, o.target);
      return true;
    }
    //
    //  If there are no targets left here, turn around and go home.
    else {
      City sieges = securedCity;
      CityEvents.enterHostility(sieges, homeCity, true, 1);
      if (sieges != null && homeCity.government != GOVERNMENT.BARBARIAN) {
        CityEvents.inflictDemands(sieges, homeCity, this);
      }
      CityEvents.signalVictory(homeCity, sieges, this);
      //
      //  TODO:  Handle recall of forces in a separate decision-pass?
      beginSecuring(homeCity);
      return true;
    }
  }
  
  
  public void selectActorBehaviour(Actor w) {
    
    Actor target = w.inCombat() ? null : findCombatTarget(w);
    if (target != null) {
      w.beginAttack(target, Task.JOB.COMBAT, this);
      return;
    }
    
    Tile sieges = w.inCombat() ? null : findSiegeTarget(w);
    if (sieges != null) {
      w.beginAttack(sieges, Task.JOB.COMBAT, this);
      return;
    }
    
    Tile stands = standLocation(w);
    if (stands != null) {
      w.embarkOnTarget(stands, 10, Task.JOB.MILITARY, this);
      return;
    }
  }
  
  
  public void actorUpdates(Actor w) {
    Actor target = w.inCombat() ? null : findCombatTarget(w);
    if (target != null) {
      w.beginAttack(target, Task.JOB.COMBAT, this);
      return;
    }
  }
  
  
  public void actorTargets(Actor actor, Target other) {
    if (actor.inCombat() && other instanceof Actor) {
      actor.performAttack((Actor) other);
    }
    if (actor.inCombat() && other instanceof Tile) {
      Building siege = (Building) ((Tile) other).above;
      actor.performAttack(siege);
    }
    return;
  }
  
  
  public void actorPasses(Actor actor, Building other) {
    return;
  }
  
  
  public void actorVisits(Actor actor, Building visits) {
    return;
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










