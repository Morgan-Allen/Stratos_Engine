

package game;
import static game.Task.*;
import static game.GameConstants.*;
import gameUI.play.*;
import graphics.common.*;
import graphics.sfx.*;
import test.LogicTest;
import util.*;



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
  
  private Employer work;
  private Building home;
  private Base     guestBase;
  private Mission  mission;
  
  private Task task;
  private Task reaction;
  private float fearLevel;
  private List <Actor> backup = new List();
  
  private Pathing inside;
  private boolean wasIndoors = true;
  private Vec3D lastPosition  = new Vec3D();
  private Vec3D exactPosition = new Vec3D();
  
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
    guestBase = (Base    ) s.loadObject();
    mission   = (Mission ) s.loadObject();
    
    task      = (Task) s.loadObject();
    reaction  = (Task) s.loadObject();
    fearLevel = s.loadFloat();
    s.loadObjects(backup);
    
    inside = (Pathing) s.loadObject();
    wasIndoors = s.loadBool();
    lastPosition .loadFrom(s.input());
    exactPosition.loadFrom(s.input());
    
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
    s.saveObject(guestBase);
    s.saveObject(mission  );
    
    s.saveObject(task     );
    s.saveObject(reaction );
    s.saveFloat (fearLevel);
    s.saveObjects(backup);
    
    s.saveObject(inside);
    s.saveBool(wasIndoors);
    lastPosition .saveTo(s.output());
    exactPosition.saveTo(s.output());
    
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
  public void enterMap(Area map, int x, int y, float buildLevel, Base owns) {
    super.enterMap(map, x, y, buildLevel, owns);
    
    map.actors.add(this);
    if (type().isVessel()) map.vessels.add(this);
  }
  
  
  public void exitMap(Area map) {
    if (inside != null) setInside(inside, false);
    
    map.actors.remove(this);
    if (type().isVessel()) map.vessels.remove(this);
    
    super.exitMap(map);
  }
  
  
  public void setDestroyed() {
    super.setDestroyed();
    if (mission != null) mission.toggleRecruit(this, false);
    if (home    != null) home.setResident(this, false);
    if (work    != null) work.setWorker(this, false);
    if (task    != null) task.onCancel();
    assignTask(null);
  }
  
  
  public void assignGuestBase(Base city) {
    this.guestBase = city;
  }
  
  
  public boolean onMap(Area map) {
    return map != null && map == this.map;
  }
  
  
  public Building home() {
    return home;
  }
  
  
  public Employer work() {
    return work;
  }
  
  
  void setHome(Building home) {
    this.home = home;
  }
  
  
  void setWork(Employer work) {
    this.work = work;
  }
  
  
  
  /**  Regular updates-
    */
  void update() {
    //
    //  Obtain some basic settings first-
    WorldSettings settings = map.world.settings;
    boolean organic = type().organic && ! type().isVessel();
    float tick = 1f / map().ticksPS;
    //
    //  Adjust health-parameters accordingly-
    if (organic) {
      hunger += settings.toggleHunger ? (tick / STARVE_INTERVAL) : 0;
      if (jobType() == JOB.RESTING && task().inContact()) {
        float rests = tick / FATIGUE_REGEN;
        float heals = tick / HEALTH_REGEN ;
        fatigue = Nums.max(0, fatigue - rests);
        injury  = Nums.max(0, injury  - heals);
      }
      else {
        fatigue += settings.toggleFatigue ? (tick / FATIGUE_INTERVAL) : 0;
        float heals = tick * 0.5f / HEALTH_REGEN;
        injury = Nums.max(0, injury - heals);
      }
    }
    else {
      float rests = tick / FATIGUE_REGEN;
      fatigue = Nums.max(0, fatigue - rests);
    }
    //
    //  Some checks to assist in case of blockage, and refreshing position-
    lastPosition.setTo(exactPosition);
    wasIndoors = indoors();
    boolean trapped = false;
    trapped |= inside == null && ! map.pathCache.hasGroundAccess(at());
    trapped |= inside != null && Visit.empty(((Building) inside).entrances());
    if (trapped) {
      AreaTile free = map.pathCache.mostOpenNeighbour(at());
      if (free == null) free = AreaTile.nearestOpenTile(at(), map, 6);
      if (free != null) {
        setInside(inside, false);
        setLocation(free, map);
      }
    }
    //
    //  Update fear-level:
    fearLevel = updateFearLevel();
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
  
  
  float updateFearLevel() {
    return 0;
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
  
  
  public void setLocation(AreaTile at, Area map) {
    if (! onMap()) {
      super.setLocation(at, map);
      return;
    }
    AreaTile old = this.at();
    super.setLocation(at, map);
    
    if (at != null) {
      float height = at.elevation;
      Element above = at.above;
      if (inside == null && above != null && above.pathType() == Type.PATH_WALLS) {
        height += above.height();
      }
      exactPosition.set(at.x + 0.5f, at.y + 0.5f, height);
    }
    
    map.flagActive(this, old, false);
    map.flagActive(this, at , true );
  }
  
  
  public void setExactLocation(Vec3D location, Area map) {
    AreaTile goes = map.tileAt(location.x, location.y);
    setLocation(goes, map);
    
    float height = exactPosition.z;
    exactPosition.set(location.x, location.y, height);
  }
  
  
  public Vec3D exactPosition(Vec3D store) {
    if (store == null) store = new Vec3D();
    return store.setTo(exactPosition);
  }
  
  
  protected void onVisit(Pathing visits) {
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
  
  
  public float fearLevel() {
    return fearLevel;
  }
  
  
  public List <Actor> backup() {
    return backup;
  }
  
  
  void setMission(Mission m) {
    //  NOTE:  This method should only be called by the Mission class, so I'm
    //  not making it public...
    this.mission = m;
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
    return Task.mainTaskFocus(this);
  }
  
  
  public boolean idle() {
    return task == null;
  }
  
  
  
  /**  Methods to assist trade and migration-
    */
  public void pickupGood(Good carried, float amount, Carrier store) {
    if (store == null || carried == null || amount <= 0) return;
    
    store.inventory().add(0 - amount, carried);
    incCarried(carried, amount);
  }
  
  
  public void offloadGood(Good good, Carrier store) {
    float amount = carried.valueFor(good);
    if (store == null || amount == 0) return;
    
    if (reports()) I.say(this+" Depositing "+carried+" at "+store);
    
    store.inventory().add(amount, good);
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
  
  
  public float shopPrice(Good good, Task purchase) {
    return good.price;
  }
  
  
  public void onArrival(Base goes, World.Journey journey) {
    if (goes.activeMap() != null) {
      AreaTile entry = ActorUtils.findTransitPoint(
        goes.activeMap(), goes, journey.from
      );
      enterMap(goes.activeMap(), entry.x, entry.y, 1, base());
    }
    if (task != null) {
      task.onArrival(goes, journey);
    }
  }
  
  
  public boolean isElement() {
    return true;
  }
  
  
  public Base guestBase() {
    return guestBase;
  }
  
  
  
  /**  Combat and survival-related code:
    */
  public void performAttack(Element other, boolean melee) {
    TaskCombat.performAttack(this, other, melee);
  }
  
  
  public boolean armed() {
    return Nums.max(type().meleeDamage, type().rangeDamage) > 0;
  }
  
  
  public void liftDamage(float damage) {
    takeDamage(0 - damage);
  }
  
  
  public void liftFatigue(float tire) {
    takeFatigue(0 - tire);
  }
  
  
  public void takeDamage(float damage) {
    if (map == null || ! map.world.settings.toggleInjury) return;
    injury += damage;
    injury = Nums.clamp(injury, 0, maxHealth() + 1);
    checkHealthState();
  }
  
  
  public void takeFatigue(float tire) {
    if (map == null || ! map.world.settings.toggleFatigue) return;
    fatigue += tire;
    fatigue = Nums.clamp(tire, 0, maxHealth() + 1);
    checkHealthState();
  }
  
  
  public void setAsKilled(String cause) {
    state = STATE_DEAD;
    if (map != null) exitMap(map);
    setDestroyed();
  }
  
  
  public void setCooldown(float cool) {
    this.cooldown = cool;
  }
  
  
  public void liftHunger(float inc) {
    this.hunger = Nums.clamp(hunger - inc, 0, maxHealth());
  }
  
  
  public void setHungerLevel(float level) {
    this.hunger = maxHealth() * level;
  }
  
  
  void updateCooldown() {
    this.cooldown = Nums.max(0, cooldown - 1);
  }
  
  
  void updateVision() {
    if (indoors()) return;
    
    if (base() != map.locals) {
      AreaFog fog = map.fogMap(base(), true);
      float range = sightRange();
      if (fog != null) fog.liftFog(at(), range);
    }
  }
  
  
  void checkHealthState() {
    if (injury + hunger > type().maxHealth && state != STATE_DEAD) {
      setAsKilled("Injury: "+injury+" Hunger "+hunger);
    }
  }
  
  
  public float sightRange() {
    float light = map == null ? 0.5f : map.lightLevel();
    return type().sightRange * (0.75f + (light * 0.5f));
  }
  
  
  public float moveSpeed() {
    int mode = task == null ? MOVE_NORMAL : task.motionMode();
    float speed = type().moveSpeed * 1f / AVG_MOVE_SPEED;
    if (mode == MOVE_RUN  ) speed *= RUN_MOVE_SPEED  / 100f;
    if (mode == MOVE_SNEAK) speed *= HIDE_MOVE_SPEED / 100f;
    return speed;
  }
  
  
  public float hungerLevel() {
    return hunger / maxHealth();
  }
  
  
  public float maxHealth () { return type().maxHealth ; }
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
    if (player == null) player = LogicTest.currentCity();
    
    String from = "";
    if (map != null && player != null && base() != null) {
      Base.POSTURE p = player.posture(base());
      if (p == Base.POSTURE.ENEMY ) from = " (E)";
      if (p == Base.POSTURE.ALLY  ) from = " (A)";
      if (p == Base.POSTURE.VASSAL) from = " (V)";
      if (p == Base.POSTURE.LORD  ) from = " (L)";
    }
    return type().name+" "+ID+from;
  }
  
  
  public String jobDesc() {
    if (task == null) return "Idle";
    return task.type.name();
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
    if (indoors() || wasIndoors) return false;
    return super.canRender(base, view);
  }
  
  
  public Vec3D renderedPosition(Vec3D store) {
    if (store != null) store.setTo(trackPosition());
    return trackPosition();
  }
  
  
  public void renderElement(Rendering rendering, Base base) {
    Sprite s = sprite();
    if (s == null) return;

    Vec3D from = lastPosition, goes = exactPosition;
    float alpha = Rendering.frameAlpha(), rem = 1 - alpha;
    s.position.set(
      (from.x * rem) + (goes.x * alpha),
      (from.y * rem) + (goes.y * alpha),
      0
    );
    
    boolean contact = task != null && task.inContact();
    if (contact) goes = task.faceTarget().exactPosition(null);
    Vec2D angleVec = new Vec2D(goes.x - from.x, goes.y - from.y);
    if (angleVec.length() > 0) s.rotation = angleVec.toAngle();
    
    final float ANIM_MULT = 1.66f;
    float animProg = Rendering.activeTime() * ANIM_MULT;
    
    if (contact) {
      s.setAnimation(task.animName(), animProg % 1, true);
    }
    else {
      animProg *= moveSpeed();
      int mode = task == null ? MOVE_NORMAL : task.motionMode();
      String animName = AnimNames.MOVE;
      if (mode == MOVE_RUN  ) animName = AnimNames.MOVE_FAST;
      if (mode == MOVE_SNEAK) animName = AnimNames.MOVE_SNEAK;
      s.setAnimation(animName, animProg % 1, true);
    }
    super.renderElement(rendering, base);
    
    
    //  Add a health-bar for general display
    Healthbar h = new Healthbar();
    h.fog = s.fog;
    h.colour = Colour.BLUE;
    h.size = 30;
    h.hurtLevel = (injury() + hunger()) / maxHealth();
    h.tireLevel = fatigue() / maxHealth();
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
      Vec3D s = new Vec3D();
      Vec3D from = lastPosition, goes = exactPosition;
      float alpha = Rendering.frameAlpha(), rem = 1 - alpha;
      s.set(
        (from.x * rem) + (goes.x * alpha),
        (from.y * rem) + (goes.y * alpha),
        0
      );
      return s;
    }
  }
}










