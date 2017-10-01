

package game;
import util.*;
import static game.Walker.*;
import static game.GameConstants.*;
import static game.CityMap.*;
import static game.City.*;



public class Formation implements
  Session.Saveable, Journeys, TileConstants, Employer
{
  
  
  /**  Data fields, setup and save/load methods-
    */
  ObjectType type;
  List <Walker> recruits = new List();
  
  boolean active = false;
  boolean away   = false;
  City    belongs     ;
  City    securedCity ;
  CityMap map         ;
  Tile    securedPoint;
  int     facing      ;
  
  
  Formation() {
    return;
  }
  
  
  public Formation(Session s) throws Exception {
    s.cacheInstance(this);
    
    type = (ObjectType) s.loadObject();
    s.loadObjects(recruits);
    
    active       = s.loadBool();
    away         = s.loadBool();
    belongs      = (City   ) s.loadObject();
    securedCity  = (City   ) s.loadObject();
    map          = (CityMap) s.loadObject();
    securedPoint = loadTile(map, s);
    facing       = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(type);
    s.saveObjects(recruits);
    
    s.saveBool(active);
    s.saveBool(away  );
    s.saveObject(belongs    );
    s.saveObject(securedCity);
    s.saveObject(map        );
    saveTile(securedPoint, map, s);
    s.saveInt(facing);
  }
  
  
  
  /**  Issuing specific marching orders-
    */
  void setupFormation(ObjectType type, City belongs) {
    this.type    = type;
    this.belongs = belongs;
    this.map     = belongs.map;
    belongs.formations.add(this);
  }
  
  
  void toggleRecruit(Walker s, boolean is) {
    this.recruits.toggleMember(s, is);
    if (is) s.formation = this;
    else    s.formation = null;
  }
  
  
  void beginSecuring(Tile point, int facing) {
    this.securedPoint = point ;
    this.facing       = facing;
    this.active       = true  ;
  }
  
  
  void stopSecuringPoint() {
    this.securedCity  = null  ;
    this.securedPoint = null  ;
    this.active       = false ;
    this.facing       = CENTRE;
  }
  
  
  void update() {
    if ((! away) && securedCity != null && formationReady()) {
      beginJourneyTo(securedCity);
    }
  }
  
  
  
  /**  Methods for handling treatment of foreign cities-
    */
  void beginSecuring(City city) {
    this.securedCity = city;
    
    if (this.map != null) {
      Tile exits = WalkerForTrade.findTransitPoint(map, city);
      beginSecuring(exits, 0); //  Make this face the border!
    }
    else {
      beginJourneyTo(securedCity);
    }
  }
  
  
  void beginJourneyTo(City goes) {
    I.say("\nREADY TO BEGIN FOREIGN MISSION!");
    for (Walker w : recruits) if (w.onMap(map)) {
      w.exitMap();
    }
    goes.world.beginJourney(map.city, goes, this);
    away = true;
  }
  
  
  public void onArrival(City goes, World.Journey journey) {
    //
    //  There are 4 cases here: arriving home or away, and arriving on a map or
    //  not.
    boolean home  = goes == belongs;
    boolean onMap = goes.map != null;
    
    if (onMap) {
      I.say("\nARRIVED ON MAP: "+goes+" FROM "+journey.from);
      
      this.map = goes.map;
      Tile entry = WalkerForTrade.findTransitPoint(map, journey.from);
      for (Walker w : recruits) {
        w.enterMap(map, entry.x, entry.y);
      }
      beginSecuring(entry, N);
      
      if (home) {
        this.away = false;
        stopSecuringPoint();
      }
      else {
        pickTacticalTarget();
      }
    }
    //
    //  In the event that this happens off-map, either a foreign army has
    //  returned home, or your own army has hit a foreign city:
    else {
      if (home) {
        I.say("\nARRIVED HOME: "+goes+" FROM "+journey.from);
        this.away = false;
      }
      else {
        I.say("\nARRIVED AT FOREIGN CITY: "+goes+" FROM "+journey.from);
        attendCityAway(goes, journey);
      }
    }
  }
  
  
  void attendCityAway(City goes, World.Journey journey) {
    City  from      = journey.from;
    float power     = formationPower();
    float cityPower = goes.armyPower;
    
    float chance = 0, casualties = 0, origTotal = recruits.size();
    boolean victory = false;
    chance     = power / (power + cityPower);
    chance     = Nums.clamp((chance * 2) - 0.5f, 0, 1);
    casualties = Rand.num() + (1 - chance);
    
    if (Rand.num() < chance) {
      setRelations(from, RELATION.LORD, goes, RELATION.VASSAL);
      casualties -= 0.25f;
      victory = true;
    }
    else {
      casualties += 0.25f;
      victory = false;
    }
    
    casualties *= recruits.size();
    while (casualties-- > 0 && ! recruits.empty()) {
      Walker lost = (Walker) Rand.pickFrom(recruits);
      recruits.remove(lost);
    }
    
    //
    //  TODO:  Consider staying to pacify the locals, depending on mission
    //  parameters, et cetera!
    
    I.say("\n"+this+" CONDUCTED ACTION AGAINST "+goes);
    I.say("  Victorious: "+victory);
    I.say("  Casualties: "+casualties / origTotal);
    
    stopSecuringPoint();
    goes.world.beginJourney(goes, from, this);
  }
  
  
  
  /**  Other utility methods:
    */
  Tile standLocation(Walker member) {
    
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
  
  
  boolean hostile(Walker a, Walker b) {
    City CA = a.homeCity, CB = b.homeCity;
    if (CA == null) CA = map.city;
    if (CB == null) CB = map.city;
    if (CA == CB) return false;
    RELATION r = CA.relations.get(CB);
    if (r == RELATION.ENEMY) return true;
    return false;
  }
  
  
  boolean formationReady() {
    if (securedPoint == null || ! active) return false;
    if (recruits.empty()) return false;
    
    for (Walker w : recruits) {
      if (standLocation(w) != w.at) return false;
    }
    return true;
  }
  
  
  int formationPower() {
    float sumStats = 0;
    for (Walker w : recruits) {
      if (w.state >= Walker.STATE_DEAD) continue;
      float stats = w.type.attackScore + w.type.defendScore;
      stats *= 1 - ((w.injury + w.hunger) / w.type.maxHealth);
      sumStats += stats;
    }
    return (int) sumStats;
  }
  
  
  Walker findCombatTarget(Walker member) {
    Pick <Walker> pick = new Pick();
    
    //  TODO:  Allow for targeting of anything noticed by other members of the
    //  team?
    float seeBonus = type.numFile;
    
    for (Walker w : map.walkers) if (hostile(w, member)) {
      float distW = CityMap.distance(member.at, w.at);
      float distF = CityMap.distance(w.at, securedPoint);
      float range = member.type.sightRange + seeBonus;
      if (distF > range + 1) continue;
      if (distW > range + 1) continue;
      pick.compare(w, 0 - distW);
    }
    
    return pick.result();
  }
  
  
  Tile findSiegeTarget(Walker member) {
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
        if (tile == null || tile.above != sieged || tile.hasFocus()) continue;
        best = tile;
        break;
      }
      
      float dist = CityMap.distance(member.at(), best);
      pick.compare(best, 0 - dist);
    }
    
    if (! (pick.result().above instanceof Building)) {
      I.complain("PROBLEMMMM");
    }
    
    return pick.result();
  }
  
  
  
  /**  Organising walkers-
    */
  void pickTacticalTarget() {
    
    if (formationPower() == 0) {
      beginSecuring(belongs);
      return;
    }
    
    Pick <Tile> pick = new Pick();
    
    for (Formation f : map.city.formations) {
      if (f.away || f.securedPoint == null) continue;
      
      float dist = CityMap.distance(f.securedPoint, securedPoint);
      pick.compare(f.securedPoint, 0 - dist);
    }
    
    for (Building b : map.buildings) {
      if (b.type.category != ObjectType.IS_ARMY_BLD) continue;
      
      float dist = CityMap.distance(b.centre(), securedPoint);
      pick.compare(b.centre(), 0 - dist);
    }
    
    if (! pick.empty()) {
      beginSecuring(pick.result(), facing);
    }
    //
    //  If there are no targets left here, turn around and go home.
    else {
      City.setRelations(belongs, RELATION.LORD, securedCity, RELATION.VASSAL);
      beginSecuring(belongs);
    }
  }
  
  
  public void selectWalkerBehaviour(Walker w) {
    
    Walker target = w.inCombat() ? null : findCombatTarget(w);
    if (target != null) {
      w.beginAttack(target, JOB.COMBAT, this);
      return;
    }
    
    Tile sieges = w.inCombat() ? null : findSiegeTarget(w);
    if (sieges != null) {
      w.beginAttack(sieges, JOB.COMBAT, this);
      return;
    }
    
    Tile stands = standLocation(w);
    if (stands != null) {
      w.embarkOnTarget(stands, 10, JOB.MILITARY, this);
      return;
    }
  }
  
  
  public void walkerUpdates(Walker w) {
    Walker target = w.inCombat() ? null : findCombatTarget(w);
    if (target != null) {
      w.beginAttack(target, JOB.COMBAT, this);
      return;
    }
  }
  
  
  public void walkerTargets(Walker walker, Target other) {
    if (walker.inCombat() && other instanceof Walker) {
      walker.performAttack((Walker) other);
    }
    if (walker.inCombat() && other instanceof Tile) {
      Building siege = (Building) ((Tile) other).above;
      walker.performAttack(siege);
    }
    return;
  }
  
  
  public void walkerPasses(Walker walker, Building other) {
    return;
  }
  
  
  public void walkerEnters(Walker walker, Building enters) {
    return;
  }
  
  
  public void walkerVisits(Walker walker, Building visits) {
    return;
  }
  
  
  public void walkerExits(Walker walker, Building enters) {
    return;
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  public String toString() {
    return "Formation ("+belongs+")";
  }
}










