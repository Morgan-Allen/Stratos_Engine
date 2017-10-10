

package game;
import util.*;
import static game.GameConstants.*;



public class BuildingForWater extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  BuildingForWater(ObjectType type) {
    super(type);
  }
  
  
  public BuildingForWater(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Life-cycle and regular updates:
    */
  void update() {
    super.update();
    inventory.set(WATER, 10);
  }
}
