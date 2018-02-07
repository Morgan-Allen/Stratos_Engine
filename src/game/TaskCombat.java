

package game;
import util.*;
import static game.City.*;
import static game.CityMap.*;
import static game.GameConstants.*;
import static game.CityMapPathCache.*;



public class TaskCombat extends Task {
  
  
  final static int
    ATTACK_NONE  = 0,
    ATTACK_MELEE = 1,
    ATTACK_RANGE = 2,
    ATTACK_FIRE  = 3
  ;
  
  final public Element primary;
  int attackMode;
  
  
  public TaskCombat(Actor actor, Element primary) {
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
  
  
  static POSTURE postureFor(Element a, Element b) {
    City CA = a.homeCity (), CB = b.homeCity ();
    City GA = a.guestCity(), GB = b.guestCity();
    if (CA == CB) return POSTURE.ALLY;
    if (GA == GB) return POSTURE.ALLY;
    if (CA == GB) return POSTURE.ALLY;
    if (GA == CB) return POSTURE.ALLY;
    return CA.posture(CB);
  }
  
  
  static boolean allied(Element a, Element b) {
    return postureFor(a, b) == POSTURE.ALLY;
  }
  
  
  static boolean hostile(Element a, Element b) {
    return postureFor(a, b) == POSTURE.ENEMY;
  }
  
  
  static float attackPower(Type t) {
    float power = Nums.max(t.meleeDamage, t.rangeDamage) + t.armourClass;
    power /= Nums.max(AVG_MELEE, AVG_MISSILE) + AVG_DEFEND;
    power *= t.maxHealth * 1f / AVG_MAX_HEALTH;
    return power;
  }
  
  
  static float attackPower(Actor a) {
    if (a.state >= Actor.STATE_DEAD) return 0;
    float power = attackPower(a.type());
    power *= 1 - ((a.injury + a.hunger) / a.type().maxHealth);
    return power;
  }
  
  
  
  static TaskCombat nextSieging(Actor actor, Mission formation) {
    if (formation.focus instanceof Element) {
      Element e = (Element) formation.focus;
      if (e.destroyed() || ! e.onMap()) return null;
      if (e.homeCity() == formation.homeCity) return null;
      return configCombat(actor, e, formation, null, JOB.COMBAT);
    }
    else return null;
  }
  
  
  //  TODO:  Allow for iteration over members of a target formation, or whatever
  //  your comrades are currently fighting.
  
  
  static TaskCombat nextReaction(Actor actor, Mission formation) {
    
    //  TODO:  Allow for a null formation to be passed in here.
    Tile from = actor.at();
    CityMap map = actor.map;
    Tile anchor = formation.standLocation(actor);
    
    //  TODO:  Grant a vision bonus for higher ground?
    float noticeBonus = AVG_FILE;
    float noticeRange = actor.type().sightRange + noticeBonus;
    Series <Actor> others = map.actorsInRange(from, noticeRange);
    
    class Option { Actor other; float rating; };
    List <Option> options = new List <Option> () {
      protected float queuePriority(Option o) { return o.rating; }
    };
    
    for (Actor other : others) if (hostile(other, actor)) {
      Tile goes = other.at();
      float distF = distance(goes, from  );
      float distA = distance(goes, anchor);
      if (distA > noticeRange) continue;
      
      float rating = 1f * distancePenalty(distF + distA);
      rating /= 1 + other.focused().size();
      
      Option o = new Option();
      o.other  = other ;
      o.rating = rating;
      options.add(o);
    }
    
    options.queueSort();
    for (Option o : options) {
      TaskCombat c = configCombat(actor, o.other, formation, null, JOB.COMBAT);
      if (c != null) return c;
    }
    
    return null;
  }
  
  
  //  NOTE:  This is used to cache query-results for tile-accessibility among
  //  all actors that belong to a formation, to help speed up performance.
  static class RangeAccess {
    Element target;
    int maxRange;
    Tile wasAt;
    Tile tiles[];
  }
  
  
  static TaskCombat configCombat(Actor actor, Element target) {
    return configCombat(actor, target, null, null, JOB.COMBAT);
  }
  
  
  static TaskCombat configHunting(Actor actor, Element target) {
    return configCombat(actor, target, null, null, JOB.HUNTING);
  }
  
  
  static TaskCombat configCombat(
    final Actor actor, final Element target,
    Mission formation, TaskCombat currentTask,
    JOB jobType
  ) {
    if (actor == null || target == null || ! target.onMap()) return null;
    Tile inRange[] = null;
    
    if (currentTask != null) {
      inRange = new Tile[] { actor.at(), (Tile) currentTask.target };
    }
    else {
      final CityMap map    = actor.map;
      final Tile    at     = target.at();
      final Box2D   area   = target.area();
      final int     range  = MAX_RANGE;
      final Tile    temp[] = new Tile[9];
      
      //  TODO:  This is a temporary hack until I can get the pathing-cache to
      //  store data for particular cities, and the like...
      final Tile from;
      if (actor.inside() instanceof Building) {
        from = ((Building) actor.inside()).mainEntrance();
      }
      else {
        from = actor.at();
      }
      
      Object areaKey = map.pathCache.openGroupHandle(from);
      String key = null;
      RangeAccess access = null;
      
      if (areaKey != null) {
        key = "RA_"+areaKey+"_"+at;
        access = (RangeAccess) map.pathCache.getCache(key);
      }
      
      if (access == null && key != null) {
        
        final Batch <Tile> accessT = new Batch();
        Flood <Tile> flood = new Flood <Tile> () {
          protected void addSuccessors(Tile front) {
            for (Tile n : CityMap.adjacent(front, temp, map)) {
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
        access.tiles    = accessT.toArray(Tile.class);
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
    float rangeMissile = actor.type().rangeDist;
    float maxRange     = Nums.max(rangeMelee, rangeMissile);
    
    Pick <Tile> pick = new Pick();
    Box2D area = target.area();
    
    //  TODO:  It would be helpful if tiles were in strictly ascending order
    //  by distance.
    
    boolean report = false;
    if (report) {
      I.say("\nChecking tiles-");
      I.say("  Melee range:   "+rangeMelee  );
      I.say("  Missile range: "+rangeMissile);
      I.say("  Max. range:    "+maxRange    );
      I.say("  Target area:   "+area        );
    }
    
    for (Tile t : inRange) {
      if (Task.hasTaskFocus(t, jobType, actor)) continue;
      
      float dist = area.distance(t.x, t.y);
      if (report) I.say("  "+t+" dist: "+dist);
      
      if (dist > maxRange) continue;
      
      float rating = 0;
      if      (dist < rangeMelee  ) rating += actor.type().meleeDamage;
      else if (dist < rangeMissile) rating += actor.type().rangeDamage;
      pick.compare(t, rating);
    }
    
    if (pick.empty()) return null;
    
    boolean needsConfig = true;
    Tile t = pick.result();
    boolean standWall = t          .pathType() == PATH_WALLS;
    boolean targWall  = target.at().pathType() == PATH_WALLS;
    boolean canTouch  = standWall == targWall;
    
    if (currentTask == null) {
      currentTask = new TaskCombat(actor, target);
    }
    else if (currentTask.target == t) {
      needsConfig = false;
    }
    
    if (canTouch && area.distance(t.x, t.y) < rangeMelee) {
      currentTask.attackMode = ATTACK_MELEE;
    }
    else {
      currentTask.attackMode = ATTACK_RANGE;
    }
    
    if (needsConfig) return (TaskCombat) (
      currentTask.configTask(formation, null, t, jobType, 0)
    );
    else {
      return currentTask;
    }
  }
  
  

  boolean checkAndUpdateTask() {
    Task self = configCombat(actor, primary, actor.mission, this, type);
    if (self == null) return false;
    return super.checkAndUpdateTask();
  }
  
  
  void toggleFocus(boolean active) {
    super.toggleFocus(active);
    primary.setFocused(actor, active);
  }
  
  
  float actionRange() {
    if (attackMode == ATTACK_MELEE) return 0;
    if (attackMode == ATTACK_RANGE) return actor.type().rangeDist + 1;
    if (attackMode == ATTACK_FIRE ) return actor.type().rangeDist + 1;
    return -1;
  }
  
  
  protected void onTarget(Target other) {
    actor.performAttack(primary, attackMode == ATTACK_MELEE);
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  public String toString() {
    return "Fighting "+primary+" from "+target;
  }
}









