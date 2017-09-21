

package game;
import game.Goods.Good;
import util.Nums;



public class HomeBuilding extends Building {
  
  
  HomeBuilding(ObjectType type) {
    super(type);
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
    Building goes = null;
    
    for (Good cons : type.consumed) {
      if (inventory.valueFor(cons) >= type.maxStock) continue;
      
      //Building tried = findNearestOfType(MARKET, 50);
      //if (tried == null || tried.inventory.valueFor(cons) <= 0) continue;
      //goes = tried;
    }
    
    if (goes != null) {
      walker.pathToward(goes, Walker.JOB_SHOPPING);
    }
    else {
      super.selectWalkerBehaviour(walker);
    }
  }
  
  
  void walkerEnters(Walker walker, Building enters) {
    for (Good cons : type.consumed) {
      float stock = enters.inventory.valueFor(cons);
      
      if (enters == this) {
        walker.offloadGood(cons, this);
      }
      else if (stock > 0 && walker.jobType == Walker.JOB_SHOPPING) {
        float taken = Nums.min(type.maxStock, stock / 2);
        walker.beginDelivery(enters, this, Walker.JOB_SHOPPING, cons, taken);
      }
    }
  }
}





