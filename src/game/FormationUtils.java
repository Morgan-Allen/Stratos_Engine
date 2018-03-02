



package game;
import util.*;
import static game.AreaMap.*;
import static game.CityCouncil.*;
import static game.GameConstants.*;



public class FormationUtils {
  
  
  static float rateCampingPoint(Tile t, Mission parent) {
    if (t == null || parent == null || parent.map() == null) return -1;
    
    AreaMap map = parent.map();
    Pathing from = parent.pathFrom();
    float rating = 0;
    boolean blocked = false;
    
    for (Tile n : map.tilesUnder(
      t.x - (AVG_RANKS / 2),
      t.y - (AVG_FILE  / 2),
      AVG_RANKS,
      AVG_FILE
    )) {
      if (map.blocked(n)) {
        rating -= 1;
      }
      else if (! map.pathCache.pathConnects(from, n, false, false)) {
        blocked = true;
      }
      else {
        rating += 1;
      }
    }
    if (blocked || rating <= 0) return -1;
    
    rating *= distancePenalty(from, t);
    return rating;
  }
  
  
  static Tile findCampingPoint(final Mission parent) {
    if (parent == null || parent.map() == null) return null;
    
    final AreaMap map = parent.map();
    final Tile init = Tile.nearestOpenTile(parent.standPoint(), map);
    if (init == null) return null;
    
    final Tile temp[] = new Tile[9];
    final Pick <Tile> pick = new Pick(0);
    
    Flood <Tile> flood = new Flood <Tile> () {
      protected void addSuccessors(Tile front) {
        for (Tile n : AreaMap.adjacent(front, temp, map)) {
          if (map.blocked(n) || n.flaggedWith() != null) continue;
          tryAdding(n);
          
          float rating = rateCampingPoint(n, parent);
          pick.compare(n, rating);
        }
      }
    };
    flood.floodFrom(init);
    return pick.result();
  }
  
  
  //  TODO:  Move this to the Council class?
  
  static Actor findOfferRecipient(Mission parent) {
    CityCouncil council = parent.awayCity().council;
    
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
    
    public SiegeSearch(AreaMap map, Tile init, Tile dest) {
      super(map, init, dest, -1);
    }
    
    protected Pathing[] adjacent(Pathing spot) {
      AreaMap.adjacent((Tile) spot, tempT, map);
      return tempT;
    }
    
    protected boolean canEnter(Pathing spot) {
      if (super.canEnter(spot)) return true;
      Element above = ((Tile) spot).above;
      if (above != null && ! above.type().isNatural()) return true;
      return false;
    }
    
    protected float cost(Pathing prior, Pathing spot) {
      Element above = ((Tile) spot).above;
      if (above != null && ! above.type().isNatural()) return 20;
      return super.cost(prior, spot);
    }
  }
  
  
  
  static class Option { Object target; Tile secures; float rating; }
  
  static Option tacticalOptionFor(
    Object focus, AreaMap map, Pathing pathFrom, boolean checkPathing
  ) {
    
    if (focus instanceof Mission) {
      Mission f = (Mission) focus;
      Target goes = f.pathGoes();
      if (f.away() || goes == null) return null;
      if (f.powerSum() == 0 || ! f.active()) return null;
      
      int power = f.powerSum();
      Tile secures = f.standPoint();
      if (power <= 0) return null;

      if (checkPathing) {
        boolean hasPath = map.pathCache.pathConnects(pathFrom, goes, true, true);
        if (! hasPath) return null;
      }
      
      float dist = AreaMap.distance(goes, pathFrom);
      float rating = AreaMap.distancePenalty(dist);
      
      Option o = new Option();
      o.target  = focus;
      o.secures = secures;
      o.rating  = rating;
      return o;
    }
    
    
    if (focus instanceof Element) {
      Element e = (Element) focus;
      if (e.destroyed()) return null;
      if (! e.type().isArmyOrWallsBuilding()) return null;
      
      if (checkPathing) {
        boolean hasPath = map.pathCache.pathConnects(pathFrom, e, true, true);
        if (! hasPath) return null;
      }
      
      Tile secures = e.centre();
      float dist = AreaMap.distance(secures, pathFrom);
      float rating = AreaMap.distancePenalty(dist);
      
      Option o = new Option();
      o.target  = focus;
      o.secures = secures;
      o.rating  = rating;
      return o;
    }
    
    return null;
  }
  
  
  static boolean updateTacticalTarget(Mission parent) {
    
    AreaMap map    = parent.map();
    Base    home   = parent.base();
    Pathing from   = parent.pathFrom();
    Tile    stands = parent.standPoint();
    Object  focus  = parent.focus();
    Base    sieges = parent.awayCity();
    boolean envoy  = parent.escorted.size() > 0;
    
    //
    //  If this is a diplomatic mission, stay in camp unless and until terms
    //  are refused-
    if (parent.hasTerms() && parent.termsAccepted) {
      parent.setFocus(home);
      return true;
    }
    
    if (parent.hasTerms() && envoy && ! parent.termsRefused) {
      if (rateCampingPoint(stands, parent) > 0) {
        return false;
      }
      Tile campPoint = findCampingPoint(parent);
      parent.setFocus(campPoint, parent.facing(), map);
      return true;
    }
    
    //
    //  If you're beaten, disband or turn around and go home:
    if (parent.casualtyLevel() > MAX_CASUALTIES / 100f) {
      CityEvents.enterHostility(sieges, home, false, 1);
      CityEvents.signalVictory(sieges, home, parent);
      parent.setFocus(home);
      return true;
    }
    if (parent.powerSum() == 0) {
      parent.disbandFormation();
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
    
    for (Base c : map.bases) for (Mission f : c.missions) {
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
        parent.setFocus(o.target, parent.facing(), map);
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
            parent.setFocus(above, parent.facing(), map);
            return true;
          }
        }
        return false;
      }
    }
    
    //
    //  If there are no targets left here, declare victory and go home:
    else {
      CityEvents.enterHostility(sieges, home, true, 1);
      CityEvents.imposeTerms(sieges, home, parent);
      CityEvents.signalVictory(home, sieges, parent);
      parent.setFocus(home);
      return true;
    }
  }
  
  
  static void updateGuardPoints(final Mission parent) {
    
    //
    //  First, check to see if an update is due:
    final Target focus = (Target) parent.focus();
    final AreaMap map = parent.map();
    final int updateTime = parent.lastUpdateTime;
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
          if (AreaMap.distance(front, focus) > MAX_DIST) return;
          
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
  
  
  static Tile standingPointPatrol(Actor member, Mission parent) {
    
    int span = MONTH_LENGTH, numRecruits = parent.recruits.size();
    int epoch = (parent.map().time / span) % numRecruits;
    
    int index = parent.recruits.indexOf(member);
    if (index == -1) return null;
    
    updateGuardPoints(parent);
    index = (index + epoch) % numRecruits;
    return parent.guardPoints.atIndex(index);
  }
  
  
  static Tile standingPointRanks(Actor member, Mission parent) {
    
    AreaMap map = parent.map();
    Tile goes = parent.standPoint();
    if (goes == null || map == null) return null;
    
    int index = parent.recruits.indexOf(member);
    if (index == -1) {
      index = parent.escorted.indexOf(member);
      if (index != -1) index += parent.recruits.size();
    }
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









