

package game;
import util.*;
import static game.GameConstants.*;
import static game.CityMap.*;
import static util.TileConstants.*;



public class Walker extends Fixture implements Session.Saveable, Journeys {
  
  
  /**  Data fields and setup/initialisation-
    */
  final public static int
    
    MAX_WANDER_RANGE = 20,
    AVG_VISIT_TIME   = 20,
    TRADE_DIST_TIME  = 50,
    
    STATE_OKAY   = 1,
    STATE_SLEEP  = 2,
    STATE_DEAD   = 3,
    STATE_DECOMP = 4
  ;
  static int nextID = 0;
  int dirs[] = new int[4];
  
  
  String ID;
  
  Building  work;
  Building  home;
  City      homeCity;
  Formation formation;
  Building  inside;
  boolean   guest;
  
  Task job;
  
  Good  carried = null;
  float carryAmount = 0;
  
  float injury ;
  float hunger ;
  float fatigue;
  float stress ;
  int   state = STATE_OKAY;
  Tally <ObjectType> skills = new Tally();
  
  
  
  Walker(ObjectType type) {
    super(type);
    this.ID = "#"+nextID++;
  }
  
  
  public Walker(Session s) throws Exception {
    super(s);
    
    ID = s.loadString();
    
    work      = (Building ) s.loadObject();
    home      = (Building ) s.loadObject();
    homeCity  = (City     ) s.loadObject();
    formation = (Formation) s.loadObject();
    inside    = (Building ) s.loadObject();
    guest     = s.loadBool();
    
    job = (Task) s.loadObject();
    
    carried     = (Good) s.loadObject();
    carryAmount = s.loadFloat();
    
    injury  = s.loadFloat();
    hunger  = s.loadFloat();
    fatigue = s.loadFloat();
    stress  = s.loadFloat();
    state   = s.loadInt();
    s.loadTally(skills);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveString(ID);
    
    s.saveObject(work     );
    s.saveObject(home     );
    s.saveObject(homeCity );
    s.saveObject(formation);
    s.saveObject(inside   );
    s.saveBool  (guest    );
    
    s.saveObject(job);
    
    s.saveObject(carried);
    s.saveFloat(carryAmount);
    
    s.saveFloat(injury );
    s.saveFloat(hunger );
    s.saveFloat(fatigue);
    s.saveFloat(stress );
    s.saveInt  (state  );
    s.saveTally(skills);
  }
  
  
  
  /**  World entry and exit-
    */
  void enterMap(CityMap map, int x, int y) {
    this.map = map;
    this.at  = map.tileAt(x, y);
    map.walkers.add(this);
    //I.say("\n"+this+" ENTERED MAP...");
  }
  
  
  void exitMap() {
    if (inside != null) setInside(inside, false);
    
    map.walkers.remove(this);
    map = null;
    at  = null;
    job = null;
  }
  
  
  void setDestroyed() {
    if (formation != null) formation.toggleRecruit(this, false);
    if (home      != null) home.residents.remove(this);
    if (work      != null) work.workers  .remove(this);
    home      = null;
    work      = null;
    formation = null;
  }
  
  
  void assignHomeCity(City city) {
    this.homeCity = city;
    this.guest    = true;
  }
  
  
  boolean onMap(CityMap map) {
    return map != null && map == this.map;
  }
  
  
  
