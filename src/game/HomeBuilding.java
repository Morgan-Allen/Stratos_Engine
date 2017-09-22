

package game;
import util.*;
import static game.Goods.*;



public class HomeBuilding extends Building {
  
  
  HomeBuilding(ObjectType type) {
    super(type);
  }
  
  
  public HomeBuilding(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
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
  
  
  void selectWalkerBehaviour(Walker walker) {
    
    //  TODO:  Get more samples of nearby buildings...
    //boolean watch = walker.type.name.equals("Noble");
    
    Building goes = null;
    for (Good cons : type.consumed) {
      if (inventory.valueFor(cons) >= type.maxStock) continue;
      
      Building tried = findNearestWithFeature(Goods.IS_MARKET, 50);
      if (tried == null || tried.inventory.valueFor(cons) < 1) continue;
      
      goes = tried;
    }
    if (goes != null) {
      walker.pathToward(goes, Walker.JOB_SHOPPING);
      return;
    }
    
    goes = findNearestWithFeature(Goods.IS_AMENITY, 50);
    if (goes != null && Rand.num() > 0.25f) {
      walker.embarkOnVisit(goes, 25);
      return;
    }
    
    super.selectWalkerBehaviour(walker);
  }
  
  
  void walkerEnters(Walker walker, Building enters) {
    
    if (walker.jobType == Walker.JOB_SHOPPING) {
      for (Good cons : type.consumed) {
        float stock = enters.inventory.valueFor(cons);
        
        if (enters == this) {
          walker.offloadGood(cons, this);
        }
        else if (stock > 0) {
          float taken = Nums.min(type.maxStock, stock / 2);
          walker.beginDelivery(enters, this, Walker.JOB_SHOPPING, cons, taken);
        }
      }
    }
  }
}





