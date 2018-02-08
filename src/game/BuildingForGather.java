

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;




public class BuildingForGather extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public BuildingForGather(BuildType type) {
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
  public static Tile[] applyPlanting(
    City city, int x, int y, int w, int h, Good... crops
  ) {
    Batch <Tile> planted = new Batch();
    CityMap map = city.activeMap();
    for (Coord c : Visit.grid(x, y, w, h, 1)) {
      
      Tile t = map.tileAt(c);
      if (t == null) continue;
      if (t.above != null && ! t.above.type().isFlora()) continue;
      
      Good seed = seedType(t, crops);
      Element crop = new Element(seed);
      crop.enterMap(map, c.x, c.y, 1, city);
      crop.setGrowLevel(-1);
      planted.add(t);
    }
    return planted.toArray(Tile.class);
  }
  
  
  public static Good seedType(Tile t, Good crops[]) {
    float index = t.x % 5;
    index += (t.y % 5) / 5f;
    index *= crops.length / 5f;
    return crops[(int) index];
  }
  
  
  
  /**  Life-cycle, update and economic functions-
    */
  public Task selectActorBehaviour(Actor actor) {
    Task coming = returnActorHere(actor);
    if (coming != null) return coming;
    
    Task delivery = TaskDelivery.pickNextDelivery(actor, this, produced());
    if (delivery != null) {
      return delivery;
    }
    
    Object[] crops = type().produced;
    Task pick = TaskGathering.pickNextCrop(this, actor, false, crops);
    if (pick != null) {
      return pick;
    }
    
    Task plant = TaskGathering.pickPlantPoint(this, actor, false, true);
    if (plant != null) {
      return plant;
    }
    
    return null;
  }
}







