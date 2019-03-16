

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
    from = AreaMap.loadTarget(active.map(), s);
    maxRange = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(totalDist);
    AreaMap.saveTarget(from, active.map(), s);
    s.saveInt(maxRange);
  }
  
  
  
  /**  Configuration and behavioural updates-
    */
  static TaskExplore configExploration(Actor actor) {
    return configExploration(actor, actor, -1);
  }
  
  
  static TaskExplore configExploration(Actor actor, Target from, int range) {
    if (actor == null || from == null) return null;
    
    AreaMap  map  = actor.map;
    AreaFog  fog  = map.fogMap(actor);
    AreaTile goes = fog.pickRandomFogPoint(from, range);
    if (goes == null) return null;
    goes = AreaTile.nearestOpenTile(goes, map);
    if (goes == null) return null;
    
    TaskExplore task = new TaskExplore(actor);
    task.totalDist = (int) AreaMap.distance(actor.at(), goes);
    task.from      = from;
    task.maxRange  = range;
    
    return (TaskExplore) task.configTask(null, null, goes, JOB.EXPLORING, 0);
  }
  
  
  
  /**  Priority-evaluation-
    */
  protected float successPriority() {
    Actor actor = (Actor) active;
    
    if (I.talkAbout == actor) {
      //I.say("??");
    }
    
    float curiosity = (actor.traits.levelOf(TRAIT_CURIOSITY) + 1) / 2;
    float bravery   = (actor.traits.levelOf(TRAIT_BRAVERY  ) + 1) / 2;
    
    float priority = CASUAL;
    priority *= (curiosity + bravery + 1) / 3f;
    return priority;
  }
  

  protected float successChance() {
    Actor actor = (Actor) active;
    float skill = 0;
    skill += actor.traits.levelOf(SKILL_SIGHT) / MAX_SKILL_LEVEL;
    skill *= actor.type().sightRange * 0.5f / AVG_SIGHT;
    skill += actor.type().moveSpeed  * 0.5f / AVG_MOVE_SPEED;
    float chance = Nums.clamp((skill + 0.5f) / 2, 0, 1);
    return chance;
  }
  
  
  protected float failCostPriority() {
    
    Actor actor = (Actor) active;
    AreaTile around = target.at();
    Pathing haven = actor.haven();
    float power = TaskCombat.attackPower(actor);
    
    AreaDanger dangerMap = actor.map().dangerMap(actor);
    float danger = dangerMap.fuzzyLevel(around.x, around.y);
    danger /= danger + power;
    
    if (haven != null && target != null) {
      float dist = AreaMap.distance(target, haven);
      danger += dist / MAX_EXPLORE_DIST;
    }
    
    if (danger <= 0) return 0;
    return danger * PARAMOUNT;
  }
  
  
  
  
  /**  Behaviour-execution-
    */
  float actionRange() {
    Actor actor = (Actor) this.active;
    return actor.sightRange();
  }


  protected void onTarget(Target target) {
    Actor   actor = (Actor) this.active;
    AreaMap map   = actor.map();
    AreaFog fog   = map.fogMap(actor);
    
    fog.liftFog(target.at(), actor.sightRange() * 2);
    
    int range = maxRange > 0 ? maxRange : (int) (actor.sightRange() * 2);
    AreaTile goes = fog.findNearbyFogPoint(from, range);
    if (goes == null) return;
    
    goes = AreaTile.nearestOpenTile(goes, map);
    if (goes == null) return;
    
    if (goes != null && totalDist < MAX_EXPLORE_DIST) {
      totalDist += AreaMap.distance(actor.at(), goes);
      configTask(null, null, goes, JOB.EXPLORING, 0);
    }
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  String animName() {
    return AnimNames.LOOK;
  }
}




