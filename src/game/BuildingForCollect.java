

package game;
import util.*;
import static game.Task.*;
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
  
  
  
  /**  Assigning walker behaviours:
    */
  public void selectWalkerBehaviour(Actor walker) {
    
    for (Good g : type.produced) {
      if (pickNextCollection(walker, g)) return;
    }
    
    super.selectWalkerBehaviour(walker);
  }
  
  
  void advanceProduction() {
    return;
  }
  
  
  boolean pickNextCollection(Actor walker, Good g) {
    Pick <Building> pick = new Pick();
    
    for (Building b : map.buildings) {
      if (! b.type.hasFeature(IS_HOUSING)) continue;
      float distW = CityMap.distance(walker.at, b.entrance);
      float distB = CityMap.distance(entrance , b.entrance);
      if (distB > type.maxDeliverRange) continue;
      
      int amount = (int) b.inventory.valueFor(g);
      if (amount <= 0) continue;
      
      pick.compare(b, amount * CityMap.distancePenalty(distW));
    }
    
    if (! pick.empty()) {
      walker.embarkOnVisit(pick.result(), 1, Task.JOB.VISITING, this);
      return true;
    }
    return false;
  }
  
  
  public void walkerVisits(Actor walker, Building visits) {
    
    if (visits == this) for (Good made : type.produced) {
      walker.offloadGood(made, this);
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
    
    else if (walker.jobType() == JOB.VISITING) {
      for (Good g : type.produced) {
        boolean taxes = g == CASH;
        int carryLimit = taxes ? 100 : 10;
        
        float amount = visits.inventory.valueFor(g);
        if (amount <= 0) continue;
        walker.pickupGood(g, amount, visits);
        
        if (walker.carryAmount >= carryLimit) {
          walker.returnTo(this);
          return;
        }
        else if (pickNextCollection(walker, g)) {
          return;
        }
        else {
          walker.returnTo(this);
          return;
        }
      }
    }
  }
  
}





