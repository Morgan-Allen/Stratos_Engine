



package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class FormationUtils {

  
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
      CityEvents.inflictDemands(siege, home, parent);
      CityEvents.signalVictory(home, siege, parent);
      parent.beginSecuring(home);
      return true;
    }
  }
  
  
  
  
  void updateGuardPoints(final Formation parent) {
    
    //
    //  First, check to see if an update is due:
    final Target focus = (Target) parent.secureFocus;
    final CityMap map = parent.map;
    final int updateTime = parent.lastUpdateTime;
    //this.map = parent.map;
    int nextUpdate = updateTime >= 0 ? (updateTime + 10) : 0;
    
    if (focus == null || map == null) {
      parent.guardPoints.clear();
      return;
    }
    if (parent.securePoint == focus.at() && map.time < nextUpdate) {
      return;
    }
    
    parent.securePoint = focus.at();
    parent.lastUpdateTime = map.time;
    Type type = focus.type();
    
    //
    //  In the case where we're defending a wall, try to spread out across
    //  the perimeter:
    if (type.isWall) {
      final int MAX_DIST = 16;
      final Pathing temp[] = new Pathing[9];
      
      Flood <Pathing> flood = new Flood <Pathing> () {
        
        protected void addSuccessors(Pathing front) {
          if (CityMap.distance(front, focus) > MAX_DIST) return;
          
          for (Pathing p : front.adjacent(temp, map)) {
            if (p == null || p.pathType() != PATH_WALLS) continue;
            tryAdding(p);
          }
        }
      };
      
      Series <Pathing> covered;
      if (type.isBuilding() && map.blocked(focus.at())) {
        covered = flood.floodFrom((Pathing) focus);
      }
      else {
        covered = flood.floodFrom(focus.at());
      }
      Batch <Tile> touched = new Batch();
      parent.guardPoints.clear();
      
      for (Pathing p : covered) {
        if (p.flaggedWith() != null || ! p.isTile()) continue;
        for (Pathing n : p.adjacent(temp, map)) {
          if (n != null && n.isTile()) {
            n.flagWith(covered);
            touched.add((Tile) n);
          }
        }
        p.flagWith(covered);
        touched.add((Tile) p);
        parent.guardPoints.add((Tile) p);
      }
      
      for (Tile t : touched) t.flagWith(null);
    }
  }
  
  
  
  static Tile standingPointRanks(Actor member, Formation parent) {
    
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
    
    Tile stands = map.tileAt(x, y);
    stands = Tile.nearestOpenTile(stands, map, AVG_FILE);
    return stands;
  }
  
  
  
  
  
}









