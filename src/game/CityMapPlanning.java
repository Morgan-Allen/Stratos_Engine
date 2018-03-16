

package game;
import util.*;
import static game.AreaMap.*;
import static game.GameConstants.*;




public class CityMapPlanning {
  
  
  final AreaMap map;
  List <Element> toBuild = new List();
  Element grid[][];
  byte reserveCounter[][];
  
  
  
  CityMapPlanning(AreaMap map) {
    this.map  = map;
  }
  
  
  void loadState(Session s) throws Exception {
    s.loadObjects(toBuild);
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      grid[c.x][c.y] = (Element) s.loadObject();
    }
    s.loadByteArray(reserveCounter);
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveObjects(toBuild);
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      s.saveObject(grid[c.x][c.y]);
    }
    s.saveByteArray(reserveCounter);
  }
  
  
  void performSetup(int size) {
    this.grid = new Element[map.size][map.size];
    this.reserveCounter = new byte[map.size][map.size];
  }
  
  
  
  /**  These are calls made to the planning-map by any elements already
    *  present on the map:
    */
  private void updateBuildDemands(Element e) {
    //  TODO:  Migrate the 'check build need' function in here?
    for (Good m : e.materials()) {
      TaskBuilding.checkNeedForBuilding(e, m, map, true);
    }
  }
  
  
  public void updatePlanning() {
    if (toBuild.empty()) return;
    //
    //  Assess what a reasonable fraction of structures to update would be-
    int maxChecked = Nums.min(100, toBuild.size()) / map.ticksPS;
    maxChecked = Nums.max(1, maxChecked);
    //
    //  We regularly 'check up' on scheduled structures to see whether they
    //  need maintenance or have become possible to build-
    for (int i = maxChecked; i-- > 0;) {
      Element e = toBuild.removeFirst();
      if (e.buildLevel() < 1 || ! e.onMap()) placeObject(e);
      toBuild.addLast(e);
    }
  }
  
  
  
  /**  Support methods for object-placement within the plan:
    */
  public void placeObject(Element e) {
    
    //
    //  NOTE:  This method may be called repeatedly, even after initial
    //  placement, to check for conditions that allow construction to actually
    //  proceed (i.e, once all conflicting elements have been removed.)
    
    boolean unplaced = ! e.onPlan();
    boolean reserves = ! e.type().isClearable();
    Type type = e.type();
    Batch <Element> conflicts = new Batch();
    
    if (unplaced) {
      toBuild.add(e);
      e.setOnPlan(true);
    }
    
    e.checkPlacingConflicts(map, conflicts);

    if (unplaced) for (Tile t : e.footprint(map, true)) {
      int footMask = type.footprint(t, e);
      if (footMask == -1) continue;
      
      if (reserves) {
        reserveCounter[t.x][t.y] += 1;
      }
      if (footMask == 1) {
        grid[t.x][t.y] = e;
      }
    }
    
    for (Element i : conflicts) {
      if (i.onPlan()) unplaceObject(i);
      if (i.onMap()) updateBuildDemands(i);
    }
    if (conflicts.empty() || e.onMap()) {
      updateBuildDemands(e);
    }
  }
  
  
  public void unplaceObject(Element e) {
    if (! e.onPlan()) return;
    ///I.say("UNPLACING: "+e);
    
    boolean reserves = ! e.type().isClearable();
    Type type = e.type();
    
    toBuild.remove(e);
    e.setOnPlan(false);
    
    for (Tile t : e.footprint(map, true)) {
      int footMask = type.footprint(t, e);
      if (footMask == -1) continue;
      
      if (reserves) {
        reserveCounter[t.x][t.y] -= 1;
      }
      if (footMask == 1) {
        Element onPlan = grid[t.x][t.y];
        if (onPlan == e) grid[t.x][t.y] = null;
      }
    }
    
    updateBuildDemands(e);
  }
  
  
  public void placeObject(Element e, int x, int y, Base owns) {
    e.setLocation(map.tileAt(x, y), map);
    e.assignBase(owns);
    placeObject(e);
  }
  
  
  public Element objectAt(Tile t) {
    return grid[t.x][t.y];
  }
  
  
  public List <Element> toBuildCopy() {
    return toBuild.copy();
  }
  
  
  
  /**  Some helper methods for dealing with infrastructure:
    */
  public static Series <Element> placeStructure(
    Type s, Base city, Box2D area, boolean built
  ) {
    return placeStructure(
      s, city, built,
      (int) area.xpos(), (int) area.ypos(),
      (int) area.xdim(), (int) area.ydim()
    );
  }
  
  
  public static Series <Element> placeStructure(
    Type s, Base city, boolean built, int x, int y, int w, int h
  ) {
    Batch <Element> placed = new Batch();
    AreaMap map = city.activeMap();
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
    AreaMap map, boolean now, Box2D area
  ) {
    markDemolish(
      map, now,
      (int) area.xpos(), (int) area.ypos(),
      (int) area.xdim(), (int) area.ydim()
    );
  }
  
  
  public static void markDemolish(
    AreaMap map, boolean now, int x, int y, int w, int h
  ) {
    for (Tile t : map.tilesUnder(x, y, w, h)) if (t != null) {
      Element plans = map.planning.objectAt(t);
      if (t.above != null && now) {
        t.above.exitMap(map);
      }
      else if (plans != null) {
        map.planning.unplaceObject(plans);
      }
    }
  }
}



