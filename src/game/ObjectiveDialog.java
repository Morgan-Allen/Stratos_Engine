


package game;
import static game.GameConstants.*;



public class ObjectiveDialog extends Formation.Objective {
  
  
  boolean updateTacticalTarget(Formation parent) {
    return false;
  }
  
  
  Tile standLocation(Actor a, Formation parent) {
    return null;
  }
  
  
  void selectActorBehaviour(Actor a, Formation parent) {
    return;
  }
  
  
  void actorUpdates(Actor a, Formation parent) {
    return;
  }
  
  
  void actorTargets(Actor a, Target other, Formation parent) {
    return;
  }
  
}












