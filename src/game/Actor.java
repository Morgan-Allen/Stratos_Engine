

package game;
import gameUI.play.*;
import graphics.common.*;
import graphics.sfx.*;
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
  
  private Building work;
  private Building home;
  private City     homeCity;
  private City     guestCity;
  Building recruiter;
  Mission  mission  ;
  
  private Task task;
  private Pathing inside;
  private int moveMode = MOVE_NORMAL;
  private Vec3D position = new Vec3D();
  
  Tally <Good> carried = new Tally();
  
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
    mission   = (Mission  ) s.loadObject();
    
    task   = (Task   ) s.loadObject();
    inside = (Pathing) s.loadObject();
    moveMode = s.loadInt();
    position.loadFrom(s.input());
    
    s.loadTally(carried);
    
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
    s.saveObject(mission  );
    
    s.saveObject(task  );
    s.saveObject(inside);
    s.saveInt(moveMode);
    position.saveTo(s.output());
    
    s.saveTally(carried);
    
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
  public void enterMap(CityMap map, int x, int y, float buildLevel, City owns) {
    if (onMap()) {
      I.complain("\nALREADY ON MAP: "+this);
      return;
    }
    if (owns == null) {
      I.complain("\nCANNOT ASSIGN NULL OWNER! "+this);
      return;
    }
    
    super.enterMap(map, x, y, buildLevel, owns);
    map.actors.add(this);
    assignHomeCity(owns);
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
    if (mission != null) mission.toggleRecruit(this, false);
    if (home      != null) home.setResident(this, false);
    if (work      != null) work.setWorker  (this, false);
    if (task      != null) task.onCancel();
    home      = null;
    work      = null;
    recruiter = null;
    mission = null;
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
  
  
  public Building home() {
    return home;
  }
  
  
  public Building work() {
    return work;
  }
  
  
  void setHome(Building home) {
    this.home = home;
  }
  
  
  void setWork(Building work) {
    this.work = work;
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
      if (mission != null) mission.actorUpdates(this);
      if (task == null || ! task.checkAndUpdateTask()) {
        beginNextBehaviour();
      }
    }
    //
    //  And update your current vision and health-
    if (onMap()) updateVision();
    if (onMap()) checkHealthState();
    if (onMap()) updateLifeCycle(homeCity(), true);
  }
  
  
  void updateOffMap(City city) {
    updateLifeCycle(city, false);
  }
  
  
  void beginNextBehaviour() {
    assignTask(null);
    assignTask(wanderTask());
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
  
  
  public Mission formation() {
    return mission;
  }
  
  
  public Pathing[] currentPath() {
    if (task == null || task.path == null) return new Pathing[0];
    return task.path;
  }
  
  
  public Task visitTask(
    Building goes, int maxTime, JOB jobType, Employer e
  ) {
    if (goes == null) return null;
    if (reports()) I.say(this+" will visit "+goes+" for time "+maxTime);
    
    Task t = new Task(this);
    return t.configTask(e, goes, null, jobType, maxTime);
  }
  
  
  public Task targetTask(
    Target goes, int maxTime, JOB jobType, Employer e
  ) {
    if (goes == null) return null;
    if (reports()) I.say(this+" will target "+goes+" for time "+maxTime);
    
    Task t = new Task(this);
    return t.configTask(e, null, goes, jobType, maxTime);
  }
  
  
  public Task wanderTask() {
    if (reports()) I.say(this+" beginning random walk...");
    
    Task t = new TaskWander(this);
    return t.configTask(null, null, null, JOB.WANDERING, 0);
  }
  
  
  public Task restingTask(Building rests) {
    if (rests == null) return null;
    if (reports()) I.say(this+" will rest at "+rests);
    
    Task t = new Task(this);
    return t.configTask(rests, rests, null, JOB.RESTING, -1);
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
  
  
  public void offloadGood(Good good, Building store) {
    float amount = carried.valueFor(good);
    if (store == null || amount == 0) return;
    
    if (reports()) I.say(this+" Depositing "+carried+" at "+store);
    
    store.addInventory(amount, good);
    carried.set(good, 0);
  }
  
  
  public void incCarried(Good good, float amount) {
    float newAmount = carried.valueFor(good) + amount;
    if (newAmount < 0) newAmount = 0;
    carried.set(good, newAmount);
  }
  
  
  public void setCarried(Good good, float amount) {
    carried.set(good, amount);
  }
  
  
  public void clearCarried() {
    carried.clear();
  }
  
  
  public void assignCargo(Tally <Good> cargo) {
    clearCarried();
    carried.add(cargo);
  }
  
  
  public Tally <Good> carried() {
    return carried;
  }
  
  
  public float carried(Good g) {
    return carried.valueFor(g);
  }
  
  
  public void onArrival(City goes, World.Journey journey) {
    if (goes.activeMap() != null) {
      Tile entry = CityBorders.findTransitPoint(
        goes.activeMap(), goes, journey.from
      );
      enterMap(goes.activeMap(), entry.x, entry.y, 1, homeCity);
    }
    if (task != null) {
      task.onArrival(goes, journey);
    }
  }
  
  
  public City homeCity() {
    return homeCity;
  }
  
  
  public City guestCity() {
    return guestCity;
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
      hits = Rand.num() < hitChance;
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
    if (map == null || ! map.world.settings.toggleInjury) return;
    injury += damage;
    injury = Nums.clamp(injury, 0, maxHealth());
    checkHealthState();
  }
  
  
  public void liftDamage(float damage) {
    takeDamage(0 - damage);
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
  
  
  public float maxHealth () { return type().maxHealth ; }
  public float sightRange() { return type().sightRange; }
  public float injury () { return injury ; }
  public float fatigue() { return fatigue; }
  public boolean alive() { return state != STATE_DEAD; }
  public boolean dead () { return state == STATE_DEAD; }
  
  
  
  /**  Stub methods related to skills, XP and bonding:
    */
  public void gainXP(Trait trait, float XP) {
    return;
  }
  
  
  public Series <Trait> allTraits() {
    return new List();
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
  
  
  public String fullName() {
    return toString();
  }
  
  
  public String toString() {
    City player = PlayUI.playerBase();
    String from = "";
    if (map != null && player != null && homeCity != null) {
      City.POSTURE p = player.posture(homeCity);
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
  

  public boolean testSelection(PlayUI UI, City base, Viewport port) {
    if (indoors()) return false;
    return super.testSelection(UI, base, port);
  }
  
  
  public boolean setSelected(PlayUI UI) {
    UI.setDetailPane(new ActorPane (UI, this));
    UI.setOptionList(new OptionList(UI, this));
    return true;
  }
  

  public boolean canRender(City base, Viewport view) {
    if (indoors()) return false;
    return super.canRender(base, view);
  }
  
  
  public void renderElement(Rendering rendering, City base) {
    Sprite s = sprite();
    if (s == null) return;
    
    Tile from = at(), goes = from;
    Pathing next = task == null ? null : task.nextOnPath();
    if (next != null && next.isTile()) goes = (Tile) next;
    
    //  TODO:  Factor this out below!
    float alpha = Rendering.frameAlpha(), rem = 1 - alpha;
    s.position.set(
      (from.x * rem) + (goes.x * alpha) + 0.5f,
      (from.y * rem) + (goes.y * alpha) + 0.5f,
    0);
    if (goes != from) {
      float angle = new Vec2D(goes.x - from.x, goes.y - from.y).toAngle();
      s.rotation = angle;
    }
    
    if (task != null && task.inContact()) {
      float animProg = Rendering.activeTime() % 1;
      //final float animProg = progress + ((nextProg - progress) * frameAlpha);
      s.setAnimation(task.animName(), animProg, true);
    }
    else {
      s.setAnimation(AnimNames.MOVE, Rendering.activeTime() % 1, true);
    }
    super.renderElement(rendering, base);
    
    
    //  Add a health-bar for general display
    Healthbar h = new Healthbar();
    h.fog = s.fog;
    h.colour = Colour.BLUE;
    h.size = 30;
    h.hurtLevel = injury () / type().maxHealth;
    h.tireLevel = fatigue() / type().maxHealth;
    h.position.setTo(s.position);
    h.position.z += type().deep + 0.1f;
    h.readyFor(rendering);
    
    //  TODO:  Add a chat-bar in cases of conversation, to represent how
    //  persuaded/converted/loyal a subject is.
  }
  
  
  public Vec3D trackPosition() {
    if (indoors()) {
      return ((Element) inside).trackPosition();
    }
    else {
      Tile from = at(), goes = from;
      Pathing next = task == null ? null : task.nextOnPath();
      if (next != null && next.isTile()) goes = (Tile) next;
      
      float alpha = Rendering.frameAlpha(), rem = 1 - alpha;
      return new Vec3D(
        (from.x * rem) + (goes.x * alpha) + 0.5f,
        (from.y * rem) + (goes.y * alpha) + 0.5f,
      0);
    }
  }
}










