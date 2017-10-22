

package game;
import util.*;
import static game.Task.*;
import static game.CityMap.*;
import static game.GameConstants.*;




public class BuildingForGather extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  Tally <Good> cropLevels = new Tally();
  
  
  public BuildingForGather(Type type) {
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
  public void selectActorBehaviour(Actor actor) {
    
    Task delivery = TaskDelivery.pickNextDelivery(actor, this, produced());
    if (delivery != null) {
      actor.assignTask(delivery);
      return;
    }
    
    Box2D box = fullArea();
    if (pickPlantPoint(actor, box, true         )) return;
    if (pickNextCrop  (actor, box, type.produced)) return;
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
  
  
  protected boolean hasFocus(Target t) {
    //  TODO:  The hasFocus() method for elements/tiles should be accomplishing
    //  this.
    for (Actor w : workers) {
      if (w.job != null && w.job.target == t) return true;
    }
    return false;
  }
  
  
  boolean pickPlantPoint(Actor actor, Box2D box, boolean start) {
    if (start && actor.inside != this) return false;
    if (cropLevels.empty()) return false;
    
    Pick <Tile> pick = new Pick();
    Tally levels = cropLevels;
    
    for (Coord c : Visit.grid(box)) {
      Tile t = map.tileAt(c.x, c.y);
      if (t == null || t.paved || hasFocus(t)) continue;
      
      Type above = t.above == null ? null : t.above.type;
      if (above != null && levels.valueFor(above) > 0) continue;
      if (above != null && above.growRate == 0       ) continue;
      
      float distW = CityMap.distance(actor.at, t);
      float distB = CityMap.distance(this  .at, t);
      pick.compare(t, 0 - (distW + distB));
    }
    
    Tile goes = pick.result();
    if (goes != null) {
      actor.embarkOnTarget(goes, 2, JOB.PLANTING, this);
      return true;
    }
    
    return false;
  }
  
  
  boolean pickNextCrop(Actor actor, Box2D box, Good... cropTypes) {
    if (Visit.empty(cropTypes)) return false;
    
    Pick <Element> pick = new Pick();
    for (Coord c : Visit.grid(box)) {
      Tile t = map.tileAt(c.x, c.y);
      
      if (t != null && t.above instanceof Element) {
        Element crop = (Element) t.above;
        if (crop.buildLevel() < 1 || hasFocus(crop)) continue;
        if (! Visit.arrayIncludes(cropTypes, crop.type)) continue;
        
        float distW = CityMap.distance(actor.at, t);
        float distB = CityMap.distance(this  .at, t);
        pick.compare(crop, 0 - (distW + distB));
      }
    }
    
    Element goes = pick.result();
    if (goes != null) {
      actor.embarkOnTarget(goes, 2, JOB.HARVEST, this);
      return true;
    }
    
    return false;
  }
  
  
  public void actorTargets(Actor actor, Target other) {
    if (other == null) return;
    
    Box2D area = new Box2D(actor.at.x, actor.at.y, 1, 1);
    area.expandBy(2);
    area.cropBy(fullArea());
    Element above = other.at().above;
    
    if (actor.jobType() == JOB.PLANTING) {
      //
      //  We have to pick a seed-type to plant first:
      Tile under = other.at();
      Good seed = seedType(under);
      if (seed == null) return;
      //
      //  Then plonk it in the ground:
      if (above != null) above.exitMap(map);
      Element crop = new Element(seed);
      crop.enterMap(map, under.x, under.y, 0);
      //
      //  Then pick another point to sow:
      if (! pickPlantPoint(actor, area, false)) {
        actor.returnTo(this);
      }
    }
    
    if (actor.jobType() == JOB.HARVEST) {
      if (above == null || ! (above.type instanceof Good)) return;
      
      above.setBuildLevel(0);
      actor.carried      = (Good) above.type;
      actor.carryAmount += CROP_YIELD / 100f;
      
      ///I.say(actor+" harvested "+actor.carryAmount+" of "+above.type);
      
      if (actor.carryAmount >= 2) {
        actor.returnTo(this);
      }
      else if (! pickNextCrop(actor, area, actor.carried)) {
        actor.returnTo(this);
      }
    }
  }
  
  
  public void actorEnters(Actor actor, Building enters) {
    if (enters == this) for (Good made : type.produced) {
      actor.offloadGood(made, this);
    }
    super.actorEnters(actor, enters);
  }
  
  
}







