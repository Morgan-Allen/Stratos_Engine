

package game;
import graphics.common.*;
import graphics.terrain.*;
import util.*;
import static game.AreaMap.*;
import static game.WorldCalendar.*;
import static game.GameConstants.*;



public class CityMapFog {
  
  
  /**  Data fields, setup and save/load methods-
    */
  final static int MAX_FOG = 100;
  
  AreaMap map;
  
  int  dayState = -1;
  byte fogVals[][];
  byte oldVals[][];
  CityMapFlagging maxMap;
  
  float floatVals[][];
  private FogOverlay fogOver;
  
  
  
  
  CityMapFog(AreaMap map) {
    this.map = map;
  }
  
  
  void loadState(Session s) throws Exception {
    dayState = s.loadInt();
    s.loadByteArray(fogVals);
    s.loadByteArray(oldVals);
    maxMap.loadState(s);
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveInt(dayState);
    s.saveByteArray(fogVals);
    s.saveByteArray(oldVals);
    maxMap.saveState(s);
  }
  
  
  void performSetup(int size) {
    this.fogVals = new byte[size][size];
    this.oldVals = new byte[size][size];
    this.maxMap = new CityMapFlagging(map, "max. fog", MAX_FOG);
    maxMap.setupWithSize(size);
    
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      maxMap.setFlagVal(c.x, c.y, MAX_FOG);
    }
    
    this.floatVals = new float[size][size];
  }
  
  
  
  /**  Regular updates-
    */
  void liftFog(Tile around, float range) {
    if (! isToggled()) return;
    
    Box2D area = new Box2D(around.x, around.y, 0, 0);
    area.expandBy(Nums.round(range, 1, true));
    
    for (Coord c : Visit.grid(area)) {
      Tile t = map.tileAt(c.x, c.y);
      float distance = distance(t, around);
      if (distance > range) continue;
      fogVals[c.x][c.y] = MAX_FOG;
    }
  }
  
  
  void updateFog() {
    dayState = WorldCalendar.dayState(map.time);
    
    if (! isToggled()) {
      return;
    }
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      byte val = oldVals[c.x][c.y] = fogVals[c.x][c.y];
      fogVals[c.x][c.y] = 0;
      
      int oldVal = maxMap.flagVal(c.x, c.y);
      int newVal = MAX_FOG - val;
      if (oldVal > newVal) maxMap.setFlagVal(c.x, c.y, newVal);
      
      floatVals[c.x][c.y] = 1 - (newVal * 1f / MAX_FOG);
    }
  }
  
  
  
  /**  Exploration-related queries:
    */
  Tile pickRandomFogPoint(Target near, int range) {
    if (! isToggled()) return null;
    return maxMap.pickRandomPoint(near, range);
  }
  
  
  Tile findNearbyFogPoint(Target near, int range) {
    if (! isToggled()) return null;
    return maxMap.findNearbyPoint(near, range);
  }
  
  
  
  /**  Common queries-
    */
  private boolean isToggled() {
    return map.world.settings.toggleFog;
  }
  
  
  public float sightLevel(Tile t) {
    if (! isToggled()) return 1;
    if (t == null) return 0;
    return oldVals[t.x][t.y] * 1f / MAX_FOG;
  }
  
  
  public float maxSightLevel(Tile t) {
    if (! isToggled()) return 1;
    if (t == null) return 0;
    return 1 - (maxMap.flagVal(t.x, t.y) / MAX_FOG);
  }
  
  
  public boolean day() {
    return dayState == STATE_LIGHT;
  }
  
  
  public boolean night() {
    return dayState == STATE_DARKNESS;
  }
  
  
  public float lightLevel() {
    if (day  ()) return 1;
    if (night()) return 0;
    return 0.5f;
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public void renderFor(float renderTime, Rendering rendering) {
    if (fogOver == null) {
      fogOver = new FogOverlay(map.size, map.size);
    }
    fogOver.updateVals(renderTime, floatVals);
    fogOver.registerFor(rendering);
  }
}














