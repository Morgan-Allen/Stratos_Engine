

package game;
import util.*;
import static game.Walker.*;
import static game.GameConstants.*;
import static game.CityMap.*;



public class Formation implements
  Session.Saveable, Journeys, TileConstants, Employer
{
  
  
  /**  Data fields, setup and save/load methods-
    */
  ObjectType type;
  List <Walker> recruits = new List();
  CityMap map;
  boolean active = false;
  Tile securedPoint;
  int  facing;
  City securedCity;
  
  
  
  Formation(ObjectType type) {
    this.type = type;
  }
  
  
  public Formation(Session s) throws Exception {
    s.cacheInstance(this);
    
    type = (ObjectType) s.loadObject();
    s.loadObjects(recruits);
    map          = (CityMap) s.loadObject();
    active       = s.loadBool();
    securedPoint = loadTile(map, s);
    facing       = s.loadInt();
    securedCity  = (City) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(type);
    s.saveObjects(recruits);
    s.saveObject(map);
    s.saveBool(active);
    saveTile(securedPoint, map, s);
    s.saveInt(facing);
    s.saveObject(securedCity);
  }
  
  
  
  /**  Issuing specific marching orders-
    */
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
    this.securedPoint = null  ;
    this.active       = false ;
    this.facing       = CENTRE;
  }
  
  
  boolean formationReady() {
    if ((! active) || securedPoint == null) return false;
    
    for (Walker w : recruits) {
      if (standLocation(w) != w.at) return false;
    }
    return true;
  }
  
  
  
  /**  Organising walkers-
    */
  public void selectWalkerBehaviour(Walker w) {
    
    Walker target = w.inCombat() ? null : findTarget(w);
    if (target != null) {
      w.beginAttack(target, JOB.COMBAT, this);
      return;
    }
    
    Tile stands = standLocation(w);
    if (stands != null) {
      w.embarkOnTarget(stands, 10, JOB.MILITARY, this);
      return;
    }
  }
  
  
  public void walkerUpdates(Walker w) {
    
    Walker target = w.inCombat() ? null : findTarget(w);
    if (target != null) {
      w.beginAttack(target, JOB.COMBAT, this);
      return;
    }
  }
  
  
  public void walkerTargets(Walker walker, Target other) {
    if (walker.inCombat()) {
      walker.performAttack((Walker) other);
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
    
    return map.tileAt(x, y);
  }
  
  
  boolean hostile(Walker a, Walker b) {
    City CA = a.homeCity, CB = b.homeCity;
    if (CA == null) CA = map.city;
    if (CB == null) CB = map.city;
    if (CA == CB) return false;
    City.RELATION r = CA.relations.get(CB);
    if (r == City.RELATION.ENEMY) return true;
    return false;
  }
  
  
  Walker findTarget(Walker member) {
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
  

  
  /**  TODO:  Start implementing this!
    */
  void beginSecuring(City city) {
    this.securedCity = city;
    this.active      = true;
    
    //  Tile exits = WalkerForTrade.findTransitPoint(map, city);
    //  etc...
  }
  
  
  
  
  /**  Off-map invasions or relief operations:
    */
  //  TODO:  You will also need to add some basic tactical AI- what to attack,
  //  and when to retreat.
  
  public void onArrival(City goes, World.Journey journey) {
    if (goes.map == null) {
      
    }
    else {
      this.map = goes.map;
      Tile entry = WalkerForTrade.findTransitPoint(map, journey.from);
      for (Walker w : recruits) {
        w.enterMap(map, entry.x, entry.y);
      }
    }
  }
  
  
  
}