  /**  Regular updates-
    */
  void update() {
    //  TODO:  Don't allow another job to be assigned while this one is in
    //  the middle of an update!
    if (job != null && job.checkAndUpdatePathing()) {
      final Task task = this.job;
      
      boolean visiting  = task.visits != null;
      boolean targeting = task.target != null;
      boolean combat    = inCombat();
      Tile    pathEnd   = (Tile) Visit.last(task.path);
      float   distance  = CityMap.distance(at, pathEnd);
      float   minRange  = Nums.max(0.1f, combat ? type.attackRange : 0);
      //
      //  If you're close enough to start the behaviour, act accordingly:
      if (distance <= minRange) {
        if (visiting && inside != task.visits) {
          setInside(task.visits, true);
        }
        if (task.timeSpent++ <= task.maxTime) {
          if (visiting) {
            onVisit(task.visits);
            task.visits.visitedBy(this);
            task.origin.walkerVisits(this, task.visits);
          }
          if (targeting) {
            onTarget(task.target);
            task.target.targetedBy(this);
            task.origin.walkerTargets(this, task.target);
          }
        }
        else {
          beginNextBehaviour();
        }
      }
      //
      //  Otherwise, close along the path:
      else {
        task.pathIndex = Nums.clamp(task.pathIndex + 1, task.path.length);
        Tile ahead = task.path[task.pathIndex];
        this.at = ahead;
        if (inside != null) setInside(inside, false);
      }
    }
    else {
      beginNextBehaviour();
    }
    //
    //  Finally, allow the current employer to monitor the walker-
    boolean originOK = job != null && job.origin != null;
    if (originOK) {
      job.origin.walkerUpdates(this);
    }
    //
    //  And update your current health-
    checkHealthState();
  }
  
  
  void beginNextBehaviour() {
    
    job = null;
    
    if (homeCity == null || homeCity == map.city) {
      if (work == null) CityBorders.findWork(map, this);
      if (home == null) CityBorders.findHome(map, this);
    }
    
    if (formation != null && formation.active) {
      formation.selectWalkerBehaviour(this);
    }
    if (job == null && work != null) {
      work.selectWalkerBehaviour(this);
    }
    if (job == null && home != null) {
      home.selectWalkerBehaviour(this);
    }
    if (job == null) {
      startRandomWalk();
    }
    
    if (reports()) {
      I.say("\n"+this+" BEGAN NEW BEHAVIOUR: "+jobType()+", TIME: "+map.time);
      if (job != null) {
        if (job.visits != null) I.say("  VISITING:  "+job.visits);
        if (job.target != null) I.say("  TARGETING: "+job.target);
      }
    }
  }
  
  
  public Task.JOB jobType() {
    if (job == null) return Task.JOB.NONE;
    return job.type;
  }
  
  
  public boolean inCombat() {
    return jobType() == Task.JOB.COMBAT;
  }
  
  
  
  /**  Pathing and visitation utilities:
    */
  private void setInside(Building b, boolean yes) {
    if (b == null) return;
    Employer j = job == null ? null : job.origin;
    
    if (yes && b != inside) {
      b.visitors.include(this);
      inside = b;
      if (j != null) j.walkerEnters(this, inside);
    }
    if (b == inside && ! yes) {
      if (j != null) j.walkerExits(this, inside);
      b.visitors.remove(this);
      inside = null;
    }
  }
  
  
  protected void onVisit(Building visits) {
    return;
  }
  
  
  protected void onTarget(Target target) {
    return;
  }
  
  
  
  /**  Miscellaneous behaviour triggers:
    */
  void embarkOnVisit(Building goes, int maxTime, Task.JOB jobType, Employer e) {
    if (goes == null) return;
    if (reports()) I.say(this+" will visit "+goes+" for time "+maxTime);
    
    job = new Task(this);
    job = job.configTask(e, goes, null, jobType, maxTime);
  }
  
  
  void embarkOnTarget(Target goes, int maxTime, Task.JOB jobType, Employer e) {
    if (goes == null) return;
    if (reports()) I.say(this+" will target "+goes+" for time "+maxTime);
    
    job = new Task(this);
    job = job.configTask(e, null, goes, jobType, maxTime);
  }
  
  
  void returnTo(Building origin) {
    if (origin == null || origin.entrance == null || inside == origin) return;
    if (reports()) I.say(this+" will return to "+origin);
    
    job = new Task(this);
    job = job.configTask(origin, origin, null, Task.JOB.RETURNING, 0);
  }
  
  
  void beginDelivery(
    Building from, Building goes, Task.JOB jobType,
    Good carried, float amount, Employer e
  ) {
    if (from == null || goes == null || goes.entrance == null) return;
    if (from != inside) return;
    
    job = new Task(this);
    job = job.configTask(e, goes, null, jobType, 0);
    if (job == null) return;
    
    if (reports()) I.say(this+" will deliver "+amount+" "+carried+" to "+goes);
    
    from.inventory.add(0 - amount, carried);
    this.carried     = carried;
    this.carryAmount = amount ;
  }
  
  
  void beginAttack(Target target, Task.JOB jobType, Employer e) {
    if (target == null) return;
    if (reports()) I.say(this+" will attack "+target);
    
    job = new Task(this);
    job = job.configTask(e, null, target, jobType, 0);
  }
  
  
  
  
  /**  Inventory methods-
    */
  void offloadGood(Good carried, Building store) {
    if (store == null || carried != this.carried) return;
    
    if (reports()) I.say(this+" Depositing "+carried+" at "+store);
    
    store.inventory.add(carryAmount, carried);
    this.carried = null;
    this.carryAmount = 0;
  }
  
  
  
