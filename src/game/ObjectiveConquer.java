

package game;
import util.*;
import static game.City.*;
import static game.CityMap.*;
import static game.GameConstants.*;



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
    
    if (focus instanceof Element) {
      Element sieged = (Element) focus;
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
      if (f.away || f.securedPoint == null || ! f.active) continue;
      
      Option o = new Option();
      o.secures = f.securedPoint;
      o.target  = f;
      float dist = CityMap.distance(o.secures, point);
      pick.compare(o, 0 - dist);
    }
    
    for (Building b : map.buildings) {
      if (! b.type.isArmyOrWallsBuilding()) continue;
      
      Option o = new Option();
      o.secures = b.centre();
      o.target  = b;
      float dist = CityMap.distance(o.secures, point);
      pick.compare(o, 0 - dist);
    }
    
    if (! pick.empty()) {
      Option o = pick.result();
      
      //  TODO:  There may be a problem here- once you start securing
      //  the siege point, all subsequent path-checks will be made from
      //  there, and your troops may not have room to stand.
      
      SiegeSearch search = new SiegeSearch(map, point, o.secures);
      search.doSearch();
      
      Pathing path[] = (Pathing[]) search.fullPath(Pathing.class);
      for (Pathing p : path) {
        Tile t = (Tile) p;
        Element above = t.above;
        if (above != null && map.blocked(t)) {
          parent.beginSecuring(t, parent.facing, above);
          return true;
        }
      }
      return false;
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
  
  
  static class SiegeSearch extends ActorPathSearch {
    
    Tile tempT[] = new Tile[9];
    
    public SiegeSearch(CityMap map, Tile init, Tile dest) {
      super(map, init, dest, -1);
    }
    
    protected Pathing[] adjacent(Pathing spot) {
      CityMap.adjacent((Tile) spot, tempT, map);
      return tempT;
    }
    
    protected boolean canEnter(Pathing spot) {
      if (super.canEnter(spot)) return true;
      Element above = ((Tile) spot).above;
      if (above != null && ! above.type.isNatural()) return true;
      return false;
    }
    
    protected float cost(Pathing prior, Pathing spot) {
      Element above = ((Tile) spot).above;
      if (above != null && ! above.type.isNatural()) return 20;
      return super.cost(prior, spot);
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
  
}



