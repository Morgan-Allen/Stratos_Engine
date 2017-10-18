

package game;
import util.*;
import static game.Task.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class Actor extends Element implements Session.Saveable, Journeys {
  
  
  /**  Data fields and setup/initialisation-
    */
  final public static int
    STATE_OKAY   = 1,
    STATE_SLEEP  = 2,
    STATE_DEAD   = 3,
    STATE_DECOMP = 4,
    
    SEX_MALE   = 1 << 0,
    SEX_FEMALE = 1 << 1
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
  Tally <Good> cargo = null;
  
  int sexData    = -1;
  int ageSeconds =  0;
  int pregnancy  =  0;
  float injury ;
  float hunger ;
  float fatigue;
  float stress ;
  int   state = STATE_OKAY;
  
  Tally <Type> skills = new Tally();
  
  
  
  Actor(Type type) {
    super(type);
    this.ID = "#"+nextID++;
  }
  
  
  public Actor(Session s) throws Exception {
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
    if (s.loadBool()) s.loadTally(cargo);
    
    sexData    = s.loadInt();
    ageSeconds = s.loadInt();
    pregnancy  = s.loadInt();
    
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
    s.saveBool(cargo != null);
    if (cargo != null) s.saveTally(cargo);
    
    s.saveInt(sexData   );
    s.saveInt(ageSeconds);
    s.saveInt(pregnancy );
    
    s.saveFloat(injury );
    s.saveFloat(hunger );
    s.saveFloat(fatigue);
    s.saveFloat(stress );
    s.saveInt  (state  );
    s.saveTally(skills);
  }
  
  
  
  /**  World entry and exit-
    */
  void enterMap(CityMap map, int x, int y, float buildLevel) {
    this.map = map;
    this.at  = map.tileAt(x, y);
    map.walkers.add(this);
  }
  
  
  void exitMap() {
    if (inside != null) setInside(inside, false);
    
    map.walkers.remove(this);
    map = null;
    at  = null;
  }
  
  
  void setDestroyed() {
    if (formation != null) formation.toggleRecruit(this, false);
    if (home      != null) home.setResident(this, false);
    if (work      != null) work.setWorker  (this, false);
    if (job       != null) job.onCancel();
    home      = null;
    work      = null;
    formation = null;
    job       = null;
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
    //
    //  TODO:  Don't allow another job to be assigned while this one is in
    //  the middle of an update!
    if (job != null && job.checkAndUpdatePathing()) {
      Task     task      = this.job;
      Employer origin    = task.origin;
      Building visits    = task.visits;
      Target   target    = task.target;
      boolean  combat    = inCombat();
      Tile     pathEnd   = (Tile) Visit.last(task.path);
      float    distance  = CityMap.distance(at, pathEnd);
      float    minRange  = Nums.max(0.1f, combat ? type.attackRange : 0);
      //
      //  If you're close enough to start the behaviour, act accordingly:
      if (distance <= minRange) {
        if (visits != null && inside != visits) {
          setInside(visits, true);
        }
        if (task.timeSpent++ <= task.maxTime) {
          if (visits != null) {
            onVisit(visits);
            task.onVisit(visits);
            visits.visitedBy(this);
            if (origin != null) origin.actorVisits(this, visits);
          }
          if (target != null) {
            onTarget(target);
            task.onTarget(target);
            target.targetedBy(this);
            if (origin != null) origin.actorTargets(this, target);
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
    //  Finally, allow the current employer to monitor the actor-
    if (job != null && job.origin != null) {
      job.origin.actorUpdates(this);
    }
    //
    //  Update vision-
    if (map != null && inside == null) {
      updateVision();
    }
    //
    //  And update your current health-
    updateAging();
    checkHealthState();
  }
  
  
  void beginNextBehaviour() {
    job = null;
    
    //  NOTE:  Subclasses are expected to override this behaviour!
    if (job == null) {
      startRandomWalk();
    }
  }
  
  
  
  /**  Pathing and visitation utilities:
    */
  private void setInside(Building b, boolean yes) {
    if (b == null) return;
    Employer origin = job == null ? null : job.origin;
    
    if (yes && b != inside) {
      b.visitors.include(this);
      inside = b;
      if (origin != null) origin.actorEnters(this, inside);
    }
    if (b == inside && ! yes) {
      if (origin != null) origin.actorExits(this, inside);
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
  void assignTask(Task task) {
    this.job = task;
  }
  
  
  void embarkOnVisit(Building goes, int maxTime, JOB jobType, Employer e) {
    if (goes == null) return;
    if (reports()) I.say(this+" will visit "+goes+" for time "+maxTime);
    
    job = new Task(this);
    job = job.configTask(e, goes, null, jobType, maxTime);
  }
  
  
  void embarkOnTarget(Target goes, int maxTime, JOB jobType, Employer e) {
    if (goes == null) return;
    if (reports()) I.say(this+" will target "+goes+" for time "+maxTime);
    
    job = new Task(this);
    job = job.configTask(e, null, goes, jobType, maxTime);
  }
  
  
  void returnTo(Building origin) {
    if (origin == null || origin.entrance == null || inside == origin) return;
    if (reports()) I.say(this+" will return to "+origin);
    
    job = new Task(this);
    job = job.configTask(origin, origin, null, JOB.RETURNING, 0);
  }
  
  
  void beginDelivery(
    Building from, Building goes, JOB jobType,
    Good carried, float amount, Employer e
  ) {
    if (from == null || goes == null || goes.entrance == null) return;
    
    TaskDelivery d = new TaskDelivery(this);
    job = d.configDelivery(from, goes, jobType, carried, amount, e);
    if (job == null) return;
    
    if (reports()) I.say(this+" will deliver "+amount+" "+carried+" to "+goes);
  }
  
  
  void beginAttack(Target target, JOB jobType, Employer e) {
    if (target == null) return;
    if (reports()) I.say(this+" will attack "+target);
    
    job = new Task(this);
    job = job.configTask(e, null, target, jobType, 0);
  }
  
  
  void beginResting(Building rests) {
    if (rests == null) return;
    if (reports()) I.say(this+" will rest at "+rests);
    
    job = new Task(this);
    job = job.configTask(rests, rests, null, JOB.RESTING, -1);
  }
  
  
  void startRandomWalk() {
    if (reports()) I.say(this+" beginning random walk...");
    
    job = new TaskWander(this);
    job = job.configTask(null, null, null, JOB.WANDERING, 0);
  }
  
  
  public JOB jobType() {
    if (job == null) return JOB.NONE;
    return job.type;
  }
  
  
  public boolean inCombat() {
    return jobType() == JOB.COMBAT;
  }
  
  
  
  /**  Methods to assist trade and migration-
    */
  void pickupGood(Good carried, float amount, Building store) {
    if (store == null || carried == null || amount <= 0) return;
    
    if (this.carried != carried) this.carryAmount = 0;
    
    store.inventory.add(0 - amount, carried);
    this.carried      = carried;
    this.carryAmount += amount ;
  }
  
  
  void offloadGood(Good carried, Building store) {
    if (store == null || carried != this.carried) return;
    
    if (reports()) I.say(this+" Depositing "+carried+" at "+store);
    
    store.inventory.add(carryAmount, carried);
    this.carried = null;
    this.carryAmount = 0;
  }
  
  
  void assignCargo(Tally <Good> cargo) {
    this.cargo = cargo;
  }
  
  
  public void onArrival(City goes, World.Journey journey) {
    if (goes.map != null) {
      Tile entry = CityBorders.findTransitPoint(goes.map, journey.from);
      enterMap(goes.map, entry.x, entry.y, 1);
    }
    if (job != null) {
      job.onArrival(goes, journey);
    }
  }
  
  
  
  /**  Combat and survival-related code:
    */
  void performAttack(Element other) {
    if (other == null || type.attackScore <= 0) return;
    
    int damage = Rand.index(type.attackScore + other.type.defendScore) + 1;
    damage = Nums.max(0, damage - other.type.defendScore);
    
    if (damage > 0) {
      other.takeDamage(damage);
    }
  }
  
  
  void takeDamage(float damage) {
    if (! GameSettings.toggleInjury) return;
    injury += damage;
    checkHealthState();
  }
  
  
  void setAsKilled(String cause) {
    state = STATE_DEAD;
    job   = null;
    exitMap();
    setDestroyed();
    ///I.say(this+" DIED FROM CAUSE: "+cause);
  }
  
  
  void updateVision() {
    return;
  }
  
  
  void checkHealthState() {
    if (injury + hunger > type.maxHealth && state != STATE_DEAD) {
      setAsKilled("Injury: "+injury+" Hunger "+hunger);
    }
  }
  
  
  boolean alive() {
    return state != STATE_DEAD;
  }
  
  
  boolean dead() {
    return state == STATE_DEAD;
  }
  
  
  
  /**  Aging, reproduction and life-cycle:
    */
  float ageYears() {
    float years = ageSeconds / (YEAR_LENGTH * 1f);
    return years;
  }
  
  
  float growLevel() {
    return Nums.min(1, ageYears() / AVG_MARRIED);
  }
  
  
  boolean adult() {
    return true;
  }
  
  
  boolean child() {
    return false;
  }
  
  
  void updateAging() {
    ageSeconds += 1;
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



