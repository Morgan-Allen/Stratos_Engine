

package game;
import util.*;
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
  
  
  
  /**  Organising walkers-
    */
  public void selectWalkerBehaviour(Walker w) {
    
    Walker target = findTarget(w);
    if (target != null) {
      w.beginAttack(target, Walker.JOB.MILITARY, this);
      return;
    }
    
    Tile stands = standLocation(w);
    if (stands != null) {
      w.embarkOnTarget(stands, 10, Walker.JOB.MILITARY, this);
      return;
    }
  }
  
  
  public void walkerPasses(Walker walker, Building other) {
    return;
  }
  
  
  public void walkerTargets(Walker walker, Target other) {
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
    City.RELATION r = CA.relations.get(b);
    if (r == City.RELATION.ENEMY) return true;
    return false;
  }
  
  
  Walker findTarget(Walker member) {
    Pick <Walker> pick = new Pick();
    
    //  TODO:  Allow for targeting of anything noticed by other members of the
    //  team!
    
    for (Walker w : map.walkers) if (hostile(w, member)) {
      float dist  = CityMap.distance(member.at, w.at);
      float range = member.type.sightRange;
      if (dist < range + 1) pick.compare(w, 0 - dist);
    }
    
    return pick.result();
  }
  

  
  /**  TODO:  Start implementing this!
    */
  void beginSecuring(City city) {
    this.securedCity = city;
    this.active      = true;
  }
  
  
  
  
  /**  Off-map invasions or relief operations:
    */
  public void onArrival(City goes, World.Journey journey) {
    
  }
  
  
  
}


