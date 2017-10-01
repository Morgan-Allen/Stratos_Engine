

package game;
import util.*;
import static game.Walker.*;

import game.Walker.JOB;

import static game.GameConstants.*;



public class BuildingForHome extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  BuildingForHome(ObjectType type) {
    super(type);
  }
  
  
  public BuildingForHome(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Life-cycle, update and economic functions-
    */
  void update() {
    super.update();
    advanceConsumption();
  }
  
  
  void advanceConsumption() {
    float conLevel = 1f / type.consumeTime;
    for (Good cons : type.consumed) {
      float amount = inventory.valueFor(cons);
      amount = Nums.max(0, amount - conLevel);
      inventory.set(cons, amount);
    }
  }
  
  
  public void selectWalkerBehaviour(Walker walker) {
    //
    //  See if you can repair your own home:
    Building repairs = BuildingForCrafts.selectBuildTarget(
      this, type.buildsWith, new Batch(this)
    );
    if (repairs != null) {
      walker.embarkOnVisit(repairs, 10, JOB.BUILDING, this);
      return;
    }
    //
    //  Failing that, see if you can go shopping:
    Building goes = null;
    for (Good cons : type.consumed) {
      if (inventory.valueFor(cons) >= type.maxStock) continue;
      
      Building tried = findNearestWithFeature(IS_MARKET, 50);
      if (tried == null || tried.inventory.valueFor(cons) < 1) continue;
      
      goes = tried;
    }
    if (goes != null) {
      walker.embarkOnVisit(goes, 5, JOB.SHOPPING, this);
      return;
    }
    //
    //  Failing that, select a leisure behaviour to perform:
    //  TODO:  Compare all nearby amenities!
    
    Pick <Building> pick = new Pick();

    pick.compare(this, 1.0f * Rand.num());
    goes = findNearestWithFeature(IS_AMENITY, 50);
    if (goes != null) {
      pick.compare(goes, 1.0f * Rand.num());
    }
    goes = pick.result();
    
    if (goes != this && goes != null) {
      walker.embarkOnVisit(goes, 25, JOB.VISITING, this);
    }
    else if (goes == this && Rand.yes()) {
      walker.embarkOnVisit(this, 10, JOB.RESTING, this);
    }
    else if (goes == this) {
      walker.startRandomWalk();
    }
    else {
      super.selectWalkerBehaviour(walker);
    }
  }
  
  
  public void walkerEnters(Walker walker, Building enters) {
    
    if (walker.jobType() == JOB.SHOPPING) {
      for (Good cons : type.consumed) {
        float stock = enters.inventory.valueFor(cons);
        
        if (enters == this) {
          walker.offloadGood(cons, this);
        }
        else if (stock > 0) {
          float taken = Nums.min(type.maxStock, stock / 2);
          walker.beginDelivery(enters, this, JOB.SHOPPING, cons, taken, this);
          return;
        }
      }
    }
  }
  
  
  public void walkerVisits(Walker walker, Building visits) {
    if (walker.jobType() == JOB.BUILDING) {
      BuildingForCrafts.advanceBuilding(walker, type.buildsWith, visits);
    }
  }
  
  
}





