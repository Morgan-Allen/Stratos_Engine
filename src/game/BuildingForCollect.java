

package game;
import util.*;
import static game.Task.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class BuildingForCollect extends BuildingForCrafts {
  
  
  /**  Data fields, construction and save/load methods-
    */
  BuildingForCollect(Type type) {
    super(type);
  }
  
  
  public BuildingForCollect(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Assigning actor behaviours:
    */
  public void selectActorBehaviour(Actor actor) {
    for (Good g : type.produced) {
      if (pickNextCollection(actor, g)) return;
    }
  }
  
  
  boolean pickNextCollection(Actor actor, Good g) {
    Pick <Building> pick = new Pick();
    Tile entrance = mainEntrance();
    
    for (Building b : map.buildings) {
      if (! b.type.hasFeature(IS_HOUSING)) continue;
      float distW = CityMap.distance(actor.at(), b.mainEntrance());
      float distB = CityMap.distance(entrance  , b.mainEntrance());
      if (distB > type.maxDeliverRange) continue;
      
      int amount = (int) b.inventory.valueFor(g);
      if (amount <= 0) continue;
      
      pick.compare(b, amount * CityMap.distancePenalty(distW));
    }
    
    if (! pick.empty()) {
      actor.embarkOnVisit(pick.result(), 1, Task.JOB.VISITING, this);
      return true;
    }
    return false;
  }
  
  
  public void actorVisits(Actor actor, Building visits) {
    
    //  TODO:  Replace this with a custom task...
    
    if (visits == this) for (Good made : type.produced) {
      actor.offloadGood(made, this);
      int amount = (int) inventory.valueFor(made);
      
      boolean taxes = made == CASH;
      if (taxes && amount > 0) {
        map.city.currentFunds += amount;
        inventory.add(0 - amount, made);
      }
      else if (amount > type.maxStock) {
        inventory.set(made, type.maxStock);
      }
    }
    
    else if (actor.jobType() == JOB.VISITING) {
      for (Good g : type.produced) {
        boolean taxes = g == CASH;
        int carryLimit = taxes ? 100 : 10;
        
        float amount = visits.inventory.valueFor(g);
        if (amount <= 0) continue;
        actor.pickupGood(g, amount, visits);
        
        if (actor.carryAmount >= carryLimit) {
          returnActorHere(actor);
          return;
        }
        else if (pickNextCollection(actor, g)) {
          return;
        }
        else {
          returnActorHere(actor);
          return;
        }
      }
    }
  }
  
}





