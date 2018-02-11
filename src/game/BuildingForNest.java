


package game;
import static game.GameConstants.*;
import util.*;



public class BuildingForNest extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  Tally <ActorType> spawnChances = new Tally();
  int spawnInterval  = MONTH_LENGTH;
  int spawnCountdown = 0;
  int maxResidents   = -1;

  Building parent = null;
  Mission activeMission = null;
  
  
  
  public BuildingForNest(BuildType type) {
    super(type);
    maxResidents = type.maxResidents;
  }
  
  
  public BuildingForNest(Session s) throws Exception {
    super(s);
    
    s.loadTally(spawnChances);
    spawnInterval = s.loadInt();
    spawnCountdown = s.loadInt();
    maxResidents = s.loadInt();
    
    parent = (Building) s.loadObject();
    activeMission = (Mission) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveTally(spawnChances);
    s.saveInt(spawnInterval);
    s.saveInt(spawnCountdown);
    s.saveInt(maxResidents);
    
    s.saveObject(parent);
    s.saveObject(activeMission);
  }
  
  
  void update() {
    super.update();
    if (! accessible()) return;
    
    if (spawnCountdown != -1 && --spawnCountdown <= 0) {
      spawnCountdown = spawnInterval;
      performSpawning();
    }
  }
  
  
  
  /**  Supplemental configuration methods-
    */
  public void assignSpawnParameters(
    int interval, int maxResidents, Object... spawnChanceArgs
  ) {
    this.spawnInterval = interval;
    this.maxResidents  = maxResidents;
    this.spawnChances.setWith(spawnChanceArgs);
  }
  
  
  
  /**  Regular updates and behaviour scripting-
    */
  public void performSpawning() {
    //
    //  Spawn new creatures as long as there is space...
    if (residents().size() < maxResidents) {
      
      int numTypes = spawnChances.size(), i = 0;
      float     chances[] = new float    [numTypes];
      ActorType types  [] = new ActorType[numTypes];
      
      for (ActorType type : spawnChances.keys()) {
        float chance = spawnChances.valueFor(type);
        chances[i] = chance;
        types  [i] = type;
        i++;
      }
      
      ActorType spawned = (ActorType) Rand.pickFrom(types, chances);
      if (spawned != null) {
        Test.spawnWalker(this, spawned, true);
      }
    }
    //
    //  And have them raid hostile targets if and when present...
    //  TODO:  Ideally, you'd like this to piggyback off the tactical AI for
    //  Missions in general.
    else if (activeMission == null) {
      
      Pick <Building> pick = new Pick();
      for (Building b : map().buildings()) {
        if (! TaskCombat.hostile(b, this)) continue;
        if (! map().pathCache.pathConnects(this, b, true, false)) continue;
        
        float rating = 1f;
        rating *= CityMap.distancePenalty(this, b);
        pick.compare(b, rating);
      }
      
      if (! pick.empty()) {
        activeMission = new Mission(
          Mission.OBJECTIVE_CONQUER, homeCity(), false
        );
        activeMission.setFocus(pick.result(), 0, map());
        for (Actor a : residents()) activeMission.toggleRecruit(a, true);
      }
    }
  }


  public Task selectActorBehaviour(Actor actor) {
    return null;
  }
  
}








