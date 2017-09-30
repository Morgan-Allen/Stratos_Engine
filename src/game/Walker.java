

package game;
import util.*;
import static game.GameConstants.*;
import static game.CityMap.*;
import static util.TileConstants.*;



public class Walker extends Fixture implements Session.Saveable {
  
  
  /**  Data fields and setup/initialisation-
    */
  public static enum JOB {
    NONE     ,
    RETURNING,
    RESTING  ,
    WANDER   ,
    DELIVER  ,
    SHOPPING ,
    TRADING  ,
    VISITING ,
    GATHERING,
    CRAFTING ,
    MILITARY ,
    HUNTING  ,
    COMBAT
  };
  final public static int
    
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
  
  
  String ID;
  
  Building  work;
  Building  home;
  City      homeCity;
  Formation formation;
  Building  inside;
  boolean   guest;
  
  static class Task {
    Employer origin;
    
    JOB type      = JOB.NONE;
    int timeSpent = 0 ;
    int maxTime   = 20;
    
    Tile path[] = null;
    int pathIndex = -1;
    
    Target   target;
    Building visits;
  }
  Task job;
  
  Good  carried = null;
  float carryAmount = 0;
  
  float injury ;
  float hunger ;
  float fatigue;
  float stress ;
  int   state = STATE_OKAY;
  
  
  
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
    
