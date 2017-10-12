

package game;
import util.*;
import static game.Task.*;
import static game.CityMap.*;
import static game.GameConstants.*;




public class BuildingForGather extends BuildingForCrafts {
  
  
  /**  Data fields, construction and save/load methods-
    */
  Tally <Good> cropLevels = new Tally();
  
  
  public BuildingForGather(ObjectType type) {
    super(type);
    cropLevels.set(type.produced[0], 1);
  }
  
  
  public BuildingForGather(Session s) throws Exception {
    super(s);
    s.loadTally(cropLevels);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveTally(cropLevels);
  }
  
  
  
  /**  Life-cycle, update and economic functions-
    */
  public void selectWalkerBehaviour(Walker walker) {
    
    Box2D box = fullArea();
    if (pickPlantPoint(walker, box, true         )) return;
    if (pickNextCrop  (walker, box, type.produced)) return;
    
    super.selectWalkerBehaviour(walker);
  }
  
  
  void advanceProduction() {
    return;
  }
  
  
  Box2D fullArea() {
    Box2D box = new Box2D(at.x, at.y, type.wide, type.high);
    int range = type.gatherRange;
    box.expandBy(range);
    return box;
  }
  
  
  Good seedType(Tile t) {
    float sumL = 0;
    for (Good g : cropLevels.keys()) {
      sumL += cropLevels.valueFor(g);
    }
    
    float index = t.x % 5;
    index += (t.y % 5) / 5f;
    index *= sumL / 5;
    
    Good seed = null;
    sumL = 0;
    for (Good g : cropLevels.keys()) {
      sumL += cropLevels.valueFor(g);
      if (sumL >= index) { seed = g; break; }
    }
    
    return seed;
  }
  
  
  private boolean hasFocus(Target t) {
    //  TODO:  The hasFocus() method for elements/tiles should be accomplishing
    //  this.
    for (Walker w : workers) {
      if (w.job != null && w.job.target == t) return true;
    }
    return false;
  }
  
  
  boolean pickPlantPoint(Walker walker, Box2D box, boolean start) {
    if (start && walker.inside != this) return false;
    if (cropLevels.empty()) return false;
    
    Pick <Tile> pick = new Pick();
    Tally levels = cropLevels;
    
    for (Coord c : Visit.grid(box)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t == null || t.paved || hasFocus(t)) continue;
      
      ObjectType above = t.above == null ? null : t.above.type;
      if (above != null && levels.valueFor(above) > 0) continue;
      if (above != null && above.growRate == 0       ) continue;
      
      float distW = CityMap.distance(walker.at, t);
      float distB = CityMap.distance(this  .at, t);
      pick.compare(t, 0 - (distW + distB));
    }
    
    Tile goes = pick.result();
    if (goes != null) {
      walker.embarkOnTarget(goes, 2, JOB.PLANTING, this);
      return true;
    }
    
    return false;
  }
  
  
  boolean pickNextCrop(Walker walker, Box2D box, Good... cropTypes) {
    if (Visit.empty(cropTypes)) return false;
    
    Pick <Fixture> pick = new Pick();
    for (Coord c : Visit.grid(box)) {
      Tile t = map.tileAt(c.x, c.y);
      
      if (t != null && t.above instanceof Fixture) {
        Fixture crop = (Fixture) t.above;
        if (crop.buildLevel < 1 || hasFocus(crop)) continue;
        if (! Visit.arrayIncludes(cropTypes, crop.type)) continue;
        
        float distW = CityMap.distance(walker.at, t);
        float distB = CityMap.distance(this  .at, t);
        pick.compare(crop, 0 - (distW + distB));
      }
    }
    
    Fixture goes = pick.result();
    if (goes != null) {
      walker.embarkOnTarget(goes, 2, JOB.GATHERING, this);
      return true;
    }
    
    return false;
  }
  
  
  public void walkerTargets(Walker walker, Target other) {
    if (other == null) return;
    
    Box2D area = new Box2D(walker.at.x, walker.at.y, 1, 1);
    area.expandBy(2);
    area.cropBy(fullArea());
    Fixture above = other.at().above;
    
    if (walker.jobType() == JOB.PLANTING) {
      //
      //  We have to pick a seed-type to plant first:
      Tile under = other.at();
      Good seed = seedType(under);
      if (seed == null) return;
      //
      //  Then plonk it in the ground:
      if (above != null) above.exitMap(map);
      Fixture crop = new Fixture(seed);
      crop.enterMap(map, under.x, under.y, 0);
      //
      //  Then pick another point to sow:
      if (! pickPlantPoint(walker, area, false)) {
        walker.returnTo(this);
      }
    }
    
    if (walker.jobType() == JOB.GATHERING) {
      if (above == null || ! (above.type instanceof Good)) return;
      
      above.buildLevel = 0;
      walker.carried      = (Good) above.type;
      walker.carryAmount += CROP_YIELD / 100f;
      
      ///I.say(walker+" harvested "+walker.carryAmount+" of "+above.type);
      
      if (walker.carryAmount >= 2) {
        walker.returnTo(this);
      }
      else if (! pickNextCrop(walker, area, walker.carried)) {
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







