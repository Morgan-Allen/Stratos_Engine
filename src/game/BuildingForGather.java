

package game;
import static game.GameConstants.*;
import static game.CityMap.*;
import util.*;




public class BuildingForGather extends BuildingForDelivery {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public BuildingForGather(ObjectType type) {
    super(type);
  }
  
  
  public BuildingForGather(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Life-cycle, update and economic functions-
    */
  void advanceProduction() {
    super.advanceProduction();
    return;
  }
  
  
  public void selectWalkerBehaviour(Walker walker) {
    
    Box2D box = new Box2D(at.x, at.y, type.wide, type.high);
    int range = type.gatherRange;
    box.expandBy(range);
    
    for (Good cropType : type.produced) {
      if (pickNextCrop(walker, cropType, box)) return;
    }
    
    super.selectWalkerBehaviour(walker);
  }
  
  
  boolean pickNextCrop(Walker walker, Good cropType, Box2D box) {
    
    Pick <Fixture> pick = new Pick();

    for (Coord c : Visit.grid(box)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t != null && t.above instanceof Fixture) {
        Fixture crop = (Fixture) t.above;
        if (crop.buildLevel < 1 || crop.type != cropType) continue;
        
        float distW = CityMap.distance(walker.at, t);
        float distB = CityMap.distance(this  .at, t);
        pick.compare(crop, 0 - (distW + distB));
      }
    }
    
    Fixture goes = pick.result();
    if (goes != null) {
      walker.embarkOnTarget(goes, 2, Walker.JOB.GATHERING, this);
      return true;
    }
    
    return false;
  }
  
  
  public void walkerTargets(Walker walker, Target other) {
    if (other == null) return;
    
    Fixture above = other.at().above;
    if (above == null || ! (above.type instanceof Good)) return;
    
    above.buildLevel = 0;
    walker.carried      = (Good) above.type;
    walker.carryAmount += CROP_YIELD / 100f;
    
    I.say(walker+" harvested "+walker.carryAmount+" of "+above.type);
    
    if (walker.carryAmount >= 2) {
      walker.returnTo(this);
    }
    else {
      Box2D area = new Box2D(walker.at.x, walker.at.y, 1, 1);
      area.expandBy(2);
      if (! pickNextCrop(walker, walker.carried, area)) {
        walker.returnTo(this);
      }
    }
  }
  
  
  public void walkerEnters(Walker walker, Building enters) {
    if (enters == this) for (Good made : type.produced) {
      walker.offloadGood(made, this);
    }
    super.walkerEnters(walker, enters);
  }
  
  
}







