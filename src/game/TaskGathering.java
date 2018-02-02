

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class TaskGathering extends Task {
  
  
  
  Building store;
  
  
  public TaskGathering(Actor actor, Building store) {
    super(actor);
    this.store = store;
  }
  
  
  public TaskGathering(Session s) throws Exception {
    super(s);
    store = (Building) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(store);
  }
  
  
  
  static Task pickPlantPoint(
    Building store, Actor actor, boolean close, boolean start
  ) {
    if (start && actor.inside() != store) return null;
    
    boolean canPlant = false;
    for (Good g : store.type().produced) if (g.isCrop) canPlant = true;
    if (! canPlant) return null;
    
    CityMapFlagging flagging = store.map.flagging.get(NEED_PLANT);
    if (flagging == null) return null;
    
    Tile goes = null;
    if (close) goes = flagging.findNearbyPoint(actor, AVG_GATHER_RANGE);
    else goes = flagging.pickRandomPoint(store, store.type().maxDeliverRange);
    if (goes == null) return null;
    
    TaskGathering task = new TaskGathering(actor, store);
    if (task.configTask(store, null, goes, JOB.PLANTING, 2) != null) {
      return task;
    }
    
    return null;
  }
  
  
  static Task pickNextCrop(
    Building store, Actor actor, boolean close, Object... cropTypes
  ) {
    if (Visit.empty(cropTypes)) return null;
    
    int spaceTaken = 0;
    for (Good g : store.type().produced) {
      spaceTaken += store.inventory(g);
    }
    if (spaceTaken >= store.type().maxStock) return null;
    
    CityMapFlagging flagging = store.map.flagging.get(store.type().gatherFlag);
    if (flagging == null) return null;
    
    Tile goes = null;
    if (close) goes = flagging.findNearbyPoint(actor, AVG_GATHER_RANGE);
    else goes = flagging.pickRandomPoint(store, store.type().maxDeliverRange);
    Element above = goes == null ? null : goes.above;
    
    if (above == null) return null;
    if (! Visit.arrayIncludes(cropTypes, above.type().yields)) return null;
    
    TaskGathering task = new TaskGathering(actor, store);
    if (task.configTask(store, null, goes, JOB.HARVEST, 2) != null) {
      return task;
    }
    
    return null;
  }
  
  
  protected void onTarget(Target other) {
    if (other == null) return;
    
    if (actor.jobType() == JOB.RETURNING && store.actorIsHere(actor)) {
      onVisit(store);
      return;
    }
    
    Element above = other.at().above;
    if (above == null || above.type().yields == null) return;
    
    Trait skill      = store.type().craftSkill;
    float skillBonus = actor.levelOf(skill) / MAX_SKILL_LEVEL;
    float multXP     = above.type().isCrop ? FARM_XP_PERCENT : GATHR_XP_PERCENT;
    
    if (actor.jobType() == JOB.PLANTING) {
      //
      //  First, initialise the crop:
      above.setGrowLevel(0);
      actor.gainXP(skill, 1 * multXP / 100);
      //
      //  Then pick another point to sow:
      Task plant = pickPlantPoint(store, actor, true, false);
      if (plant != null) {
        actor.assignTask(plant);
      }
      else {
        returnToStore();
      }
    }
    
    if (actor.jobType() == JOB.HARVEST) {
      
      if      (above.type().isCrop      ) above.setGrowLevel(-1);
      else if (above.type().growRate > 0) above.setGrowLevel( 0);
      
      float yield = above.type().yieldAmount;
      yield *= 1 + (skillBonus * 0.5f);
      
      actor.incCarried(above.type().yields, yield);
      actor.gainXP(skill, 1 * multXP / 100);
      
      if (actor.carryAmount() >= 2) {
        returnToStore();
      }
      else {
        Task pick = pickNextCrop(store, actor, true, actor.carried());
        if (pick != null) {
          actor.assignTask(pick);
        }
        else {
          returnToStore();
        }
      }
    }
  }
  
  
  void returnToStore() {
    if (store.complete()) {
      configTask(store, store, null, JOB.RETURNING, 0);
    }
    else {
      configTask(store, null, store.at(), JOB.RETURNING, 0);
    }
  }
  
  
  protected void onVisit(Building visits) {
    if (visits == store) {
      for (Good made : store.type().produced) {
        actor.offloadGood(made, store);
      }
    }
  }
  
}








