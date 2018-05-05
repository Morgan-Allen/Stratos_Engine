


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
  
  
  
  boolean supervised(boolean now) {
    boolean supervised = false;
    for (Actor a : workers()) {
      if (now) {
        if (a.inside() == this && a.jobType() == Task.JOB.WAITING) {
          supervised = true;
        }
      }
      else {
        if (Task.hasTaskFocus(this, Task.JOB.WAITING)) {
          supervised = true;
        }
      }
    }
    return supervised;
  }
  
  
  public Task selectActorBehaviour(Actor actor) {
    
    if (! supervised(false)) {
      Task sup = TaskWaiting.configWaiting(actor, this, TaskWaiting.TYPE_OVERSIGHT);
      if (sup != null) return sup;
    }
    
    return null;
  }
  
  
  public boolean canUsePower(ActorTechnique power) {
    boolean supervised = false;
    for (Actor a : workers()) {
      if (a.onMap() && a.health.alive()) supervised = true;
    }
    return supervised;
  }
  
}












