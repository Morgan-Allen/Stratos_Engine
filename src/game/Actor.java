

package game;
import static game.Task.*;
import static game.GameConstants.*;
import gameUI.play.*;
import graphics.common.*;
import graphics.sfx.*;
import graphics.widgets.Composite;
import test.LogicTest;
import util.*;



public class Actor extends Element implements
  Session.Saveable, Journeys, Active, Carrier
{
  
  /**  Data fields and setup/initialisation-
    */
  final public static int
    MOVE_NORMAL  = 1,
    MOVE_RUN     = 2,
    MOVE_CLIMB   = 3,
    MOVE_SNEAK   = 4,
    MOVE_SWIM    = 5
  ;
  
  static int nextID = 0;
  int dirs[] = new int[4];
  
  
  private String ID = "";
  private String customName = "";
  
  private Employer work;
  private Building home;
  private Mission  mission;
  private Base     offmap;
  
  private Task task;
  private Task reaction;
  
  private Series <Active> seen = new List();
  private Series <Actor> considered = new List();
  
  private Pathing haven;
  private float fearLevel;
  private List <Active> backup = new List();
  
  private Pathing inside;
  private boolean wasIndoors = true;
  private Vec3D lastPosition  = new Vec3D();
  private Vec3D exactPosition = new Vec3D();
  private Actor attached = null, attachTo = null;
  private float hideRoll = -1;
  
  final public ActorHealth health = initHealth();
  final public ActorTraits traits = initTraits();
  final public ActorBonds  bonds  = initBonds ();
  final public ActorOutfit outfit = initOutfit();
  
  ActorHealth initHealth() { return new ActorHealth(this); }
  ActorTraits initTraits() { return new ActorTraits(this); }
  ActorBonds  initBonds () { return new ActorBonds (this); }
  ActorOutfit initOutfit() { return new ActorOutfit(this); }
  
  
  
  
  public Actor(ActorType type) {
    super(type);
    this.ID = "#"+nextID++;
  }
  
  
  public Actor(Session s) throws Exception {
    super(s);
    
    ID = s.loadString();
    customName = s.loadString();
    
    work      = (Employer) s.loadObject();
    home      = (Building) s.loadObject();
    mission   = (Mission ) s.loadObject();
    offmap    = (Base    ) s.loadObject();
    
    task      = (Task) s.loadObject();
    reaction  = (Task) s.loadObject();
    
    s.loadObjects(seen);
    s.loadObjects(considered);
    
    fearLevel = s.loadFloat();
    s.loadObjects(backup);
    
    inside = (Pathing) s.loadObject();
    wasIndoors = s.loadBool();
    lastPosition .loadFrom(s.input());
    exactPosition.loadFrom(s.input());
    
    attached = (Actor) s.loadObject();
    attachTo = (Actor) s.loadObject();
    hideRoll = s.loadFloat();
    
    health.loadState(s);
    traits.loadState(s);
    bonds .loadState(s);
    outfit.loadState(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveString(ID);
    s.saveString(customName);
    
    s.saveObject(work   );
    s.saveObject(home   );
    s.saveObject(mission);
    s.saveObject(offmap );
    
    s.saveObject(task    );
    s.saveObject(reaction);
    
    s.saveObjects(seen);
    s.saveObjects(considered);
    s.saveFloat(fearLevel);
    s.saveObjects(backup);
    
    s.saveObject(inside);
    s.saveBool(wasIndoors);
    lastPosition .saveTo(s.output());
    exactPosition.saveTo(s.output());
    
    s.saveObject(attached);
    s.saveObject(attachTo);
    s.saveFloat(hideRoll);
    
    health.saveState(s);
    traits.saveState(s);
    bonds .saveState(s);
    outfit.saveState(s);
  }
  
  
  public ActorType type() {
    return (ActorType) super.type();
  }
  
  
  public boolean mobile() {
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
    if (task != null) task.onMapExit();
    
    super.exitMap(map);
  }
  
  
  public void setDestroyed() {
    super.setDestroyed();
    wipeEmployment();
  }
  
  
  public void wipeEmployment() {
    if (mission != null) mission.toggleRecruit(this, false);
    if (home    != null) home.setResident(this, false);
    if (work    != null) work.setWorker(this, false);
    if (task    != null) task.onCancel();
    assignTask(null, this);
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
  
  
  void setOffmap(Base offmap) {
    this.offmap = offmap;
  }
  
  
  public Base offmapBase() {
    return offmap;
  }
  
  
  public Base guestBase() {
    return bonds.guestBase();
  }
  
  
  
  /**  Regular updates-
    */
  void update() {
    
    //
    //  Some checks to assist in case of blockage, and refreshing position-
    lastPosition.setTo(exactPosition);
    wasIndoors = indoors();
    boolean escape = checkPathingEscape();
    if (escape) {
      AreaTile free = map.pathCache.mostOpenNeighbour(at());
      if (free == null) free = AreaTile.nearestOpenTile(at(), map, 6);
      if (free != null) {
        setInside(inside, false);
        setLocation(free, map);
      }
      if (attachTo != null) {
        attachTo.setPassenger(this, false);
      }
    }
    
    if (health.active() && attachTo == null) {
      //
      //  NOTE:  The order of subroutine calls here is somewhat delicate, so
      //  do not tamper without cause...
      //
      //  Update vision, fear-level and reactions:
      if (onMap()) updateVision();
      if (onMap()) fearLevel = updateFearLevel();
      if (onMap()) updateReactions();
      //
      //  Update any ongoing tasks-
      if (home    != null && onMap()) home   .actorUpdates(this);
      if (work    != null && onMap()) work   .actorUpdates(this);
      if (mission != null && onMap()) mission.actorUpdates(this);
      
      if (onMap() && reaction != null && ! reaction.checkAndUpdateTask()) {
        assignReaction(null);
      }
      else if (onMap() && (task == null || ! task.checkAndUpdateTask())) {
        hideRoll = Rand.num();
        updateConsidered();
        beginNextBehaviour();
      }
      
      //
      //  And update anything you're carrying-
      if (attached != null && onMap()) {
        attached.setExactLocation(exactPosition, map, false);
      }
    }
    //
    //  And update health-state and life-cycle-
    if (onMap()) traits.updateTraits();
    if (onMap()) bonds.updateBonds();
    if (onMap()) health.updateHealth(map());
    if (onMap()) health.checkHealthState();
    if (onMap() && health.alive()) health.updateLifeCycle(base(), true);
  }
  
  
  boolean checkPathingEscape() {
    //
    //  TODO:  Use proper collision-checks for this!
    
    if (attachTo != null) {
      return attachTo.checkPathingEscape();
    }
    
    boolean trapped = false;
    trapped |=
      inside == null &&
      type().moveMode != Type.MOVE_AIR &&
      ! map.pathCache.hasGroundAccess(at())
    ;
    trapped |=
      inside != null &&
      inside.mainEntrance() == null &&
      Visit.empty(inside.adjacent(null, map)) &&
      inside.allowsExit(this)
    ;
    return trapped;
  }
  
  
  void updateOffMap(Base city) {
    //  TODO:  Update traits as well.
    health.updateHealthOffmap(city);
    health.updateLifeCycle(city, false);
  }
  
  
  void beginNextBehaviour() {
    assignTask(null, this);
    assignTask(TaskWander.nextWandering(this), this);
  }
  
  
  void updateReactions() {
    return;
  }
  
  
  Pathing updateHaven() {
    return null;
  }
  
  
  float updateFearLevel() {
    return 0;
  }
  
  
  
  /**  Pathing and visitation utilities:
    */
  public void setInside(Pathing b, boolean yes) {
    if (b == null || ((Element) b).destroyed()) {
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
      Element above = at.above;
      float height = 0;
      if (inside == null && above != null && above.pathType() == Type.PATH_WALLS) {
        height += above.height();
      }
      height = Nums.max(height, moveHeight());
      height += at.elevation;
      exactPosition.set(at.x + 0.5f, at.y + 0.5f, height);
    }
    
    map.flagActive(this, old, false);
    map.flagActive(this, at , true );
  }
  
  
  public void setExactLocation(Vec3D location, Area map, boolean jump) {
    
    AreaTile goes = map.tileAt(location.x, location.y);
    setLocation(goes, map);
    
    float height = exactPosition.z;
    exactPosition.set(location.x, location.y, height);
    
    if (jump) {
      lastPosition.setTo(exactPosition);
      wasIndoors = indoors();
    }
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
  
  
  protected float moveHeight() {
    return 0;
  }
  
  
  public Pathing inside() {
    return inside;
  }
  
  
  public boolean indoors() {
    return inside != null;
  }
  
  
  
  /**  Miscellaneous behaviour triggers:
    */
  public void assignTask(Task task, Object source) {
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
  
  
  public List <Active> backup() {
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
    Pathing goes, int maxTime, JOB jobType, Employer e
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
  
  
  public boolean inEmergency() {
    if (task == null) return false;
    return task.emergency();
  }
  
  
  public Target jobFocus() {
    return Task.mainTaskFocus(this);
  }
  
  
  public boolean idle() {
    return task == null;
  }
  
  
  
  /**  Methods to assist with riding and passengers...
    */
  public void setPassenger(Actor p, boolean is) {
    if (is && attached == null) {
      p.attachTo = this;
      attached = p;
    }
    else if (p == attached && ! is) {
      p.attachTo = null;
      attached = null;
    }
    else {
      I.say("\nWARNING: Incorrect passenger parameters!");
    }
  }
  
  
  public Actor passenger() {
    return attached;
  }
  
  
  public boolean isPassenger() {
    return attachTo != null;
  }
  
  
  public Tally <Good> inventory() {
    return outfit.carried();
  }
  
  
  public float shopPrice(Good good, Task purchase) {
    return good.price;
  }
  
  
  
  /**  Handling migration and off-map tasks-
    */
  public void onArrival(Base goes, World.Journey journey) {
    
    if (goes.activeMap() != null && inside() == null) {
      AreaTile entry = ActorUtils.findTransitPoint(
        goes.activeMap(), goes, journey.from, this
      );
      enterMap(goes.activeMap(), entry.x, entry.y, 1, base());
    }
    
    if (goes.activeMap() == null) {
      goes.toggleVisitor(this, true);
    }
    
    if (task != null && ! task.updateOnArrival(goes, journey)) {
      assignTask(null, this);
    }
  }
  
  
  public void onDeparture(Base goes, World.Journey journey) {
    if (goes.activeMap() == null) {
      goes.toggleVisitor(this, false);
    }
    return;
  }
  
  
  public boolean isActor() {
    return true;
  }
  
  
  
  /**  Combat and survival-related code:
    */
  public int meleeDamage() {
    Good weapon = type().weaponType;
    int amount = type().meleeDamage;
    if (weapon != null) {
      amount = Nums.max(amount, weapon.meleeDamage);
      amount += outfit.carried(weapon);
    }
    amount += traits.levelOf(STAT_DAMAGE);
    return amount;
  }
  
  
  public int rangeDamage() {
    Good weapon = type().weaponType;
    int amount = type().rangeDamage;
    if (weapon != null) {
      amount = Nums.max(amount, weapon.rangeDamage);
      amount += outfit.carried(weapon);
    }
    amount += traits.levelOf(STAT_DAMAGE);
    return amount;
  }
  
  
  public int armourClass() {
    Good armour = type().armourType;
    int amount = type().armourClass;
    if (armour != null) {
      amount = Nums.max(amount, armour.armourClass);
      amount += outfit.carried(armour);
    }
    amount += traits.levelOf(STAT_ARMOUR);
    return amount;
  }
  
  
  public int shieldBonus() {
    Good armour = type().armourType;
    int amount = type().shieldBonus;
    if (armour != null) {
      amount = Nums.max(amount, armour.shieldBonus);
      amount += outfit.carried(armour);
    }
    amount += traits.levelOf(STAT_SHIELD);
    return amount;
  }
  
  
  public int attackRange() {
    //  TODO:  Include mods from equipment and conditions?
    Good weapon = type().weaponType;
    int amount = type().rangeDist;
    if (weapon != null) amount = weapon.rangeDist;
    return amount;
  }
  
  
  public float sightRange() {
    //  TODO:  Include mods from equipment and conditions?
    float light = map == null ? 0.5f : map.lightLevel();
    return type().sightRange * (0.75f + (light * 0.5f));
  }
  
  
  public float hideLevel() {
    int mode = task == null ? MOVE_NORMAL : task.motionMode();
    float hide = traits.levelOf(SKILL_EVADE) / MAX_SKILL_LEVEL;
    if (mode == MOVE_SNEAK) hide += HIDE_EVADE_BONUS / 100f;
    hide = Nums.clamp(hide + (hideRoll / 2), 0, 1);
    return hide;
  }
  
  
  public float moveSpeed() {
    int mode = task == null ? MOVE_NORMAL : task.motionMode();
    float speed = type().moveSpeed * 1f / AVG_MOVE_SPEED;
    if (mode == MOVE_RUN  ) speed *= RUN_MOVE_SPEED  / 100f;
    if (mode == MOVE_SNEAK) speed *= HIDE_MOVE_SPEED / 100f;
    speed *= Nums.clamp(1 + traits.levelOf(STAT_SPEED), 0, 2);
    return speed;
  }
  
  
  public float actSpeed() {
    float speed = 1f;
    speed *= Nums.clamp(1 + traits.levelOf(STAT_ACTION), 0, 2);
    return speed;
  }
  
  
  public void performAttack(Element other, boolean melee) {
    TaskCombat.performAttack(this, other, melee);
  }
  
  
  public void takeDamage(float damage) {
    health.takeDamage(damage);
  }
  
  
  public boolean armed() {
    return Nums.max(meleeDamage(), rangeDamage()) > 0;
  }
  
  
  void updateVision() {
    if (indoors()) return;
    
    AreaTile from = centre();
    
    if (base() != map.locals) {
      AreaFog fog = map.fogMap(base(), true);
      float range = sightRange();
      if (fog != null) fog.liftFog(from, range);
    }
    
    if (guestBase() != map.locals) {
      AreaFog fog = map.fogMap(guestBase(), true);
      float range = sightRange();
      if (fog != null) fog.liftFog(from, range);
    }
    
    float noticeRange = sightRange();
    if (mission != null && mission.active()) noticeRange += AVG_FILE;
    seen = map.activeInRange(at(), noticeRange);
    seen = filterToNotice(seen, noticeRange, -1);
  }
  
  
  void updateConsidered() {
    int max = AVG_MAX_NOTICE;
    considered = (Series) filterToNotice((Series) map.actors(), -1, max);
    haven = updateHaven();
  }
  
  
  Series <Active> filterToNotice(
    Series <Active> around, float noticeRange, int maxNotice
  ) {
    Batch <Active> filtered = new Batch();
    AreaFog fog = map.fogMap(base(), true);
    
    for (Active a : around) {
      
      float dist = Area.distance(this, a);
      float sight = 1, hide = 0;
      
      if (noticeRange > 0) {
        sight = 1 - (dist / noticeRange);
      }
      else if (a.base() != base()) {
        sight = fog.sightLevel(a.at());
      }
      if (a.mobile()) {
        Actor m = (Actor) a;
        hide = m.hideLevel();
      }
      
      if (sight - hide > 0) {
        filtered.add(a);
      }
    }
    
    if (maxNotice > 0 && filtered.size() > maxNotice) {
      Batch <Active> culled = new Batch();
      float cullRatio = maxNotice * 1f / filtered.size();
      int segmentSize = filtered.size() / Nums.ceil(1f / cullRatio);
      
      int index = Rand.index(segmentSize);
      for (Active a : filtered) {
        float modulusFraction = (index % segmentSize) * 1f / segmentSize;
        if (modulusFraction < cullRatio) culled.add(a);
        index++;
      }
      filtered = culled;
    }
    
    return filtered;
  }
  
  
  public Series <Active> seen() {
    return seen;
  }
  
  
  public Series <Actor> considered() {
    return considered;
  }
  
  
  public Pathing haven() {
    return haven;
  }
  
  
  public float growLevel() {
    return health.growLevel();
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
  
  
  public void setCustomName(String name) {
    this.customName = name;
  }
  
  
  public String fullName() {
    if (customName.length() > 0) return customName;
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
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (indoors() || ! onMap()) return;
    final Vec3D viewPos = renderedPosition(null);
    Selection.renderSimpleCircle(
      this, viewPos, rendering, Colour.transparency(hovered ? 0.5f : 0.5f)
    );
  }
  
  
  public boolean setSelected(PlayUI UI) {
    UI.setDetailPane(new PaneActor (UI, this));
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

    Type t = type();
    Vec3D from = lastPosition, goes = exactPosition;
    float alpha = Rendering.frameAlpha(), rem = 1 - alpha;
    float offX = (t.wide - 1) / 2f, offY = (t.high - 1) / 2f;
    
    ///I.say("Update: "+map.numUpdates+", alpha: "+alpha);
    
    s.position.set(
      (from.x * rem) + (goes.x * alpha) + offX,
      (from.y * rem) + (goes.y * alpha) + offY,
      (from.z * rem) + (goes.z * alpha)
    );
    
    boolean contact = task != null && task.inContact();
    if (contact) goes = task.faceTarget().exactPosition(null);
    Vec2D angleVec = new Vec2D(goes.x - from.x, goes.y - from.y);
//<<<<<<< HEAD
    //if (angleVec.length() > 0) s.rotation = angleVec.toAngle();
    
    if (contact) {
      float animProg = task.animProgress(alpha);
      updateSprite(s, task.animName(), angleVec, animProg, true);
//=======
    /*
    if (contact) {
      float animProg = task.actionProgress();
      updateSprite(s, task.animName(), angleVec, animProg, true);
>>>>>>> charts_display
      //*/
    }
    else {
      final float ANIM_MULT = 1.66f;
      float animProg = Rendering.activeTime() * ANIM_MULT;
      animProg *= moveSpeed();
      int mode = task == null ? MOVE_NORMAL : task.motionMode();
      String animName = AnimNames.MOVE;
      if (mode == MOVE_RUN  ) animName = AnimNames.MOVE_FAST;
      if (mode == MOVE_SNEAK) animName = AnimNames.MOVE_SNEAK;
      updateSprite(s, animName, angleVec, animProg, true);
    }
    super.renderElement(rendering, base);
    
    
    //  Add a health-bar for general display
    
    //boolean report = I.talkAbout == this;
    
    Healthbar h = new Healthbar();
    h.fog       = s.fog;
    h.colour    = Colour.BLUE;
    h.size      = 30;
    h.hurtLevel = (health.injury() + health.hunger()) / health.maxHealth();
    h.tireLevel = health.fatigue() / health.maxHealth();
    h.position.setTo(s.position);
    h.position.z += type().deep + 0.1f;
    h.readyFor(rendering);
    
    //if (report) I.say("Bar fog: "+h.fog);
    
    //  TODO:  Add a chat-bar in cases of conversation, to represent how
    //  persuaded/converted/loyal a subject is.
  }
  
  
  protected void updateSprite(
    Sprite s, String animName, Vec2D angleVec, float animProg, boolean loop
  ) {
    if (angleVec.length() > 0) s.rotation = angleVec.toAngle();
    s.setAnimation(animName, animProg % 1, loop);
  }
  
  
  protected float renderedFog(Base base) {
    if (base == this.base()) return 1;
    Sprite s = sprite();
    AreaFog fog = map.fogMap(base, false);
    if (fog == null || s == null) return 0;
    return fog.displayedFog(s.position.x, s.position.y, this);
  }
  
  
  public Vec3D trackPosition() {
    if (indoors()) {
      return ((Element) inside).trackPosition();
    }
    else {
      Type t = type();
      Vec3D s = new Vec3D();
      Vec3D from = lastPosition, goes = exactPosition;
      float alpha = Rendering.frameAlpha(), rem = 1 - alpha;
      float offX = (t.wide - 1) / 2f, offY = (t.high - 1) / 2f;
      s.set(
        (from.x * rem) + (goes.x * alpha) + offX,
        (from.y * rem) + (goes.y * alpha) + offY,
        (from.z * rem) + (goes.z * alpha)
      );
      return s;
    }
  }
  
  
  public Composite portrait() {
    final String key = "actor_"+this.hashCode();
    final Composite cached = Composite.fromCache(key);
    if (cached != null) return cached;
    
    //final int size = SelectionPane.PORTRAIT_SIZE;
    final int size = 40;
    
    Type type = type();
    if (type == null || type.icon == null) {
      return Composite.withSize(size, size, key);
    }
    return Composite.withImage(type.icon, key);
  }
  
}










