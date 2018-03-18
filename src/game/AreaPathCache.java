

package game;
import util.*;
import static game.Area.*;
import static game.GameConstants.Pathing;
import static game.GameConstants.Target;



public class AreaPathCache {
  
  
  /**  Data fields, constants, construction and save/load methods-
    */
  final static int
    AREA_SIZE = 16
  ;
  
  public static class Zone implements Flood.Fill {
    
    int ID;
    boolean flagTiling   = false;
    boolean flagDeletion = false;
    
    int aX, aY;
    int numTiles;
    boolean grounded;
    
    AreaTile tiles[];
    Batch <AreaTile> toAdd = new Batch(4);
    Batch <AreaTile> toRem = new Batch(4);
    
    List <Border> borders = new List();
    ZoneGroup group;
    ZoneGroup openGroup;
    
    Object pathFlag = null;
    
    public void flagWith(Object o) { pathFlag = o; }
    public Object flaggedWith() { return pathFlag; }
    public String toString() { return "A_"+ID; }
    
    public AreaTile[] tiles() { return tiles; }
    public int numTiles() { return numTiles; }
    public Coord coord() { return new Coord(aX, aY); }
    
    public ZoneGroup group() { return group; }
    public Series <Border> borders() { return borders; }
  }
  
  public static class Border {
    
    Zone with;
    int size;
    boolean open;
    
    public Zone with() { return with; }
  }
  
  public static class ZoneGroup {
    
    int ID;
    boolean flagDeletion = false;
    
    List <Zone> zones;
    boolean hasGroundAccess;
    int totalTiles;
    
    public String toString() { return "G_"+ID; }
    
    public Series <Zone> zones() { return zones; }
  }
  
  
  final Area map;
  Zone zoneLookup[][];
  boolean flagDirty[][];
  
  int nextAreaID  = 0;
  int nextGroupID = 0;
  List <Zone     > zones  = new List();
  List <ZoneGroup> groups = new List();
  
  List <Zone> needRefresh = new List();
  List <Zone> needDelete  = new List();

  //  Note:  These are refreshed frequently, so there's no need to save or load-
  
  private Pathing temp[] = new Pathing[9];
  private AreaTile tempForFrom[] = new AreaTile[9];
  private AreaTile tempForGoes[] = new AreaTile[9];

  Table <String, Object> tempCache;
  
  
  AreaPathCache(Area map) {
    this.map = map;
  }
  
  
  void performSetup(int size) {
    int dirtyGS = Nums.round(map.size * 1f / AREA_SIZE, 1, true);
    zoneLookup = new Zone[size][size];
    flagDirty  = new boolean[dirtyGS][dirtyGS];
    tempCache  = new Table((size * size) / 2);
  }
  
  
  void loadState(Session s) throws Exception {
    return;
  }
  
  
  void saveState(Session s) throws Exception {
    return;
  }
  
  
  
  /**  Basic group/zone accessors-
    */
  public Series <Zone> zones() {
    return zones;
  }
  
  
  public Series <ZoneGroup> groups() {
    return groups;
  }
  
  
  
  /**  Generic caching methods-
    */
  Object openGroupHandle(AreaTile at) {
    return groupFor(zoneFor(at), true);
  }
  
  
  Object getCache(String key) {
    return tempCache.get(key);
  }
  
  
  void putCache(String key, Object object) {
    tempCache.put(key, object);
  }
  
  
  
