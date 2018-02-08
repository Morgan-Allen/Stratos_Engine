


package game;
import static game.GameConstants.*;





public class BuildingForFaith extends Building {
  
  
  public BuildingForFaith(BuildType type) {
    super(type);
  }
  
  
  public BuildingForFaith(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
}
