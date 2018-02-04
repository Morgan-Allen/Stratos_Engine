

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
  static TaskExplore configExploration(Actor actor) {
    return configExploration(actor, actor, -1);
  }
  
  
  static TaskExplore configExploration(Actor actor, Target from, int range) {
    CityMap map  = actor.map;
    Tile    goes = map.fog.pickRandomFogPoint(from, range);
    if (goes == null) return null;
    goes = Tile.nearestOpenTile(goes, map);
    if (goes == null) return null;
    
    TaskExplore task = new TaskExplore(actor);
    task.totalDist += CityMap.distance(actor.at(), goes);
    return (TaskExplore) task.configTask(null, null, goes, JOB.EXPLORING, 0);
  }
  
  
  protected void onTarget(Target target) {
    CityMap map = actor.map;
    Tile goes = map.fog.findNearbyFogPoint(actor, actor.type().sightRange + 2);
    if (goes == null) return;
    goes = Tile.nearestOpenTile(goes, map);
    if (goes == null) return;
    
    if (goes != null && totalDist < MAX_EXPLORE_DIST) {
      totalDist += CityMap.distance(actor.at(), goes);
      configTask(null, null, goes, JOB.EXPLORING, 0);
    }
  }
  
}








