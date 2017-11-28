

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class BuildingForWater extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  float fillLevel = 0;
  
  
  BuildingForWater(Type type) {
    super(type);
  }
  
  
  public BuildingForWater(Session s) throws Exception {
    super(s);
    fillLevel = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat(fillLevel);
  }
  
  
  
  /**  Life-cycle and regular updates:
    */
  void update() {
    super.update();
    
    fillLevel = inventory.valueFor(WATER) / 10f;
    
    //  TODO:  Obtain water from rainfall?
    boolean hasSource = false;
    
    Tile at = at();
    int high = at.elevation;
    
    for (Coord c : Visit.perimeter(at.x, at.y, type.wide, type.high)) {
      Tile t = map.tileAt(c);
      if (t == null || t.terrain == null) continue;
      
      if (t.terrain.isWater && t.elevation >= high) {
        hasSource = true;
      }
    }
    
    if (hasSource) {
      Series <Element> fills = new Flood <Element> () {
        Tile temp[] = new Tile[8];
        
        protected void addSuccessors(Element front) {
          int w = front.type.wide, h = front.type.high;
          Tile at = front.at();
          int high = at.elevation;
          
          if (w > 1 || h > 1) {
            for (Coord c : Visit.perimeter(at.x, at.y, w, h)) {
              tryAddingTile(map.tileAt(c), high);
            }
          }
          else for (Tile t : CityMap.adjacent(at, temp, map)) {
            tryAddingTile(t, high);
          }
        }
        
        private void tryAddingTile(Tile t, int fromHigh) {
          if (t == null || t.above == null) return;
          if (! t.above.type.isWater      ) return;
          if (t.elevation > fromHigh      ) return;
          tryAdding(t.above);
        }
      }.floodFrom(this);
      
      for (Element e : fills) if (e.type.isBuilding()) {
        Building b = (Building) e;
        b.inventory.set(WATER, 10);
      }
    }
  }
  
  
  
  /**  Rendering, debug and interface methods:
    */
  public int debugTint() {
    if (fillLevel > 0) return type.tint;
    return PAVE_COLOR;
  }
  
}





