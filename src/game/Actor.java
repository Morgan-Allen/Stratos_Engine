

package game;
import gameUI.play.*;
import graphics.common.*;
import graphics.sfx.*;
import util.*;
import static game.Task.*;
import static game.AreaMap.*;
import static game.GameConstants.*;



public class Actor extends Element implements
  Session.Saveable, Journeys, Active, Carrier
{
  
  
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
  private Base     homeCity;
  private Base     guestCity;
  Building recruiter;
  Mission  mission  ;
  
  private Task task;
  private Task reaction;
  
  private Pathing inside;
  private int moveMode = MOVE_NORMAL;
  private Vec3D position = new Vec3D();
  
  Tally <Good> carried = new Tally();
  
  int sexData    = -1;
  int ageSeconds =  0;
  int pregnancy  =  0;
  float injury  ;
  float hunger  ;
  float fatigue ;
  float stress  ;
  float cooldown;
  int   state = STATE_OKAY;
  
  
  
  public Actor(ActorType type) {
    super(type);
    this.ID = "#"+nextID++;
  }
  
  
  public Actor(Session s) throws Exception {
    super(s);
    
    ID = s.loadString();
    
    work      = (Building) s.loadObject();
    home      = (Building) s.loadObject();
    homeCity  = (Base    ) s.loadObject();
    guestCity = (Base    ) s.loadObject();
    recruiter = (Building) s.loadObject();
    mission   = (Mission ) s.loadObject();
    
    task     = (Task) s.loadObject();
    reaction = (Task) s.loadObject();
    
    inside = (Pathing) s.loadObject();
    moveMode = s.loadInt();
    position.loadFrom(s.input());
    
    s.loadTally(carried);
    
    sexData    = s.loadInt();
    ageSeconds = s.loadInt();
    pregnancy  = s.loadInt();
    
    injury   = s.loadFloat();
    hunger   = s.loadFloat();
    fatigue  = s.loadFloat();
    stress   = s.loadFloat();
    cooldown = s.loadFloat();
    state    = s.loadInt();
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
    
    s.saveObject(task    );
    s.saveObject(reaction);
    
    s.saveObject(inside);
    s.saveInt(moveMode);
    position.saveTo(s.output());
    
    s.saveTally(carried);
    
    s.saveInt(sexData   );
    s.saveInt(ageSeconds);
    s.saveInt(pregnancy );
    
    s.saveFloat(injury  );
    s.saveFloat(hunger  );
    s.saveFloat(fatigue );
    s.saveFloat(stress  );
    s.saveFloat(cooldown);
    s.saveInt  (state   );
  }
  
  
  public ActorType type() {
    return (ActorType) super.type();
  }
  
  
  public boolean isActor() {
    return true;
  }
  
  
  
  /**  World entry and exit-
    */
  public void enterMap(AreaMap map, int x, int y, float buildLevel, Base owns) {
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
  
  
  public void exitMap(AreaMap map) {
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
  
  
  public void assignHomeCity(Base city) {
    this.homeCity = city;
  }
  
  
  public void assignGuestCity(Base city) {
    this.guestCity = city;
  }
  
  
  public boolean onMap(AreaMap map) {
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
    if (home    != null && onMap()) home   .actorUpdates(this);
    if (work    != null && onMap()) work   .actorUpdates(this);
    if (mission != null && onMap()) mission.actorUpdates(this);
    if (onMap() && reaction != null && reaction.checkAndUpdateTask()) {
      assignReaction(null);
    }
    else if (onMap() && (task == null || ! task.checkAndUpdateTask())) {
      beginNextBehaviour();
    }
    if (onMap()) updateReactions();
    //
    //  And update your current vision and health-
    if (onMap()) updateCooldown();
    if (onMap()) updateVision();
    if (onMap()) checkHealthState();
    if (onMap()) updateLifeCycle(base(), true);
  }
  
  
  void updateOffMap(Base city) {
    updateLifeCycle(city, false);
  }
  
  
  void beginNextBehaviour() {
    assignTask(null);
    assignTask(TaskWander.configWandering(this));
  }
  
  
  void updateReactions() {
    return;
  }
  
  
  
  /**  Pathing and visitation utilities:
    */
  public void setInside(Pathing b, boolean yes) {
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
  
  
  public void setLocation(Tile at, AreaMap map) {
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
    
    map.flagActive(this, old, false);
    map.flagActive(this, at , true );
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
    if (this.task != null) this.task.toggleFocus(true);
  }
  
  
  public void assignReaction(Task reaction) {
    if (this.reaction != null) this.reaction.toggleFocus(false);
    this.reaction = reaction;
    if (this.reaction != null) this.reaction.toggleFocus(true);
  }
  
  
  public Task task() {
    return task;
  }
  
  
  public Task reaction() {
    return reaction;
  }
  
  
  public Mission mission() {
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
  
  
  public JOB jobType() {
    if (task == null) return JOB.NONE;
    return task.type;
  }
  
  
  public float jobPriority() {
    if (task == null) return Task.NO_PRIORITY;
    return task.priority();
  }
  
  
  public Target jobFocus() {
    return Task.focusTarget(task);
  }
  
  
  public boolean idle() {
    return task == null;
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
    if (cargo == null || cargo == this.carried) return;
    clearCarried();
    carried.add(cargo);
  }
  
  
  public Tally <Good> carried() {
    return carried;
  }
  
  
  public float carried(Good g) {
    return carried.valueFor(g);
  }
  
  
  public Tally <Good> inventory() {
    return carried;
  }
  
  
  public void onArrival(Base goes, World.Journey journey) {
    if (goes.activeMap() != null) {
      Tile entry = ActorUtils.findTransitPoint(
        goes.activeMap(), goes, journey.from
      );
      enterMap(goes.activeMap(), entry.x, entry.y, 1, homeCity);
    }
    if (task != null) {
      task.onArrival(goes, journey);
    }
  }
  
  
  public Base base() {
    return homeCity;
  }
  
  
  public Base guestBase() {
    return guestCity;
  }
  
  
  
  /**  Combat and survival-related code:
    */
  public void performAttack(Element other, boolean melee) {
    TaskCombat.performAttack(this, other, melee);
  }
  
  
  public boolean armed() {
    return Nums.max(type().meleeDamage, type().rangeDamage) > 0;
  }
  
  
  public void takeDamage(float damage) {
    if (map == null || ! map.world.settings.toggleInjury) return;
    injury += damage;
    injury = Nums.clamp(injury, 0, maxHealth() + 1);
    checkHealthState();
  }
  
  
  public void liftDamage(float damage) {
    takeDamage(0 - damage);
  }
  
  
  public void takeFatigue(float tire) {
    if (map == null || ! map.world.settings.toggleFatigue) return;
    fatigue += tire;
    fatigue = Nums.clamp(tire, 0, maxHealth() + 1);
    checkHealthState();
  }
  
  
  public void liftFatigue(float tire) {
    takeFatigue(0 - tire);
  }
  
  
  public void setAsKilled(String cause) {
    
    //I.say("  "+this+" killed, cause: "+cause);
    //I.say("    Home: "+home+", work: "+work);
    
    state = STATE_DEAD;
    if (map != null) exitMap(map);
    setDestroyed();
  }
  
  
  public void setCooldown(float cool) {
    this.cooldown = cool;
  }
  
  
  public void setHungerLevel(float level) {
    this.hunger = maxHealth() * level;
  }
  
  
  void updateCooldown() {
    this.cooldown = Nums.max(0, cooldown - 1);
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
  public float injury  () { return injury  ; }
  public float fatigue () { return fatigue ; }
  public float cooldown() { return cooldown; }
  public float hunger  () { return hunger  ; }
  public boolean alive() { return state != STATE_DEAD; }
  public boolean dead () { return state == STATE_DEAD; }
  
  
  
  /**  Stub methods related to skills, XP and bonding:
    */
  public int classLevel() {
    return 1;
  }
  
  
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
  
  
  void updateLifeCycle(Base city, boolean onMap) {
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
    
    Base player = PlayUI.playerBase();
    if (player == null) player = Test.currentCity();
    
    String from = "";
    if (map != null && player != null && homeCity != null) {
      Base.POSTURE p = player.posture(homeCity);
      if (p == Base.POSTURE.ENEMY ) from = " (E)";
      if (p == Base.POSTURE.ALLY  ) from = " (A)";
      if (p == Base.POSTURE.VASSAL) from = " (V)";
      if (p == Base.POSTURE.LORD  ) from = " (L)";
    }
    return type().name+" "+ID+from;
  }
  
  
  public String jobDesc() {
    if (task == null) return "Idle";
    return task.toString();
  }
  

  public boolean testSelection(PlayUI UI, Base base, Viewport port) {
    if (indoors()) return false;
    return super.testSelection(UI, base, port);
  }
  
  
  public boolean setSelected(PlayUI UI) {
    UI.setDetailPane(new ActorPane (UI, this));
    return true;
  }
  

  public boolean canRender(Base base, Viewport view) {
    if (indoors()) return false;
    return super.canRender(base, view);
  }
  
  
  public void renderElement(Rendering rendering, Base base) {
    Sprite s = sprite();
    if (s == null) return;
    
    Tile from = at(), goes = from;
    Target next = task == null ? null : task.nextOnPath();
    if (next != null && next.isTile()) goes = (Tile) next;
    if (from == next && task != null ) next = task.faceTarget();
    
    //  TODO:  Factor this out below!
    float alpha = Rendering.frameAlpha(), rem = 1 - alpha;
    s.position.set(
      (from.x * rem) + (goes.x * alpha) + 0.5f,
      (from.y * rem) + (goes.y * alpha) + 0.5f,
      0
    );
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










