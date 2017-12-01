

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.Pathing;




public class CityMapPathCache {
  
  
  /**  Data fields, constants, construction and save/load methods-
    */
  final static int
    AREA_SIZE = 16
  ;
  
  static class Area implements Flood.Fill {
    
    int ID;
    boolean flagTiling   = false;
    boolean flagDeletion = false;
    
    int aX, aY;
    int numTiles;
    Tile tiles[];
    Batch <Tile> toAdd = new Batch(4);
    Batch <Tile> toRem = new Batch(4);
    
    List <Area> borders = new List();
    AreaGroup group;
    
    Object pathFlag = null;
    
    public void flagWith(Object o) { pathFlag = o; }
    public Object flaggedWith() { return pathFlag; }
    public String toString() { return "A_"+ID; }
  }
  
  static class AreaGroup {
    
    int ID;
    boolean flagDeletion = false;
    List <Area> areas = new List();
    
    public String toString() { return "G_"+ID; }
  }
  
  
  final CityMap map;
  Area areaLookup[][];
  boolean flagDirty[][];
  
  int nextAreaID  = 0;
  int nextGroupID = 0;
  List <Area     > areas  = new List();
  List <AreaGroup> groups = new List();
  
  List <Area> needRefresh = new List();
  List <Area> needDelete  = new List();
  
  private Pathing temp[] = new Pathing[8];
  private Tile tempForFrom[] = new Tile[8];
  private Tile tempForGoes[] = new Tile[8];
  
  
  CityMapPathCache(CityMap map) {
    this.map = map;
  }
  
  
  void performSetup(int size) {
    int dirtyGS = Nums.round(map.size * 1f / AREA_SIZE, 1, true);
    areaLookup = new Area[size][size];
    flagDirty  = new boolean[dirtyGS][dirtyGS];
  }
  
  
  void loadState(Session s) throws Exception {
    return;
  }
  
  
  void saveState(Session s) throws Exception {
    return;
  }
  
  
  
  /**  Query methods for distance and connection-
    */
  boolean pathConnects(Tile from, Tile goes) {
    if (from == null || goes == null) return false;
    Area fromA = areaFor(from);
    Area goesA = areaFor(goes);
    if (fromA == null || goesA == null) return false;
    AreaGroup fromG = groupFor(fromA);
    AreaGroup goesG = groupFor(goesA);
    if (fromG == null || goesG == null) return false;
    return fromG == goesG;
  }
  
  
  boolean pathConnects(Element from, Element goes) {
    if (from == null || goes == null) return false;
    
    //  TODO:
    //  You'll also need respectable methods of estimating distance.
    Tile fromA[] = around(from, tempForFrom);
    Tile goesA[] = around(goes, tempForGoes);
    
    for (Tile f : fromA) for (Tile g : goesA) {
      if (pathConnects(f, g)) return true;
    }
    return false;
  }
  
  
  private Tile[] around(Element e, Tile temp[]) {
    Tile at = e.at();
    Type t  = e.type;
    
    if (t.isBuilding()) {
      return ((Building) e).entrances();
    }
    else if (t.wide == 1 && t.high == 1) {
      return CityMap.adjacent(at, temp, map);
    }
    else {
      Batch <Tile> around = new Batch();
      for (Coord c : Visit.perimeter(at.x, at.y, t.wide, t.high)) {
        around.add(map.tileAt(c));
      }
      return around.toArray(Tile.class);
    }
  }
  
  
  
