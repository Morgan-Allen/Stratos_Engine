

package game;
import static game.GameConstants.*;



public class BuildingForNest extends Building {
  
  
  public BuildingForNest(BuildType type) {
    super(type);
  }
  
  
  public BuildingForNest(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  public Task selectActorBehaviour(Actor actor) {
    return null;
  }
  
}