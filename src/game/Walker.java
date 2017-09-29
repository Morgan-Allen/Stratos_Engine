

package game;
import static game.GameConstants.*;
import static game.CityMap.*;
import util.*;
import static util.TileConstants.*;



public class Walker implements Session.Saveable {
  
  
  /**  Data fields and setup/initialisation-
    */
  final public static int
    JOB_NONE      = -1,
    JOB_RESTING   =  0,
    JOB_WANDER    =  1,
    JOB_DELIVER   =  2,
    JOB_SHOPPING  =  3,
    JOB_TRADING   =  4,
    JOB_VISITING  =  5,
    JOB_GATHERING =  6,
    JOB_CRAFTING  =  7,
    JOB_HUNTING   =  8,
    JOB_MILITARY  =  9,
    
    MAX_WANDER_TIME = 20,
    AVG_VISIT_TIME  = 20,
    TRADE_DIST_TIME = 50,
    
    STATE_OKAY   = 1,
    STATE_SLEEP  = 2,
    STATE_DEAD   = 3,
    STATE_DECOMP = 4
  ;
  static int nextID = 0;
  int dirs[] = new int[4];
  
  
  ObjectType type;
  String ID;
  
  CityMap map;
  int x, y, facing = N;
  
  Building  work;
  Building  home;
  Formation formation;
  Building  inside;
  
  static class Job {
    Employer origin;
    
    int jobType   = JOB_NONE;
    int timeSpent = 0 ;
    int maxTime   = 20;
    
    Tile path[] = null;
    int pathIndex = -1;
    Tile     target;
    Building visits;
  }
  Job job;
  
  Good carried = null;
  float carryAmount = 0;
  
  float injury ;
  float hunger ;
  float fatigue;
  float stress ;
  int   state = STATE_OKAY;
  
  
  
  Walker(ObjectType type) {
    this.type = type;
    this.ID = "#"+nextID++;
  }
  
  
  public Walker(Session s) throws Exception {
    s.cacheInstance(this);
    
    type = (ObjectType) s.loadObject();
    ID   = s.loadString();
    
    map    = (CityMap) s.loadObject();
    x      = s.loadInt();
    y      = s.loadInt();
    facing = s.loadInt();
    
    work      = (Building ) s.loadObject();
    home      = (Building ) s.loadObject();
    formation = (Formation) s.loadObject();
    inside    = (Building ) s.loadObject();
    
    if (s.loadBool()) {
      Job j = this.job = new Job();
      j.origin    = (Employer) s.loadObject();
      j.jobType   = s.loadInt();
      j.timeSpent = s.loadInt();
      j.maxTime   = s.loadInt();
      
      int PL = s.loadInt();
      if (PL == -1) j.path = null;
      else {
        j.path = new Tile[PL];
        for (int i = 0; i < PL; i++) j.path[i] = Tile.loadTile(map, s);
      }
      j.pathIndex = s.loadInt();
      j.target    = Tile.loadTile(map, s);
      j.visits    = (Building) s.loadObject();
    }
    else job = null;
    
    carried     = (Good) s.loadObject();
    carryAmount = s.loadFloat();
    
    injury  = s.loadFloat();
    hunger  = s.loadFloat();
    fatigue = s.loadFloat();
    stress  = s.loadFloat();
    state   = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(type);
    s.saveString(ID);
    
    s.saveObject(map);
    s.saveInt(x);
    s.saveInt(y);
    s.saveInt(facing);
    
    s.saveObject(work     );
    s.saveObject(home     );
    s.saveObject(formation);
    s.saveObject(inside   );
    
    s.saveBool(job != null);
    if (job != null) {
      s.saveObject(job.origin);
      s.saveInt(job.jobType  );
      s.saveInt(job.timeSpent);
      s.saveInt(job.maxTime  );
      
      if (job.path == null) s.saveInt(-1);
      else {
        s.saveInt(job.path.length);
        for (Tile t : job.path) Tile.saveTile(t, map, s);
      }
      s.saveInt(job.pathIndex);
      Tile.saveTile(job.target, map, s);
      s.saveObject(job.visits);
    }
    
    s.saveObject(carried);
    s.saveFloat(carryAmount);
    
    s.saveFloat(injury );
    s.saveFloat(hunger );
    s.saveFloat(fatigue);
    s.saveFloat(stress );
    s.saveInt  (state  );
  }
  
  
  
  /**  World entry and exit-
    */
  void enterMap(CityMap map, int x, int y) {
    this.map = map;
    this.x   = x  ;
    this.y   = y  ;
    map.walkers.add(this);
  }
  
  
  void exitMap() {
    if (inside != null) setInside(inside, false);
    map.walkers.remove(this);
    map = null;
    if (job != null) {
      job.visits = null;
      job.target = null;
      job.path   = null;
    }
  }
  
  
  void setDestroyed() {
    if (home != null) home.walkers.remove(this);
    home = null;
  }
  
  
  
  /**  Regular updates-
    */
  void update() {
    
    if (jobType() == JOB_WANDER) {
      updateRandomWalk();
    }
    else if (job != null) {
      boolean visiting  = job.visits != null;
      boolean targeting = job.target != null;
      
      //  TODO:  You should also re-route if the path is blocked!
      if (visiting && Visit.last(job.path) != job.visits.entrance) {
        updatePathing(job);
      }
      
      if (job != null && ++job.pathIndex >= job.path.length) {
        if (visiting && inside != job.visits) {
          setInside(job.visits, true);
        }
        else if (job.timeSpent++ <= job.maxTime) {
          if (visiting) {
            onVisit(job.visits);
            job.origin.walkerVisits(this, job.visits);
          }
          if (targeting) {
            onTarget(job.target);
            job.origin.walkerTargets(this, job.target);
          }
        }
        else {
          beginNextBehaviour();
        }
      }
      
      else {
        Tile ahead = job.path[job.pathIndex];
        x = ahead.x;
        y = ahead.y;
        if (inside != null) setInside(inside, false);
      }
    }
    else {
      beginNextBehaviour();
    }
  }
  
  
  void beginNextBehaviour() {
    if (formation != null && formation.active) {
      formation.selectWalkerBehaviour(this);
    }
    else if (work != null) {
      work.selectWalkerBehaviour(this);
    }
    else if (home != null) {
      home.selectWalkerBehaviour(this);
    }
    if (job == null) {
      startRandomWalk();
    }
  }
  
  
  public int jobType() {
    if (job == null) return JOB_NONE;
    return job.jobType;
  }
  
  
  
