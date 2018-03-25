

package game;
import static game.GameConstants.*;
import util.*;




public class AreaDanger {
  
  
  final Base base;
  final Area map;
  float baseValues[][];
  float fuzzValues[][];
  
  
  
  AreaDanger(Base base, Area map) {
    this.base = base;
    this.map  = map;
  }
  
  void performSetup(int baseSize) {
    this.baseValues = new float[baseSize][baseSize];
    this.fuzzValues = new float[baseSize][baseSize];
  }
  
  void loadState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, map.flagSize, map.flagSize, 1)) {
      baseValues[c.x][c.y] = s.loadFloat();
      fuzzValues[c.x][c.y] = s.loadFloat();
    }
  }
  
  void saveState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, map.flagSize, map.flagSize, 1)) {
      s.saveFloat(baseValues[c.x][c.y]);
      s.saveFloat(fuzzValues[c.x][c.y]);
    }
  }
  
  
  void updateDanger() {
    
    final float FUZZ_RANGE = AVG_SIGHT * 2;
    
    for (Coord c : Visit.grid(0, 0, map.size(), map.size(), Area.FLAG_RES)) {
      Series <Active> at = map.gridActive(map.tileAt(c));
      float danger = 0;
      
      if (at.size() > 0) for (Active a : at) {
        if (! base.isEnemyOf(a.base())) continue;
        float power = TaskCombat.attackPower((Element) a);
        danger += power;
      }
      
      baseValues[c.x / Area.FLAG_RES][c.y / Area.FLAG_RES] = danger;
    }
    
    for (Coord c : Visit.grid(0, 0, map.flagSize, map.flagSize, 1)) {
      
      final float
        gx  = c.x + 0.5f,
        gy  = c.y + 0.5f,
        r   = FUZZ_RANGE / Area.FLAG_RES
      ;
      Vec2D pos = new Vec2D(gx * Area.FLAG_RES, gy * Area.FLAG_RES);
      
      final int
        lim  = map.flagSize - 1,
        minX = Nums.max(0  , Nums.round(gx - r, 1, false)),
        maxX = Nums.min(lim, Nums.round(gx + r, 1, true )),
        minY = Nums.max(0  , Nums.round(gy - r, 1, false)),
        maxY = Nums.min(lim, Nums.round(gy + r, 1, true ))
      ;
      
      float sum = 0, sumWeights = 0;
      
      for (int x = minX; x < maxX; x++) {
        for (int y = minY; y < maxY; y++) {
          float midX = (x + 0.5f) * Area.FLAG_RES;
          float midY = (y + 0.5f) * Area.FLAG_RES;
          float dist = pos.lineDist(midX, midY);
          
          float weight = 1 - (dist / FUZZ_RANGE);
          if (weight <= 0) continue;
          
          sum += baseValues[x][y] * weight;
          sumWeights += weight;
        }
      }
      
      fuzzValues[c.x][c.y] = sum / sumWeights;
    }
  }
  
  
  public float baseLevel(int tileX, int tileY) {
    return baseValues[tileX / Area.FLAG_RES][tileY / Area.FLAG_RES];
  }
  
  
  public float fuzzyLevel(int tileX, int tileY) {
    return fuzzValues[tileX / Area.FLAG_RES][tileY / Area.FLAG_RES];
  }
  
  
}




