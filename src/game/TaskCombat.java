

package game;
import util.*;
import static game.CityMap.*;
import static game.City.*;
import static game.GameConstants.*;



public class TaskCombat extends Task {
  
  
  /**  Data fields, construction and save/load methods-
    */
  Tile stands = null;
  boolean inMelee = true;
  
  
  public TaskCombat(Actor actor) {
    super(actor);
  }
  
  
  public TaskCombat(Session s) throws Exception {
    super(s);
    stands = CityMap.loadTile(actor.map, s);
    inMelee = s.loadBool();
  }


  public void saveState(Session s) throws Exception {
    super.saveState(s);
    CityMap.saveTile(stands, actor.map, s);
    s.saveBool(inMelee);
  }
  
  
  
  /**  Supplemental config and setup methods-
    */
  static TaskCombat actorCombat(Actor member, Formation parent) {
    
    //  TODO:  Make the anchor the actor's current stand-point instead?
    
    CityMap map = parent.map;
    Tile anchor = parent.securePoint;
    
    if (map == null) return null;
    Pick <Actor> pick = new Pick();
    
    Tile from = member.at();
    float seeBonus = AVG_FILE;
    float range = member.type.sightRange + seeBonus;
    float attackRange = member.type.rangeDist;
    
    Series <Actor> others = map.actorsInRange(from, range);
    for (Actor other : others) if (hostile(other, member, map)) {
      Tile goes = other.at();
      float distW = CityMap.distance(goes, from  );
      float distF = CityMap.distance(goes, anchor);
      if (distF > range + 1) continue;
      if (distW > range + 1) continue;
      
      boolean path = map.pathCache.openPathConnects(from, goes);
      if (distW > attackRange && ! path) continue;
      
      pick.compare(other, 0 - distW);
    }
    
    Actor struck = pick.result();
    TaskCombat task = new TaskCombat(member);
    task.stands = struck.at();
    
    if (task.configTask(parent, null, struck, JOB.COMBAT, 0) != null) {
      return task;
    }
    else return null;
  }
  
  
  static TaskCombat siegeCombat(Actor member, Formation parent) {
    //if (true) return null;
    
    final CityMap map = parent.map;
    Object focus = parent.secureFocus;
    if (! (focus instanceof Element)) return null;
    
    final Element sieged = (Element) focus;
    if (sieged.destroyed()) return null;
    
    final Box2D area   = sieged.area().expandBy(-0.5f);
    final Tile  from   = member.at();
    final Tile  temp[] = new Tile[9];
    final float range  = Nums.max(member.type.rangeDist, 1);
    
    final Vars.Ref <Tile> result = new Vars.Ref();
    
    /*
    for (Tile t : sieged.perimeter(map)) {
      if (Task.hasTaskFocus(t, JOB.COMBAT)) continue;
      if (map.pathCache.openPathConnects(from, t)) {
        result.value = t;
        break;
      }
    }
    //*/
    
    //*
    Flood <Tile> flood = new Flood <Tile> () {
      protected void addSuccessors(Tile front) {
        if (result.value != null) return;
        for (Tile n : CityMap.adjacent(front, temp, map)) {
          if (n == null || area.distance(n.x, n.y) > range) continue;
          tryAdding(n);
          
          boolean reach = map.pathCache.openPathConnects(from, n);
          if (n.above == sieged || ! reach    ) continue;
          if (Task.hasTaskFocus(n, JOB.COMBAT)) continue;
          
          result.value = n;
          return;
        }
      }
    };
    flood.floodFrom(sieged.centre());
    //*/
    
    TaskCombat task = new TaskCombat(member);
    task.stands = result.value;
    task.inMelee = CityMap.distance(task.stands, from) <= 1;
    
    if (task.configTask(parent, null, sieged, JOB.COMBAT, 0) != null) {
      return task;
    }
    else return null;
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
  
  
  static boolean checkMeleeBetter(Actor a) {
    return a.type.meleeDamage > a.type.rangeDamage;
  }
  
  
  
  /**  Behavioural routines-
    */
  boolean checkAndUpdateTask() {
    inMelee = checkMeleeBetter(actor);
    return super.checkAndUpdateTask();
  }
  
  
  void toggleFocus(boolean active) {
    super.toggleFocus(active);
    if (stands == null) return;
    stands.setFocused(actor, active);
  }
  
  
  float actionRange() {
    return inMelee ? 0.1f : actor.type.rangeDist;
  }


  protected void onTarget(Target other) {
    Type type = other.type();
    if (type.isActor()) {
      actor.performAttack((Actor) other, inMelee);
    }
    if (type.isBuilding() || type.isFixture()) {
      Element siege = (Element) ((Tile) other).above;
      actor.performAttack(siege, inMelee);
    }
  }
  
}




