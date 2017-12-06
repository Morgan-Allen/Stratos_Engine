

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
    STATE_DECOMP = 4
  ;
  static int nextID = 0;
  int dirs[] = new int[4];
  
  
  String ID;
  
  Building  work;
  Building  home;
  City      homeCity;
  boolean   guest;
  Building  recruiter;
  Formation formation;
  
  Task task;
  Pathing inside;
  
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
    guest     = s.loadBool();
    recruiter = (Building ) s.loadObject();
    formation = (Formation) s.loadObject();
    
    task   = (Task   ) s.loadObject();
    inside = (Pathing) s.loadObject();
    
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
    s.saveBool  (guest    );
    s.saveObject(recruiter);
    s.saveObject(formation);
    
    s.saveObject(task  );
    s.saveObject(inside);
    
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
    super.enterMap(map, x, y, buildLevel);
    map.actors.add(this);
  }
  
  
  void exitMap(CityMap map) {
    if (inside != null) setInside(inside, false);
    map.actors.remove(this);
    super.exitMap(map);
  }
  
  
  void setDestroyed() {
    super.setDestroyed();
    if (formation != null) formation.toggleRecruit(this, false);
    if (home      != null) home.setResident(this, false);
    if (work      != null) work.setWorker  (this, false);
    if (task      != null) task.onCancel();
    home      = null;
    work      = null;
    recruiter = null;
    formation = null;
    assignTask(null);
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
    //  Some checks to assist in case of blockage...
    Tile at = at();
    if (inside == null && ! map.pathCache.hasGroundAccess(at)) {
      if (at.above != null && at.above.type.isBuilding()) {
        setInside((Building) at.above, true);
      }
      else {
        Tile free = map.pathCache.mostOpenNeighbour(at);
        if (free != null) setLocation(free, map);
      }
    }
    //
    //  Task updates-
    if (task != null && task.checkAndUpdatePathing()) {
      Task     task     = this.task;
      Employer origin   = task.origin;
      Building visits   = task.visits;
      Target   target   = task.target;
      boolean  combat   = inCombat();
      Pathing  pathEnd  = (Pathing) Visit.last(task.path);
      float    distance = CityMap.distance(at, pathEnd);
      float    minRange = 0.1f;
      
      if (combat && ! indoors()) minRange = type.attackRange;
      
      if (visits != null && inside == visits) {
        if (task.timeSpent++ <= task.maxTime) {
          onVisit(visits);
          task.onVisit(visits);
          visits.visitedBy(this);
          if (origin != null) origin.actorVisits(this, visits);
        }
        else {
          beginNextBehaviour();
        }
      }
      else if (distance <= minRange) {
        if (target != null && task.timeSpent++ <= task.maxTime) {
          onTarget(target);
          task.onTarget(target);
          target.targetedBy(this);
          if (origin != null) origin.actorTargets(this, target);
        }
        else {
          beginNextBehaviour();
        }
      }
      else {
        int index = Nums.clamp(task.pathIndex + 1, task.path.length);
        if (index != -1) {
          Pathing ahead = task.path[task.pathIndex = index];
          setLocation(ahead.at(), map);
          if (ahead.isTile()) setInside(inside, false);
          else setInside(ahead, true);
        }
        else {
          task.path = null;
          task.pathIndex = -1;
        }
      }
      //
      //  Either way, allow the employer to monitor yourself:
      if (origin != null) origin.actorUpdates(this);
    }
    else {
      beginNextBehaviour();
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
    assignTask(null);
    
    //  NOTE:  Subclasses are expected to override this behaviour!
    if (idle()) {
      startRandomWalk();
    }
  }
  
  
  
  /**  Pathing and visitation utilities:
    */
  void setInside(Pathing b, boolean yes) {
    if (b == null || ! b.onMap()) {
      if (b == inside) inside = null;
      return;
    }
    final Pathing old = inside;
    if (yes && b != old) {
      if (old != null) {
        setInside(old, false);
      }
      b.setInside(this, true);
      inside = b;
    }
    if (b == old && ! yes) {
      b.setInside(this, false);
      inside = null;
    }
  }
  
  
  void setLocation(Tile at, CityMap map) {
    if (! onMap()) {
      super.setLocation(at, map);
      return;
    }
    Tile old = this.at();
    super.setLocation(at, map);
    map.flagActor(this, old, false);
    map.flagActor(this, at , true );
  }
  
  
  protected void onVisit(Building visits) {
    return;
  }
  
  
  protected void onTarget(Target target) {
    return;
  }
  
  
  public Pathing inside() {
    return inside;
  }
  
  
  public boolean indoors() {
    return inside != null;
  }
  
  
  
  /**  Miscellaneous behaviour triggers:
    */
  void assignTask(Task task) {
    ///I.say("\n"+this+" assigned task: "+task);
    ///I.reportStackTrace();
    
    Target oldT = Task.focusTarget(this.task);
    this.task = task;
    Target newT = Task.focusTarget(this.task);
    
    if (oldT != null) oldT.setFocused(this, false);
    if (newT != null) newT.setFocused(this, true );
  }
  
  
  void embarkOnVisit(Building goes, int maxTime, JOB jobType, Employer e) {
    if (goes == null) return;
    if (reports()) I.say(this+" will visit "+goes+" for time "+maxTime);
    
    Task t = new Task(this);
    assignTask(t.configTask(e, goes, null, jobType, maxTime));
  }
  
  
  void embarkOnTarget(Target goes, int maxTime, JOB jobType, Employer e) {
    if (goes == null) return;
    if (reports()) I.say(this+" will target "+goes+" for time "+maxTime);
    
    Task t = new Task(this);
    assignTask(t.configTask(e, null, goes, jobType, maxTime));
  }
  
  
  void beginAttack(Target target, JOB jobType, Employer e) {
    if (target == null) return;
    if (reports()) I.say(this+" will attack "+target);
    
    Task t = new Task(this);
    assignTask(t.configTask(e, null, target, jobType, 0));
  }
  
  
  void beginResting(Building rests) {
    if (rests == null) return;
    if (reports()) I.say(this+" will rest at "+rests);
    
    Task t = new Task(this);
    assignTask(t.configTask(rests, rests, null, JOB.RESTING, -1));
  }
  
  
  void startRandomWalk() {
    if (reports()) I.say(this+" beginning random walk...");
    
    Task t = new TaskWander(this);
    assignTask(t.configTask(null, null, null, JOB.WANDERING, 0));
  }
  
  
  public JOB jobType() {
    if (task == null) return JOB.NONE;
    return task.type;
  }
  
  
  public Target jobFocus() {
    return Task.focusTarget(task);
  }
  
  
  public boolean idle() {
    return task == null;
  }
  
  
  public boolean inCombat() {
    JOB type = jobType();
    return type == JOB.COMBAT || type == JOB.HUNTING;
  }
  
  
  
  /**  Methods to assist trade and migration-
    */
  void pickupGood(Good carried, float amount, Building store) {
    if (store == null || carried == null || amount <= 0) return;
    
    store.inventory.add(0 - amount, carried);
    incCarried(carried, amount);
  }
  
  
  void offloadGood(Good carried, Building store) {
    if (store == null || carried != this.carried) return;
    
    if (reports()) I.say(this+" Depositing "+carried+" at "+store);
    
    store.inventory.add(carryAmount, carried);
    this.carried = null;
    this.carryAmount = 0;
  }
  
  
  void incCarried(Good carried, float amount) {
    if (this.carried != carried) this.carryAmount = 0;
    this.carried      = carried;
    this.carryAmount += amount ;
    if (this.carryAmount < 0) this.carryAmount = 0;
  }
  
  
  void assignCargo(Tally <Good> cargo) {
    this.cargo = cargo;
  }
  
  
  float carried(Good g) {
    if (this.carried == g) return carryAmount;
    return 0;
  }
  
  
  public void onArrival(City goes, World.Journey journey) {
    if (goes.map != null) {
      Tile entry = CityBorders.findTransitPoint(goes.map, journey.from);
      enterMap(goes.map, entry.x, entry.y, 1);
    }
    if (task != null) {
      task.onArrival(goes, journey);
    }
  }
  
  
  public City homeCity() {
    return homeCity;
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
    if (map == null || ! map.settings.toggleInjury) return;
    injury += damage;
    checkHealthState();
  }
  
  
  void setAsKilled(String cause) {
    state = STATE_DEAD;
    if (map != null) exitMap(map);
    setDestroyed();
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
  
  
  boolean man() {
    return false;
  }
  
  
  boolean woman() {
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
  
  
  public String jobDesc() {
    if (task == null) return "Idle";
    return task.toString();
  }
}



