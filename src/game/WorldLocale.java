

package game;
import game.World.Route;
import graphics.common.*;
import util.*;



//  TODO:  Make this a constant!  You can set up a World with a set of locales.


public class WorldLocale implements Session.Saveable {
  
  
  float mapX, mapY;
  Table <WorldLocale, Route> routes = new Table();
  boolean homeland = false;
  
  String label;
  ImageAsset planetImage;
  
  
  
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
    
    label = s.loadString();
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
    
    s.saveString(label);
  }
  
  public float mapX() { return mapX; }
  public float mapY() { return mapY; }
  public boolean homeland() { return homeland; }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  public String name() {
    return label;
  }
  
  
  public String toString() {
    return label;
  }
  
  
  public ImageAsset planetImage() {
    return planetImage;
  }
}









