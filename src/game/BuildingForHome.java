

package game;
import util.*;
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
      if (inventory.valueFor(cons) < 0) continue;
      inventory.add(0 - conLevel, cons);
    }
  }
  
  
  public void selectWalkerBehaviour(Walker walker) {
    //
    //  TODO:  Get more samples of nearby buildings...
    
    Building goes = null;
    for (Good cons : type.consumed) {
      if (inventory.valueFor(cons) >= type.maxStock) continue;
      
      Building tried = findNearestWithFeature(IS_MARKET, 50);
      if (tried == null || tried.inventory.valueFor(cons) < 1) continue;
      
      goes = tried;
    }
    if (goes != null) {
      walker.embarkOnVisit(goes, 0, Walker.JOB.SHOPPING, this);
      return;
    }
    
    goes = findNearestWithFeature(IS_AMENITY, 50);
    if (goes != null && Rand.num() > 0.25f) {
      walker.embarkOnVisit(goes, 25, Walker.JOB.VISITING, this);
      return;
    }
    
    super.selectWalkerBehaviour(walker);
  }
  
  
  public void walkerEnters(Walker walker, Building enters) {
    
    if (walker.jobType() == Walker.JOB.SHOPPING) {
      for (Good cons : type.consumed) {
        float stock = enters.inventory.valueFor(cons);
        
        if (enters == this) {
          walker.offloadGood(cons, this);
        }
        else if (stock > 0) {
          float taken = Nums.min(type.maxStock, stock / 2);
          walker.beginDelivery(enters, this, Walker.JOB.SHOPPING, cons, taken);
        }
      }
    }
  }
  
  
}





