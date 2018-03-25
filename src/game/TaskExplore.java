

package game;
import static game.GameConstants.*;
import graphics.common.*;
import util.*;




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
    from = Area.loadTarget(active.map(), s);
    maxRange = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(totalDist);
    Area.saveTarget(from, active.map(), s);
    s.saveInt(maxRange);
  }
  
  
  
  /**  Configuration and behavioural updates-
    */
  static TaskExplore configExploration(Actor actor) {
    return configExploration(actor, actor, -1);
  }
  
  
  static TaskExplore configExploration(Actor actor, Target from, int range) {
    Area     map  = actor.map;
    AreaFog  fog  = map.fogMap(actor.base(), true);
    AreaTile goes = fog.pickRandomFogPoint(from, range);
    if (goes == null) return null;
    goes = AreaTile.nearestOpenTile(goes, map);
    if (goes == null) return null;
    
    TaskExplore task = new TaskExplore(actor);
    task.totalDist = (int) Area.distance(actor.at(), goes);
    task.from      = from;
    task.maxRange  = range;
    
    return (TaskExplore) task.configTask(null, null, goes, JOB.EXPLORING, 0);
  }
  
  
  float actionRange() {
    Actor actor = (Actor) this.active;
    return actor.sightRange();
  }
  
  
  protected void onTarget(Target target) {
    Actor   actor = (Actor) this.active;
    Area    map   = actor.map();
    AreaFog fog   = map.fogMap(actor.base(), true);
    
    fog.liftFog(target.at(), actor.sightRange());
    
    int range = maxRange > 0 ? maxRange : (int) (actor.sightRange() * 2);
    AreaTile goes = fog.findNearbyFogPoint(from, range);
    if (goes == null) return;
    
    goes = AreaTile.nearestOpenTile(goes, map);
    if (goes == null) return;
    
    if (goes != null && totalDist < MAX_EXPLORE_DIST) {
      totalDist += Area.distance(actor.at(), goes);
      configTask(null, null, goes, JOB.EXPLORING, 0);
    }
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  String animName() {
    return AnimNames.LOOK;
  }
}




