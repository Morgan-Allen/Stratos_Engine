

package game;
import util.*;
import static game.GameConstants.*;



public class TaskCrafting extends Task {
  
  
  public TaskCrafting(Actor actor) {
    super(actor);
  }
  
  
  public TaskCrafting(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  static TaskCrafting configCrafting(Actor actor, BuildingForCrafts venue) {
    TaskCrafting task = new TaskCrafting(actor);
    if (task.configTask(venue, venue, null, JOB.CRAFTING, 10) == null) {
      return null;
    }
    return task;
  }
  
  
  
  //  NOTE:  Used purely for debug purposes, possibly remove later.
  public static int totalCraftTime = 0;
  public static double totalProgInc = 0;
  
  
  protected void onVisit(Building visits) {
    Actor actor = (Actor) this.active;
    
    BuildingForCrafts venue = (BuildingForCrafts) visits;
    if (! venue.canAdvanceCrafting()) return;
    
    Trait skill     = venue.type().craftSkill;
    float skillMult = actor.levelOf(skill) / MAX_SKILL_LEVEL;
    
    float progInc = 1f + (1f * skillMult);
    progInc *= 1f / venue.type().craftTime;
    
    totalCraftTime += 1;
    totalProgInc += progInc;
    
    actor.gainXP(skill, 1 * CRAFT_XP_PERCENT / 100f);
    
    for (Good need : venue.needed()) {
      venue.addInventory(0 - progInc, need);
    }
    
    BuildingForCrafts.ItemOrder order = venue.nextUnfinishedOrder();
    
    if (order != null) {
      float progress = order.progress;
      progress = Nums.min(progress + progInc, 1);
      order.progress = progress;
    }
    
    else {
      float progress  = venue.craftProgress;
      progress = Nums.min(progress + progInc, 1);
      
      if (progress >= 1) {
        for (Good made : venue.produced()) {
          if (venue.inventory(made) >= venue.stockLimit(made)) continue;
          venue.addInventory(1, made);
          venue.base().makeTotals.add(1, made);
        }
        progress = 0;
      }
      
      venue.craftProgress = progress;
    }
  }
  
}




