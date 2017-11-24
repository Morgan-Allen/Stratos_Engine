

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class CityMapPlanning {
  
  
  final CityMap map;
  List <Element> toBuild = new List();
  Element grid[][];
  
  
  
  CityMapPlanning(CityMap map) {
    this.map  = map;
  }
  
  
  void loadState(Session s) throws Exception {
    s.loadObjects(toBuild);
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      grid[c.x][c.y] = (Element) s.loadObject();
    }
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveObjects(toBuild);
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      s.saveObject(grid[c.x][c.y]);
    }
  }
  
  
  void performSetup(int size) {
    this.grid = new Element[map.size][map.size];
  }
  
  
  
  /**  Support methods for object-placement within the plan:
    */
  private void togglePlacement(Element e, boolean doPlace) {
    if (e == null) return;
    //
    //  First, we compile a list of all elements that might interfere
    //  with this placement, either in the physical area or within the
    //  plan-map:
    Type type = e.type;
    Tile at   = e.at();
    Batch <Element> inArea = new Batch();
    Batch <Element> inPlan = new Batch();
    for (Coord c : Visit.grid(at.x, at.y, type.wide, type.high, 1)) {
      Element areaE = map.above(c);
      Element planE = grid[c.x][c.y];
      if (areaE != null && areaE != e) inArea.include(areaE);
      if (planE != null && planE != e) inPlan.include(planE);
    }
    //
    //  If we're placing a fresh element, give any prior elements a
    //  chance to remove themselves, marking the underlying grid
    //  accordingly-
    if (doPlace) for (Element i : inPlan) {
      togglePlacement(i, false);
    }
    else {
      toBuild.remove(e);
      I.say("  REMOVING FROM BUILD LIST: "+e);
    }
    for (Coord c : Visit.grid(at.x, at.y, type.wide, type.high, 1)) {
      grid[c.x][c.y] = doPlace ? e : null;
    }
    //
    //  If the ground is cleared (or you're already on the map,) start
    //  (or stop) signalling demands for building or salvage-
    if (inArea.empty() || e.onMap()) {
      updateBuildDemands(e);
    }
    for (Element i : inArea) {
      updateBuildDemands(i);
    }
  }
  
  
  Element placeObject(Type type, int x, int y) {
    return placeObject(type, map.tileAt(x, y));
  }
  
  
  Element placeObject(Type type, Tile t) {
    Element e = (Element) type.generate();
    e.setLocation(t);
    placeObject(e);
    return e;
  }
  
  
  void placeObject(Element e) {
    toBuild.add(e);
  }
  
  
  void unplaceObject(Element e) {
    togglePlacement(e, false);
  }
  
  
  Element objectAt(Tile t) {
    return grid[t.x][t.y];
  }
  
  
  
  
  /**  These are calls made to the planning-map by any elements already
    *  present on the map:
    */
  private void updateBuildDemands(Element e) {
    //  TODO:  Migrate the 'check build need' function in here?
    
    for (Good m : e.type.builtFrom) {
      TaskBuilding2.checkNeedForBuilding(e, m, map);
    }
  }
  
  
  void updatePlanning() {
    int maxChecked = Nums.min(100, toBuild.size());
    //
    //  We regularly 'check up' on scheduled structures to see whether
    //  they need maintenance or have become possible to build-
    for (int i = maxChecked; i-- > 0;) {
      Element e = toBuild.removeFirst();
      if (e.buildLevel() < 1 || ! e.onMap()) {
        togglePlacement(e, true);
      }
      toBuild.addLast(e);
    }
  }
  
  

  
  /**  Some helper methods for dealing with infrastructure:
    */
  public static void applyPaving(
    CityMap map, int x, int y, int w, int h, boolean is
  ) {
    for (Coord c : Visit.grid(x, y, w, h, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t == null) continue;
      if (is) {
        Element e = (Element) ROAD.generate();
        e.enterMap(map, x, y, 1);
      }
      else if (t.aboveType() == ROAD) {
        t.above.exitMap(map);
      }
    }
  }
  
  
  public static void demolish(
    CityMap map, int x, int y, int w, int h
  ) {
    for (Coord c : Visit.grid(x, y, w, h, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t       == null) continue;
      if (t.above != null) t.above.exitMap(map);
    }
  }
}







