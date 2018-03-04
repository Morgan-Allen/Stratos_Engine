

package game;
import util.*;
import static game.GameConstants.*;
import static game.BuildingForCrafts.*;
import graphics.common.*;



public class TaskCrafting extends Task {
  
  
  final BuildingForCrafts venue;
  final Recipe recipe;
  ItemOrder order;
  
  
  
  public TaskCrafting(Actor actor, BuildingForCrafts venue, Recipe recipe) {
    super(actor);
    this.venue  = venue;
    this.recipe = recipe;
  }
  
  
  public TaskCrafting(Session s) throws Exception {
    super(s);
    venue  = (BuildingForCrafts) s.loadObject();
    recipe = venue.type().recipes[s.loadInt()];
    order  = venue.orders.atIndex(s.loadInt());
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(Visit.indexOf(recipe, venue.type().recipes));
    s.saveInt(venue.orders.indexOf(order));
  }
  
  
  static TaskCrafting nextCraftingTask(Actor actor, BuildingForCrafts venue) {
    Pick <TaskCrafting> pick = new Pick();
    
    for (Recipe recipe : venue.type().recipes) {
      
      boolean anyRoom = false, allMaterials = true;
      ItemOrder fills = null;
      Good made = recipe.made;
      float supply = venue.base().inventory(made) + 0;
      float demand = venue.base().needLevel(made) + 1;
      float rating = 2.0f - Nums.clamp(supply / demand, 0, 1);
      
      if (venue.inventory(made) < venue.stockLimit(made)) {
        anyRoom = true;
      }
      else for (ItemOrder order : venue.orders()) {
        if (order.progress < 1 && order.itemType == made) {
          anyRoom = true;
          fills = order;
          rating += 10f;
          break;
        }
      }
      for (Good need : recipe.inputs) {
        if (venue.inventory(need) <= 0) allMaterials = false;
      }
      if (fills == null && ! Visit.arrayIncludes(venue.produced(), made)) {
        continue;
      }
      
      if (anyRoom && allMaterials) {
        TaskCrafting task = configCrafting(actor, venue, recipe, fills);
        pick.compare(task, rating);
      }
    }
    
    return pick.result();
  }
  
  
  static TaskCrafting configCrafting(
    Actor actor, BuildingForCrafts venue, Recipe recipe, ItemOrder fills
  ) {
    TaskCrafting task = new TaskCrafting(actor, venue, recipe);
    if (task.configTask(venue, venue, null, JOB.CRAFTING, 10) == null) {
      task.order = fills;
      return null;
    }
    return task;
  }
  
  
  
  //  NOTE:  Used purely for debug purposes, possibly remove later.
  public static int totalCraftTime = 0;
  public static double totalProgInc = 0;
  
  

  protected void onVisit(Building visits) {
    
    Actor actor     = (Actor) this.active;
    float maxAmount = order == null ? venue.stockLimit(recipe.made) : 1;
    float progress  = order == null ? venue.inventory(recipe.made) : order.progress;
    Trait skill     = recipe.craftSkill;
    float skillMult = actor.levelOf(skill) / MAX_SKILL_LEVEL;
    
    //I.say("Progress on "+recipe.made+": "+progress);
    
    float progInc = 1f + (1f * skillMult);
    progInc *= 1f / recipe.craftTime;
    progInc = Nums.clamp(progInc, 0, maxAmount - progress);
    
    totalCraftTime += 1;
    totalProgInc += progInc;
    
    actor.gainXP(skill, 1 * CRAFT_XP_PERCENT / 100f);
    
    for (Good need : recipe.inputs) {
      venue.addInventory(0 - progInc, need);
    }
    
    if (order == null) {
      venue.setInventory(recipe.made, progress + progInc);
    }
    else {
      order.progress = progress;
    }
    venue.base().makeTotals.add(progInc, recipe.made);
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  String animName() {
    return AnimNames.BUILD;
  }
}






