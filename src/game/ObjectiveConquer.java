

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
  boolean checkTacticalTargetValid(
    Object focus, CityMap map, Tile entry, Tile current
  ) {
    if (focus instanceof Formation) {
      Formation opposed = (Formation) focus;
      Tile secures = opposed.securePoint;
      int  power   = opposed.powerSum();
      boolean hasPath = map.pathCache.pathConnects(entry, secures);
      if (hasPath && secures == current && power > 0) return true;
    }
    
    if (focus instanceof Element) {
      Element sieged = (Element) focus;
      boolean hasPath = false;
      for (Tile t : sieged.perimeter(map)) {
        if (map.pathCache.pathConnects(entry, t)) hasPath = true;
      }
      if (hasPath && ! sieged.destroyed()) return true;
    }
    
    if (focus instanceof City) {
      return true;
    }
    
    return false;
  }
  
  
  boolean updateTacticalTarget(Formation parent) {
    
    CityMap map   = parent.map;
    City    home  = parent.homeCity;
    Tile    entry = parent.entryPoint;
    Tile    point = parent.securePoint;
    Object  focus = parent.secureFocus;
    City    siege = parent.secureCity;
    
    //
    //  If you're beaten, turn around and go home:
    //  TODO:  Allow for retreat at partial strength!
    //  TODO:  If you're already home, disband and/or flee...
    if (parent.powerSum() == 0) {
      CityEvents.enterHostility(siege, home, false, 1);
      CityEvents.signalVictory(siege, home, parent);
      parent.beginSecuring(home);
      return true;
    }
    
    //
    //  Determine whether your current target has been beaten yet-
    if (checkTacticalTargetValid(focus, map, entry, point)) {
      return false;
    }
    
    //
    //  Otherwise, look for either an enemy formation to engage, or
    //  a building to tear down:
    class Option { Object target; Tile secures; }
    Pick <Option> pick = new Pick();
    
    for (Formation f : map.city.formations) {
      if (f.away || f.securePoint == null) continue;
      if (f.powerSum() == 0 || ! f.active) continue;
      
      Option o = new Option();
      o.secures = f.securePoint;
      o.target  = f;
      float dist = CityMap.distance(o.secures, point);
      pick.compare(o, 0 - dist);
    }
    
    for (Building b : map.buildings) {
      if (b.destroyed() || ! b.type.isArmyOrWallsBuilding()) continue;
      
      Option o = new Option();
      o.secures = b.centre();
      o.target  = b;
      float dist = CityMap.distance(o.secures, point);
      pick.compare(o, 0 - dist);
    }
    
    if (! pick.empty()) {
      Option o = pick.result();
      
      if (checkTacticalTargetValid(o.target, map, entry, o.secures)) {
        parent.beginSecuring(o.secures, parent.facing, o.target, map);
        return true;
      }
      
      else {
        SiegeSearch search = new SiegeSearch(map, entry, o.secures);
        search.doSearch();
        Pathing path[] = (Pathing[]) search.fullPath(Pathing.class);
        for (Pathing p : path) {
          Tile t = (Tile) p;
          Element above = t.above;
          int pathT = t.pathType();
          
          if (above != null && (pathT == PATH_BLOCK || pathT == PATH_WALLS)) {
            parent.beginSecuring(t, parent.facing, above, map);
            return true;
          }
        }
        return false;
      }
    }
    
    //
    //  If there are no targets left here, declare victory and go home:
    else {
      CityEvents.enterHostility(siege, home, true, 1);
      if (siege != null && home.government != GOVERNMENT.BARBARIAN) {
        CityEvents.inflictDemands(siege, home, parent);
      }
      CityEvents.signalVictory(home, siege, parent);
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
  Tile standLocation(Actor member, Formation parent) {
    
    CityMap map = parent.map;
    Tile c = parent.securePoint;
    if (c == null || map == null) return null;
    
    int index = parent.recruits.indexOf(member);
    if (index == -1) return null;
    
    int ranks = AVG_RANKS   ;
    int file  = AVG_FILE    ;
    int x     = index % file;
    int y     = index / file;
    x += c.x - (file  / 2);
    y += c.y + (ranks / 2);
    
    x = Nums.clamp(x, map.size);
    y = Nums.clamp(y, map.size);
    
    //  TODO:  Find the nearest path-accessible point here!
    
    return map.tileAt(x, y);
  }
  
  
  void selectActorBehaviour(Actor a, Formation parent) {
    
    TaskCombat taskC = a.inCombat() ? null : TaskCombat.nextReaction(a, parent);
    if (taskC != null) {
      a.assignTask(taskC);
      return;
    }
    
    TaskCombat taskS = a.inCombat() ? null : TaskCombat.nextSieging(a, parent);
    if (taskS != null) {
      a.assignTask(taskS);
      return;
    }
    
    Tile stands = standLocation(a, parent);
    if (stands != null) {
      a.embarkOnTarget(stands, 10, Task.JOB.MILITARY, parent);
      return;
    }
  }
  
  
  void actorUpdates(Actor a, Formation parent) {
    TaskCombat taskC = a.inCombat() ? null : TaskCombat.nextReaction(a, parent);
    if (taskC != null) {
      a.assignTask(taskC);
      return;
    }
  }
  
  
  void actorTargets(Actor a, Target other, Formation parent) {
    return;
  }
  
}



