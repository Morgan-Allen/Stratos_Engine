

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;




public class BuildingForHunt extends BuildingForCrafts {
  
  
  public BuildingForHunt(Type type) {
    super(type);
  }
  
  
  public BuildingForHunt(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  
  public void selectActorBehaviour(Actor actor) {
    
    TaskExplore task = new TaskExplore(actor);
    task = task.configExploration();
    
    if (task != null) {
      actor.assignTask(task);
      return;
    }
    
    
    //  TODO:  Implement this once exploring is tested.
    /*
    Pick <Actor> forHunt = new Pick();
    
    for (Actor a : map.walkers) {
      if (a.type.category != Type.IS_ANIMAL_WLK) continue;
      if (a.type.predator || a.growLevel() < 1 ) continue;
      
      float dist = CityMap.distance(actor.at(), a.at());
      if (dist > MAX_EXPLORE_DIST) continue;
      
      forHunt.compare(a, CityMap.distancePenalty(dist));
    }
    
    if (! forHunt.empty()) {
      
    }
    //*/
    
  }
  
  
}












