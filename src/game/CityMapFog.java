

package game;
import util.*;
import static game.CityMap.*;
import static game.WorldCalendar.*;
import static game.GameConstants.*;



public class CityMapFog {
  
  
  /**  Data fields, setup and save/load methods-
    */
  CityMap map;
  
  int  dayState = -1;
  byte fogVals[][];
  byte oldVals[][];
  int  maxVals[][][];
  
  
  
  CityMapFog(CityMap map) {
    this.map = map;
  }
  
  
  void loadState(Session s) throws Exception {
    dayState = s.loadInt();
    s.loadByteArray(fogVals);
    s.loadByteArray(oldVals);
    
    for (int[][] level : maxVals) {
      for (Coord c : Visit.grid(0, 0, level.length, level.length, 1)) {
        level[c.x][c.y] = s.loadInt();
      }
    }
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveInt(dayState);
    s.saveByteArray(fogVals);
    s.saveByteArray(oldVals);
    
    for (int[][] level : maxVals) {
      for (Coord c : Visit.grid(0, 0, level.length, level.length, 1)) {
        s.saveInt(level[c.x][c.y]);
      }
    }
  }
  
  
  void performSetup(int size) {
    this.fogVals = new byte[size][size];
    this.oldVals = new byte[size][size];
    
    Batch <int[][]> levels = new Batch();
    int span = size;
    while (span > 1) {
      int level[][] = new int[span][span];
      levels.add(level);
      span = Nums.round(span / 4f, 1, true);
    }
    this.maxVals = (int[][][]) levels.toArray(int[][].class);
  }
  
  
  
  /**  Regular updates-
    */
  void liftFog(Tile around, float range) {
    if (! GameSettings.toggleFog) {
      return;
    }
    Box2D area = new Box2D(around.x, around.y, 0, 0);
    area.expandBy(Nums.round(range, 1, true));
    
    for (Coord c : Visit.grid(area)) {
      Tile t = map.tileAt(c.x, c.y);
      float distance = distance(t, around);
      if (distance > range) continue;
      fogVals[c.x][c.y] = 100;
    }
  }
  
  
  void updateFog() {
    dayState = WorldCalendar.dayState(map.time);
    
    if (! GameSettings.toggleFog) {
      return;
    }
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      byte val = oldVals[c.x][c.y] = fogVals[c.x][c.y];
      fogVals[c.x][c.y] = 0;
      int max = maxVals[0][c.x][c.y];
      if (val > max) {
        incMipVal(c.x, c.y, val - max);
      }
    }
  }
  
  
  private void incMipVal(int x, int y, int inc) {
    int l = 0;
    while (l < maxVals.length) {
      maxVals[l][x][y] += inc;
      x /= 4;
      y /= 4;
      l += 1;
    }
  }
  
  
  
  /**  Exploration-related queries:
    */
  Tile pickRandomFogPoint(Element near) {
    
    boolean report = near.reports();
    if (report) I.say("\nGETTING TILE TO LOOK AT...");
    
    Tile from = near.at();
    int res = (int) Nums.pow(4, maxVals.length - 1);
    Coord mip = new Coord(0, 0);
    
    for (int l = maxVals.length; l-- > 0;) {
      if (report) I.say("  Current level: "+l);
      
      int level[][] = maxVals[l];
      int sideX = Nums.min(4, level.length - mip.x);
      int sideY = Nums.min(4, level.length - mip.y);
      int maxSum = res * res * 100;
      Pick <Coord> pick = new Pick(0);
      
      for (Coord c : Visit.grid(mip.x, mip.y, sideX, sideY, 1)) {
        float rating = 1 - (level[c.x][c.y] * 1f / maxSum), dist = 0;
        dist += Nums.abs(from.x - ((c.x + 0.5f) * res));
        dist += Nums.abs(from.y - ((c.y + 0.5f) * res));
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
  
  
  Tile findNearbyFogPoint(Element near, int range) {
    Tile from = near.at();
    int minX = from.x - range, minY = from.y - range;
    Pick <Tile> pick = new Pick();
    
    for (Coord c : Visit.grid(minX, minY, range * 2, range * 2, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      float dist = CityMap.distance(from, t);
      if (dist > range || maxVals[0][t.x][t.y] != 0) continue;
      pick.compare(t, 0 - dist);
    }
    
    return pick.result();
  }
  
  
  
  /**  Common queries-
    */
  float sightLevel(Tile t) {
    return t == null ? 0 : (oldVals[t.x][t.y] / 100f);
  }
  
  
  float maxSightLevel(Tile t) {
    return t == null ? 0 : (maxVals[0][t.x][t.y] / 100f);
  }
  
  
  boolean day() {
    return dayState == STATE_LIGHT;
  }
  
  
  boolean night() {
    return dayState == STATE_DARKNESS;
  }
  
  
  float lightLevel() {
    if (day  ()) return 1;
    if (night()) return 0;
    return 0.5f;
  }
  
}




