

package game;
import static game.GameConstants.*;
import util.*;



public class BuildingForDelivery extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  boolean stalled = false;
  
  
  BuildingForDelivery(ObjectType type) {
    super(type);
  }
  
  
  public BuildingForDelivery(Session s) throws Exception {
    super(s);
    stalled = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveBool(stalled);
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
    
    boolean anyRoom = false, allMaterials = true;
    
    for (Good made : produced()) {
      if (inventory.valueFor(made) < stockLimit(made)) anyRoom = true;
    }
    for (Good need : needed()) {
      if (inventory.valueFor(need) <= 0) allMaterials = false;
    }
    
    stalled = (! allMaterials) || (! anyRoom);
    
    if (! stalled) {
      float prog = 1f / type.craftTime;
      
      for (Good need : needed()) {
        inventory.add(0 - prog, need);
      }
      craftProgress = Nums.min(craftProgress + prog, 1);
      
      if (craftProgress >= 1) {
        for (Good made : produced()) {
          if (inventory.valueFor(made) >= stockLimit(made)) continue;
          inventory.add(1, made);
          
          I.say(this+" crafted 1 "+made);
        }
        craftProgress = 0;
      }
    }
  }
  
  
  public void selectWalkerBehaviour(Walker walker) {
    
    if (walker.inside != this) {
      walker.startReturnHome();
      return;
    }
    
    class Order { Building goes; Good good; float amount; }
    Pick <Order> pick = new Pick();
    
    for (Good made : produced()) {
      int amount = (int) inventory.valueFor(made);
      if (amount <= 0) continue;
      
      Building goes = findNearestDemanding(null, made, type.maxDeliverRange);
      if (goes == null) continue;
      
      amount = Nums.min(amount, 10                                   );
      amount = Nums.min(amount, 2 + (int) goes.demands.valueFor(made));
      
      float distFactor = 10 + CityMap.distance(entrance, goes.entrance);
      Order o = new Order();
      o.goes   = goes  ;
      o.good   = made  ;
      o.amount = amount;
      pick.compare(o, amount / distFactor);
    }
    
    if (! pick.empty()) {
      Order o = pick.result();
      walker.beginDelivery(this, o.goes, Walker.JOB_DELIVER, o.good, o.amount);
    }
    else {
      walker.startRandomWalk();
    }
  }
  
  
  public void walkerEnters(Walker walker, Building enters) {
    
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


