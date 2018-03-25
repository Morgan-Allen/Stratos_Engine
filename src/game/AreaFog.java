

package game;
import static game.Area.*;
import static game.GameConstants.*;
import graphics.common.*;
import graphics.terrain.*;
import util.*;



public class AreaFog {
  
  
  /**  Data fields, setup and save/load methods-
    */
  final static int MAX_FOG = 100;
  
  final Base base;
  final Area map;
  
  byte fogVals[][];
  byte oldVals[][];
  byte maxVals[][];
  AreaFlagging flagMap;
  
  float viewVals[][];
  private FogOverlay fogOver;
  
  
  
  AreaFog(Base base, Area map) {
    this.base = base;
    this.map  = map;
  }
  
  
  void performSetup(int size) {
    this.fogVals = new byte[size][size];
    this.oldVals = new byte[size][size];
    this.maxVals = new byte[size][size];
    this.flagMap = new AreaFlagging(map, "max. fog", MAX_FOG);
    flagMap.setupWithSize(size);
    
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      flagMap.setFlagVal(c.x, c.y, MAX_FOG);
    }
    
    this.viewVals = new float[size][size];
  }
  
  
  void loadState(Session s) throws Exception {
    s.loadByteArray(fogVals);
    s.loadByteArray(oldVals);
    s.loadByteArray(maxVals);
    flagMap.loadState(s);
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveByteArray(fogVals);
    s.saveByteArray(oldVals);
    s.saveByteArray(maxVals);
    flagMap.saveState(s);
  }
  
  
  
  /**  Regular updates-
    */
  void liftFog(AreaTile around, float range) {
    if (! isToggled()) return;
    
    Box2D area = new Box2D(around.x, around.y, 0, 0);
    area.expandBy(Nums.round(range, 1, true));
    
    for (Coord c : Visit.grid(area)) {
      AreaTile t = map.tileAt(c.x, c.y);
      float distance = distance(t, around);
      if (distance > range) continue;
      if (distance < 0) distance = 0;
      byte newVal = (byte) (MAX_FOG * (1f - (distance / range)));
      byte oldVal = fogVals[c.x][c.y];
      if (newVal > oldVal) fogVals[c.x][c.y] = newVal;
    }
  }
  
  
  void updateFog() {
    if (! isToggled()) {
      return;
    }
    
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      byte oldMax = maxVals[c.x][c.y];
      byte val = oldVals[c.x][c.y] = fogVals[c.x][c.y];
      fogVals[c.x][c.y] = 0;
      
      if (val > oldMax) {
        maxVals[c.x][c.y] = val;
        flagMap.setFlagVal(c.x, c.y, 0);
      }
      
      viewVals[c.x][c.y] = ((val * 2f) + (oldMax * 1f)) / (3 * MAX_FOG);
    }
    
  }
  
  
  
  /**  Exploration-related queries:
    */
  AreaTile pickRandomFogPoint(Target near, int range) {
    if (! isToggled()) return null;
    return flagMap.pickRandomPoint(near, range);
  }
  
  
  AreaTile findNearbyFogPoint(Target near, int range) {
    if (! isToggled()) return null;
    return flagMap.findNearbyPoint(near, range);
  }
  
  
  
  /**  Common queries-
    */
  private boolean isToggled() {
    return map.world.settings.toggleFog;
  }
  
  
  public float sightLevel(AreaTile t) {
    if (! isToggled()) return 1;
    if (t == null) return 0;
    return oldVals[t.x][t.y] * 1f / MAX_FOG;
  }
  
  
  public float maxSightLevel(AreaTile t) {
    if (! isToggled()) return 1;
    if (t == null) return 0;
    return maxVals[t.x][t.y] * 1f / MAX_FOG;
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public void renderFor(float renderTime, Rendering rendering) {
    if (fogOver == null) {
      fogOver = new FogOverlay(map.size, map.size);
    }
    fogOver.updateVals(renderTime, viewVals);
    fogOver.registerFor(rendering);
  }
}














