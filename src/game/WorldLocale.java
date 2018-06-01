

package game;
import game.World.Route;
import util.*;



//  TODO:  Have this extend Constant instead.


public class WorldLocale implements Session.Saveable {
  
  
  float mapX, mapY;
  Table <WorldLocale, Route> routes = new Table();
  
  String label;
  
  public float mapX() { return mapX; }
  public float mapY() { return mapY; }
  public String toString() { return label; }
  
  
  
  WorldLocale() {
  }
  
  
  public WorldLocale(Session s) throws Exception {
    s.cacheInstance(this);
    
    mapX = s.loadFloat();
    mapY = s.loadFloat();
    
    for (int d = s.loadInt(); d-- > 0;) {
      WorldLocale with = (WorldLocale) s.loadObject();
      Route route = new Route();
      route.distance = s.loadInt();
      route.moveMode = s.loadInt();
      routes.put(with, route);
    }
  }
  
  
  public void saveState(Session s) throws Exception {

    s.saveFloat(mapX);
    s.saveFloat(mapY);

    s.saveInt(routes.size());
    for (WorldLocale d : routes.keySet()) {
      s.saveObject(d);
      Route route = routes.get(d);
      s.saveInt(route.distance);
      s.saveInt(route.moveMode);
    }
  }
  
  
}









