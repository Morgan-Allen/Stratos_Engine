

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
  
  
  
  /**  Priority-evaluation-
    */
  protected float successPriority() {
    Actor actor = (Actor) active;
    
    float curiosity = (actor.traits.levelOf(TRAIT_CURIOSITY) + 2) / 2;
    float bravery   = (actor.traits.levelOf(TRAIT_BRAVERY  ) + 2) / 2;
    
    float priority = ROUTINE;
    priority *= (curiosity + bravery + 1) / 2f;
    return priority;
  }
  

  protected float successChance() {
    Actor actor = (Actor) active;
    float skill = 0;
    skill += actor.type().sightRange * 0.5f / AVG_SIGHT;
    skill += actor.type().moveSpeed  * 0.5f / AVG_MOVE_SPEED;
    float chance = Nums.clamp((skill + 0.5f) / 2, 0, 1);
    return chance;
  }
  
  
  protected float failCostPriority() {
    Actor actor = (Actor) active;
    AreaTile around = target.at();
    
    AreaDanger dangerMap = actor.map().dangerMap(actor.base(), true);
    float danger = dangerMap.fuzzyLevel(around.x, around.y);
    if (danger <= 0) return 0;
    
    float power = TaskCombat.attackPower(actor);
    return (danger / (danger + power)) * PARAMOUNT;
  }
  
  
  
  
  /**  Behaviour-execution-
    */
  float actionRange() {
    Actor actor = (Actor) this.active;
    return actor.sightRange();
  }


  protected void onTarget(Target target) {
    Actor   actor = (Actor) this.active;
    Area    map   = actor.map();
    AreaFog fog   = map.fogMap(actor.base(), true);
    
    fog.liftFog(target.at(), actor.sightRange() * 2);
    
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




