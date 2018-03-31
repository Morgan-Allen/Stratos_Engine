

package game;
import static game.GameConstants.*;
import static game.Area.*;
import static game.BuildingForGather.*;
import graphics.common.*;
import util.*;



public class TaskGathering extends Task {
  
  
  
  BuildingForGather store;
  Good plants = null;
  
  
  public TaskGathering(Actor actor, BuildingForGather store) {
    super(actor);
    this.store = store;
  }
  
  
  public TaskGathering(Session s) throws Exception {
    super(s);
    store = (BuildingForGather) s.loadObject();
    plants = (Good) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(store);
    s.saveObject(plants);
  }
  
  
  
  static boolean canPlant(BuildingForGather store) {
    boolean canPlant = false;
    for (Good g : store.type().produced) if (g.isCrop) canPlant = true;
    return canPlant;
  }
  
  
  static Task pickPlantPoint(
    BuildingForGather store, Actor actor, boolean close, boolean start
  ) {
    //  TODO:  Add an extra step for seed collection...
    
    if (! canPlant(store)) return null;
    
    Pick <AreaTile> pick = new Pick();
    Area map = store.map();
    Pathing from = Task.pathOrigin(actor);
    
    for (Plot p : store.plots()) {
      for (AreaTile t : map.tilesUnder(p)) {
        if (t == null || t.hasFocus()) continue;
        if (! map.pathCache.pathConnects(from, t, true, false)) continue;
        
        if (! store.canPlant(t)) continue;
        
        Good g = store.seedType(t);
        Element e = map.above(t);
        if (e != null && e.type() == g && e.growLevel() >= 0) continue;
        
        float rating = Area.distancePenalty(actor, t);
        pick.compare(t, rating);
      }
    }
    
    if (pick.empty()) return null;
    
    AreaTile goes = pick.result();
    TaskGathering task = new TaskGathering(actor, store);
    
    if (task.configTask(store, null, goes, JOB.PLANTING, 2) != null) {
      task.plants = store.seedType(goes);
      return task;
    }
    
    return null;
  }
  
  
  static Task pickNextCrop(
    BuildingForGather store, Actor actor, boolean close, Object... cropTypes
  ) {
    if (Visit.empty(cropTypes)) return null;

    Area map = store.map();
    int spaceTaken = 0;
    Pathing from = Task.pathOrigin(actor);
    
    for (Good g : store.type().produced) {
      spaceTaken += store.inventory(g);
    }
    if (spaceTaken >= store.type().maxStock) return null;
    
    if (store.plots().size() > 0) {
      Pick <AreaTile> pick = new Pick(0);

      for (Plot p : store.plots()) {
        for (AreaTile t : map.tilesUnder(p)) {
          if (t == null || t.hasFocus()) continue;
          if (! map.pathCache.pathConnects(from, t, true, false)) continue;
          
          Element above = t.above;
          if (above == null || above.growLevel() <= 0) continue;
          if (! Visit.arrayIncludes(cropTypes, above.type().yields)) continue;
          
          float rating = Area.distancePenalty(actor, t);
          rating *= above.growLevel();
          pick.compare(t, rating);
        }
      }
      
      if (pick.empty()) return null;
      
      AreaTile goes = pick.result();
      TaskGathering task = new TaskGathering(actor, store);
      
      if (task.configTask(store, null, goes, JOB.HARVEST, 2) != null) {
        return task;
      }
    }
    
    else {
      AreaFlagging flagging = store.map.flagging.get(store.type().gatherFlag);
      if (flagging == null) return null;
      
      AreaTile goes = null;
      if (close) goes = flagging.findNearbyPoint(actor, AVG_GATHER_RANGE);
      else goes = flagging.pickRandomPoint(store, store.type().maxDeliverRange);
      Element above = goes == null ? null : goes.above;
      
      if (above == null) return null;
      if (! Visit.arrayIncludes(cropTypes, above.type().yields)) return null;
      
      TaskGathering task = new TaskGathering(actor, store);
      if (task.configTask(store, null, goes, JOB.HARVEST, 2) != null) {
        return task;
      }
    }
    
    return null;
  }
  
  
  protected void onTarget(Target other) {
    if (other == null) return;
    
    Actor    actor = (Actor) this.active;
    AreaTile at    = other.at();
    Area     map   = actor.map();
    
    if (actor.jobType() == JOB.RETURNING && store.actorIsHere(actor)) {
      onVisit(store);
      return;
    }
    
    if (actor.jobType() == JOB.PLANTING) {
      Recipe recipe     = TaskCrafting.recipeFor(plants.yields, store);
      Trait  skill      = recipe.craftSkill;
      float  skillBonus = actor.traits.levelOf(skill) / MAX_SKILL_LEVEL;
      float  multXP     = plants.isCrop ? FARM_XP_PERCENT : GATHR_XP_PERCENT;
      //
      //  First, initialise the crop:
      Element crop = at.above;
      if (crop == null || crop.type() != plants) {
        crop = new Element(plants);
        crop.enterMap(map, at.x, at.y, 1, store.base());
      }
      crop.setGrowLevel(0 + skillBonus / 4);
      actor.traits.gainXP(skill, 1 * multXP / 100);
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
      Element above = map.above(at);
      if (above == null || above.type().yields == null) return;
      
      Type   plants     = above.type();
      Recipe recipe     = TaskCrafting.recipeFor(plants.yields, store);
      Trait  skill      = recipe.craftSkill;
      float  skillBonus = actor.traits.levelOf(skill) / MAX_SKILL_LEVEL;
      float  multXP     = plants.isCrop ? FARM_XP_PERCENT : GATHR_XP_PERCENT;
      
      Good gathers = plants.yields;
      float yield  = plants.yieldAmount;
      yield *= plants.isCrop ? above.growLevel() : 1;
      yield *= 1 + (skillBonus * 0.5f);
      
      actor.incCarried(gathers, yield);
      actor.traits.gainXP(skill, 1 * multXP / 100);
      
      if      (plants.isCrop      ) above.setGrowLevel(-1);
      else if (plants.growRate > 0) above.setGrowLevel( 0);
      
      if (actor.carried(gathers) >= 2) {
        returnToStore();
      }
      else {
        Task pick = pickNextCrop(store, actor, true, plants.yields);
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
    Actor actor = (Actor) this.active;
    if (visits == store) {
      for (Good made : store.type().produced) {
        actor.offloadGood(made, store);
      }
    }
  }
  
  
  
  /**  Graphical, interface and debug methods-
    */
  String animName() {
    return AnimNames.BUILD;
  }
  
  
  Target faceTarget() {
    return super.faceTarget();
  }
}










