

package game;
import util.*;
import static game.AreaMap.*;
import static game.GameConstants.*;




public class TaskExplore extends Task {
  
  
  /**  Data fields, construction and save/load methods-
    */
  int totalDist = 0;
  Target from = null;
  int maxRange = -1;
  
  
  public TaskExplore(Actor actor) {
    super(actor);
  }
  
  
  public TaskExplore(Session s) throws Exception {
    super(s);
    totalDist += s.loadInt();
    from = (Target) s.loadObject();
    maxRange = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(totalDist);
    s.saveObject(from);
    s.saveInt(maxRange);
  }
  
  
  
  /**  Configuration and behavioural updates-
    */
  static TaskExplore configExploration(Actor actor) {
    return configExploration(actor, actor, -1);
  }
  
  
  static TaskExplore configExploration(Actor actor, Target from, int range) {
    AreaMap map  = actor.map;
    Tile    goes = map.fog.pickRandomFogPoint(from, range);
    if (goes == null) return null;
    goes = Tile.nearestOpenTile(goes, map);
    if (goes == null) return null;
    
    TaskExplore task = new TaskExplore(actor);
    task.totalDist = (int) AreaMap.distance(actor.at(), goes);
    task.from      = from;
    task.maxRange  = range;
    
    return (TaskExplore) task.configTask(null, null, goes, JOB.EXPLORING, 0);
  }
  
  
  protected void onTarget(Target target) {
    Actor actor = (Actor) this.active;
    AreaMap map = actor.map();
    
    int range = maxRange > 0 ? maxRange : (int) (actor.sightRange() * 2);
    Tile goes = map.fog.findNearbyFogPoint(from, range);
    if (goes == null) return;
    
    goes = Tile.nearestOpenTile(goes, map);
    if (goes == null) return;

    if (goes != null && totalDist < MAX_EXPLORE_DIST) {
      totalDist += AreaMap.distance(actor.at(), goes);
      configTask(null, null, goes, JOB.EXPLORING, 0);
    }
  }
  
}






