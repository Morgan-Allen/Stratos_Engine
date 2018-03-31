

package game;
import util.*;
import static game.Base.*;
import static game.Area.*;
import static game.GameConstants.*;
import graphics.common.*;



public class TaskCombat extends Task {
  
  
  
  /**  Data fields, construction and save/load methods-
    */
  final static int
    ATTACK_NONE  = 0,
    ATTACK_MELEE = 1,
    ATTACK_RANGE = 2,
    ATTACK_FIRE  = 3
  ;
  
  final public Element primary;
  int attackMode;
  
  
  public TaskCombat(Active actor, Element primary) {
    super(actor);
    this.primary = primary;
  }
  
  
  public TaskCombat(Session s) throws Exception {
    super(s);
    primary = (Element) s.loadObject();
    attackMode = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(primary);
    s.saveInt(attackMode);
  }
  
  
  
  /**  Utility methods for determining hostility, power, et cetera:
    */
  public static POSTURE postureFor(Element a, Element b) {
    Base CA = a.base (), CB = b.base ();
    Base GA = a.guestBase(), GB = b.guestBase();
    if (CA == CB              ) return POSTURE.ALLY;
    if (GA != null && GA == GB) return POSTURE.ALLY;
    if (GB != null && CA == GB) return POSTURE.ALLY;
    if (GA != null && GA == CB) return POSTURE.ALLY;
    return CA.posture(CB);
  }
  
  
  public static POSTURE postureFor(Target a, Target b) {
    if (a == null  || b == null ) return POSTURE.NEUTRAL;
    if (a.isTile() || b.isTile()) return POSTURE.NEUTRAL;
    return postureFor((Element) a, (Element) b);
  }
  
  
  public static boolean allied(Target a, Target b) {
    //  TODO:  Replace?
    return postureFor(a, b) == POSTURE.ALLY;
  }
  
  
  public static boolean hostile(Target a, Target b) {
    //  TODO:  Replace?
    return postureFor(a, b) == POSTURE.ENEMY;
  }
  
  
  public static float hostility(Target a, Target b) {
    POSTURE p = postureFor(a, b);
    switch(p) {
      case ENEMY   : return 1;
      case ALLY    : return -1;
      case VASSAL  : return -0.5f;
      case LORD    : return -1;
      case NEUTRAL : return 0;
      case TRADING : return -0.5f;
    }
    return 0;
  }
  
  
  public static float attackPower(Type t) {
    float power = Nums.max(t.meleeDamage, t.rangeDamage) + t.armourClass;
    power /= (TOP_DAMAGE / 2) + (TOP_ARMOUR / 2);
    power *= t.maxHealth * 1f / AVG_MAX_HEALTH;
    return power;
  }
  
  
  public static float attackPower(Element f) {
    if (f.type().isActor()) {
      Actor a = (Actor) f;
      if (! a.health.active()) return 0;
      float power = attackPower(a.type());
      power *= 1 - ((a.health.injury() + a.health.hunger()) / a.type().maxHealth);
      return power;
    }
    else {
      Element e = (Element) f;
      if (e.destroyed()) return 0;
      float power = attackPower(e.type());
      power *= e.buildLevel();
      return power;
    }
  }
  
  
  public static boolean wallBonus(Element from, Element goes) {
    boolean wallBonus = false;
    wallBonus |= from.at().pathType() == Type.PATH_WALLS;
    wallBonus &= goes.at().pathType() != Type.PATH_WALLS;
    return wallBonus;
  }
  
  
  public static boolean beaten(Element focus) {
    if (focus.type().isActor()) {
      return ! ((Actor) focus).health.active();
    }
    else {
      return focus.destroyed();
    }
  }
  
  
  public static boolean killed(Element focus) {
    if (focus.type().isActor()) {
      return ((Actor) focus).health.dead();
    }
    else {
      return focus.destroyed();
    }
  }
  
  
  
  
  /**  Factory methods for actual combat behaviours-
    */
  static TaskCombat nextSieging(Actor actor, Mission mission, Object focus) {
    if (focus instanceof Element) {
      Element e = (Element) focus;
      if (beaten(e) || ! e.onMap()) return null;
      if (e.base() == mission.base()) return null;
      return configCombat(actor, e, mission, null, JOB.COMBAT);
    }
    else return null;
  }
  
  
  
  static TaskCombat nextReaction(Active actor) {
    return nextReaction(actor, null, null, 0);
  }
  
  
  //  TODO:  Pass in a series of noticed actives here, rather than relying on
  //  proximity.
  
