

package game;
import static game.CityMap.*;
import util.*;



public class Formation implements Session.Saveable, World.Journeys {
  
  
  /**  Data fields, setup and save/load methods-
    */
  List <Walker> soldiers = new List();
  
  CityMap map;
  Building garrison;
  
  boolean active = false;
  City belongs;
  Tile securedPoint;
  City securedCity;
  
  
  
  Formation() {
    return;
  }
  
  
  public Formation(Session s) throws Exception {
    s.cacheInstance(this);
    
    s.loadObjects(soldiers);
    map          = (CityMap ) s.loadObject();
    garrison     = (Building) s.loadObject();
    active       = s.loadBool();
    belongs      = (City) s.loadObject(); 
    securedPoint = Tile.loadTile(map, s);
    securedCity  = (City) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObjects(soldiers);
    s.saveObject(map);
    s.saveObject(garrison);
    s.saveBool(active);
    s.saveObject(belongs);
    Tile.saveTile(securedPoint, map, s);
    s.saveObject(securedCity);
  }
  
  
  
  /**  Issuing specific marching orders-
    */
  void beginSecuring(Tile point) {
    this.securedPoint = point;
    this.active       = true ;
  }
  
  
  void beginSecuring(City city) {
    this.securedCity = city;
    this.active      = true;
  }
  
  
  //  Now you need to have all actors in the formation converge on a specific
  //  location & facing according to their rank within the formation.
  
  Tile standLocation(Walker member) {
    //  TODO- that...
    
    return null;
  }
  
  
  
  
  
  
  /**  Off-map invasions or relief operations:
    */
  public void onArrival(City goes, World.Journey journey) {
    
  }
  
  
  
}









