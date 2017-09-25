

package game;
import static game.BuildingSet.*;
import util.*;



public class CraftBuilding extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  CraftBuilding(ObjectType type) {
    super(type);
  }
  
  
  public CraftBuilding(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Life-cycle, update and economic functions-
    */
  void enterMap(CityMap map, int x, int y) {
    super.enterMap(map, x, y);
    this.updateDemands();
  }
  
  
  void update() {
    super.update();
    updateDemands();
    advanceProduction();
  }
  
  
  void updateDemands() {
    for (Good need : type.needed) {
      float gap = type.maxStock - inventory.valueFor(need);
      demands.set(need, gap);
    }
  }
  
  
  void advanceProduction() {
    if (craftProgress > 1) return;
    
    boolean anyRoom = false;
    for (Good made : type.produced) {
      if (inventory.valueFor(made) < type.maxStock) anyRoom = true;
    }
    if (! anyRoom) return;
    
    for (Good need : type.needed) {
      if (inventory.valueFor(need) <= 0) return;
    }
    
    float prog = 1f / type.craftTime;
    for (Good need : type.needed) {
      inventory.add(0 - prog, need);
    }
    
    craftProgress = Nums.min(craftProgress + prog, 1);
    if (craftProgress < 1) return;
    
    for (Good made : type.produced) {
      if (inventory.valueFor(made) >= type.maxStock) continue;
      inventory.add(1, made);
      
      I.say(this+" crafted 1 "+made);
    }
    
    craftProgress = 0;
  }
  
  
  void selectWalkerBehaviour(Walker walker) {
    for (Good made : type.produced) {
      int goodAmount = (int) inventory.valueFor(made);
      if (goodAmount <= 0) return;
      
      Building goes = findNearestDemanding(null, made, type.maxDeliverRange);
      if (goes == null) return;
      
      goodAmount = Nums.min(goodAmount, 10);
      walker.beginDelivery(this, goes, Walker.JOB_DELIVER, made, goodAmount);
    }
  }
  
  
  void walkerEnters(Walker walker, Building enters) {
    
    if (enters == this) for (Good need : type.needed) {
      if (walker.carried == need) {
        walker.offloadGood(need, this);
      }
    }
    
    for (Good made : type.produced) {
      if (walker.carried == made) {
        walker.offloadGood(made, enters);
        walker.startReturnHome();
        return;
      }
    }
  }
}