  /**  Methods for flagging changes and regular updates:
    */
  void checkPathingChanged(Tile at) {
    
    //  First, make sure there's some change to merit an update:
    Area core = areaLookup[at.x][at.y];
    boolean blocked = map.blocked(at.x, at.y);
    //if (blocked == (core == null)) return;
    
    //  Then set up some tracking variables-
    final int aX = at.x / AREA_SIZE;
    final int aY = at.y / AREA_SIZE;
    Area tail = null, head = null, edge = core;
    int numGaps = 0, gapFlag = -1;
    boolean multiArea = false, didRefresh = false;
    
    //  The plan is to circle this tile, checking how many areas it
    //  impinges on and whether there are multiple 'gaps' in area-
    //  adjacency- the latter might indicate a bottleneck in pathing
    //  that could potentially require a more comprehensive update (see
    //  below.)
    for (Pathing p : at.adjacent(temp, map)) {
      Tile n;
      if (p == null || ! p.isTile()) n = null;
      else n = (Tile) p;
      tail = n == null ? null : areaLookup[n.x][n.y];
      
      //  We don't merge with areas outside a given 16x16 unit:
      if (tail != null && (tail.aX != aX || tail.aY != aY)) {
        multiArea = true;
        tail = null;
      }
      //  Record the first area on the perimeter, null or otherwise-
      if (gapFlag == -1) {
        head = tail;
      }
      //  And check whether more than one area is bumped into, or any
      //  gaps in adjacency-
      if (tail != null && gapFlag != 1) {
        if (edge == null) {
          edge = tail;
        }
        else if (tail != edge) {
          multiArea = true;
          markForDeletion(tail);
        }
        gapFlag = 1;
      }
      if (tail == null && gapFlag != 0) {
        gapFlag = 0;
        numGaps += 1;
      }
    }
    //  Literal corner-case- if the perimeter starts and ends in a gap,
    //  don't count it twice.
    if (tail == head && head == null) {
      numGaps -= 1;
    }
    
    //  If possible, don't flag the area for deletion- just add or
    //  remove a single tile.  (Note that in the case of deleting a
    //  tile, there must not be a potential bottleneck.)
    if (edge != null && ! (multiArea || edge.flagDeletion)) {
      if (blocked && numGaps < 2) {
        areaLookup[at.x][at.y] = null;
        edge.toRem.add(at);
        markForRefresh(edge);
        didRefresh = true;
        if (map.settings.reportPathCache) {
          I.say("Removed single tile: "+at+" from "+edge);
        }
      }
      if (core == null && ! blocked) {
        areaLookup[at.x][at.y] = edge;
        edge.toAdd.add(at);
        markForRefresh(edge);
        didRefresh = true;
        if (map.settings.reportPathCache) {
          I.say("Added single tile: "+at+" to "+edge);
        }
      }
    }
    
    //  If a simple merge/delete operation wasn't possible, tear down
    //  and start over-
    if (edge != null && ! didRefresh) {
      markForDeletion(edge);
    }
    if (! didRefresh) {
      flagDirty[aX][aY] = true;
    }
  }
  
  
  private void markForDeletion(Area area) {
    if (area.flagDeletion) return;
    area.flagDeletion = true;
    needDelete.add(area);
    if (map.settings.reportPathCache) {
      I.say("\n  Flag To Delete: "+area);
    }
  }
  
  
  private void markForRefresh(Area area) {
    if (area.flagTiling) return;
    area.flagTiling = true;
    needRefresh.add(area);
    if (map.settings.reportPathCache) {
      I.say("\n  Flag To Refresh: "+area);
    }
  }
  
  
  private void refreshTiling(Area area) {
    if (area.toAdd.empty() && area.toRem.empty()) return;
    
    Batch <Tile> tiles = new Batch();
    for (Tile t : area.tiles) if (! area.toRem.includes(t)) tiles.add(t);
    Visit.appendTo(tiles, area.toAdd);
    
    area.toAdd.clear();
    area.toRem.clear();
    area.tiles = tiles.toArray(Tile.class);
    area.numTiles = area.tiles.length;
    area.flagTiling = false;
  }
  
  
  private void updateDirtyBounds() {
    int dirtyGS = flagDirty.length;
    for (Coord c : Visit.grid(0, 0, dirtyGS, dirtyGS, 1)) {
      if (! flagDirty[c.x][c.y]) continue;
      int aX = c.x * AREA_SIZE;
      int aY = c.y * AREA_SIZE;
      for (Coord t : Visit.grid(aX, aY, AREA_SIZE, AREA_SIZE, 1)) {
        Tile u = map.tileAt(t);
        if (u != null) areaFor(u);
      }
      flagDirty[c.x][c.y] = false;
    }
  }
  
  
  void updatePathCache() {
    
    for (Area a : needRefresh) {
      refreshTiling(a);
    }
    needRefresh.clear();
    
    for (Area a : needDelete) {
      deleteArea(a);
    }
    needDelete.clear();
    
    updateDirtyBounds();
    
    for (Area a : areas) {
      groupFor(a);
    }
  }
  
  
  
