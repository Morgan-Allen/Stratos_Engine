

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
  
  Element primary;
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
  
  
  
  static boolean hostile(Actor a, Actor b, CityMap map) {
    City CA = a.homeCity, CB = b.homeCity;
    if (CA == null) CA = map.city;
    if (CB == null) CB = map.city;
    if (CA == CB  ) return false;
    POSTURE r = CA.posture(CB);
    if (r == POSTURE.ENEMY) return true;
    return false;
  }
  
  
  static float attackPower(Actor a) {
    if (a.state >= Actor.STATE_DEAD) return 0;
    float stats = Nums.max(a.type.meleeDamage, a.type.rangeDamage);
    stats += a.type.armourClass;
    stats /= Nums.max(AVG_MELEE, AVG_MISSILE) + AVG_DEFEND;
    stats *= 1 - ((a.injury + a.hunger) / a.type.maxHealth);
    return stats;
  }
  
  
  
  static TaskCombat nextSieging(Actor actor, Formation formation) {
    if (formation.secureFocus instanceof Element) {
      Element e = (Element) formation.secureFocus;
      if (e.type.isActor()) return null;
      return configCombat(actor, e, formation, null);
    }
    else return null;
  }
  
  
  //  TODO:  Allow for iteration over members of a target formation, or whatever
  //  your comrades are currently fighting.
  
  
  static TaskCombat nextReaction(Actor actor, Formation formation) {
    
    //  TODO:  Allow for a null formation to be passed in here.
    Tile from = actor.at();
    CityMap map = actor.map;
    Tile anchor = formation.objective.standLocation(actor, formation);
    
    //  TODO:  Grant a vision bonus for higher ground?
    float noticeBonus = AVG_FILE;
    float noticeRange = actor.type.sightRange + noticeBonus;
    Series <Actor> others = map.actorsInRange(from, noticeRange);
    
    class Option { Actor other; float rating; };
    List <Option> options = new List <Option> () {
      protected float queuePriority(Option o) { return o.rating; }
    };
    
    for (Actor other : others) if (hostile(other, actor, map)) {
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
      TaskCombat c = configCombat(actor, o.other, formation, null);
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
  
  
  static TaskCombat configCombat(
    final Actor actor, final Element target,
    Formation formation, TaskCombat currentTask
  ) {
    if (! target.onMap()) return null;
    Tile inRange[] = null;
    
    if (currentTask != null) {
      inRange = new Tile[] { actor.at(), (Tile) currentTask.target };
    }
    else {
      final CityMap map    = actor.map;
      final Tile    from   = actor.at();
      final Tile    at     = target.at();
      final Box2D   area   = target.area();
      final int     range  = MAX_RANGE;
      final Tile    temp[] = new Tile[9];
      
      //  TODO:  You may want to key by range, not just by target/area.
      
      Object areaKey = map.pathCache.openGroupHandle(from);
      String key = "RA_"+areaKey+"_"+at;
      RangeAccess access = (RangeAccess) map.pathCache.getCache(key);
      
      if (access == null) {
        
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
      
      inRange = access.tiles;
    }
    
    float rangeMelee   = 1.5f;
    float rangeMissile = actor.type.rangeDist;
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
      if (Task.hasTaskFocus(t, JOB.COMBAT, actor)) continue;
      
      float dist = area.distance(t.x, t.y);
      if (report) I.say("  "+t+" dist: "+dist);
      
      if (dist > maxRange) continue;
      
      float rating = 0;
      if      (dist < rangeMelee  ) rating += actor.type.meleeDamage;
      else if (dist < rangeMissile) rating += actor.type.rangeDamage;
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
      currentTask.configTask(formation, null, t, JOB.COMBAT, 0)
    );
    else {
      return currentTask;
    }
  }
  
  

  boolean checkAndUpdateTask() {
    Task self = configCombat(actor, primary, actor.formation, this);
    if (self == null) return false;
    return super.checkAndUpdateTask();
  }
  
  
  void toggleFocus(boolean active) {
    super.toggleFocus(active);
    primary.setFocused(actor, active);
  }
  
  
  float actionRange() {
    if (attackMode == ATTACK_MELEE) return 0;
    if (attackMode == ATTACK_RANGE) return actor.type.rangeDist + 1;
    if (attackMode == ATTACK_FIRE ) return actor.type.rangeDist + 1;
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









