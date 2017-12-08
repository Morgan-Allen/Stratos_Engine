

package game;
import util.*;
import static game.City.*;
import static game.CityMap.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



public class ObjectiveConquer extends Formation.Objective {
  
  
  /**  Data fields, construction and save/load methods:
    */
  public ObjectiveConquer() {
    super();
  }
  
  
  public ObjectiveConquer(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Selecting large-scale tactical objectives:
    */
  boolean updateTacticalTarget(Formation parent) {
    
    CityMap map   = parent.map;
    City    home  = parent.homeCity;
    Tile    point = parent.securedPoint;
    Object  focus = parent.secureFocus;
    City    siege = parent.securedCity;
    
    //
    //  Determine whether your current target has been beaten yet-
    if (focus instanceof Formation) {
      Formation opposed = (Formation) focus;
      Tile secures = opposed.securedPoint;
      int  power   = opposed.powerSum();
      if (secures == point && power > 0) return false;
    }
    
    if (focus instanceof Building) {
      Building sieged = (Building) focus;
      if (! sieged.destroyed()) return false;
    }
    
    if (focus instanceof City) {
      return false;
    }
    
    //
    //  If you're beaten, turn around and go home:
    //  TODO:  Allow for retreat at partial strength!
    if (parent.powerSum() == 0) {
      CityEvents.enterHostility(siege, home, false, 1);
      CityEvents.signalVictory(siege, home, parent);
      parent.beginSecuring(home);
      return true;
    }
    
    //
    //  Otherwise, look for either an enemy formation to engage, or
    //  a building to tear down:
    class Option { Object target; Tile secures; }
    Pick <Option> pick = new Pick();
    
    for (Formation f : map.city.formations) {
      if (f.away || f.securedPoint == null) continue;
      
      Option o = new Option();
      o.secures = f.securedPoint;
      o.target  = f;
      float dist = CityMap.distance(o.secures, point);
      pick.compare(o, 0 - dist);
    }
    
    for (Building b : map.buildings) {
      if (b.type.category != Type.IS_ARMY_BLD) continue;
      
      Option o = new Option();
      o.secures = b.centre();
      o.target  = b;
      float dist = CityMap.distance(o.secures, point);
      pick.compare(o, 0 - dist);
    }
    
    if (! pick.empty()) {
      Option o = pick.result();
      parent.beginSecuring(o.secures, parent.facing, o.target);
      return true;
    }
    
    //
    //  If there are no targets left here, turn around and go home.
    else {
      CityEvents.enterHostility(siege, home, true, 1);
      if (siege != null && home.government != GOVERNMENT.BARBARIAN) {
        CityEvents.inflictDemands(siege, home, parent);
      }
      CityEvents.signalVictory(home, siege, parent);
      //
      //  TODO:  Handle recall of forces in a separate decision-pass?
      parent.beginSecuring(home);
      return true;
    }
  }
  
  
  
  
  /**  Handling individual actor behaviours:
    */
  void selectActorBehaviour(Actor a, Formation parent) {
    
    Actor target = a.inCombat() ? null : TaskCombat.findCombatTarget(a, parent);
    TaskCombat taskC = TaskCombat.configCombat(a, target, parent);
    if (taskC != null) {
      a.assignTask(taskC);
      return;
    }
    
    Tile sieges = a.inCombat() ? null : TaskCombat.findSiegeTarget(a, parent);
    TaskCombat taskS = TaskCombat.configCombat(a, sieges, parent);
    if (taskS != null) {
      a.assignTask(taskS);
      return;
    }
    
    Tile stands = parent.standLocation(a);
    if (stands != null) {
      a.embarkOnTarget(stands, 10, Task.JOB.MILITARY, parent);
      return;
    }
  }
  
  
  void actorUpdates(Actor a, Formation parent) {
    Actor target = a.inCombat() ? null : TaskCombat.findCombatTarget(a, parent);
    TaskCombat taskC = TaskCombat.configCombat(a, target, parent);
    if (taskC != null) {
      a.assignTask(taskC);
      return;
    }
  }
  
  
  void actorTargets(Actor a, Target other, Formation parent) {
    return;
  }
  
  
  /*
  void actorTargets(Actor a, Target other, Formation parent) {
    if (a.inCombat() && other instanceof Actor) {
      a.performAttack((Actor) other);
    }
    if (a.inCombat() && other instanceof Tile) {
      Building siege = (Building) ((Tile) other).above;
      a.performAttack(siege);
    }
    return;
  }
  
  
  Actor findCombatTarget(Actor member, Formation parent) {
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
    
    for (Actor other : others) if (Formation.hostile(other, member, map)) {
      float distW = CityMap.distance(other.at(), member.at());
      float distF = CityMap.distance(other.at(), point);
      if (distF > range + 1) continue;
      if (distW > range + 1) continue;
      pick.compare(other, 0 - distW);
    }
    
    return pick.result();
  }
  
  
  Tile findSiegeTarget(Actor member, Formation parent) {
    
    CityMap map = parent.map;
    Object focus = parent.secureFocus;
    if (! (focus instanceof Building)) return null;
    
    Building sieged = (Building) focus;
    if (sieged.destroyed()) return null;
    
    Tile c = sieged.at();
    Pick <Tile> pick = new Pick();
    
    for (Coord p : Visit.perimeter(
      c.x, c.y, sieged.type.wide, sieged.type.high
    )) {
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
    
    Tile goes = pick.result();
    if (goes == null) return null;
    
    if (! (goes.above instanceof Building)) {
      I.complain("PROBLEMMMM");
    }
    return goes;
  }
  //*/
  
}



