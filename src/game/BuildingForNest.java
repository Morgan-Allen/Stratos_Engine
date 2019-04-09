

package game;
import static game.GameConstants.*;
import util.*;



public class BuildingForNest extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */ 
  int customSpawnInterval = -1;
  Tally <ActorType> customSpawnTypes = new Tally();
  
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
  public void assignCustomSpawnParameters(int interval, Tally <ActorType> types) {
    this.customSpawnInterval = interval;
    this.customSpawnTypes    = types;
  }
  
  
  void updateWorkers(int period) {
    
    spawnCountdown += period;
    int interval = type().nestSpawnInterval;
    Tally <ActorType> spawns = type().workerTypes;
    
    if (customSpawnInterval > 0) {
      interval = customSpawnInterval;
      spawns   = customSpawnTypes;
    }
    
    if (spawnCountdown < interval) return;
    
    for (ActorType w : spawns.keys()) {
      if (numWorkers(w) < spawns.valueFor(w) && w.socialClass == CLASS_COMMON) {
        
        //  TODO:  Allow for the possibility of off-map migrants as well?
        //ActorUtils.generateMigrant(w, this, false);
        
        Actor spawned = (Actor) w.generate();
        spawned.enterMap(map, at().x, at().y, 1, base());
        spawned.setInside(this, true);
        setWorker  (spawned, true);
        setResident(spawned, true);
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




