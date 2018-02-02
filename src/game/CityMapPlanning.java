

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
    Type type = e.type();
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
      ///I.say("  REMOVING FROM BUILD LIST: "+e);
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
  
  
  public void placeObject(Element e) {
    toBuild.add(e);
  }
  
  
  public void placeObject(Element e, int x, int y, City owns) {
    e.setLocation(map.tileAt(x, y), map);
    if (e.type().isBuilding()) ((Building) e).assignHomeCity(owns);
    toBuild.add(e);
  }
  
  
  public void unplaceObject(Element e) {
    togglePlacement(e, false);
  }
  
  
  public Element objectAt(Tile t) {
    return grid[t.x][t.y];
  }
  
  
  public List <Element> toBuildCopy() {
    return toBuild.copy();
  }
  
  
  
  
  /**  These are calls made to the planning-map by any elements already
    *  present on the map:
    */
  private void updateBuildDemands(Element e) {
    //  TODO:  Migrate the 'check build need' function in here?
    
    for (Good m : e.type().builtFrom) {
      TaskBuilding.checkNeedForBuilding(e, m, map);
    }
  }
  
  
  public void updatePlanning() {
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
  public static Series <Element> placeStructure(
    Type s, City city, Box2D area, boolean built
  ) {
    return placeStructure(
      s, city, built,
      (int) area.xpos(), (int) area.ypos(),
      (int) area.xdim(), (int) area.ydim()
    );
  }
  
  
  public static Series <Element> placeStructure(
    Type s, City city, boolean built, int x, int y, int w, int h
  ) {
    Batch <Element> placed = new Batch();
    CityMap map = city.activeMap();
    for (Coord c : Visit.grid(x, y, w, h, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t == null) continue;
      Element e = (Element) s.generate();
      if (built) {
        e.enterMap(map, t.x, t.y, 1, city);
      }
      else {
        map.planning.placeObject(e, t.x, t.y, city);
      }
      placed.add(e);
    }
    return placed;
  }
  
  
  public static void markDemolish(
    CityMap map, boolean now, Box2D area
  ) {
    markDemolish(
      map, now,
      (int) area.xpos(), (int) area.ypos(),
      (int) area.xdim(), (int) area.ydim()
    );
  }
  
  
  public static void markDemolish(
    CityMap map, boolean now, int x, int y, int w, int h
  ) {
    for (Tile t : map.tilesUnder(x, y, w, h)) if (t != null) {
      Element plans = map.planning.objectAt(t);
      if (plans != null) {
        map.planning.unplaceObject(plans);
      }
      if (t.above != null && now) {
        t.above.exitMap(map);
      }
    }
  }
}







