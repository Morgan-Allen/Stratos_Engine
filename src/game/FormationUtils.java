



package game;
import util.*;
import static game.CityMap.*;
import static game.CityCouncil.*;
import static game.GameConstants.*;



public class FormationUtils {
  
  
  static Actor findOfferRecipient(Formation parent) {
    CityCouncil council = parent.map.city.council;
    
    Actor monarch = council.memberWithRole(Role.MONARCH);
    if (monarch != null && monarch.onMap()) return monarch;
    
    Actor minister = council.memberWithRole(Role.PRIME_MINISTER);
    if (minister != null && monarch.onMap()) return monarch;
    
    Actor consort = council.memberWithRole(Role.CONSORT);
    if (consort != null && consort.onMap()) return consort;
    
    return null;
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
  
  
  
  static class Option { Object target; Tile secures; float rating; }
  
  static Option tacticalOptionFor(
    Object focus, CityMap map, Pathing pathFrom, boolean checkPathing
  ) {
    
    if (focus instanceof Formation) {
      Formation f = (Formation) focus;
      Target goes = f.pathGoes();
      if (f.away || goes == null) return null;
      if (f.powerSum() == 0 || ! f.active) return null;
      
      int power = f.powerSum();
      Tile secures = f.standPoint;
      if (power <= 0) return null;

      if (checkPathing) {
        boolean hasPath = map.pathCache.pathConnects(pathFrom, goes, true, true);
        if (! hasPath) return null;
      }
      
      float dist = CityMap.distance(goes, pathFrom);
      float rating = CityMap.distancePenalty(dist);
      
      Option o = new Option();
      o.target  = focus;
      o.secures = secures;
      o.rating  = rating;
      return o;
    }
    
    
    if (focus instanceof Element) {
      Element e = (Element) focus;
      if (e.destroyed()) return null;
      if (! e.type.isArmyOrWallsBuilding()) return null;
      
      if (checkPathing) {
        boolean hasPath = map.pathCache.pathConnects(pathFrom, e, true, true);
        if (! hasPath) return null;
      }
      
      Tile secures = e.centre();
      float dist = CityMap.distance(secures, pathFrom);
      float rating = CityMap.distancePenalty(dist);
      
      Option o = new Option();
      o.target  = focus;
      o.secures = secures;
      o.rating  = rating;
      return o;
    }
    
    return null;
  }
  
  
  static boolean updateTacticalTarget(Formation parent) {
    
    CityMap map    = parent.map;
    City    home   = parent.homeCity;
    Pathing from   = parent.pathFrom();
    Tile    stands = parent.standPoint;
    Object  focus  = parent.secureFocus;
    City    siege  = parent.secureCity;
    
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
    if (tacticalOptionFor(focus, map, from, true) != null) {
      return false;
    }
    
    //
    //  Otherwise, look for either an enemy formation to engage, or
    //  a building to tear down:
    Pick <Option> pick = new Pick();
    
    for (Formation f : map.city.formations) {
      Option o = tacticalOptionFor(f, map, from, false);
      if (o != null && o.secures != null) pick.compare(o, o.rating);
    }
    
    for (Building b : map.buildings) {
      Option o = tacticalOptionFor(b, map, from, false);
      if (o != null && o.secures != null) pick.compare(o, o.rating);
    }
    
    if (! pick.empty()) {
      Option o = pick.result();
      
      if (tacticalOptionFor(o.target, map, from, true) != null) {
        parent.beginSecuring(o.target, parent.facing, map);
        return true;
      }
      
      else {
        SiegeSearch search = new SiegeSearch(map, stands, o.secures);
        search.doSearch();
        Pathing path[] = (Pathing[]) search.fullPath(Pathing.class);
        for (Pathing p : path) {
          Tile t = (Tile) p;
          Element above = t.above;
          int pathT = t.pathType();
          
          if (above != null && (pathT == PATH_BLOCK || pathT == PATH_WALLS)) {
            parent.beginSecuring(above, parent.facing, map);
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
      CityEvents.imposeTerms(siege, home, parent);
      CityEvents.signalVictory(home, siege, parent);
      parent.beginSecuring(home);
      return true;
    }
  }
  
  
  
  
  static void updateGuardPoints(final Formation parent) {
    
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
    if (map.time < nextUpdate) {
      return;
    }
    
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
  
  
  static Tile standingPointPatrol(Actor member, Formation parent) {
    
    int span = MONTH_LENGTH, numRecruits = parent.recruits.size();
    int epoch = (parent.map.time / span) % numRecruits;
    
    int index = parent.recruits.indexOf(member);
    if (index == -1) return null;
    
    updateGuardPoints(parent);
    index = (index + epoch) % numRecruits;
    return parent.guardPoints.atIndex(index);
  }
  
  
  
  static Tile standingPointRanks(Actor member, Formation parent) {
    
    CityMap map = parent.map;
    Tile goes = parent.standPoint;
    if (goes == null || map == null) return null;
    
    int index = parent.recruits.indexOf(member);
    if (index == -1) return null;
    
    int ranks = AVG_RANKS   ;
    int file  = AVG_FILE    ;
    int x     = index % file;
    int y     = index / file;
    x += goes.x - (file  / 2);
    y += goes.y + (ranks / 2);
    
    x = Nums.clamp(x, map.size);
    y = Nums.clamp(y, map.size);
    
    Tile stands = map.tileAt(x, y);
    stands = Tile.nearestOpenTile(stands, map, AVG_FILE);
    return stands;
  }
  
  
}









