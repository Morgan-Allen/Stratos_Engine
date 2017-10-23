

package game;
import util.*;
import static game.Task.*;
import static game.CityMap.*;
import static game.GameConstants.*;




public class BuildingForGather extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public BuildingForGather(Type type) {
    super(type);
  }
  
  
  public BuildingForGather(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Utility methods for filling up crop areas:
    */
  static Tile[] applyPlanting(
    CityMap map, int x, int y, int w, int h, Good... crops
  ) {
    Batch <Tile> planted = new Batch();
    for (Coord c : Visit.grid(x, y, w, h, 1)) {
      
      Tile t = map.tileAt(c);
      if (t == null || t.paved) continue;
      if (t.above != null && t.above.type.growRate == 0) continue;
      
      Good seed = seedType(t, crops);
      Element crop = new Element(seed);
      crop.enterMap(map, c.x, c.y, -1);
      planted.add(t);
    }
    return planted.toArray(Tile.class);
  }
  
  
  static Good seedType(Tile t, Good crops[]) {
    float index = t.x % 5;
    index += (t.y % 5) / 5f;
    index *= crops.length / 5f;
    return crops[(int) index];
  }
  
  
  
  /**  Life-cycle, update and economic functions-
    */
  public void selectActorBehaviour(Actor actor) {
    
    Task delivery = TaskDelivery.pickNextDelivery(actor, this, produced());
    if (delivery != null) {
      actor.assignTask(delivery);
      return;
    }
    
    if (actor.inside != this) {
      actor.returnTo(this);
      return;
    }
    
    if (pickNextCrop  (actor, false, type.produced)) return;
    if (pickPlantPoint(actor, false, true         )) return;
  }
  
  
  boolean pickPlantPoint(Actor actor, boolean close, boolean start) {
    if (start && actor.inside != this) return false;
    
    boolean canPlant = false;
    for (Good g : type.produced) if (g.isCrop) canPlant = true;
    if (! canPlant) return false;
    
    CityMapFlagging flagging = map.flagging.get(NEED_PLANT);
    if (flagging == null) return false;
    
    Tile goes = null;
    if (close) goes = flagging.findNearbyPoint(actor, AVG_GATHER_RANGE);
    else goes = flagging.pickRandomPoint(this, type.maxDeliverRange);
    
    if (goes != null) {
      actor.embarkOnTarget(goes, 2, JOB.PLANTING, this);
      return true;
    }
    
    return false;
  }
  
  
  boolean pickNextCrop(Actor actor, boolean close, Good... cropTypes) {
    if (Visit.empty(cropTypes)) return false;
    
    int spaceTaken = 0;
    for (Good g : type.produced) spaceTaken += inventory.valueFor(g);
    if (spaceTaken >= type.maxStock) return false;
    
    CityMapFlagging flagging = map.flagging.get(type.gatherFlag);
    if (flagging == null) return false;
    
    Tile goes = null;
    if (close) goes = flagging.findNearbyPoint(actor, AVG_GATHER_RANGE);
    else goes = flagging.pickRandomPoint(this, type.maxDeliverRange);
    Element above = goes == null ? null : goes.above;
    
    if (above == null) return false;
    if (! Visit.arrayIncludes(cropTypes, above.type.yields)) return false;
    
    actor.embarkOnTarget(goes, 2, JOB.HARVEST, this);
    return true;
  }
  
  
  public void actorTargets(Actor actor, Target other) {
    if (other == null) return;
    
    Element above = other.at().above;
    if (above == null || above.type.yields == null) return;
    
    if (actor.jobType() == JOB.PLANTING) {
      //
      //  First, initialise the crop:
      above.setBuildLevel(0);
      //
      //  Then pick another point to sow:
      if (! pickPlantPoint(actor, true, false)) {
        actor.returnTo(this);
      }
    }
    
    if (actor.jobType() == JOB.HARVEST) {
      
      if      (above.type.isCrop      ) above.setBuildLevel(-1);
      else if (above.type.growRate > 0) above.setBuildLevel( 0);
      
      actor.carried      = above.type.yields;
      actor.carryAmount += above.type.yieldAmount;
      
      ///I.say(actor+" harvested "+actor.carryAmount+" of "+above.type);
      
      if (actor.carryAmount >= 2) {
        actor.returnTo(this);
      }
      else if (! pickNextCrop(actor, true, actor.carried)) {
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







