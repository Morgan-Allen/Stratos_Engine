

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
    
    MOVE_NORMAL  = 1,
    MOVE_RUN     = 2,
    MOVE_CLIMB   = 3,
    MOVE_SNEAK   = 4,
    MOVE_SWIM    = 5
  ;
  static int nextID = 0;
  int dirs[] = new int[4];
  
  
  private String ID;
  
  Building  work;
  Building  home;
  City      homeCity;
  City      guestCity;
  Building  recruiter;
  Formation formation;
  
  private Task task;
  private Pathing inside;
  private int moveMode = MOVE_NORMAL;
  private Vec3D position = new Vec3D();
  
  private Object carried = null;
  private float carryAmount = 0;
  private Tally <Good> cargo = null;
  
  int sexData    = -1;
  int ageSeconds =  0;
  int pregnancy  =  0;
  float injury ;
  float hunger ;
  float fatigue;
  float stress ;
  int   state = STATE_OKAY;
  
  
  
  public Actor(Type type) {
    super(type);
    this.ID = "#"+nextID++;
  }
  
  
  public Actor(Session s) throws Exception {
    super(s);
    
    ID = s.loadString();
    
    work      = (Building ) s.loadObject();
    home      = (Building ) s.loadObject();
    homeCity  = (City     ) s.loadObject();
    guestCity = (City     ) s.loadObject();
    recruiter = (Building ) s.loadObject();
    formation = (Formation) s.loadObject();
    
    task   = (Task   ) s.loadObject();
    inside = (Pathing) s.loadObject();
    moveMode = s.loadInt();
    position.loadFrom(s.input());
    
    carried     = s.loadObject();
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
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveString(ID);
    
    s.saveObject(work     );
    s.saveObject(home     );
    s.saveObject(homeCity );
    s.saveObject(guestCity);
    s.saveObject(recruiter);
    s.saveObject(formation);
    
    s.saveObject(task  );
    s.saveObject(inside);
    s.saveInt(moveMode);
    position.saveTo(s.output());
    
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
  }
  
  
  
  /**  World entry and exit-
    */
  public void enterMap(CityMap map, int x, int y, float buildLevel) {
    if (onMap()) {
      I.complain("\nALREADY ON MAP: "+this);
      return;
    }
    super.enterMap(map, x, y, buildLevel);
    map.actors.add(this);
  }
  
  
  public void exitMap(CityMap map) {
    if (inside != null) setInside(inside, false);
    map.actors.remove(this);
    
    if (map.actors.includes(this)) {
      I.complain("\nMap still contains "+this);
    }
    super.exitMap(map);
  }
  
  
  public void setDestroyed() {
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
  
  
  public void assignHomeCity(City city) {
    this.homeCity = city;
  }
  
  
  public void assignGuestCity(City city) {
    this.guestCity = city;
  }
  
  
  public boolean onMap(CityMap map) {
    return map != null && map == this.map;
  }
  
  
  
  /**  Regular updates-
    */
  void update() {
    //
    //  Some checks to assist in case of blockage...
    Tile at = at();
    if (inside == null && at != null && ! map.pathCache.hasGroundAccess(at)) {
      if (at.above != null && at.above.type().isBuilding()) {
        setInside((Building) at.above, true);
      }
      else {
        Tile free = map.pathCache.mostOpenNeighbour(at);
        if (free != null) setLocation(free, map);
      }
    }
    //
    //  Task updates-
    if (onMap()) {
      if (home      != null) home     .actorUpdates(this);
      if (work      != null) work     .actorUpdates(this);
      if (formation != null) formation.actorUpdates(this);
      if (task == null || ! task.checkAndUpdateTask()) {
        beginNextBehaviour();
      }
    }
    //
    //  And update your current vision and health-
    if (onMap()) updateVision();
    if (onMap()) checkHealthState();
    if (onMap()) updateLifeCycle(map.city, true);
  }
  
  
  void updateOffMap(City city) {
    updateLifeCycle(city, false);
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
  
  
  public void setLocation(Tile at, CityMap map) {
    if (! onMap()) {
      super.setLocation(at, map);
      return;
    }
    Tile old = this.at();
    super.setLocation(at, map);
    
    if (at != null) {
      float height = at.elevation;
      if (inside == null && at.above != null) height += at.above.type().deep;
      position.set(at.x, at.y, height);
    }
    
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
  public void assignTask(Task task) {
    if (this.task != null) this.task.toggleFocus(false);
    this.task = task;
    if (this.task != null) this.task.toggleFocus(true );
  }
  
  
  public Task task() {
    return task;
  }
  
  
  public Pathing[] currentPath() {
    if (task == null || task.path == null) return new Pathing[0];
    return task.path;
  }
  
  
  public Task embarkOnVisit(
    Building goes, int maxTime, JOB jobType, Employer e
  ) {
    if (goes == null) return null;
    if (reports()) I.say(this+" will visit "+goes+" for time "+maxTime);
    
    Task t = new Task(this);
    assignTask(t.configTask(e, goes, null, jobType, maxTime));
    return task;
  }
  
  
  public Task embarkOnTarget(
    Target goes, int maxTime, JOB jobType, Employer e
  ) {
    if (goes == null) return null;
    if (reports()) I.say(this+" will target "+goes+" for time "+maxTime);
    
    Task t = new Task(this);
    assignTask(t.configTask(e, null, goes, jobType, maxTime));
    return task;
  }
  
  
  //  TODO:  Start moving these all out into dedicated sub-tasks.
  
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
  public void pickupGood(Good carried, float amount, Building store) {
    if (store == null || carried == null || amount <= 0) return;
    
    store.addInventory(0 - amount, carried);
    incCarried(carried, amount);
  }
  
  
  public void offloadGood(Good carried, Building store) {
    if (store == null || carried != this.carried) return;
    
    if (reports()) I.say(this+" Depositing "+carried+" at "+store);
    
    store.addInventory(carryAmount, carried);
    this.carried = null;
    this.carryAmount = 0;
  }
  
  
  public void incCarried(Good carried, float amount) {
    
    //  TODO:  Add a proper warning system for this?
    /*
    if (this.carried != null && carried != this.carried) {
      I.say("ERASING GOOD: "+carryAmount+" "+carried);
      I.say("???");
    }
    //*/
    
    if (this.carried != carried) this.carryAmount = 0;
    this.carried      = carried;
    this.carryAmount += amount ;
    if (this.carryAmount < 0) this.carryAmount = 0;
  }
  
  
  public void assignCargo(Tally <Good> cargo) {
    this.cargo = cargo;
  }
  
  
  public Tally <Good> cargo() {
    return cargo;
  }
  
  
  public Object carried() {
    return carried;
  }
  
  
  public float carryAmount() {
    return carryAmount;
  }
  
  
  public float carried(Good g) {
    float sum = 0;
    if (this.carried == g) sum += carryAmount;
    if (this.cargo != null) sum += cargo.valueFor(g);
    return sum;
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
  void performAttack(Element other, boolean melee) {
    int damage = melee ? type().meleeDamage : type().rangeDamage;
    int armour = other.type().armourClass;
    if (other == null || damage <= 0) return;
    
    //
    //  TODO:  Move this out into the TaskCombat class.
    Trait   attackSkill = melee ? SKILL_MELEE : SKILL_RANGE;
    Trait   defendSkill = melee ? SKILL_MELEE : SKILL_EVADE;
    boolean wallBonus   = wallBonus(this, other);
    boolean wallPenalty = wallBonus(other, this);
    boolean hits = true;
    float XP = 1;
    
    if (other.type().isActor()) {
      float attackBonus = levelOf(attackSkill) * 2f / MAX_SKILL_LEVEL;
      float defendBonus = levelOf(defendSkill) * 2f / MAX_SKILL_LEVEL;
      if (wallBonus  ) attackBonus += WALL_HIT_BONUS / 100f;
      if (wallPenalty) defendBonus += WALL_DEF_BONUS / 100f;
      
      float hitChance = Nums.clamp(attackBonus + 0.5f - defendBonus, 0, 1);
      hits = Rand.num() > hitChance;
      XP   = (1.5f - hitChance) * FIGHT_XP_PERCENT / 100f;
      
      float otherXP = (0.5f + hitChance) * FIGHT_XP_PERCENT / 100f;
      ((Actor) other).gainXP(defendSkill, otherXP);
    }
    
    if (hits) {
      if (wallBonus  ) damage += WALL_DMG_BONUS;
      if (wallPenalty) armour += WALL_ARM_BONUS;
      damage = Rand.index(damage + armour) + 1;
      damage = Nums.max(0, damage - armour);
      if (damage > 0) other.takeDamage(damage);
    }
    
    gainXP(attackSkill, XP);
  }
  
  
  boolean wallBonus(Element from, Element goes) {
    boolean wallBonus = false;
    wallBonus |= from.at().pathType() == PATH_WALLS;
    wallBonus &= goes.at().pathType() != PATH_WALLS;
    return wallBonus;
  }
  
  
  public void takeDamage(float damage) {
    if (map == null || ! map.city.world.settings.toggleInjury) return;
    injury += damage;
    checkHealthState();
  }
  
  
  public void setAsKilled(String cause) {
    state = STATE_DEAD;
    if (map != null) exitMap(map);
    setDestroyed();
  }
  
  
  void updateVision() {
    return;
  }
  
  
  void checkHealthState() {
    if (injury + hunger > type().maxHealth && state != STATE_DEAD) {
      setAsKilled("Injury: "+injury+" Hunger "+hunger);
    }
  }
  
  
  public boolean alive() {
    return state != STATE_DEAD;
  }
  
  
  public boolean dead() {
    return state == STATE_DEAD;
  }
  
  
  
  /**  Stub methods related to skills, XP and bonding:
    */
  public void gainXP(Trait trait, float XP) {
    return;
  }
  
  
  public float levelOf(Trait trait) {
    return 0;
  }
  
  
  public float bondLevel(Actor with) {
    return 0;
  }
  
  
  public Series <Actor> allBondedWith(int type) {
    return new Batch();
  }
  
  
  
  /**  Aging, reproduction and life-cycle:
    */
  public void setSexData(int sexData) {
    this.sexData    = sexData;
  }
  
  
  public void setAgeYears(float ageYears) {
    this.ageSeconds = (int) (ageYears * YEAR_LENGTH);
  }
  
  
  public float ageYears() {
    float years = ageSeconds / (YEAR_LENGTH * 1f);
    return years;
  }
  
  
  public float growLevel() {
    return Nums.min(1, ageYears() / AVG_MARRIED);
  }
  
  
  public boolean adult() {
    return true;
  }
  
  
  public boolean child() {
    return false;
  }
  
  
  public boolean man() {
    return false;
  }
  
  
  public boolean woman() {
    return false;
  }
  
  
  void updateLifeCycle(City city, boolean onMap) {
    ageSeconds += 1;
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  protected boolean reports() {
    if (I.talkAbout == null) return false;
    return I.talkAbout == home || I.talkAbout == work;
  }
  
  
  public String ID() {
    return ID;
  }
  
  
  public void setID(String newID) {
    this.ID = newID;
  }
  
  
  public String toString() {
    String from = "";
    if (map != null && map.city != null && homeCity != null) {
      City.POSTURE p = map.city.posture(homeCity);
      if (p == City.POSTURE.ENEMY ) from = " (E)";
      if (p == City.POSTURE.ALLY  ) from = " (A)";
      if (p == City.POSTURE.VASSAL) from = " (V)";
      if (p == City.POSTURE.LORD  ) from = " (L)";
    }
    return type().name+" "+ID+from;
  }
  
  
  public String jobDesc() {
    if (task == null) return "Idle";
    return task.toString();
  }
}



