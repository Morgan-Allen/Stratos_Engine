

package game;
import util.*;
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
    
    if (! actorIsHereWithPrompt(actor)) return;
    
    Task delivery = TaskDelivery.pickNextDelivery(actor, this, produced());
    if (delivery != null) {
      actor.assignTask(delivery);
      return;
    }
    
    if (TaskGathering.pickNextCrop(this, actor, false, type.produced)) {
      return;
    }
    if (TaskGathering.pickPlantPoint(this, actor, false, true)) {
      return;
    }
  }
}