  /**  Query methods for distance and connection-
    */
  public boolean pathConnects(AreaTile from, AreaTile goes) {
    ZoneGroup fromG = groupFor(zoneFor(from), false);
    ZoneGroup goesG = groupFor(zoneFor(goes), false);
    if (fromG == null || goesG == null) return false;
    return fromG == goesG;
  }
  
  
  public boolean openPathConnects(AreaTile from, AreaTile goes) {
    final Zone fromA = zoneFor(from);
    final Zone goesA = zoneFor(goes);
    ZoneGroup fromG = groupFor(fromA, true);
    ZoneGroup goesG = groupFor(goesA, true);
    if (fromG == null || goesG == null) return false;
    return fromG == goesG;
  }
  
  
  public boolean pathConnects(
   Actor from, Target goes, boolean checkAdjacent, boolean open
  ) {
    return pathConnects(Task.pathOrigin(from), goes, checkAdjacent, open);
  }
  
  
  public boolean pathConnects(
    Pathing from, Target goes, boolean checkAdjacent, boolean open
  ) {
    if (from == null || goes == null) return false;
    AreaTile fromA[] = around(from, tempForFrom, false);
    AreaTile goesA[] = around(goes, tempForGoes, checkAdjacent);
    if (fromA == null || goesA == null) return false;
    
    for (AreaTile f : fromA) if (f != null) {
      for (AreaTile g : goesA) if (g != null) {
        if (open) {
          if (openPathConnects(f, g)) return true;
        }
        else {
          if (pathConnects(f, g)) return true;
        }
      }
    }
    return false;
  }
  
  
  private AreaTile[] around(Target e, AreaTile temp[], boolean checkAdjacent) {
    Type t = e.type();
    
    if (t.isBuilding()) {
      Building b = (Building) e;
      if (b.complete()) return b.entrances();
    }
    
    if (checkAdjacent) {
      AreaTile at = e.at();
      if (t.wide > 1 || t.high > 1) {
        temp = new AreaTile[(t.wide + 1 + t.high + 1) * 2];
        int i = 0;
        for (AreaTile a : map.tilesAround(at.x, at.y, t.wide, t.high)) {
          temp[i++] = a;
        }
        return temp;
      }
      else {
        Area.adjacent(at, temp, map);
        temp[8] = at;
        return temp;
      }
    }
    else {
      for (int i = temp.length; i-- > 1;) temp[i] = null;
      temp[0] = e.at();
      return temp;
    }
  }
  
  
  public AreaTile mostOpenNeighbour(AreaTile at) {
    Pick <AreaTile> pick = new Pick();
    for (AreaTile t : Area.adjacent(at, null, map)) {
      ZoneGroup group = groupFor(zoneFor(t), false);
      if (group != null) pick.compare(t, group.totalTiles);
    }
    return pick.result();
  }
  
  
  public boolean hasGroundAccess(AreaTile at) {
    ZoneGroup group = groupFor(zoneFor(at), false);
    if (group == null || ! group.hasGroundAccess) return false;
    return true;
  }
  
  
  public int groundTileAccess(AreaTile at) {
    ZoneGroup group = groupFor(zoneFor(at), false);
    if (group == null || ! group.hasGroundAccess) return 0;
    return group.totalTiles;
  }
  
  
  