  static TaskCombat nextReaction(
    Active actor, Target anchor, Employer employer, float noticeBonus
  ) {
    AreaTile from = actor.at();
    Area map = actor.map();
    float noticeRange = ((Element) actor).sightRange() + noticeBonus;
    Series <Active> others = map.activeInRange(from, noticeRange);
    
    class Option { Element other; float rating; };
    List <Option> options = new List <Option> () {
      protected float queuePriority(Option o) { return o.rating; }
    };
    
    for (Active other : others) {
      float priority = attackPriority(actor, (Element) other, true);
      if (priority <= 0) continue;
      
      AreaTile goes = other.at();
      float distF = distance(goes, from);
      float distA = anchor == null ? 0 : distance(goes, anchor);
      if (distA > noticeRange) continue;
      
      priority *= distancePenalty(distF + distA);
      priority /= 1 + other.focused().size();
      
      Option o = new Option();
      o.other  = (Element) other;
      o.rating = priority;
      options.add(o);
    }
    
    options.queueSort();
    for (Option o : options) {
      TaskCombat c = configCombat(actor, o.other, employer, null, JOB.COMBAT);
      if (c != null) return c;
    }
    
    return null;
  }
  
  
  //  NOTE:  This is used to cache query-results for tile-accessibility among
  //  all actors that belong to a formation, to help speed up performance.
  static class RangeAccess {
    Element target;
    int maxRange;
    AreaTile wasAt;
    AreaTile tiles[];
  }
  
  
  public static TaskCombat configCombat(Actor actor, Element target) {
    return configCombat(actor, target, null, null, JOB.COMBAT);
  }
  
  
  public static TaskCombat configHunting(Actor actor, Element target) {
    return configCombat(actor, target, null, null, JOB.HUNTING);
  }
  
  
  static TaskCombat configCombat(
    final Active active, final Element target, Employer employer,
    TaskCombat currentTask, JOB jobType
  ) {
    if (active == null || target == null || ! target.onMap()) return null;
    //
    //  In the case of immobile actives, such as turrets, you don't bother with
    //  the fancy pathing-connection tests.  You just check if the target is in
    //  range.
    if (! active.isActor()) {
      float distance     = Area.distance(active.at(), target);
      float rangeMelee   = 1.5f;
      float rangeMissile = active.type().rangeDist;
      float rateMelee    = active.type().meleeDamage;
      float rateRange    = active.type().rangeDamage;
      
      if (distance > rangeMelee  ) rateMelee = 0;
      if (distance > rangeMissile) rateRange = 0;
      
      if (rateMelee <= 0 && rateRange <= 0) return null;
      
      if (currentTask == null) currentTask = new TaskCombat(active, target);
      currentTask.configTask(employer, null, target.at(), jobType, 0);
      
      if (rateMelee > rateRange) currentTask.attackMode = ATTACK_MELEE;
      else currentTask.attackMode = ATTACK_RANGE;
      
      return currentTask;
    }
    //
    //  Otherwise, try and ensure that it's possible to path toward some tile
    //  within range of the target-
    AreaTile inRange[] = null;
    if (currentTask != null) {
      inRange = new AreaTile[] { active.at(), (AreaTile) currentTask.target };
    }
    else {
      final Area     map    = active.map();
      final AreaTile at     = target.at();
      final Box2D    area   = target.area();
      final int      range  = MAX_RANGE;
      final AreaTile temp[] = new AreaTile[9];
      //
      //  TODO:  This is a temporary hack until I can get the pathing-cache to
      //  store data for particular cities...
      final AreaTile from;
      Pathing inside = active.isActor() ? ((Actor) active).inside() : null;
      if (inside instanceof Building) {
        from = ((Building) inside).mainEntrance();
      }
      else {
        from = active.at();
      }
      
      Object areaKey = map.pathCache.openGroupHandle(from);
      String key = null;
      RangeAccess access = null;
      
      if (areaKey != null) {
        key = "RA_"+areaKey+"_"+at;
        access = (RangeAccess) map.pathCache.getCache(key);
      }
      
      if (access == null && key != null) {
        
        final Batch <AreaTile> accessT = new Batch();
        Flood <AreaTile> flood = new Flood <AreaTile> () {
          protected void addSuccessors(AreaTile front) {
            for (AreaTile n : Area.adjacent(front, temp, map)) {
              if (n == null || n.flaggedWith() != null) continue;
              if (area.distance(n.x, n.y) > range     ) continue;
              
              tryAdding(n);
              boolean reach = map.pathCache.openPathConnects(from, n);
              if (n.above == target || ! reach) continue;
              accessT.add(n);
            }
          }
        };
        flood.floodFrom(target.centre());
        
        access = new RangeAccess();
        access.tiles    = accessT.toArray(AreaTile.class);
        access.wasAt    = at;
        access.maxRange = range;
        access.target   = target;
        
        map.pathCache.putCache(key, access);
      }
      
      if (access != null) {
        inRange = access.tiles;
      }
    }
    
    if (inRange == null) {
      return null;
    }
    
    float rangeMelee   = 1.5f;
    float rangeMissile = active.type().rangeDist;
    float maxRange     = Nums.max(rangeMelee, rangeMissile);
    
    Pick <AreaTile> pick = new Pick();
    
    //  TODO:  It would be helpful if tiles were in strictly ascending order
    //  by distance.
    
    boolean report = false;
    if (report) {
      I.say("\nChecking tiles-");
      I.say("  Melee range:   "+rangeMelee  );
      I.say("  Missile range: "+rangeMissile);
      I.say("  Max. range:    "+maxRange    );
      I.say("  Target at:     "+target.at() );
    }
    
    for (AreaTile t : inRange) {
      if (Task.hasTaskFocus(t, jobType, active)) continue;
      
      float dist = Area.distance(target, t);
      if (report) I.say("  "+t+" dist: "+dist);
      
      if (dist > maxRange) continue;
      
      float rating = 0;
      if      (dist < rangeMelee  ) rating += active.type().meleeDamage;
      else if (dist < rangeMissile) rating += active.type().rangeDamage;
      pick.compare(t, rating);
    }
    
    if (pick.empty()) return null;
    
    boolean needsConfig = true;
    AreaTile t = pick.result();
    boolean standWall = t          .pathType() == Type.PATH_WALLS;
    boolean targWall  = target.at().pathType() == Type.PATH_WALLS;
    boolean canTouch  = standWall == targWall;
    
    if (currentTask == null) {
      currentTask = new TaskCombat(active, target);
    }
    else if (currentTask.target == t) {
      needsConfig = false;
    }
    
    //  TODO:  Decide on a preferred attack-mode first?
    
    if (canTouch && Area.distance(target, t) < rangeMelee) {
      currentTask.attackMode = ATTACK_MELEE;
    }
    else {
      currentTask.attackMode = ATTACK_RANGE;
    }
    
    if (needsConfig) return (TaskCombat) (
      currentTask.configTask(employer, null, t, jobType, 0)
    );
    else {
      return currentTask;
    }
  }
  
  
  
