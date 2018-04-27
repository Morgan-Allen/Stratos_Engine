

package game;
import util.*;
import static game.GameConstants.*;
import static game.BuildingForCrafts.*;
import graphics.common.*;



public class TaskCrafting extends Task {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final BuildingForCrafts venue;
  final Recipe recipe;
  int orderID = -1;
  int timeSpent = 0;
  
  
  
  public TaskCrafting(Actor actor, BuildingForCrafts venue, Recipe recipe) {
    super(actor);
    this.venue  = venue;
    this.recipe = recipe;
  }
  
  
  public TaskCrafting(Session s) throws Exception {
    super(s);
    venue     = (BuildingForCrafts) s.loadObject();
    recipe    = venue.type().recipes[s.loadInt()];
    orderID   = s.loadInt();
    timeSpent = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(venue);
    s.saveInt(Visit.indexOf(recipe, venue.type().recipes));
    s.saveInt(orderID);
    s.saveInt(timeSpent);
  }
  
  
  
  /**  External factory methods-
    */
  static TaskCrafting nextCraftingTask(Actor actor, BuildingForCrafts venue) {
    Pick <TaskCrafting> pick = new Pick();
    
    for (Recipe recipe : venue.type().recipes) {
      
      boolean anyRoom = false, allMaterials = true;
      ItemOrder fills = null;
      Good made = recipe.made;
      boolean standardGood = Visit.arrayIncludes(venue.produced(), made);
      float supply = venue.base().inventory(made) + 0;
      float demand = venue.base().needLevel(made) + 1;
      float amount = venue.inventory(made);
      float limit  = venue.stockLimit(made);
      float rating = 2.0f - Nums.clamp(supply / demand, 0, 1);
      
      if (standardGood && amount < limit) {
        anyRoom = true;
      }
      else if (! standardGood) for (ItemOrder order : venue.orders()) {
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
    if (fills != null) {
      task.orderID = fills.orderID;
    }
    if (task.configTask(venue, venue, null, JOB.CRAFTING, 5) != null) {
      return task;
    }
    return null;
  }
  
  
  static Recipe recipeFor(Good good, Building store) {
    for (Recipe r : store.type().recipes) {
      if (r.made == good) return r;
    }
    return null;
  }
  
  
  
  /**  Priority-evaluation-
    */
  protected float successPriority() {
    Actor actor = (Actor) active;
    float diligence = (actor.traits.levelOf(TRAIT_DILIGENCE) + 1) / 2;
    float priority = ROUTINE;
    priority *= diligence + 0.5f;
    return priority;
  }
  
  
  
  /**  Behaviour-execution-
    */
  //  NOTE:  Used purely for debug purposes, possibly remove later.
  public static int totalCraftTime = 0;
  public static double totalProgInc = 0;
  

  protected void onVisit(Pathing visits) {
    boolean report = recipe.made.isUsable && false;
    
    ItemOrder order = venue.orderWithID(orderID);
    Actor actor     = (Actor) this.active;
    float maxAmount = order == null ? venue.stockLimit(recipe.made) : 1;
    float progress  = order == null ? venue.inventory(recipe.made) : order.progress;
    Trait skill     = recipe.craftSkill;
    float skillMult = actor.traits.levelOf(skill) / MAX_SKILL_LEVEL;
    
    if (report) {
      I.say("Progress on "+recipe.made+": "+progress+"/"+maxAmount);
      I.add("  #"+order.hashCode());
    }
    
    float progInc = 1f + (1f * skillMult);
    progInc *= 1f / recipe.craftTime;
    progInc = Nums.clamp(progInc, 0, maxAmount - progress);
    boolean didUnit = ((int) progress) < (int) (progress + progInc);
    
    totalCraftTime += 1;
    totalProgInc += progInc;
    
    actor.traits.gainXP(skill, 1 * CRAFT_XP_PERCENT / 100f);
    
    for (Good need : recipe.inputs) {
      venue.addInventory(0 - progInc, need);
    }
    if (order == null) {
      venue.setInventory(recipe.made, progress + progInc);
    }
    else {
      order.progress = progress + progInc;
    }
    venue.base().makeTotals.add(progInc, recipe.made);
    
    if (++timeSpent < DAY_LENGTH && ! didUnit) {
      Task next = TaskCrafting.nextCraftingTask(actor, venue);
      if (next != null) actor.assignTask(next, this);
    }
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  String animName() {
    return AnimNames.BUILD;
  }
}






