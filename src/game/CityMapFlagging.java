

package game;
import util.*;
import static game.CityMap.*;



public class CityMapFlagging {
  
  
  /**  Data fields, construction and save/load methods-
    */
  CityMap map;
  Object  key;
  int range;
  int flagVals[][][];
  
  
  CityMapFlagging(CityMap map, Object key, int range) {
    this.map = map;
    this.key = key;
    this.range = range;
  }
  
  
  void setupWithSize(int size) {
    Batch <int[][]> levels = new Batch();
    int span = size;
    while (span > 1) {
      int level[][] = new int[span][span];
      levels.add(level);
      span = Nums.round(span / 4f, 1, true);
    }
    this.flagVals = (int[][][]) levels.toArray(int[][].class);
  }
  
  
  void loadState(Session s) throws Exception {
    for (int[][] level : flagVals) {
      for (Coord c : Visit.grid(0, 0, level.length, level.length, 1)) {
        level[c.x][c.y] = s.loadInt();
      }
    }
  }
  
  
  void saveState(Session s) throws Exception {
    for (int[][] level : flagVals) {
      for (Coord c : Visit.grid(0, 0, level.length, level.length, 1)) {
        s.saveInt(level[c.x][c.y]);
      }
    }
  }
  
  
  
  /**  Common updates and basic queries:
    */
  void setFlagVal(int x, int y, int val) {
    int old = flagVals[0][x][y];
    if (old == val) return;
    incFlagVal(x, y, val - old);
  }
  
  
  void incFlagVal(int x, int y, int inc) {
    int l = 0;
    while (l < flagVals.length) {
      flagVals[l][x][y] += inc;
      x /= 4;
      y /= 4;
      l += 1;
    }
  }
  
  
  int flagVal(int x, int y) {
    return flagVals[0][x][y];
  }
  
  
  
  /**  More complex queries-
    */
  Tile pickRandomPoint(Element near, int maxRange) {
    
    boolean report = near.reports();
    if (report) I.say("\nGETTING TILE TO LOOK AT...");
    
    Tile from = near.at();
    int res = (int) Nums.pow(4, flagVals.length - 1);
    Coord mip = new Coord(0, 0);
    
    //  TODO:  This will need to be converted into a full-blown search to
    //  handle some of the nuances of complex queries.
    
    for (int l = flagVals.length; l-- > 0;) {
      if (report) I.say("  Current level: "+l);
      
      int level[][] = flagVals[l];
      int sideX = Nums.min(4, level.length - mip.x);
      int sideY = Nums.min(4, level.length - mip.y);
      int maxSum = res * res * range;
      Pick <Coord> pick = new Pick(0);
      
      for (Coord c : Visit.grid(mip.x, mip.y, sideX, sideY, 1)) {
        float rating = level[c.x][c.y] * 1f / maxSum, dist = 0;
        dist += Nums.abs(from.x - ((c.x + 0.5f) * res));
        dist += Nums.abs(from.y - ((c.y + 0.5f) * res));
        
        if (maxRange > 0 && dist > maxRange + res) continue;
        
        rating *= res * Rand.num() / (dist + res);
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
        mip.x *= 4;
        mip.y *= 4;
        res   /= 4;
      }
    }
    
    return map.tileAt(mip.x, mip.y);
  }
  
  
  Tile findNearbyPoint(Element near, int range) {
    
    Tile from = near.at();
    int minX = from.x - range, minY = from.y - range;
    Pick <Tile> pick = new Pick();
    
    //  TODO:  Randomise this is a little, and avoid having two or more actors
    //  pick the same point at once.
    
    for (Coord c : Visit.grid(minX, minY, range * 2, range * 2, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      float dist = CityMap.distance(from, t);
      if (dist > range || flagVals[0][t.x][t.y] == 0) continue;
      pick.compare(t, 0 - dist);
    }
    
    return pick.result();
  }
  
  
  int totalSum() {
    int sum = 0;
    int l = flagVals.length - 1;
    for (int[] r : flagVals[l]) for (int v : r) sum += v;
    return sum;
  }
  
}