  /**  Priority-evaluation:
    */
  static float attackPriority(Active actor, Element primary, boolean quick) {
    
    if (beaten(primary) || primary.indoors()) return -1;
    float targetPower = attackPower(primary);
    
    float priority = 0, empathy = 1, cruelty = 1;
    if (actor.isActor()) {
      empathy = (((Actor) actor).levelOf(TRAIT_EMPATHY) + 2) / 2;
      cruelty = 2 - empathy;
    }
    
    if (hostile(actor, primary)) priority += ROUTINE * cruelty;
    if (allied (actor, primary)) priority -= ROUTINE * empathy;
    
    if (Task.inCombat(primary)) {
      Target victim = Task.mainTaskFocus(primary);
      if (hostile(actor, victim)) priority -= PARAMOUNT * cruelty;
      if (allied (actor, victim)) priority += PARAMOUNT * empathy;
    }
    
    if (priority <= 0) return -1;
    if (quick) return priority;
    
    float othersWinChance = 0;
    for (Active a : primary.focused()) if (a != actor) {
      if (a.jobType() == JOB.COMBAT && allied(a, actor)) {
        float power = attackPower((Element) a);
        othersWinChance += power * 0.5f / Nums.max(1, targetPower);
      }
    }
    if (othersWinChance > 0 && othersWinChance < 1) {
      priority += (1 - othersWinChance) * PARAMOUNT * empathy;
    }
    priority += CASUAL * cruelty;
    
    if (priority <= 0) return -1;
    return priority;
  }
  
  
  protected float successPriority() {
    return attackPriority(active, primary, false);
  }
  
  
  protected float successChance() {
    if (! active.isActor()) return 1;
    
    Actor actor = (Actor) active;
    float power = attackPower((Element) active);
    float fear  = actor.fearLevel();
    
    AreaDanger dangerMap = actor.map().dangerMap(active.base(), true);
    AreaTile around = actor.at();
    
    float danger = dangerMap.fuzzyLevel(around.x, around.y);
    danger = Nums.max(danger, attackPower(primary));
    
    Series <Actor> backup = actor.backup();
    for (Actor a : backup) if (a != actor) {
      float backPower = attackPower(a);
      if (a.jobType() == JOB.COMBAT) {
        backPower *= Area.distance(mainTaskFocus(a), primary);
      }
      else {
        backPower /= 2;
      }
      backPower *= Area.distancePenalty(actor, a);
      power += backPower;
    }
    
    float chance = power / (danger + power);
    chance = (chance + 1 - fear) / 2;
    
    return chance;
  }
  
  
  protected float failCostPriority() {
    return PARAMOUNT;
  }


