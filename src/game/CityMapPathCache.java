

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.Pathing;
import static game.GameConstants.Target;




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
    boolean grounded;
    
    Tile tiles[];
    Batch <Tile> toAdd = new Batch(4);
    Batch <Tile> toRem = new Batch(4);
    
    //List <Area> borders = new List();
    List <Border> borders = new List();
    AreaGroup group;
    
    Object pathFlag = null;
    
    public void flagWith(Object o) { pathFlag = o; }
    public Object flaggedWith() { return pathFlag; }
    public String toString() { return "A_"+ID; }
  }
  
  static class Border {
    
    Area with;
    int size;
    boolean open;
  }
  
  static class AreaGroup {
    
    int ID;
    boolean flagDeletion = false;
    
    List <Area> areas;
    boolean hasGroundAccess;
    int totalTiles;
    
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
  
  private Pathing temp[] = new Pathing[9];
  private Tile tempForFrom[] = new Tile[9];
  private Tile tempForGoes[] = new Tile[9];
  
  
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
    AreaGroup fromG = groupFor(areaFor(from));
    AreaGroup goesG = groupFor(areaFor(goes));
    if (fromG == null || goesG == null) return false;
    return fromG == goesG;
  }
  
  
  boolean openPathConnects(Tile from, Tile goes) {
    final Area fromA = areaFor(from);
    final Area goesA = areaFor(goes);
    AreaGroup fromG = groupFor(fromA);
    AreaGroup goesG = groupFor(goesA);
    if (fromG == null || goesG == null) return false;
    if (fromG != goesG) return false;
    
    //  TODO:  Later, you'll want separate group-layers for each faction.
    //  See to that.
    final Vars.Bool matched = new Vars.Bool();
    new Flood <Area> () {
      protected void addSuccessors(Area front) {
        if (matched.val) return;
        
        for (Border b : front.borders) if (b.open) {
          if (b.with == goesA) {
            matched.val = true;
            return;
          }
          
          if (b.with.flaggedWith() != null) continue;
          tryAdding(b.with);
        }
      }
    }.floodFrom(fromA);
    return matched.val;
  }
  
  
  boolean pathConnects(
    Pathing from, Target goes, boolean checkAdjacent
  ) {
    if (from == null || goes == null) return false;
    Tile fromA[] = around(from, tempForFrom, false);
    Tile goesA[] = around(goes, tempForGoes, checkAdjacent);
    
    for (Tile f : fromA) if (f != null) {
      for (Tile g : goesA) if (g != null) {
        if (pathConnects(f, g)) return true;
      }
    }
    return false;
  }
  
  
  private Tile[] around(Target e, Tile temp[], boolean checkAdjacent) {
    Type t = e.type();
    Tile at = e.at();
    
    if (t.isBuilding()) {
      Building b = (Building) e;
      if (b.complete()) return b.entrances();
    }
    
    if (checkAdjacent) {
      CityMap.adjacent(at, temp, map);
      temp[8] = at;
      return temp;
    }
    else {
      for (int i = temp.length; i-- > 1;) temp[i] = null;
      temp[0] = at;
      return temp;
    }
  }
  
  
  Tile mostOpenNeighbour(Tile at) {
    Pick <Tile> pick = new Pick();
    
    for (Tile t : CityMap.adjacent(at, null, map)) {
      AreaGroup group = groupFor(areaFor(t));
      if (group != null) pick.compare(t, group.totalTiles);
    }
    return pick.result();
  }
  
  
  boolean hasGroundAccess(Tile at) {
    AreaGroup group = groupFor(areaFor(at));
    if (group == null || ! group.hasGroundAccess) return false;
    return true;
  }
  
  
  int groundTileAccess(Tile at) {
    AreaGroup group = groupFor(areaFor(at));
    if (group == null || ! group.hasGroundAccess) return 0;
    return group.totalTiles;
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

      //  Note:  In the case that this tile is/was an entrance to a
      //  building with other exists, we have to treat it as a potential
      //  'border' to another area:
      Tile n;
      if (p == null) n = null;
      else if (p.isTile()) n = (Tile) p;
      else {
        n = null;
        if (((Building) p).entrances().length > 1) multiArea = true;
      }
      
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
      AreaGroup group = groupFor(edge);
      
      if (blocked && numGaps < 2) {
        areaLookup[at.x][at.y] = null;
        edge.toRem.add(at);
        edge .numTiles   -= 1;
        group.totalTiles -= 1;
        markForRefresh(edge);
        didRefresh = true;
        if (map.settings.reportPathCache) {
          I.say("\nRemoved single tile: "+at+" from "+edge);
        }
      }
      
      if (core == null && ! blocked) {
        areaLookup[at.x][at.y] = edge;
        edge.toAdd.add(at);
        edge .numTiles   += 1;
        group.totalTiles += 1;
        markForRefresh(edge);
        didRefresh = true;
        if (map.settings.reportPathCache) {
          I.say("\nAdded single tile: "+at+" to "+edge);
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
    if (t == null) return null;
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
    final Batch <Tile> gated  = new Batch();
    
    Series <Tile> covered = new Flood <Tile> () {
      protected void addSuccessors(Tile front) {
        for (Pathing p : front.adjacent(temp, map)) {
          if (p == null || p.flaggedWith() != null) continue;
          
          if (! p.isTile()) {
            Building b = (Building) p;
            for (Tile n : b.entrances()) if (n != p) {
              gated.add(n);
              //edging.add(n);
              //n.flagWith(edging);
            }
            continue;
          }
          
          Tile n = (Tile) p;
          if (n.x / AREA_SIZE != aX || n.y / AREA_SIZE != aY) {
            edging.add(n);
            n.flagWith(edging);
            continue;
          }
          
          tryAdding(n);
        }
      }
    }.floodFrom(t);
    
    area = new Area();
    area.ID       = nextAreaID++;
    area.numTiles = covered.size();
    area.grounded = t.pathType() != PATH_WALLS;
    area.tiles    = covered.toArray(Tile.class);
    area.aX       = aX;
    area.aY       = aY;
    areas.add(area);
    
    for (Tile c : covered) {
      areaLookup[c.x][c.y] = area;
    }
    
    for (Tile e : gated) {
      Area b = areaFor(e);
      if (b != null && b != area) {
        toggleBorders(area, b, true);
      }
    }
    
    Batch <Area> bordering = new Batch();
    for (Tile e : edging) {
      e.flagWith(null);
    }
    for (Tile e : edging) {
      Area b = areaFor(e);
      if (b != null && b != area) bordering.include(b);
    }
    for (Area b : bordering) {
      Border with = toggleBorders(area, b, true);
      with.open = true;
    }
    
    if (map.settings.reportPathCache) {
      I.say("\n  Adding Area: "+area+" "+area.aX+"|"+area.aY);
      I.say("    Tiles: "+area.numTiles+"  Borders: ");
      for (Border b : area.borders) I.add(b.with.ID+" ");
      if (area.borders.empty()) I.add("none");
      I.say("    Tiles:\n      ");
      int count = 0;
      for (Tile a : area.tiles) {
        I.add(a+" ");
        if (++count >= 8) { I.add("\n      "); count = 0; }
      }
    }
    
    return area;
  }
  
  
  private void deleteArea(Area area) {
    if (area.tiles == null) return;
    
    if (map.settings.reportPathCache) {
      I.say("\n  Deleting Area "+area);
    }
    
    refreshTiling(area);
    for (Tile c : area.tiles) if (areaLookup[c.x][c.y] == area) {
      areaLookup[c.x][c.y] = null;
    }
    for (Border b : area.borders) {
      toggleBorders(area, b.with, false);
    }
    if (area.group != null) {
      deleteGroup(area.group);
    }
    
    area.tiles = null;
    areas.remove(area);
  }
  
  
  private Border toggleBorders(Area a, Area b, boolean yes) {
    if (yes) {
      Border made = borderBetween(a, b, true);
      borderBetween(b, a, true);
      return made;
    }
    else {
      Border TA = borderBetween(a, b, false);
      Border TB = borderBetween(b, a, false);
      if (TA != null) a.borders.remove(TA);
      if (TB != null) b.borders.remove(TB);
      return null;
    }
  }
  
  
  private Border borderBetween(Area a, Area o, boolean init) {
    for (Border b : a.borders) {
      if (b.with == o) return b;
    }
    if (init) {
      Border b = new Border();
      b.with = o;
      a.borders.add(b);
      return b;
    }
    return null;
  }
  
  
  
  /**  Querying and generating area-group membership:
    */
  AreaGroup groupFor(Area area) {
    if (area == null) return null;
    AreaGroup group = area.group;
    if (group != null && ! group.flagDeletion) return group;
    
    if (group != null && group.flagDeletion) {
      deleteGroup(group);
      group = null;
    }
    
    Series <Area> covered = new Flood <Area> () {
      protected void addSuccessors(Area front) {
        for (Border n : front.borders) {
          if (n.with.flaggedWith() != null) continue;
          tryAdding(n.with);
        }
      }
    }.floodFrom(area);
    
    group = new AreaGroup();
    group.ID = nextGroupID++;
    Visit.appendTo(group.areas = new List(), covered);
    groups.add(group);
    
    for (Area a : covered) {
      a.pathFlag = null;
      a.group = group;
      group.hasGroundAccess |= a.grounded;
      group.totalTiles += a.numTiles;
    }
    
    if (map.settings.reportPathCache) {
      I.say("\n  Adding Group "+group+": ");
      for (Area a : group.areas) I.add(a+" ");
    }
    
    return group;
  }
  
  
  private void deleteGroup(AreaGroup group) {
    if (group.areas == null) return;
    if (map.settings.reportPathCache) {
      I.say("\n  Deleting Group "+group);
    }
    for (Area a : group.areas) if (a.group == group) {
      a.group = null;
    }
    group.areas = null;
    groups.remove(group);
  }
  
}







