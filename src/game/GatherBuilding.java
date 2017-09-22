

package game;
import static game.Terrains.*;
import static game.BuildingSet.*;
import util.*;




public class GatherBuilding extends CraftBuilding {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public GatherBuilding(ObjectType type) {
    super(type);
  }
  
  
  public GatherBuilding(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Life-cycle, update and economic functions-
    */
  public static class Crop extends Fixture {
    
    
    public Crop(ObjectType type) {
      super(type);
    }
    
    
    public Crop(Session s) throws Exception {
      super(s);
    }
    
    
    public void saveState(Session s) throws Exception {
      super.saveState(s);
    }


    void updateGrowth() {
      buildLevel += SCAN_PERIOD * 1f / RIPEN_PERIOD;
      if (buildLevel >= 1) buildLevel = 1;
      //I.say("Updating growth at "+x+" "+y+": "+buildLevel);
    }
    
  }
  
  
  
  
  
  void advanceProduction() {
  }
  
  
  void selectWalkerBehaviour(Walker walker) {
    
    Box2D box = new Box2D(x, y, type.wide, type.high);
    int range = 4;
    box.expandBy(range);
    
    for (Good cropType : CROP_TYPES) {
      if (pickNextCrop(walker, cropType, box)) return;
    }
    
    super.selectWalkerBehaviour(walker);
  }
  
  
  boolean pickNextCrop(Walker walker, Good cropType, Box2D box) {
    
    Tile atW = map.tileAt(walker.x, walker.y);
    Tile atB = map.tileAt(this  .x, this  .y);
    Pick <Crop> pick = new Pick();

    for (Coord c : Visit.grid(box)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t != null && t.above instanceof Crop) {
        Crop crop = (Crop) t.above;
        if (crop.buildLevel < 1 || crop.type != cropType) continue;
        
        float distW = PathSearch.distance(atW, t);
        float distB = PathSearch.distance(atB, t);
        pick.compare(crop, 0 - (distW + distB));
      }
    }
    
    Crop goes = pick.result();
    if (goes != null) {
      walker.beginGathering(goes, 2);
      return true;
    }
    
    return false;
  }
  
  
  void walkerTargets(Walker walker, Fixture other) {
    Crop crop = (Crop) other;
    crop.buildLevel = 0;
    
    walker.carried      = (Good) crop.type;
    walker.carryAmount += CROP_YIELD / 100f;
    
    I.say(walker+" harvested "+walker.carryAmount+" of "+crop.type);
    
    if (walker.carryAmount >= 2) {
      walker.startReturnHome();
    }
    else {
      Box2D area = new Box2D(walker.x, walker.y, 1, 1);
      area.expandBy(2);
    }
  }
  
  
  void walkerEnters(Walker walker, Building enters) {
    if (enters == this) for (Good made : type.produced) {
      if (walker.carried == made) {
        walker.offloadGood(made, this);
      }
    }
    super.walkerEnters(walker, enters);
  }
  
  
}







