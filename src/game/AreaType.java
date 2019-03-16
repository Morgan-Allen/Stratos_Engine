

package game;
import game.World.*;
import graphics.common.*;
import util.*;



public class AreaType extends Constant {
  
  
  float mapX, mapY;
  Table <AreaType, Route> routes = new Table();
  boolean homeland = false;
  
  String label;
  String info;
  ImageAsset planetImage;
  
  
  
  public AreaType(Class baseClass, String ID, String label) {
    super(baseClass, ID, Type.IS_STORY);
    this.label = label;
  }
  
  
  public float mapX() { return mapX; }
  public float mapY() { return mapY; }
  public boolean homeland() { return homeland; }
  
  
  
  
  /**  Setting up routes and distance calculations-
    */
  public void initPosition(float mapX, float mapY, boolean homeland) {
    this.mapX = mapX;
    this.mapY  = mapY;
    this.homeland = homeland;
  }
  
  
  public static void setupRoute(AreaType a, AreaType b, int distance, int moveMode) {
    Route route = new Route();
    route.distance = distance;
    route.moveMode = moveMode;
    a.routes.put(b, route);
    b.routes.put(a, route);
  }
  
  
  /*
  public Area addArea(float mapX, float mapY, String label, boolean homeland) {
    Area l = new Area();
    l.mapX  = mapX;
    l.mapY  = mapY;
    l.label = label;
    l.homeland = homeland;
    this.locales.add(l);
    return l;
  }
  
  
  public Area addArea(float mapX, float mapY) {
    return addArea(mapX, mapY, "Locale at "+mapX+"|"+mapY, false);
  }
  //*/
  
  
  
  
  
  
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