    if (s.loadBool()) {
      Task j = this.job = new Task();
      j.origin    = (Employer) s.loadObject();
      j.type      = JOB.values()[s.loadInt()];
      j.timeSpent = s.loadInt();
      j.maxTime   = s.loadInt();
      
      int PL = s.loadInt();
      if (PL == -1) {
        j.path = null;
      }
      else {
        j.path = new Tile[PL];
        for (int i = 0; i < PL; i++) j.path[i] = loadTile(map, s);
      }
      j.pathIndex = s.loadInt();
      j.target    = (Target  ) s.loadObject();
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
    super.saveState(s);
    
    s.saveString(ID);
    
    s.saveObject(work     );
    s.saveObject(home     );
    s.saveObject(homeCity );
    s.saveObject(formation);
    s.saveObject(inside   );
    s.saveBool  (guest    );
    
    s.saveBool(job != null);
    if (job != null) {
      s.saveObject(job.origin);
      s.saveInt(job.type.ordinal());
      s.saveInt(job.timeSpent);
      s.saveInt(job.maxTime);
      
      if (job.path == null) s.saveInt(-1);
      else {
        s.saveInt(job.path.length);
        for (Tile t : job.path) saveTile(t, map, s);
      }
      s.saveInt(job.pathIndex);
      s.saveObject(job.target);
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
    this.at  = map.tileAt(x, y);
    map.walkers.add(this);
  }
  
  
  void exitMap() {
    if (inside != null) setInside(inside, false);
    map.walkers.remove(this);
    map = null;
    at  = null;
    if (job != null) {
      job.visits = null;
      job.target = null;
      job.path   = null;
    }
  }
  
  
  void setDestroyed() {
    if (formation != null) formation.toggleRecruit(this, false);
    if (home      != null) home.resident.remove(this);
    if (work      != null) work.resident.remove(this);
    home = null;
  }
  
  
  void assignHomeCity(City city) {
    this.homeCity = city;
    this.guest    = true;
  }
  
  
  
  /**  Regular updates-
    */
  void update() {
    //  TODO:  Don't allow another job to be assigned while this one is in
    //  the middle of an update!
    if (job != null && checkAndUpdatePathing(job)) {
      
      boolean visiting  = job.visits != null;
      boolean targeting = job.target != null;
      boolean combat    = inCombat();
      Tile    pathEnd   = (Tile) Visit.last(job.path);
      float   distance  = CityMap.distance(at, pathEnd);
      float   minRange  = Nums.max(0.1f, combat ? type.attackRange : 0);
      //
      //  If you're close enough to start the behaviour, act accordingly:
      if (distance <= minRange) {
        if (visiting && inside != job.visits) {
          setInside(job.visits, true);
        }
        if (job.timeSpent++ <= job.maxTime) {
          if (visiting) {
            onVisit(job.visits);
            job.visits.visitedBy(this);
            job.origin.walkerVisits(this, job.visits);
          }
          if (targeting) {
            onTarget(job.target);
            job.target.targetedBy(this);
            job.origin.walkerTargets(this, job.target);
          }
        }
        else {
          beginNextBehaviour();
        }
      }
      //
      //  Otherwise, close along the path:
      else {
        job.pathIndex = Nums.clamp(job.pathIndex + 1, job.path.length);
        Tile ahead = job.path[job.pathIndex];
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
  
  
  public JOB jobType() {
    if (job == null) return JOB.NONE;
    return job.type;
  }
  
  
  public boolean inCombat() {
    return jobType() == JOB.COMBAT;
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
  
  
  private void beginTask(
    Employer origin, Building visits, Target target, JOB jobType, int maxTime
  ) {
    Task j = this.job = new Task();
    j.origin    = origin ;
    j.type      = jobType;
    j.timeSpent = 0      ;
    j.maxTime   = maxTime;
    j.visits    = visits ;
    j.target    = target ;
    
    if (maxTime == -1) j.maxTime = AVG_VISIT_TIME;
    j.path = updatePathing(j);
    
    if (j.path != null) {
      if (reports()) I.say("  Path is: "+j.path.length+" tiles long...");
    }
    else {
      if (reports()) I.say("  Could not find path!");
      this.job = null;
    }
  }
  
  
  private boolean checkAndUpdatePathing(Task j) {
    if (checkPathing(j)) return true;
    
    job.path = updatePathing(job);
    if (job.path != null) return true;
    
    job = null;
    return false;
  }
  
  
  private boolean checkPathing(Task j) {
    if (j == null || j.path == null        ) return false;
    if (Visit.last(j.path) != pathTarget(j)) return false;
    
    for (int i = 0; i < type.sightRange; i++) {
      if (i >= j.path.length) break;
      Tile t = j.path[i];
      if (map.blocked(t.x, t.y)) return false;
    }
    
    return true;
  }
  
  
  private Tile pathTarget(Task j) {
    if (j == null) return null;
    Tile t = null;
    if (t == null && j.visits != null) t = j.visits.entrance;
    if (t == null && j.target != null) t = j.target.at();
    if (t == null && j.path   != null) t = (Tile) Visit.last(j.path);
    return t;
  }
  
  
  private Tile[] updatePathing(Task j) {
    boolean visiting = j.visits != null;
    
    if (reports()) {
      I.say(this+" pathing toward "+(visiting ? j.visits : j.target));
    }
    
    Tile from  = (inside == null) ? this.at : inside.entrance;
    Tile heads = pathTarget(j);
    
    if (from == null || heads == null) return null;
    
    PathSearch search = new PathSearch(map, this, from, heads);
    search.setPaveOnly(visiting && map.paved(from.x, from.y));
    search.doSearch();
    return search.fullPath(Tile.class);
  }
  
  
  protected void onVisit(Building visits) {
    return;
  }
  
  
  protected void onTarget(Target target) {
    return;
  }
  
  
  
  /**  Miscellaneous behaviour triggers:
    */
  void embarkOnVisit(Building goes, int maxTime, JOB jobType, Employer e) {
    if (goes == null) return;
    if (reports()) I.say(this+" will visit "+goes+" for time "+maxTime);
    beginTask(e, goes, null, jobType, maxTime);
  }
  
  
  void embarkOnTarget(Target goes, int maxTime, JOB jobType, Employer e) {
    if (goes == null) return;
    if (reports()) I.say(this+" will target "+goes+" for time "+maxTime);
    beginTask(e, null, goes, jobType, maxTime);
  }
  
  
  void returnTo(Building origin) {
    if (origin == null || origin.entrance == null || inside == origin) return;
    if (reports()) I.say(this+" will return to "+origin);
    beginTask(origin, origin, null, JOB.RETURNING, 0);
  }
  
  
  void beginDelivery(
    Building from, Building goes, JOB jobType,
    Good carried, float amount
  ) {
    if (from == null || goes == null || goes.entrance == null) return;
    if (from != inside) return;
    
    if (reports()) I.say(this+" will deliver "+amount+" "+carried+" to "+goes);
    
    from.inventory.add(0 - amount, carried);
    this.carried     = carried;
    this.carryAmount = amount ;
    beginTask(from, goes, null, jobType, 0);
  }
  
  
  void offloadGood(Good carried, Building store) {
    if (store == null || carried != this.carried) return;
    
    if (reports()) I.say(this+" Depositing "+carried+" at "+store);
    
    store.inventory.add(carryAmount, carried);
    this.carried = null;
    this.carryAmount = 0;
  }
  
  
  
  /**  Combat and survival-related code:
    */
  void beginAttack(Walker target, JOB jobType, Employer e) {
    if (target == null) return;
    
    if (reports()) I.say(this+" will attack "+target);
    
    beginTask(e, null, target, jobType, 0);
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
      I.say("\n"+this+" HAS BEEN KILLED!");
      state = STATE_DEAD;
      job   = null;
      exitMap();
      setDestroyed();
    }
  }
  
  
  
  /**  Wandering code:
    */
  //  TODO:  Consider moving this out to a separate class?
  
  void startRandomWalk() {
    if (reports()) I.say(this+" beginning random walk...");
    
    if (inside != null && inside.entrance != null) {
      this.at = inside.entrance;
      setInside(inside, false);
    }
    facing = T_ADJACENT[Rand.index(4)];
    
    Task j = this.job = new Task();
    j.type      = JOB.WANDER;
    j.timeSpent = 0;
    j.maxTime   = 0;
    j.path      = extractRandomWalk();
  }
  
  
  Tile[] extractRandomWalk() {
    Batch <Tile> walk = new Batch();
    Tile next = this.at;
    
    while (walk.size() < MAX_WANDER_TIME) {
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



