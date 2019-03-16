

package game;
import game.World.Route;
import graphics.common.*;
import util.*;



//  TODO:  Make this a constant?  You may need to store local visitors, is the
//  problem.  And stuff like terraforming or associated scenarios as well.



public class WorldLocale implements Session.Saveable {
  
  
  float mapX, mapY;
  Table <WorldLocale, Route> routes = new Table();
  boolean homeland = false;
  
  String label;
  ImageAsset planetImage;
  
  
  List <Actor> visitors = new List();
  private boolean active;
  private Area map;
  
  
  
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
    
    s.loadObjects(visitors);
    active = s.loadBool();
    map    = (Area) s.loadObject();
    
    
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
    
    s.saveObjects(visitors);
    s.saveBool(active);
    s.saveObject(map);
    
    
    s.saveString(label);
  }
  
  public float mapX() { return mapX; }
  public float mapY() { return mapY; }
  public boolean homeland() { return homeland; }
  
  
  
  
  public void attachMap(Area map) {
    this.map    = map;
    this.active = map == null ? false : true;
  }
  
  
  public Area activeMap() {
    return map;
  }
  
  
  public boolean isOffmap() {
    return map == null;
  }
  
  
  
  /**  Methods for handling traders and migrants-
    */
  void updateLocale() {
    for (Actor a : visitors) if (! a.onMap()) {
      a.updateOffMap(this);
    }
  }
  
  
  public void toggleVisitor(Actor visitor, boolean is) {
    
    WorldLocale offmap = visitor.offmap();
    if (offmap != this && ! is) return;
    if (offmap == this &&   is) return;
    if (offmap != null &&   is) offmap.toggleVisitor(visitor, false);
    
    visitors.toggleMember(visitor, is);
    visitor.setOffmap(is ? this : null);
  }
  
  
  public Series <Actor> visitors() {
    return visitors;
  }
  
  
  
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









