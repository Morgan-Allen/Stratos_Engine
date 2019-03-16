


package game;
import static game.GameConstants.*;
import util.*;



public class BuildingForNest extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  int spawnCountdown = 0;
  
  
  public BuildingForNest(BuildType type) {
    super(type);
  }
  
  
  public BuildingForNest(Session s) throws Exception {
    super(s);
    spawnCountdown = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(spawnCountdown);
  }
  
  
  
  /**  Regular updates and behaviour scripting-
    */
  void updateWorkers(int period) {
    
    spawnCountdown += period;
    if (spawnCountdown < type().nestSpawnInterval) return;
    
    for (ActorType w : type().workerTypes.keys()) {
      if (numWorkers(w) < maxWorkers(w) && w.socialClass == CLASS_COMMON) {
        
        //  TODO:  Allow for the possibility of off-map migrants as well?
        
        //ActorUtils.generateMigrant(w, this, false);
        
        Actor spawn = (Actor) w.generate();
        spawn.enterMap(map, at().x, at().y, 1, base());
        spawn.setInside(this, true);
        setResident(spawn, true);
      }
    }
  }


  public boolean allowsResidence(Actor actor) {
    if (actor.work() == this) return true;
    return false;
  }


  public Task selectActorBehaviour(Actor actor) {
    if (AreaMap.distance(actor, this) > MAX_WANDER_RANGE) {
      Task waits = TaskWaiting.configWaiting(actor, this);
      return waits;
    }
    return null;
  }
  
}








