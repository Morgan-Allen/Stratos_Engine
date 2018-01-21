

package game;
import util.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



public class BuildingForWater extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  float fillLevel = 0;
  
  
  public BuildingForWater(Type type) {
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
    if (! complete()) return;
    
    
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
        Tile temp[] = new Tile[9];
        
        protected void addSuccessors(Element front) {
          int w = front.type.wide, h = front.type.high;
          Tile at = front.at();
          int high = at.elevation;
          
          if (w > 1 || h > 1) {
            for (Tile t : front.perimeter(map)) {
              tryAddingTile(t, high);
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
  
  
  Tile[] selectEntrances() {
    Tile at = this.at();
    List <Tile> all = new List();
    
    all.add(map.tileAt(at.x + (type.wide / 2), at.y + (type.high / 2)));
    all.add(tileAt(0, -1, N));
    all.add(tileAt(0, -1, E));
    all.add(tileAt(0, -1, S));
    all.add(tileAt(0, -1, W));
    
    for (Tile e : all) {
      if (e == null || ! checkEntranceOkay(e, -1)) all.remove(e);
    }
    return all.toArray(Tile.class);
  }
  


  /**  Rendering, debug and interface methods:
    */
  public int debugTint() {
    if (fillLevel > 0) return type.tint;
    return PAVE_COLOR;
  }
  
}






