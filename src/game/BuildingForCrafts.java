

package game;
import static game.Walker.*;
import static game.GameConstants.*;
import util.*;



public class BuildingForCrafts extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  float craftProgress;
  boolean stalled = false;
  
  
  BuildingForCrafts(ObjectType type) {
    super(type);
  }
  
  
  public BuildingForCrafts(Session s) throws Exception {
    super(s);
    craftProgress = s.loadFloat();
    stalled = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat(craftProgress);
    s.saveBool(stalled);
  }
  
  
  
  /**  Life-cycle, update and economic functions-
    */
  void enterMap(CityMap map, int x, int y, float buildLevel) {
    super.enterMap(map, x, y, buildLevel);
  }
  
  
  void update() {
    super.update();
    advanceProduction();
  }
  
  
  
  Good[] needed  () { return type.needed  ; }
  Good[] produced() { return type.produced; }
  
  float stockNeeded(Good need) { return type.maxStock; }
  float stockLimit (Good made) { return type.maxStock; }
  
  
  void updateDemands() {
    super.updateDemands();
    for (Good need : needed()) {
      float gap = stockNeeded(need) - inventory.valueFor(need);
      demands.add(gap, need);
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
          //I.say(this+" crafted 1 "+made);
        }
        craftProgress = 0;
      }
    }
  }
  
  
  float craftProgress() {
    return craftProgress;
  }
  
  
  
  /**  Handling walker behaviours:
    */
  public void selectWalkerBehaviour(Walker walker) {
    //
    //  Try and find a nearby building to construct:
    int maxRange = Walker.MAX_WANDER_TIME;
    Pick <Building> pickB = new Pick();
    
    for (Good w : type.buildsWith) {
      for (Building b : map.buildings) {
        int   need       = b.type.materialNeed(w);
        float amountDone = b.materials.valueFor(w);
        float amountGot  = b.inventory.valueFor(w);
        float dist       = CityMap.distance(entrance, b.entrance);
        if (amountDone >= need || amountGot <= 0) continue;
        if (dist > maxRange) continue;
        
        pickB.compare(b, (need - amountDone) * 10 / (10 + dist));
      }
    }
    
    Building builds = pickB.result();
    if (builds != null) {
      walker.embarkOnVisit(builds, 10, JOB.BUILDING, this);
      return;
    }
    
    //
    //  Failing that, go here if you aren't already:
    if (walker.inside != this) {
      walker.returnTo(this);
      return;
    }
    //
    //  Find someone to deliver to:
    class Order { Building goes; Good good; float amount; }
    Pick <Order> pickD = new Pick();
    
    for (Good made : produced()) {
      int amount = (int) inventory.valueFor(made);
      if (amount <= 0) continue;
      
      //  TODO:  Iterate over suitable building-types here.
      Building goes = findNearestDemanding(null, made, type.maxDeliverRange);
      if (goes == null) continue;
      
      amount = Nums.min(amount, 10                                   );
      amount = Nums.min(amount, 2 + (int) goes.demands.valueFor(made));
      
      float distFactor = 10 + CityMap.distance(entrance, goes.entrance);
      Order o = new Order();
      o.goes   = goes  ;
      o.good   = made  ;
      o.amount = amount;
      pickD.compare(o, amount / distFactor);
    }
    if (! pickD.empty()) {
      Order o = pickD.result();
      walker.beginDelivery(this, o.goes, JOB.DELIVER, o.good, o.amount, this);
    }
    //
    //  And failing all that, start crafting:
    else {
      walker.embarkOnVisit(this, -1, JOB.CRAFTING, this);
    }
  }
  
  
  public void walkerEnters(Walker walker, Building enters) {
    
    if (walker.jobType() == JOB.DELIVER) {
      if (enters == this) for (Good need : needed()) {
        walker.offloadGood(need, this);
      }
      else for (Good made : produced()) {
        walker.offloadGood(made, enters);
      }
      walker.returnTo(this);
    }
    
    if (walker.jobType() == JOB.BUILDING) {
      advanceBuilding(walker, enters);
    }
  }
  
  
  void advanceBuilding(Walker builds, Building b) {
    float totalNeed = 0, totalDone = 0;
    boolean didWork = false;
    
    for (Good g : type.buildsWith) {
      int   need       = b.type.materialNeed(g);
      float amountDone = b.materials.valueFor(g);
      float amountGot  = b.inventory.valueFor(g);
      if (amountDone >= need || amountGot <= 0) continue;
      
      totalNeed += need;
      totalDone = amountDone;
      
      if (! didWork) {
        float puts = Nums.min(0.1f, amountGot);
        b.materials.add(puts    , g);
        b.inventory.add(0 - puts, g);
        didWork = true;
      }
    }
    
    b.buildLevel = 1.5f * (totalDone / totalNeed);
  }
}