  public float harmLevel() {
    return FULL_HARM;
  }
  
  
  
  /**  Behaviour-execution-
    */
  boolean checkAndUpdateTask() {
    Task self = configCombat(active, primary, origin, this, type);
    if (self == null) return false;
    return super.checkAndUpdateTask();
  }
  
  
  void toggleFocus(boolean activeNow) {
    target .setFocused(active, activeNow);
    primary.setFocused(active, activeNow);
  }
  
  
  public Target mainFocus() {
    return primary;
  }


  float actionRange() {
    if (attackMode == ATTACK_MELEE) return 1.5f;
    if (attackMode == ATTACK_RANGE) return active.type().rangeDist + 1;
    if (attackMode == ATTACK_FIRE ) return active.type().rangeDist + 1;
    return -1;
  }
  
  
  int motionMode() {
    return Actor.MOVE_RUN;
  }


  boolean checkContact(Pathing[] path) {
    float range = Area.distance(active, primary);
    float maxRange = actionRange();
    return range < maxRange;
  }
  
  
  
  protected void onTarget(Target other) {
    active.performAttack(primary, attackMode == ATTACK_MELEE);
    
    ///I.say("PERFORMING ATTACK VS. "+primary);
    
    if (primary.type().isActor() && ! beaten(primary)) {
      Task next = TaskCombat.configCombat(active, primary, origin, null, type);
      if (next != null) active.assignTask(next);
    }
    
    if (primary.type().isBuilding() && ! beaten(primary)) {
      Task next = TaskCombat.configCombat(active, primary, origin, null, type);
      if (next != null) active.assignTask(next);
    }
  }


  static void performAttack(Element attacks, Element other, boolean melee) {
    
    int damage = melee ? attacks.meleeDamage() : attacks.rangeDamage();
    int armour = other.armourClass();
    if (other == null || damage <= 0) return;
    
    Trait   attackSkill = melee ? SKILL_MELEE : SKILL_RANGE;
    Trait   defendSkill = melee ? SKILL_MELEE : SKILL_EVADE;
    boolean wallBonus   = TaskCombat.wallBonus(attacks, other);
    boolean wallPenalty = TaskCombat.wallBonus(other, attacks);
    boolean hits = true;
    float XP = 1, otherXP = 0, hitChance = 0, attackBonus = 0, defendBonus = 0;
    
    if (attacks.type().isActor()) {
      Actor attackA = (Actor) attacks;
      attackBonus = attackA.levelOf(attackSkill) * 2f / MAX_SKILL_LEVEL;
    }
    if (other.type().isActor()) {
      Actor otherA = (Actor) other;
      defendBonus = otherA.levelOf(defendSkill) * 2f / MAX_SKILL_LEVEL;
    }
    
    if (wallBonus  ) attackBonus += WALL_HIT_BONUS / 100f;
    if (wallPenalty) defendBonus += WALL_DEF_BONUS / 100f;
    
    hitChance = Nums.clamp(attackBonus + 1 - defendBonus, 0, 2) / 2;
    hits      = Rand.num() < hitChance;
    XP        = (1.5f - hitChance) * FIGHT_XP_PERCENT / 100f;
    otherXP   = (0.5f + hitChance) * FIGHT_XP_PERCENT / 100f;
    
    if (hits) {
      if (wallBonus  ) damage += WALL_DMG_BONUS;
      if (wallPenalty) armour += WALL_ARM_BONUS;
      damage = Rand.index(damage + armour) + 1;
      damage = Nums.max(0, damage - armour);
      if (damage > 0) other.takeDamage(damage);
    }
    
    if (attacks.type().isActor()) {
      Actor attackA = (Actor) attacks;
      attackA.gainXP(attackSkill, XP);
    }
    if (other.type().isActor()) {
      Actor otherA = (Actor) other;
      otherA.gainXP(defendSkill, otherXP);
    }
    
    
    Area map = attacks.map();
    if (map.ephemera.active()) {
      Good weaponType = attacks.type().weaponType;
      Ephemera.applyCombatFX(weaponType, (Active) attacks, other, ! melee, hits, map);
    }
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  public String toString() {
    return "Fighting "+primary+" from "+target;
  }
  
  
  String animName() {
    if (attackMode == ATTACK_MELEE) return AnimNames.STRIKE;
    if (attackMode == ATTACK_RANGE) return AnimNames.FIRE  ;
    return AnimNames.STRIKE;
  }
  

  Target faceTarget() {
    return primary;
  }
}