  /**  Pathing and visitation utilities:
    */
  private void setInside(Building b, boolean yes) {
    if (b == null || home == null) return;
    
    if (yes && b != inside) {
      b.visitors.include(this);
      inside = b;
      home.walkerEnters(this, inside);
    }
    if (b == inside && ! yes) {
      home.walkerExits(this, inside);
      b.visitors.remove(this);
      inside = null;
    }
  }
  
  
  private void beginJob(
    Employer origin, Building visits, Tile target, int jobType, int maxTime
  ) {
    Job j = this.job = new Job();
    j.origin    = origin ;
    j.jobType   = jobType;
    j.timeSpent = 0      ;
    j.maxTime   = maxTime;
    j.visits    = visits ;
    j.target    = target ;
    
    if (j.maxTime == -1) j.maxTime = AVG_VISIT_TIME;
    j.path = updatePathing(j);
    
    if (j.path != null) {
      I.say("  Path is: "+j.path.length+" tiles long...");
    }
    else {
      I.say("  Could not find path!");
      this.job = null;
    }
  }
  
  
  private Tile[] updatePathing(Job j) {
    boolean visiting = j.visits != null;
    I.say(this+" pathing toward "+(visiting ? j.visits : j.target));
    
    Tile at    = (inside == null) ? map.tileAt(x, y) : inside.entrance;
    Tile heads = null;
    if (visiting) heads = j.visits.entrance;
    else          heads = j.target;
    
    PathSearch search = new PathSearch(map, this, at, heads);
    search.setPaveOnly(visiting && map.paved(at.x, at.y));
    search.doSearch();
    return search.fullPath(Tile.class);
  }
  
  
  protected void onVisit(Building visits) {
    return;
  }
  
  
  protected void onTarget(Tile target) {
    return;
  }
  
  
  
  /**  Miscellaneous behaviour triggers:
    */
  void embarkOnVisit(Building goes, int maxTime, int jobType, Employer e) {
    if (goes == null) return;
    I.say(this+" will visit "+goes+" for time "+maxTime);
    beginJob(e, goes, null, jobType, maxTime);
  }
  
  
  void embarkOnTarget(Tile goes, int maxTime, int jobType, Employer e) {
    if (goes == null) return;
    I.say(this+" will target "+goes+" for time "+maxTime);
    beginJob(e, null, goes, jobType, maxTime);
  }
  
  
  void returnTo(Building origin) {
    if (origin == null || origin.entrance == null || inside == origin) return;
    I.say(this+" will return home...");
    beginJob(origin, origin, null, JOB_RESTING, 0);
  }
  
  
  void beginDelivery(
    Building from, Building goes, int jobType,
    Good carried, float amount
  ) {
    if (from == null || goes == null || goes.entrance == null) return;
    if (from != inside) return;
    
    I.say(this+" will deliver "+amount+" "+carried+" to "+goes);
    
    from.inventory.add(0 - amount, carried);
    this.carried     = carried;
    this.carryAmount = amount ;
    beginJob(from, goes, null, jobType, 0);
  }
  
  
  void offloadGood(Good carried, Building store) {
    if (store == null || carried != this.carried) return;
    
    I.say(this+" Depositing "+carried+" at "+store);
    
    store.inventory.add(carryAmount, carried);
    this.carried = null;
    this.carryAmount = 0;
  }
  
  
  void performAttack(Walker other) {
    if (type.attackScore <= 0) return;
    
    int damage = Rand.index(type.attackScore + other.type.defendScore) + 1;
    damage -= other.type.defendScore;
    
    other.injury += damage;
    other.checkHealthState();
  }
  
  
  void checkHealthState() {
    if (injury + hunger > type.maxHealth && state != STATE_DEAD) {
      state = STATE_DEAD;
      job   = null;
    }
  }
  
  
  
  /**  Wandering code:
    */
  //  TODO:  Consider moving this out to a separate class.
  
  void startRandomWalk() {
    if (inside == null || inside.entrance == null) return;
    
    I.say(this+" beginning random walk...");
    Job j = this.job = new Job();
    j.jobType   = JOB_WANDER;
    j.timeSpent = 0;
    j.maxTime   = MAX_WANDER_TIME;
    
    Tile at = inside.entrance;
    x      = at.x;
    y      = at.y;
    facing = T_ADJACENT[Rand.index(4)];
    
    if (inside != null) setInside(inside, false);
  }
  
  
  void updateRandomWalk() {
    if (inside != null) setInside(inside, false);
    
    int nx, ny, numDirs = 0;
    int backDir = (facing + 4) % 8;
    
    for (int dir : T_ADJACENT) {
      if (dir == backDir) continue;
      nx = x + T_X[dir];
      ny = y + T_Y[dir];
      if (! map.paved(nx, ny)) continue;
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
    x = x + T_X[facing];
    y = y + T_Y[facing];
    
    if (++job.timeSpent >= job.maxTime) {
      beginNextBehaviour();
    }
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  public String toString() {
    return type.name+" "+ID;
  }
}







