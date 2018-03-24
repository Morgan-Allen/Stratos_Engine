

package game;
import static game.GameConstants.*;
import util.*;




public class AreaDanger {
  
  
  final Base base;
  final Area area;
  
  float baseValues[][];
  //AreaFlagging flagging;
  //float fuzzyValues[][];
  
  
  
  AreaDanger(Base base, Area area) {
    this.base = base;
    this.area = area;
  }
  
  void performSetup(int baseSize) {
    this.baseValues = new float[baseSize][baseSize];
  }
  
  void loadState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, area.flagSize, area.flagSize, 1)) {
      baseValues[c.x][c.y] = s.loadFloat();
    }
  }
  
  void saveState(Session s) throws Exception {
    for (Coord c : Visit.grid(0, 0, area.flagSize, area.flagSize, 1)) {
      s.saveFloat(baseValues[c.x][c.y]);
    }
  }
  
  
  void updateDanger() {
    
    for (Coord c : Visit.grid(0, 0, area.size(), area.size(), Area.FLAG_RES)) {
      Series <Active> at = area.gridActive(area.tileAt(c));
      float danger = 0;
      
      if (at.size() > 0) for (Active a : at) {
        if (! base.isEnemyOf(a.base())) continue;
        float power = TaskCombat.attackPower(a);
        danger += power;
      }
      
      baseValues[c.x / Area.FLAG_RES][c.y / Area.FLAG_RES] = danger;
    }
    
  }
  
  
  public float baseLevel(int tileX, int tileY) {
    return baseValues[tileX / Area.FLAG_RES][tileY / Area.FLAG_RES];
  }
  
  
}


