


package game;
import static game.GameConstants.*;
import util.*;



public class BuildingForNest extends Building implements BaseEvents.Trouble {
  
  
  /**  Data fields, construction and save/load methods-
    */
  int spawnInterval  = DAY_LENGTH;
  Tally <ActorType> spawnChances = new Tally();
  int spawnCountdown =  0;
  int maxResidents   = -1;
  boolean doRaids   = false;
  boolean doContact = false;
  
  Building parent = null;
  Mission activeMission = null;
  
  
  
  public BuildingForNest(BuildType type) {
    super(type);
    maxResidents  = type.maxResidents;
    spawnInterval = type.nestSpawnInterval;
    spawnChances.add(type.workerTypes);
    doRaids   = type.hasFeature(DOES_RAIDS);
    doContact = type.hasFeature(DOES_CONTACT);
  }
  
  
  public BuildingForNest(Session s) throws Exception {
    super(s);
    
    s.loadTally(spawnChances);
    spawnInterval  = s.loadInt();
    spawnCountdown = s.loadInt();
    maxResidents   = s.loadInt();
    doRaids   = s.loadBool();
    doContact = s.loadBool();
    
    parent = (Building) s.loadObject();
    activeMission = (Mission) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveTally(spawnChances);
    s.saveInt(spawnInterval );
    s.saveInt(spawnCountdown);
    s.saveInt(maxResidents  );
    s.saveBool(doRaids  );
    s.saveBool(doContact);
    
    s.saveObject(parent);
    s.saveObject(activeMission);
  }
  
  
  void update() {
    super.update();
    
    if (! complete()) {
      return;
    }
    
    if (spawnCountdown != -1 && --spawnCountdown <= 0) {
      spawnCountdown = spawnInterval;
      performSpawning();
    }
  }
  
  
  
  /**  Supplemental configuration methods-
    */
  public void assignSpawnParameters(
    int interval, int maxResidents, boolean doRaids, Object... spawnChanceArgs
  ) {
    this.spawnInterval = interval;
    this.maxResidents  = maxResidents;
    this.doRaids       = doRaids;
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
        float current = numResidents(type) * 1f / maxResidents;
        float chance = spawnChances.valueFor(type);
        chances[i] = Nums.max(0, chance - current);
        types  [i] = type;
        i++;
      }
      ActorType spawnType = (ActorType) Rand.pickFrom(types, chances);
      
      if (spawnType != null) {
        Actor spawn = (Actor) spawnType.generate();
        spawn.enterMap(map, at().x, at().y, 1, base());
        spawn.setInside(this, true);
        setResident(spawn, true);
      }
    }
    //
    //  And have them raid hostile targets if and when present...
    //  TODO:  Ideally, you'd like this to piggyback off the tactical AI for
    //  Missions in general...
    else if (doRaids && (activeMission == null || activeMission.complete())) {
      
      Pick <Building> pick = new Pick();
      for (Building b : map().buildings()) {
        if (! TaskCombat.hostile(b, this)) continue;
        if (! map().pathCache.pathConnects(this, b, true, false)) continue;
        
        float rating = 1f;
        rating *= Area.distancePenalty(this, b);
        pick.compare(b, rating);
      }
      
      if (! pick.empty()) {
        activeMission = new MissionForStrike(base());
        activeMission.setLocalFocus(pick.result());
        for (Actor a : residents()) activeMission.toggleRecruit(a, true);
        activeMission.beginMission(base());
      }
    }
  }
  
  
  public boolean allowsResidence(Actor actor) {
    if (actor.work() == this) return true;
    return false;
  }


  public Task selectActorBehaviour(Actor actor) {
    if (Area.distance(actor, this) > MAX_WANDER_RANGE) {
      Task waits = TaskWaiting.configWaiting(actor, this);
      return waits;
    }
    return null;
  }
  
}