  /**  Querying and generating area-ownership:
    */
  Area rawArea(Tile t) {
    if (t == null) return null;
    return areaLookup[t.x][t.y];
  }
  
  
  Area areaFor(Tile t) {
    Area area = areaLookup[t.x][t.y];
    
    if (area != null && ! area.flagDeletion) {
      return area;
    }
    if (area != null && area.flagDeletion) {
      deleteArea(area);
      area = null;
    }
    
    if (map.blocked(t)) {
      return null;
    }
    
    final int aX = t.x / AREA_SIZE, aY = t.y / AREA_SIZE;
    final Batch <Tile> edging = new Batch();
    
    Series <Tile> covered = new Flood <Tile> () {
      void addSuccessors(Tile front) {
        for (Pathing p : front.adjacent(temp, map)) {
          if (p == null || p.flaggedWith() != null) continue;
          
          if (! p.isTile()) {
            Building b = (Building) p;
            for (Tile n : b.entrances()) if (n != p) {
              edging.add(n);
              n.pathFlag = edging;
            }
            continue;
          }
          
          Tile n = (Tile) p;
          if (n.x / AREA_SIZE != aX || n.y / AREA_SIZE != aY) {
            edging.add(n);
            n.pathFlag = edging;
            continue;
          }
          
          tryAdding(n);
        }
      }
    }.floodFrom(t);
    
    area = new Area();
    area.ID       = nextAreaID++;
    area.numTiles = covered.size();
    area.tiles    = covered.toArray(Tile.class);
    area.aX       = aX;
    area.aY       = aY;
    areas.add(area);
    
    for (Tile c : covered) {
      areaLookup[c.x][c.y] = area;
    }
    
    Batch <Area> bordering = new Batch();
    for (Tile e : edging) {
      e.pathFlag = null;
    }
    for (Tile e : edging) {
      Area b = areaFor(e);
      if (b != null && b != area) bordering.include(b);
    }
    for (Area b : bordering) {
      toggleBorders(area, b, true);
    }
    
    if (map.settings.reportPathCache) {
      I.say("\n  Adding Area: "+area+" "+area.aX+"|"+area.aY);
      I.say("    Tiles: "+area.numTiles+"  Borders: ");
      for (Area b : area.borders) I.add(b.ID+" ");
      if (area.borders.empty()) I.add("none");
      I.say("    All Tiles: "+I.list(area.tiles));
    }
    
    return area;
  }
  
  
  private void deleteArea(Area area) {
    if (map.settings.reportPathCache) {
      I.say("\n  Deleting Area "+area);
    }
    
    refreshTiling(area);
    for (Tile c : area.tiles) if (areaLookup[c.x][c.y] == area) {
      areaLookup[c.x][c.y] = null;
    }
    for (Area b : area.borders) {
      toggleBorders(area, b, false);
    }
    if (area.group != null) {
      deleteGroup(area.group);
    }
    
    area.tiles = null;
    areas.remove(area);
  }
  
  
  private void toggleBorders(Area a, Area b, boolean yes) {
    a.borders.toggleMember(b, yes);
    b.borders.toggleMember(a, yes);
  }
  
  
  
  /**  Querying and generating area-group membership:
    */
  AreaGroup groupFor(Area area) {
    AreaGroup group = area.group;
    if (group != null && ! group.flagDeletion) return group;
    
    if (group != null && group.flagDeletion) {
      deleteGroup(group);
      group = null;
    }
    
    Series <Area> covered = new Flood <Area> () {
      void addSuccessors(Area front) {
        for (Area n : front.borders) {
          if (n.flaggedWith() != null) continue;
          tryAdding(n);
        }
      }
    }.floodFrom(area);
    
    group = new AreaGroup();
    group.ID = nextGroupID++;
    Visit.appendTo(group.areas, covered);
    groups.add(group);
    
    for (Area a : covered) {
      a.pathFlag = null;
      a.group = group;
    }
    
    if (map.settings.reportPathCache) {
      I.say("\n  Adding Group "+group+": ");
      for (Area a : group.areas) I.add(a+" ");
    }
    
    return group;
  }
  
  
  private void deleteGroup(AreaGroup group) {
    if (map.settings.reportPathCache) {
      I.say("\n  Deleting Group "+group);
    }
    for (Area a : group.areas) if (a.group == group) {
      a.group = null;
    }
    groups.remove(group);
  }
  
}







