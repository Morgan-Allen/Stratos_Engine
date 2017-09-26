

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
    updateDemands();
  }
  
  
  void update() {
    super.update();
    updateDemands();
    advanceProduction();
  }
  
  
  
  Good[] needed  () { return type.needed  ; }
  Good[] produced() { return type.produced; }
  
  float stockNeeded(Good need) { return type.maxStock; }
  float stockLimit (Good made) { return type.maxStock; }
  
  
  void updateDemands() {
    for (Good need : needed()) {
      float gap = stockNeeded(need) - inventory.valueFor(need);
      demands.set(need, gap);
    }
  }
  
  
  void advanceProduction() {
    if (craftProgress > 1) return;
    
    boolean anyRoom = false;
    for (Good made : produced()) {
      if (inventory.valueFor(made) < stockLimit(made)) anyRoom = true;
    }
    if (! anyRoom) return;
    
    for (Good need : needed()) {
      if (inventory.valueFor(need) <= 0) return;
    }
    
    float prog = 1f / type.craftTime;
    for (Good need : needed()) {
      inventory.add(0 - prog, need);
    }
    
    craftProgress = Nums.min(craftProgress + prog, 1);
    if (craftProgress < 1) return;
    
    for (Good made : produced()) {
      if (inventory.valueFor(made) >= stockLimit(made)) continue;
      inventory.add(1, made);
      
      I.say(this+" crafted 1 "+made);
    }
    
    craftProgress = 0;
  }
  
  
  void selectWalkerBehaviour(Walker walker) {
    for (Good made : produced()) {
      int amount = (int) inventory.valueFor(made);
      if (amount <= 0) continue;
      
      Building goes = findNearestDemanding(null, made, type.maxDeliverRange);
      if (goes == null) continue;
      
      amount = Nums.min(amount, 10                                   );
      amount = Nums.min(amount, 2 + (int) goes.demands.valueFor(made));
      
      walker.beginDelivery(this, goes, Walker.JOB_DELIVER, made, amount);
    }
  }
  
  
  void walkerEnters(Walker walker, Building enters) {
    
    if (enters == this) for (Good need : needed()) {
      if (walker.carried == need) {
        walker.offloadGood(need, this);
      }
    }
    
    for (Good made : produced()) {
      if (walker.carried == made) {
        walker.offloadGood(made, enters);
        walker.startReturnHome();
        return;
      }
    }
  }
}


