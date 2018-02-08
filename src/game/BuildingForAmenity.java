

package game;
import static game.GameConstants.*;



public class BuildingForAmenity extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public BuildingForAmenity(BuildType type) {
    super(type);
  }
  
  
  public BuildingForAmenity(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
}
