

package game;
import util.*;
import static game.CityMap.*;




public class CityMapFlagging {
  
  
  /**  Data fields, construction and save/load methods-
    */
  CityMap map;
  Object  key;
  int range;
  
  Object flagLevels[];
  
  
  CityMapFlagging(CityMap map, Object key, int range) {
    this.map = map;
    this.key = key;
    this.range = range;
  }
  
  
  void setupWithSize(int size) {
    Batch levels = new Batch();
    int span = size, dim = 1;
    while (span > 1) {
      Object level = dim == 1 ?
        new byte[span][span] :
        new int [span][span]
      ;
      levels.add(level);
      span = Nums.round(span * 1f / FLAG_RES, 1, true);
      dim *= FLAG_RES;
    }
    this.flagLevels = levels.toArray(Object.class);
  }
  
  
  void loadState(Session s) throws Exception {
    s.loadByteArray(baseLevel());
    
    for (int i = 1; i < flagLevels.length; i++) {
      int level[][] = upperLevel(i);
      for (Coord c : Visit.grid(0, 0, level.length, level.length, 1)) {
        level[c.x][c.y] = s.loadInt();
      }
    }
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveByteArray(baseLevel());
    
    for (int i = 1; i < flagLevels.length; i++) {
      int level[][] = upperLevel(i);
      for (Coord c : Visit.grid(0, 0, level.length, level.length, 1)) {
        s.saveInt(level[c.x][c.y]);
      }
    }
  }
  
  
  
  /**  Common updates and basic queries:
    */
  byte[][] baseLevel() {
    return (byte[][]) flagLevels[0];
  }
  
  
  int[][] upperLevel(int l) {
    return (int[][]) flagLevels[l];
  }
  
  
  public void setFlagVal(int x, int y, int val) {
    int old = baseLevel()[x][y];
    if (old == val) return;
    incFlagVal(x, y, val - old);
  }
  
  
  public void incFlagVal(int x, int y, int inc) {
    int l = 0;
    while (l < flagLevels.length) {
      if (l == 0) ((byte[][]) flagLevels[0])[x][y] += inc;
      else        ((int [][]) flagLevels[l])[x][y] += inc;
      
      x /= FLAG_RES;
      y /= FLAG_RES;
      l += 1;
    }
  }
  
  
  public int flagVal(int x, int y) {
    return baseLevel()[x][y];
  }
  
  
  
  /**  More complex queries-
    */
  public Tile pickDistantPoint(Element near, int maxRange, float randomness) {
    
    boolean report = near.reports();
    if (report) I.say("\nGETTING TILE TO LOOK AT...");
    
    Tile from = near.at();
    int res = (int) Nums.pow(FLAG_RES, flagLevels.length - 1);
    Coord mip = new Coord(0, 0);
    
    //  TODO:  This will need to be converted into a full-blown search to
    //  handle some of the nuances of complex queries.
    
    for (int l = flagLevels.length; l-- > 0;) {
      if (report) I.say("  Current level: "+l);
      
      boolean base = l == 0;
      byte levelB[][] = base ? baseLevel() : null;
      int  levelI[][] = base ? null : upperLevel(l);
      int levelSize = base ? levelB.length : levelI.length;
      
      int sideX = Nums.min(FLAG_RES, levelSize - mip.x);
      int sideY = Nums.min(FLAG_RES, levelSize - mip.y);
      int maxSum = res * res * range;
      Pick <Coord> pick = new Pick(0);
      
      for (Coord c : Visit.grid(mip.x, mip.y, sideX, sideY, 1)) {
        float val = base ? levelB[c.x][c.y] : levelI[c.x][c.y];
        float rating = val * 1f / maxSum, dist = 0;
        dist += Nums.abs(from.x - ((c.x + 0.5f) * res));
        dist += Nums.abs(from.y - ((c.y + 0.5f) * res));
        
        if (maxRange > 0 && dist > maxRange + res) continue;
        
        if (base) {
          Tile t = map.tileAt(c);
          if (t.hasFocus()) continue;
          
          //  TODO:  This might not work.  Flagged tiles will typically
          //  be inside a blocked structure, and entrances are not
          //  always unique.  You'll need to vary the check, depending
          //  on the object above a given tile.
          
          //if (! map.pathCache.pathConnects(t, from)) continue;
        }
        
        float roll = (1f + randomness) + (Rand.num() * randomness);
        rating *= res * roll / (dist + res);
        pick.compare(new Coord(c), rating);
        
        if (report) I.say("    "+c+" -> "+rating);
      }
      
      if (pick.empty()) {
        if (report) I.say("    No result found!");
        return null;
      }
      
      mip.setTo(pick.result());
      if (report) I.say("    Delving in at: "+mip);
      
      if (l == 0) {
        break;
      }
      else {
        mip.x *= FLAG_RES;
        mip.y *= FLAG_RES;
        res   /= FLAG_RES;
      }
    }
    
    return map.tileAt(mip.x, mip.y);
  }
  
  
  public Tile pickRandomPoint(Element near, int range) {
    return pickDistantPoint(near, range, 1);
  }
  
  
  public Tile findNearbyPoint(Element near, int range) {
    
    Tile from = near.at();
    int minX = from.x - range, minY = from.y - range;
    Pick <Tile> pick = new Pick();
    byte vals[][] = (byte[][]) flagLevels[0];
    
    for (Coord c : Visit.grid(minX, minY, range * 2, range * 2, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t == null || t.hasFocus()) continue;
      
      float dist = CityMap.distance(from, t);
      if (dist > range || vals[t.x][t.y] == 0) continue;
      
      pick.compare(t, 0 - dist);
    }
    
    return pick.result();
  }
  
  
  public int totalSum() {
    int sum = 0;
    int l = flagLevels.length - 1;
    for (int[] r : upperLevel(l)) for (int v : r) sum += v;
    return sum;
  }
  
}








