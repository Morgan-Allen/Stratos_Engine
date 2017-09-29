

package game;
import static game.GameConstants.*;
import static game.CityMap.*;
import util.*;



public class Formation implements
  Session.Saveable, Journeys, TileConstants, Employer
{
  
  /**  Data fields, setup and save/load methods-
    */
  //List <Walker> soldiers = new List();
  
  CityMap map;
  BuildingForMilitary garrison;
  
  boolean active = false;
  City belongs;
  Tile securedPoint;
  int  facing;
  City securedCity;
  
  
  
  Formation() {
    return;
  }
  
  
  public Formation(Session s) throws Exception {
    s.cacheInstance(this);
    
    //s.loadObjects(soldiers);
    map          = (CityMap) s.loadObject();
    garrison     = (BuildingForMilitary) s.loadObject();
    active       = s.loadBool();
    belongs      = (City) s.loadObject(); 
    securedPoint = Tile.loadTile(map, s);
    facing       = s.loadInt();
    securedCity  = (City) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    //s.saveObjects(soldiers);
    s.saveObject(map);
    s.saveObject(garrison);
    s.saveBool(active);
    s.saveObject(belongs);
    Tile.saveTile(securedPoint, map, s);
    s.saveInt(facing);
    s.saveObject(securedCity);
  }
  
  
  
  /**  Issuing specific marching orders-
    */
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
    Tile stands = standLocation(w);
    if (stands == null) return;
    
    w.embarkOnTarget(stands, 0, Walker.JOB.MILITARY, this);
  }
  
  
  public void walkerPasses(Walker walker, Building other) {
    return;
  }
  
  
  public void walkerTargets(Walker walker, Tile other) {
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
    
    int index = garrison.enlisted.indexOf(member);
    if (index == -1) return null;
    
    int ranks = garrison.type.numRanks;
    int file  = garrison.type.numFile ;
    
    int x = index % file;
    int y = index / file;
    x += c.x - (file  / 2);
    y += c.y + (ranks / 2);
    
    return map.tileAt(x, y);
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


