

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;




public class TaskExplore extends Task {
  
  
  /**  Data fields, construction and save/load methods-
    */
  int totalDist = 0;
  
  
  public TaskExplore(Actor actor) {
    super(actor);
  }
  
  
  public TaskExplore(Session s) throws Exception {
    super(s);
    totalDist += s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(totalDist);
  }
  
  
  
  /**  Configuration and behavioural updates-
    */
  TaskExplore configExploration() {
    CityMap map  = actor.map;
    Tile    goes = map.fog.pickRandomFogPoint(actor);
    if (goes == null || map.blocked(goes.x, goes.y)) return null;
    
    totalDist += CityMap.distance(actor.at(), goes);
    return (TaskExplore) configTask(null, null, goes, JOB.EXPLORING, 0);
  }
  
  
  protected void onTarget(Target target) {
    CityMap map = actor.map;
    Tile goes = map.fog.findNearbyFogPoint(actor, actor.type.sightRange + 2);
    
    if (goes != null && totalDist < MAX_EXPLORE_DIST) {
      totalDist += CityMap.distance(actor.at(), goes);
      configTask(null, null, goes, JOB.EXPLORING, 0);
    }
  }
  
}








