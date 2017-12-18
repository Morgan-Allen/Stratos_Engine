

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
  
  
  
  static boolean pickPlantPoint(
    Building store, Actor actor, boolean close, boolean start
  ) {
    if (start && actor.inside != store) return false;
    
    boolean canPlant = false;
    for (Good g : store.type.produced) if (g.isCrop) canPlant = true;
    if (! canPlant) return false;
    
    CityMapFlagging flagging = store.map.flagging.get(NEED_PLANT);
    if (flagging == null) return false;
    
    Tile goes = null;
    if (close) goes = flagging.findNearbyPoint(actor, AVG_GATHER_RANGE);
    else goes = flagging.pickRandomPoint(store, store.type.maxDeliverRange);
    
    if (goes == null) return false;
    
    TaskGathering task = new TaskGathering(actor, store);
    if (task.configTask(store, null, goes, JOB.PLANTING, 2) != null) {
      actor.assignTask(task);
      return true;
    }
    
    return false;
  }
  
  
  static boolean pickNextCrop(
    Building store, Actor actor, boolean close, Good... cropTypes
  ) {
    if (Visit.empty(cropTypes)) return false;
    
    int spaceTaken = 0;
    for (Good g : store.type.produced) {
      spaceTaken += store.inventory.valueFor(g);
    }
    if (spaceTaken >= store.type.maxStock) return false;
    
    CityMapFlagging flagging = store.map.flagging.get(store.type.gatherFlag);
    if (flagging == null) return false;
    
    Tile goes = null;
    if (close) goes = flagging.findNearbyPoint(actor, AVG_GATHER_RANGE);
    else goes = flagging.pickRandomPoint(store, store.type.maxDeliverRange);
    Element above = goes == null ? null : goes.above;
    
    if (above == null) return false;
    if (! Visit.arrayIncludes(cropTypes, above.type.yields)) return false;
    
    TaskGathering task = new TaskGathering(actor, store);
    if (task.configTask(store, null, goes, JOB.HARVEST, 2) != null) {
      actor.assignTask(task);
      return true;
    }
    
    return false;
  }
  
  
  protected void onTarget(Target other) {
    if (other == null) return;
    
    if (actor.jobType() == JOB.RETURNING && store.actorIsHere(actor)) {
      onVisit(store);
      return;
    }
    
    Element above = other.at().above;
    if (above == null || above.type.yields == null) return;
    
    Trait skill      = store.type.craftSkill;
    float skillBonus = actor.levelOf(skill) / MAX_SKILL_LEVEL;
    float multXP     = above.type.isCrop ? FARM_XP_PERCENT : GATHR_XP_PERCENT;
    
    if (actor.jobType() == JOB.PLANTING) {
      //
      //  First, initialise the crop:
      above.setGrowLevel(0);
      actor.gainXP(skill, 1 * multXP / 100);
      //
      //  Then pick another point to sow:
      if (! pickPlantPoint(store, actor, true, false)) {
        returnToStore();
      }
    }
    
    if (actor.jobType() == JOB.HARVEST) {
      
      if      (above.type.isCrop      ) above.setGrowLevel(-1);
      else if (above.type.growRate > 0) above.setGrowLevel( 0);
      
      float yield = above.type.yieldAmount;
      yield *= 1 + (skillBonus * 0.5f);
      
      actor.carried      = above.type.yields;
      actor.carryAmount += yield;
      
      actor.gainXP(skill, 1 * multXP / 100);
      ///I.say(actor+" harvested "+actor.carryAmount+" of "+above.type);
      
      if (actor.carryAmount >= 2) {
        returnToStore();
      }
      else if (! pickNextCrop(store, actor, true, actor.carried)) {
        returnToStore();
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
      for (Good made : store.type.produced) {
        actor.offloadGood(made, store);
      }
    }
  }
  
}








