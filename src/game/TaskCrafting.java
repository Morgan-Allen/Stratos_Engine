

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
    if (task.configTask(venue, venue, null, JOB.CRAFTING, 1) == null) {
      return null;
    }
    return task;
  }
  
  
  
  protected void onVisit(Building visits) {
    
    BuildingForCrafts venue = (BuildingForCrafts) visits;
    ActorAsPerson crafts = (ActorAsPerson) actor;
    if (! venue.canAdvanceCrafting()) return;
    
    Trait skill = venue.type.craftSkill;
    float progress = venue.craftProgress;
    float progInc = 1f / venue.type.craftTime;
    float skillMult = crafts.levelOf(skill) / MAX_SKILL_LEVEL;
    
    progInc *= 1f + (1f * skillMult);
    crafts.gainXP(skill, 1 * CRAFT_XP_PERCENT / 100f);
    
    for (Good need : venue.needed()) {
      venue.inventory.add(0 - progInc, need);
    }
    progress = Nums.min(progress + progInc, 1);
    
    if (progress >= 1) {
      for (Good made : venue.produced()) {
        if (venue.inventory.valueFor(made) >= venue.stockLimit(made)) continue;
        venue.inventory.add(1, made);
        venue.map.city.makeTotals.add(1, made);
      }
      progress = 0;
    }
    
    venue.craftProgress = progress;
  }
  
  
  
}