  /**  Combat and survival-related code:
    */
  void performAttack(Fixture other) {
    if (other == null || type.attackScore <= 0) return;
    
    int damage = Rand.index(type.attackScore + other.type.defendScore) + 1;
    damage = Nums.max(0, damage - other.type.defendScore);
    
    if (damage > 0) {
      other.takeDamage(damage);
    }
  }
  
  
  void takeDamage(float damage) {
    injury += damage;
    checkHealthState();
  }
  
  
  void checkHealthState() {
    if (injury + hunger > type.maxHealth && state != STATE_DEAD) {
      //I.say("\n"+this+" HAS BEEN KILLED!");
      state = STATE_DEAD;
      job   = null;
      exitMap();
      setDestroyed();
    }
  }
  
  
  
  /**  Wandering code:
    */
  //  TODO:  Consider moving this out to a separate class.
  
  void startRandomWalk() {
    if (reports()) I.say(this+" beginning random walk...");
    
    if (inside != null && inside.entrance != null) {
      this.at = inside.entrance;
      setInside(inside, false);
    }
    facing = T_ADJACENT[Rand.index(4)];
    
    Task j = this.job = new Task(this);
    j.type      = Task.JOB.WANDERING;
    j.timeSpent = 0;
    j.maxTime   = 0;
    j.path      = extractRandomWalk();
  }
  
  
  Tile[] extractRandomWalk() {
    Batch <Tile> walk = new Batch();
    Tile next = this.at;
    
    while (walk.size() < MAX_WANDER_RANGE) {
      boolean prefPave = map.paved(next.x, next.y);
      int nx, ny, numDirs = 0;
      int backDir = (facing + 4) % 8;
      
      for (int dir : T_ADJACENT) {
        if (dir == backDir) continue;
        nx = next.x + T_X[dir];
        ny = next.y + T_Y[dir];
        if (prefPave && ! map.paved(nx, ny)) continue;
        if (map.blocked(nx, ny)) continue;
        dirs[numDirs] = dir;
        numDirs++;
      }
      if (numDirs == 0) {
        facing = backDir;
      }
      else if (numDirs > 1) {
        facing = dirs[Rand.index(numDirs)];
      }
      else {
        facing = dirs[0];
      }
      next = map.tileAt(
        next.x + T_X[facing],
        next.y + T_Y[facing]
      );
      walk.add(next);
    }
    
    return walk.toArray(Tile.class);
  }
  
  
  
  /**  Migration code:
    */
  public void onArrival(City goes, World.Journey journey) {
    if (goes.map != null) {
      Tile entry = CityBorders.findTransitPoint(goes.map, journey.from);
      enterMap(goes.map, entry.x, entry.y);
      beginNextBehaviour();
    }
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  protected boolean reports() {
    if (I.talkAbout == null) return false;
    return I.talkAbout == home || I.talkAbout == work;
  }
  
  
  public String toString() {
    String from = homeCity == null ? "" : " ("+homeCity.name+")";
    return type.name+" "+ID+from;
  }
}



