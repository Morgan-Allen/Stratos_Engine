

package game;
import static game.GameConstants.*;
import static game.RelationSet.*;
import util.*;




public class AreaDanger {
  
  
  final Faction faction;
  final AreaMap map;
  float baseValues[][];
  float fuzzValues[][];
  float maxValue;
  
  
  
  AreaDanger(Faction base, AreaMap map) {
    this.faction = base;
    this.map = map;
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
    maxValue = s.loadFloat();
  }
  
  void saveState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, map.flagSize, map.flagSize, 1)) {
      s.saveFloat(baseValues[c.x][c.y]);
      s.saveFloat(fuzzValues[c.x][c.y]);
    }
    s.saveFloat(maxValue);
  }
  
  
  void updateDanger() {
    
    final float FUZZ_RANGE = AVG_SIGHT * 2;
    maxValue = 0.1f;
    RelationSet relations = map.world.federation(faction).relations;
    
    for (Coord c : Visit.grid(0, 0, map.size(), map.size(), AreaMap.FLAG_RES)) {
      Series <Active> at = map.gridActive(map.tileAt(c));
      float danger = 0;
      
      if (at.size() > 0) for (Active a : at) {
        boolean enemy = relations.hasBondType(a.base().faction(), BOND_ENEMY);
        if (! enemy) continue;
        float power = TaskCombat.attackPower((Element) a);
        danger += power;
      }
      
      baseValues[c.x / AreaMap.FLAG_RES][c.y / AreaMap.FLAG_RES] = danger;
    }
    
    for (Coord c : Visit.grid(0, 0, map.flagSize, map.flagSize, 1)) {
      
      final int
        lim  = map.flagSize - 1,
        off  = Nums.round(FUZZ_RANGE * 1f / AreaMap.FLAG_RES, 1, true),
        minX = Nums.max(0  , c.x - off),
        maxX = Nums.min(lim, c.x + off),
        minY = Nums.max(0  , c.y - off),
        maxY = Nums.min(lim, c.y + off)
      ;
      
      Vec2D diff = new Vec2D();
      float sum = 0;
      
      for (int x = minX; x <= maxX; x++) {
        for (int y = minY; y <= maxY; y++) {
          
          diff.set(x - c.x, y - c.y);
          float dist = diff.length() * AreaMap.FLAG_RES;
          float weight = 1 - (dist / FUZZ_RANGE);
          if (weight <= 0) continue;
          
          sum += baseValues[x][y] * weight;
        }
      }
      
      fuzzValues[c.x][c.y] = sum;
      maxValue = Nums.max(maxValue, sum);
    }
  }
  
  
  public float baseLevel(int tileX, int tileY) {
    return baseValues[tileX / AreaMap.FLAG_RES][tileY / AreaMap.FLAG_RES];
  }
  
  
  public float fuzzyLevel(int tileX, int tileY) {
    return fuzzValues[tileX / AreaMap.FLAG_RES][tileY / AreaMap.FLAG_RES];
  }
  
  
  public float maxValue() {
    return maxValue;
  }
  
  
}




