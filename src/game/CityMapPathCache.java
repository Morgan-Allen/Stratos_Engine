

package game;
import util.*;
import static game.CityMap.*;




public class CityMapPathCache {
  
  
  /**  Data fields, constants, construction and save/load methods-
    */
  final static int
    AREA_SIZE = 16
  ;
  
  static class AreaGroup {
    List <Area> areas = new List();
    boolean flagDeletion = false;
    int ID;
    
    public String toString() { return "G_"+ID; }
  }
  
  static class Area {
    
    int ID;
    
    int numTiles;
    Tile tiles[];
    Batch <Tile> toAdd = new Batch(4);
    Batch <Tile> toRem = new Batch(4);
    boolean flagTiling = false;
    
    //Box2D area = new Box2D();
    int aX, aY;
    AreaGroup group;
    List <Area> borders = new List();
    boolean flagDeletion = false;
    
    Object pathFlag = null;
    
    public String toString() { return "A_"+ID; }
  }
  
  
  final CityMap map;
  Area areaLookup[][];
  
  int nextAreaID = 0;
  int nextGroupID = 0;
  List <Area> areas = new List();
  List <AreaGroup> groups = new List();
  
  List <Area> needRefresh = new List();
  private Tile temp[] = new Tile[8];
  
  
  CityMapPathCache(CityMap map) {
    this.map = map;
  }
  
  void performSetup(int size) {
    areaLookup = new Area[size][size];
  }
  
  void loadState(Session s) throws Exception {
    
  }
  
  void saveState(Session s) throws Exception {
    
  }
  
  
  
  /**  Query methods for distance and connection-
    */
  boolean pathConnects(Tile from, Tile goes) {
    Area fromA = areaFor(from);
    Area goesA = areaFor(goes);
    if (fromA == null || goesA == null) return false;
    AreaGroup fromG = groupFor(fromA);
    AreaGroup goesG = groupFor(goesA);
    if (fromG == null || goesG == null) return false;
    return fromG == goesG;
  }
  
  
  
  /**  Methods for flagging changes and regular updates:
    */
  void flagPathingChanged(Tile at) {
    
    //  First, make sure there's some change to merit an update:
    Area core = areaLookup[at.x][at.y];
    boolean blocked = map.blocked(at.x, at.y);
    if (blocked == (core == null)) return;
    
    //  Then set up some tracking variables....
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
    for (Tile n : CityMap.adjacent(at, temp, map)) {
      if (n == null) continue;
      tail = areaLookup[n.x][n.y];
      
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
    
    //  If possible, don't flag the area for deletion.  Just add or
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
  }
  
  
  private void markForDeletion(Area area) {
    if (area.flagDeletion) return;
    area.flagDeletion = true;
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
  
  
  void updatePathCache() {
    for (Area a : needRefresh) refreshTiling(a);
    
    //  Delete all redundant areas before you create new ones!
    
  }
  
  
  
  /**  Querying and generating area-ownership:
    */
  Area areaFor(Tile t) {
    Area area = areaLookup[t.x][t.y];
    if (area != null && ! area.flagDeletion) return area;
    
    if (area != null && area.flagDeletion) {
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
      area.group.flagDeletion = true;
      area.group.areas.remove(area);
      area.tiles = null;
      areas.remove(area);
      area = null;
    }
    
    if (map.blocked(t.x, t.y)) return null;
    
    int aX = t.x / AREA_SIZE, aY = t.y / AREA_SIZE;
    Batch <Tile> covered  = new Batch();
    List  <Tile> frontier = new List();
    Batch <Tile> edging   = new Batch();
    frontier.add(t);
    covered.add(t);
    t.pathFlag = covered;
    
    while (! frontier.empty()) {
      Tile front = frontier.removeFirst();
      for (Tile n : CityMap.adjacent(front, temp, map)) {
        if (n == null || n.pathFlag != null) continue;
        if (map.blocked(n.x, n.y)) continue;
        if (n.x / AREA_SIZE != aX || n.y / AREA_SIZE != aY) {
          edging.add(n);
          n.pathFlag = edging;
          continue;
        }
        frontier.addLast(n);
        covered.add(n);
        n.pathFlag = covered;
      }
    }
    
    area = new Area();
    area.ID       = nextAreaID++;
    area.numTiles = covered.size();
    area.tiles    = covered.toArray(Tile.class);
    area.aX       = aX;
    area.aY       = aY;
    areas.add(area);
    
    for (Tile c : covered) {
      c.pathFlag = null;
      areaLookup[c.x][c.y] = area;
    }
    
    Batch <Area> bordering = new Batch();
    for (Tile e : edging) {
      e.pathFlag = null;
    }
    for (Tile e : edging) {
      bordering.include(areaFor(e));
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
      if (map.settings.reportPathCache) {
        I.say("\n  Deleting Group "+group);
      }
      for (Area a : group.areas) if (a.group == group) {
        a.group = null;
      }
      groups.remove(group);
      group = null;
    }

    Batch <Area> covered  = new Batch();
    List  <Area> frontier = new List();
    frontier.add(area);
    covered.add(area);
    area.pathFlag = covered;
    
    while (! frontier.empty()) {
      Area front = frontier.removeFirst();
      for (Area n : front.borders) {
        if (n.pathFlag != null) continue;
        if (n.tiles == null) I.complain("Hit deleted area!");
        frontier.add(n);
        covered.add(n);
        n.pathFlag = covered;
      }
    }
    
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
  
}







