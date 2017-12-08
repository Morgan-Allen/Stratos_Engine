

package game;
import util.*;
import static game.CityMap.*;
import static game.City.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



public class TaskCombat extends Task {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public TaskCombat(Actor actor) {
    super(actor);
  }
  
  
  public TaskCombat(Session s) throws Exception {
    super(s);
  }


  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  static TaskCombat configCombat(Actor a, Target other, Employer e) {
    if (a == null || other == null) return null;
    
    TaskCombat task = new TaskCombat(a);
    if (task.configTask(e, null, other, JOB.COMBAT, 0) != null) {
      return task;
    }
    
    return null;
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
  
  
  static Actor findCombatTarget(Actor member, Formation parent) {
    CityMap map = parent.map;
    Tile point = parent.securedPoint;
    
    if (map == null) return null;
    Pick <Actor> pick = new Pick();
    
    float seeBonus = AVG_FILE;
    float range = member.type.sightRange + seeBonus;
    
    Series <Actor> others = map.actors;
    if (others.size() > 100 || true) {
      others = map.actorsInRange(member.at(), range);
    }
    
    for (Actor other : others) if (hostile(other, member, map)) {
      float distW = CityMap.distance(other.at(), member.at());
      float distF = CityMap.distance(other.at(), point);
      if (distF > range + 1) continue;
      if (distW > range + 1) continue;
      if (! map.pathCache.pathConnects(member.at(), other.at())) continue;
      
      pick.compare(other, 0 - distW);
    }
    
    return pick.result();
  }
  
  
  static Tile findSiegeTarget(Actor member, Formation parent) {
    
    CityMap map = parent.map;
    Object focus = parent.secureFocus;
    if (! (focus instanceof Element)) return null;
    
    Element sieged = (Element) focus;
    if (sieged.destroyed()) return null;
    
    Pick <Tile> pick = new Pick();
    for (Tile p : sieged.perimeter(map)) {
      if (map.blocked(p.x, p.y)) continue;
      Tile best = null;
      
      for (int dir : T_ADJACENT) {
        Tile tile = map.tileAt(p.x + T_X[dir], p.y + T_Y[dir]);
        if (tile == null || tile.above != sieged) continue;
        if (Task.hasTaskFocus(tile, Task.JOB.COMBAT)) continue;
        
        best = tile;
        break;
      }
      
      float dist = CityMap.distance(member.at(), best);
      pick.compare(best, 0 - dist);
    }
    
    return pick.result();
  }
  
  
  
  
  
  /**  Behavioural routines-
    */
  protected void onTarget(Target other) {
    if (actor.inCombat() && other instanceof Actor) {
      actor.performAttack((Actor) other);
    }
    if (actor.inCombat() && other instanceof Tile) {
      Element siege = (Element) ((Tile) other).above;
      actor.performAttack(siege);
    }
  }
  
  
  
}









