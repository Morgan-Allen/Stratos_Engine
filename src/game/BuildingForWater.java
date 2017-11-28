

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class BuildingForWater extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  BuildingForWater(Type type) {
    super(type);
  }
  
  
  public BuildingForWater(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Life-cycle and regular updates:
    */
  void update() {
    super.update();
    
    //  TODO:  Obtain water from either rainfall or adjacent open fresh-
    //  water sources.
    
    Tile at = at();
    for (Coord c : Visit.perimeter(at.x, at.y, type.wide, type.high)) {
      Tile t = map.tileAt(c);
      if (t == null) continue;
      
      
    }
    
    //  TODO:  Propagate water to any downhill aqueduct-structures that
    //  need it.
    
    ///inventory.set(WATER, 10);
  }
  
  
  static Series <Element> propagateFlow(Element origin) {
    
    CityMap map = origin.map;
    Tile temp[] = new Tile[8];
    
    final Batch <Element> inFlow = new Batch();
    final List <Element> frontier = new List();
    frontier.add(origin);
    
    class Adds {
      void tryAddingFrom(Tile t) {
        if (t == null || t.above == null) return;
        Element e = t.above;
        if (e.pathFlag == inFlow) return;
        if (! e.type.isWater   ) return;
        frontier.add(e);
      }
    }
    Adds adding = new Adds();
    
    while (frontier.size() > 0) {
      
      Element front = frontier.removeFirst();
      inFlow.add(front);
      front.pathFlag = inFlow;
      
      Tile at = front.at();
      int w = front.type.wide, h = front.type.high;
      
      if (w > 1 || h > 1) {
        for (Coord c : Visit.perimeter(at.x, at.y, w, h)) {
          adding.tryAddingFrom(map.tileAt(c));
        }
      }
      else for (Tile t : CityMap.adjacent(at, temp, map)) {
        adding.tryAddingFrom(t);
      }
    }
    
    for (Element e : inFlow) e.pathFlag = null;
    return inFlow;
  }
  
  
  
}



















