

package game;
import util.*;
import static game.CityMap.*;
import static game.WorldCalendar.*;
import static game.GameConstants.*;



public class CityMapFog {
  
  
  /**  Data fields, setup and save/load methods-
    */
  CityMap map;
  
  int dayState = -1;
  byte fogVals[][];
  byte oldVals[][];
  byte maxVals[][];
  int mipMap[][][];
  
  
  
  CityMapFog(CityMap map) {
    this.map = map;
  }
  
  
  void loadState(Session s) throws Exception {
    dayState = s.loadInt();
    s.loadByteArray(fogVals);
    s.loadByteArray(oldVals);
    s.loadByteArray(maxVals);
    
    for (int[][] level : mipMap) {
      for (Coord c : Visit.grid(0, 0, level.length, level.length, 1)) {
        level[c.x][c.y] = s.loadInt();
      }
    }
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveInt(dayState);
    s.saveByteArray(fogVals);
    s.saveByteArray(oldVals);
    s.saveByteArray(maxVals);
    
    for (int[][] level : mipMap) {
      for (Coord c : Visit.grid(0, 0, level.length, level.length, 1)) {
        s.saveInt(level[c.x][c.y]);
      }
    }
  }
  
  
  void performSetup(int size) {
    this.fogVals = new byte[size][size];
    this.oldVals = new byte[size][size];
    this.maxVals = new byte[size][size];
    
    Batch <int[][]> levels = new Batch();
    int span = size;
    while ((span = span / 4) > 1) {
      int level[][] = new int[span][span];
      levels.add(level);
    }
    this.mipMap = (int[][][]) levels.toArray(int[][].class);
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
      byte max = maxVals[c.x][c.y];
      if (val > max) {
        maxVals[c.x][c.y] = max;
        incMipVal(c.x, c.y, val - max);
      }
    }
  }
  
  
  void incMipVal(int x, int y, int inc) {
    int l = 0;
    while (l < mipMap.length) {
      x /= 4;
      y /= 4;
      mipMap[l][x][y] += inc;
      l += 1;
    }
  }
  
  
  
  /**  Exploration-related queries:
    */
  Tile pickRandomFogPoint(Element near) {
    Tile from = near.at();
    int res = 1 << (4 * mipMap.length);
    Coord mip = new Coord(0, 0);
    
    for (int l = mipMap.length; l-- > 0;) {
      int level[][] = mipMap[l], side = Nums.min(4, level.length);
      
      Pick <Coord> pick = new Pick(0);
      for (Coord c : Visit.grid(mip.x, mip.y, side, side, 1)) {
        float rating = 1 - (level[c.x][c.y] * 1f / (res * res)), dist = 0;
        dist += Nums.abs(from.x - ((c.x + 0.5f) * res));
        dist += Nums.abs(from.y - ((c.y + 0.5f) * res));
        pick.compare(c, rating * CityMap.distancePenalty(dist) * Rand.num());
      }
      
      if (pick.empty()) return null;
      
      mip.setTo(pick.result());
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
      if (dist > range || maxVals[t.x][t.y] != 0) continue;
      pick.compare(t, 0 - dist);
    }
    
    return pick.result();
  }
  
  
  
  /**  Common queries-
    */
  float sightLevel(Tile t) {
    return t == null ? 0 : (oldVals[t.x][t.y] / 100f);
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




