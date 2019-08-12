

package game;
import static game.GameConstants.*;
import static game.RelationSet.*;
import static game.AreaMap.*;
import graphics.common.*;
import util.*;



public class TaskCombat extends Task {
  
  
  
  /**  Data fields, construction and save/load methods-
    */
  final static int
    ATTACK_NONE  = 0,
    ATTACK_MELEE = 1,
    ATTACK_RANGE = 2,
    ATTACK_FIRE  = 3
  ;
  final static float
    RANGE_MELEE = 1.5f
  ;
  
  final public Element primary;
  boolean defends;
  int attackMode;
  
  
  public TaskCombat(Active actor, Element primary) {
    super(actor);
    this.primary = primary;
  }
  
  
  public TaskCombat(Session s) throws Exception {
    super(s);
    primary = (Element) s.loadObject();
    defends = s.loadBool();
    attackMode = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(primary);
    s.saveBool(defends);
    s.saveInt(attackMode);
  }
  
  
  
  /**  Utility methods for determining hostility, power, et cetera:
    */
  public static int postureFor(Element a, Element b) {
    Base CA = a.base     (), CB = b.base     ();
    Base GA = a.guestBase(), GB = b.guestBase();
    if (CA == CB              ) return BOND_ALLY;
    if (GA != null && GA == GB) return BOND_ALLY;
    if (GB != null && CA == GB) return BOND_ALLY;
    if (GA != null && GA == CB) return BOND_ALLY;
    return CA.posture(CB);
  }
  
  
  public static int postureFor(Target a, Target b) {
    if (a == null  || b == null ) return BOND_NEUTRAL;
    if (a.isTile() || b.isTile()) return BOND_NEUTRAL;
    return postureFor((Element) a, (Element) b);
  }
  
  
  public static boolean allied(Target a, Target b) {
    //  TODO:  Replace?
    int p = postureFor(a, b);
    return p == BOND_ALLY || p == BOND_VASSAL || p == BOND_LORD;
  }
  
  
  public static boolean hostile(Target a, Target b) {
    //  TODO:  Replace?
    int p = postureFor(a, b);
    return p == BOND_ENEMY;
  }
  
  
  public static float hostility(Target a, Target b) {
    int p = postureFor(a, b);
    switch(p) {
      case BOND_ENEMY   : return  1.0f;
      case BOND_ALLY    : return -1.0f;
      case BOND_VASSAL  : return -1.0f;
      case BOND_LORD    : return -1.0f;
      case BOND_NEUTRAL : return  0.0f;
      case BOND_TRADING : return -0.5f;
    }
    return 0;
  }
  
  
  public static float attackPower(Type t) {
    Good w = t.weaponType;
    Good a = t.armourType;
    int melee  = w == null ? t.meleeDamage : w.meleeDamage;
    int range  = w == null ? t.rangeDamage : w.rangeDamage;
    int armour = a == null ? t.armourClass : w.armourClass;
    
    float power = Nums.max(melee, range) + armour;
    power /= (TOP_DAMAGE / 2) + (TOP_ARMOUR / 2);
    power *= t.maxHealth * 1f / AVG_MAX_HEALTH;
    return power;
  }
  
  
  public static float attackPower(Element f) {
    if (f.type().isActor()) {
      Actor a = (Actor) f;
      if (! a.health.active()) return 0;
      float power = attackPower(a.type());
      power *= 1 - a.health.hurtLevel();
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
  
  
  static TaskCombat nextDefending(Active actor) {
    Series <Active> others = (Series) ((Actor) actor).considered();
    return nextReaction(actor, null, null, true, others);
  }
  
  
  static TaskCombat nextReaction(Active actor, Series <Active> others) {
    return nextReaction(actor, null, null, true, others);
  }
  
  
  static TaskCombat nextReaction(
    Active actor, Target anchor,
    Employer employer, boolean defends, Series <Active> others
  ) {
    if (! actor.map().world.settings.toggleCombat) return null;
    AreaTile from = actor.at();
    
    class Option { Element other; float rating; };
    List <Option> options = new List <Option> () {
      protected float queuePriority(Option o) { return o.rating; }
    };
    
    for (Active other : others) {
      float priority = attackPriority(actor, (Element) other, true, defends);
      if (priority <= 0) continue;
      
      AreaTile goes = other.at();
      float distF = distance(goes, from);
      float distA = anchor == null ? 0 : distance(goes, anchor);
      
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
      if (c == null) return null;
      c.defends = defends;
      return c;
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
  
  
  static AreaTile updateStandPoint(
    Active active, final Element target,
    TaskCombat currentTask, JOB jobType
  ) {
    //
    //  Otherwise, try and ensure that it's possible to path toward some tile
    //  within range of the target-
    final AreaMap map = target.map();
    AreaTile inRange[] = null;
    if (currentTask != null) {
      inRange = new AreaTile[] { active.at(), (AreaTile) currentTask.target };
    }
    else {
      final AreaTile at     = target.at();
      final Box2D    area   = target.area();
      final int      range  = MAX_RANGE;
      final AreaTile temp[] = new AreaTile[9];
      //
      //  TODO:  This is a temporary hack until I can get the pathing-cache to
      //  store data for particular cities...
      final AreaTile from;
      Pathing inside = active.mobile() ? ((Actor) active).inside() : null;
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
            for (AreaTile n : AreaMap.adjacent(front, temp, map)) {
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
    
    float meleeDamage  = ((Element) active).meleeDamage();
    float rangeDamage  = ((Element) active).rangeDamage();
    float rangeMissile = ((Element) active).attackRange();
    float maxRange     = Nums.max(RANGE_MELEE, rangeMissile);
    
    boolean report = false;
    if (report) {
      I.say("\nChecking tiles: ");
      I.say("  Melee range:   "+RANGE_MELEE );
      I.say("  Missile range: "+rangeMissile);
      I.say("  Max. range:    "+maxRange    );
      I.say("  Target at:     "+target.at() );
    }
    
    Pick <AreaTile> pick = new Pick();
    for (AreaTile t : inRange) {
      if (Task.hasTaskFocus(t, jobType, active)) continue;
      
      float distT = AreaMap.distanceSubRadius(target, t);
      ///I.say("    "+t+" : "+distT);
      if (distT > maxRange) continue;
      
      float rating = 0;
      if      (distT < RANGE_MELEE ) rating = Nums.max(rating, meleeDamage);
      else if (distT < rangeMissile) rating = Nums.max(rating, rangeDamage);
      
      float distA = AreaMap.distancePenalty(t, active);
      rating *= distA;

      if (report) I.say("  "+t+" distT: "+I.shorten(distT, 1)+", rating: "+rating);
      pick.compare(t, rating);
    }
    
    if (report) I.say("  Final pick: "+pick.result()+", rating: "+pick.bestRating());
    
    if (pick.empty()) return null;
    return pick.result();
  }
  
  
  static TaskCombat configCombat(
    final Active active, final Element target, Employer employer,
    TaskCombat currentTask, JOB jobType
  ) {
    if (active == null || target == null || ! target.onMap()) return null;
    final AreaMap map = active.map();
    if (map == null || ! map.world.settings.toggleCombat) return null;
    //
    //  In the case of immobile actives, such as turrets, you don't bother with
    //  the fancy pathing-connection tests.  You just check if the target is in
    //  range.
    if (! active.mobile()) {
      float distance     = AreaMap.distanceSubRadius(active.at(), target);
      float rangeMissile = active.type().rangeDist;
      float rateMelee    = active.type().meleeDamage;
      float rateRange    = active.type().rangeDamage;
      
      if (distance > RANGE_MELEE ) rateMelee = 0;
      if (distance > rangeMissile) rateRange = 0;
      
      if (rateMelee <= 0 && rateRange <= 0) return null;
      
      if (currentTask == null) currentTask = new TaskCombat(active, target);
      currentTask.configTask(employer, null, target.at(), jobType, 0);
      
      if (rateMelee > rateRange) currentTask.attackMode = ATTACK_MELEE;
      else currentTask.attackMode = ATTACK_RANGE;
      
      return currentTask;
    }
    
    
    boolean needsConfig = true;
    AreaTile t = updateStandPoint(active, target, currentTask, jobType);
    if (t == null && currentTask != null) t = updateStandPoint(active, target, null, jobType);
    if (t == null) return null;
    
    boolean standWall = t          .pathType() == Type.PATH_WALLS;
    boolean targWall  = target.at().pathType() == Type.PATH_WALLS;
    boolean canTouch  = standWall == targWall;
    
    if (currentTask == null) {
      currentTask = new TaskCombat(active, target);
    }
    else if (currentTask.target == t) {
      needsConfig = false;
    }
    
    //  TODO:  Decide on a preferred attack-mode first...?
    
    if (canTouch && AreaMap.distanceSubRadius(target, t) < RANGE_MELEE) {
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
  static float attackPriority(
    Active actor, Element primary, boolean quick, boolean defends
  ) {
    if (beaten(primary) || primary.indoors()) return -1;
    
    float priority = 0, empathy = 1, cruelty = 1;
    if (actor.mobile()) {
      empathy = (((Actor) actor).traits.levelOf(TRAIT_EMPATHY) + 2) / 2;
      cruelty = 2 - empathy;
    }
    
    if (hostile(actor, primary) && ! defends) priority += ROUTINE * cruelty;
    if (allied (actor, primary)) priority -= ROUTINE * empathy;
    
    if (Task.inCombat(primary)) {
      Target victim = Task.mainTaskFocus(primary);
      if (hostile(actor, victim)) priority -= PARAMOUNT * cruelty;
      if (allied(actor, victim) && defends) priority += PARAMOUNT * empathy;
    }
    
    if (priority <= 0) return -1;
    if (quick) return priority;
    
    /*
    float targetPower = attackPower(primary);
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
    //*/
    
    priority += CASUAL * cruelty;
    
    if (priority <= 0) return -1;
    return priority;
  }
  
  
  protected float successPriority() {
    return attackPriority(active, primary, false, defends);
  }
  
  
  static float attackChance(Actor actor, Element primary) {
    //
    //  Firstly, compute fear-levels in the immediate vicinity.
    float selfPower  = attackPower(actor);
    float otherPower = attackPower(primary);
    float localFear  = actor.fearLevel();
    //
    //  Then, compute an estimate of what you might expect fear-levels to be
    //  at the target point, based on general danger in the area and any backup
    //  you can rely on.
    Mission mission = actor.mission();
    AreaTile around = actor.at();
    AreaDanger dangerMap = actor.map().dangerMap(actor);
    
    float backupPower = 0;
    if (mission != null && mission.localFocus() == primary) {
      backupPower += MissionForStrike.powerSum(mission);
    }
    backupPower = Nums.max(backupPower, selfPower);
    
    float farDanger = dangerMap.fuzzyLevel(around.x, around.y);
    farDanger = Nums.max(farDanger, otherPower);
    float farFear = farDanger / (farDanger + backupPower);
    //
    //  Then blend the two estimates depending on how close the primary target
    //  is-
    float proximity = AreaMap.distancePenalty(actor, primary);
    float fearAvg = (localFear * proximity) + (farFear * (1 - proximity));
    return Nums.clamp(1 - fearAvg, 0, 1);
  }
  
  
  protected float successChance() {
    if (! active.mobile()) return 1;
    return attackChance((Actor) active, primary);
  }
  
  
  protected float failCostPriority() {
    return PARAMOUNT;
  }


  public float harmLevel() {
    return FULL_HARM;
  }
  
  
  public boolean emergency() {
    //  TODO:  This should only be true if the target is currently visible.  
    return true;
  }
  
  
  
  /**  Behaviour-execution-
    */
  boolean checkAndUpdateTask() {
    if (! updateStanding()) return false;
    return super.checkAndUpdateTask();
  }
  
  
  boolean updateStanding() {
    //  NOTE:  This method is intended for override by certain subclasses.
    Task self = configCombat(active, primary, origin, this, type);
    return self != null;
  }
  
  
  void toggleFocus(boolean activeNow) {
    if (target  != null) target .setFocused(active, activeNow);
    if (primary != null) primary.setFocused(active, activeNow);
  }
  
  
  public Target mainFocus() {
    return primary;
  }


  float actionRange() {
    if (attackMode == ATTACK_MELEE) return RANGE_MELEE;
    else return Nums.max(RANGE_MELEE, ((Element) active).attackRange());
  }
  
  
  int motionMode() {
    //  TODO:  This might not always be the case.  Check again...
    return Actor.MOVE_RUN;
  }
  
  
  boolean checkTargetContact(Target from) {
    float range = AreaMap.distanceSubRadius(active, primary);
    float maxRange = actionRange();
    return range < maxRange;
  }
  
  
  
  protected void onTarget(Target other) {
    active.performAttack(primary, attackMode == ATTACK_MELEE);
    
    if (primary.type().isActor() && ! beaten(primary)) {
      Task next = TaskCombat.configCombat(active, primary, origin, null, type);
      if (next != null) active.assignTask(next, this);
    }
    
    if (primary.type().isBuilding() && ! beaten(primary)) {
      Task next = TaskCombat.configCombat(active, primary, origin, null, type);
      if (next != null) active.assignTask(next, this);
    }
  }


  static void performAttack(Element attacks, Element other, boolean melee) {
    
    int damage = melee ? attacks.meleeDamage() : attacks.rangeDamage();
    int armour = other.armourClass() + (melee ? 0 : other.shieldBonus());
    if (other == null || damage <= 0) return;
    
    Trait   attackSkill = melee ? SKILL_MELEE : SKILL_SIGHT;
    Trait   defendSkill = melee ? SKILL_MELEE : SKILL_EVADE;
    boolean wallBonus   = TaskCombat.wallBonus(attacks, other);
    boolean wallPenalty = TaskCombat.wallBonus(other, attacks);
    boolean hits = true;
    float XP = 1, otherXP = 0, hitChance = 0, attackBonus = 0, defendBonus = 0;
    
    if (attacks.type().isActor()) {
      Actor attackA = (Actor) attacks;
      attackBonus = attackA.traits.levelOf(attackSkill) * 2f / MAX_SKILL_LEVEL;
    }
    if (other.type().isActor()) {
      Actor otherA = (Actor) other;
      defendBonus = otherA.traits.levelOf(defendSkill) * 2f / MAX_SKILL_LEVEL;
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
      attackA.traits.gainXP(attackSkill, XP);
    }
    if (other.type().isActor()) {
      Actor otherA = (Actor) other;
      otherA.traits.gainXP(defendSkill, otherXP);
    }
    
    AreaMap map = attacks.map();
    Good weaponType = attacks.type().weaponType;
    Ephemera.applyCombatFX(weaponType, (Active) attacks, other, ! melee, hits, map);
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