  /**  Methods for flagging changes and regular updates:
    */
  void checkPathingChanged(AreaTile at) {
    
    //  First, make sure there's some change to merit an update:
    Zone core = zoneLookup[at.x][at.y];
    boolean blocked = map.blocked(at.x, at.y);
    //if (blocked == (core == null)) return;
    
    //  Then set up some tracking variables-
    final int aX = at.x / AREA_SIZE;
    final int aY = at.y / AREA_SIZE;
    Zone tail = null, head = null, edge = core;
    int numGaps = 0, gapFlag = -1;
    boolean multiArea = false, didRefresh = false;
    
    //  The plan is to circle this tile, checking how many zones it
    //  impinges on and whether there are multiple 'gaps' in zone-
    //  adjacency- the latter might indicate a bottleneck in pathing
    //  that could potentially require a more comprehensive update (see
    //  below.)
    for (Pathing p : at.adjacent(temp, map)) {

      //  Note:  In the case that this tile is/was an entrance to a
      //  building with other exists, we have to treat it as a potential
      //  'border' to another zone:
      AreaTile n;
      if (p == null) n = null;
      else if (p.isTile()) n = (AreaTile) p;
      else {
        n = null;
        if (((Building) p).entrances().length > 1) multiArea = true;
      }
      
      tail = n == null ? null : zoneLookup[n.x][n.y];
      
      //  We don't merge with zones outside a given 16x16 unit:
      if (tail != null && (tail.aX != aX || tail.aY != aY)) {
        multiArea = true;
        tail = null;
      }
      //  Record the first zone on the perimeter, null or otherwise-
      if (gapFlag == -1) {
        head = tail;
      }
      //  And check whether more than one zone is bumped into, or any
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
    
    //  If possible, don't flag the zone for deletion- just add or
    //  remove a single tile.  (Note that in the case of deleting a
    //  tile, there must not be a potential bottleneck.)
    if (edge != null && ! (multiArea || edge.flagDeletion)) {
      ZoneGroup openG   = groupFor(edge, true );
      ZoneGroup closedG = groupFor(edge, false);
      
      if (blocked && numGaps < 2) {
        zoneLookup[at.x][at.y] = null;
        edge.toRem.add(at);
        edge   .numTiles   -= 1;
        openG  .totalTiles -= 1;
        closedG.totalTiles -= 1;
        markForRefresh(edge);
        didRefresh = true;
        if (report()) {
          I.say("\nRemoved single tile: "+at+" from "+edge);
        }
      }
      
      if (core == null && ! blocked) {
        zoneLookup[at.x][at.y] = edge;
        edge.toAdd.add(at);
        edge   .numTiles   += 1;
        openG  .totalTiles += 1;
        closedG.totalTiles += 1;
        markForRefresh(edge);
        didRefresh = true;
        if (report()) {
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
  
  
  private void markForDeletion(Zone zone) {
    if (zone.flagDeletion) return;
    zone.flagDeletion = true;
    needDelete.add(zone);
    if (report()) {
      I.say("\n  Flag To Delete: "+zone);
    }
  }
  
  
  private void markForRefresh(Zone zone) {
    if (zone.flagTiling) return;
    zone.flagTiling = true;
    needRefresh.add(zone);
    if (report()) {
      I.say("\n  Flag To Refresh: "+zone);
    }
  }
  
  
  private void refreshTiling(Zone zone) {
    if (zone.toAdd.empty() && zone.toRem.empty()) return;
    
    Batch <AreaTile> tiles = new Batch();
    for (AreaTile t : zone.tiles) if (! zone.toRem.includes(t)) tiles.add(t);
    Visit.appendTo(tiles, zone.toAdd);
    
    zone.toAdd.clear();
    zone.toRem.clear();
    zone.tiles = tiles.toArray(AreaTile.class);
    zone.numTiles = zone.tiles.length;
    zone.flagTiling = false;
  }
  
  
  private void updateDirtyBounds() {
    int dirtyGS = flagDirty.length;
    for (Coord c : Visit.grid(0, 0, dirtyGS, dirtyGS, 1)) {
      if (! flagDirty[c.x][c.y]) continue;
      int aX = c.x * AREA_SIZE;
      int aY = c.y * AREA_SIZE;
      for (Coord t : Visit.grid(aX, aY, AREA_SIZE, AREA_SIZE, 1)) {
        AreaTile u = map.tileAt(t);
        if (u != null) zoneFor(u);
      }
      flagDirty[c.x][c.y] = false;
    }
  }
  
  
  public void updatePathCache() {
    
    for (Zone a : needRefresh) {
      refreshTiling(a);
    }
    needRefresh.clear();
    
    for (Zone a : needDelete) {
      deleteArea(a);
    }
    needDelete.clear();
    
    updateDirtyBounds();
    
    for (Zone a : zones) {
      groupFor(a, true );
      groupFor(a, false);
    }
    
    tempCache.clear();
  }
  
  
  
  /**  Querying and generating zone-ownership:
    */
  public Zone rawZone(AreaTile t) {
    if (t == null) return null;
    return zoneLookup[t.x][t.y];
  }
  
  
  public Zone zoneFor(AreaTile t) {
    if (t == null) return null;
    Zone zone = zoneLookup[t.x][t.y];
    
    if (zone != null && ! zone.flagDeletion) {
      return zone;
    }
    if (zone != null && zone.flagDeletion) {
      deleteArea(zone);
      zone = null;
    }
    
    if (map.blocked(t)) {
      return null;
    }
    
    final int aX = t.x / AREA_SIZE, aY = t.y / AREA_SIZE;
    final Batch <AreaTile> edging = new Batch();
    final Batch <AreaTile> gated  = new Batch();
    
    Series <AreaTile> covered = new Flood <AreaTile> () {
      protected void addSuccessors(AreaTile front) {
        for (Pathing p : front.adjacent(temp, map)) {
          if (p == null || p.flaggedWith() != null) continue;
          
          if (! p.isTile()) {
            Building b = (Building) p;
            for (AreaTile n : b.entrances()) if (n != p) {
              gated.add(n);
              //edging.add(n);
              //n.flagWith(edging);
            }
            continue;
          }
          
          AreaTile n = (AreaTile) p;
          if (n.x / AREA_SIZE != aX || n.y / AREA_SIZE != aY) {
            edging.add(n);
            n.flagWith(edging);
            continue;
          }
          
          tryAdding(n);
        }
      }
    }.floodFrom(t);
    
    zone = new Zone();
    zone.ID       = nextAreaID++;
    zone.numTiles = covered.size();
    zone.grounded = t.pathType() != Type.PATH_WALLS;
    zone.tiles    = covered.toArray(AreaTile.class);
    zone.aX       = aX;
    zone.aY       = aY;
    zones.add(zone);
    
    for (AreaTile c : covered) {
      zoneLookup[c.x][c.y] = zone;
    }
    
    for (AreaTile e : gated) {
      Zone b = zoneFor(e);
      if (b != null && b != zone) {
        toggleBorders(zone, b, true);
      }
    }
    
    Batch <Zone> bordering = new Batch();
    for (AreaTile e : edging) {
      e.flagWith(null);
    }
    for (AreaTile e : edging) {
      Zone b = zoneFor(e);
      if (b != null && b != zone) bordering.include(b);
    }
    for (Zone b : bordering) {
      Border with = toggleBorders(zone, b, true);
      with.open = true;
    }
    
    if (report()) {
      I.say("\n  Adding Area: "+zone+" "+zone.aX+"|"+zone.aY);
      I.say("    Tiles: "+zone.numTiles+"  Borders: ");
      for (Border b : zone.borders) I.add(b.with.ID+" ");
      if (zone.borders.empty()) I.add("none");
      I.say("    Tiles:\n      ");
      int count = 0;
      for (AreaTile a : zone.tiles) {
        I.add(a+" ");
        if (++count >= 8) { I.add("\n      "); count = 0; }
      }
    }
    
    return zone;
  }
  
  
  private void deleteArea(Zone zone) {
    if (zone.tiles == null) return;
    
    if (report()) {
      I.say("\n  Deleting Area "+zone);
    }
    
    refreshTiling(zone);
    for (AreaTile c : zone.tiles) if (zoneLookup[c.x][c.y] == zone) {
      zoneLookup[c.x][c.y] = null;
    }
    for (Border b : zone.borders) {
      toggleBorders(zone, b.with, false);
    }
    if (zone.group != null) {
      deleteGroup(zone.group);
    }
    
    zone.tiles = null;
    zones.remove(zone);
  }
  
  
  private Border toggleBorders(Zone a, Zone b, boolean yes) {
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
  
  
  private Border borderBetween(Zone a, Zone o, boolean init) {
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
  
  
  
  /**  Querying and generating zone-group membership:
    */
  ZoneGroup groupFor(final Zone zone, final boolean open) {
    if (zone == null) return null;
    ZoneGroup group = open ? zone.openGroup : zone.group;
    if (group != null && ! group.flagDeletion) return group;
    
    if (group != null && group.flagDeletion) {
      deleteGroup(group);
      group = null;
    }
    
    Series <Zone> covered = new Flood <Zone> () {
      protected void addSuccessors(Zone front) {
        for (Border n : front.borders) {
          if (open && ! n.open) continue;
          if (n.with.flaggedWith() != null) continue;
          tryAdding(n.with);
        }
      }
    }.floodFrom(zone);
    
    group = new ZoneGroup();
    group.ID = nextGroupID++;
    Visit.appendTo(group.zones = new List(), covered);
    groups.add(group);
    
    for (Zone a : covered) {
      a.pathFlag = null;
      if (open) a.openGroup = group;
      else a.group = group;
      group.hasGroundAccess |= a.grounded;
      group.totalTiles += a.numTiles;
    }
    
    if (report()) {
      I.say("\n  Adding "+(open ? "Open" : "Closed")+" Group "+group+": ");
      for (Zone a : group.zones) I.add(a+" ");
    }
    
    return group;
  }
  
  
  private void deleteGroup(ZoneGroup group) {
    if (group.zones == null) return;
    if (report()) {
      I.say("\n  Deleting Group "+group);
    }
    for (Zone a : group.zones) {
      if (a.group     == group) a.group = null;
      if (a.openGroup == group) a.group = null;
    }
    group.zones = null;
    groups.remove(group);
  }
  
  
  
  /**  Rendering, debug and interface methods.
    */
  boolean report() {
    return map.world.settings.reportPathCache;
  }
  
}







