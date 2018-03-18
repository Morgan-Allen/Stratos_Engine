



package game;
import util.*;
import static game.Area.*;
import static game.BaseCouncil.*;
import static game.GameConstants.*;



public class MissionUtils {
  
  
  static float rateCampingPoint(AreaTile t, Mission parent) {
    if (t == null || parent == null || parent.map() == null) return -1;
    
    Area map = parent.map();
    Pathing from = parent.pathFrom();
    float rating = 0;
    boolean blocked = false;
    
    for (AreaTile n : map.tilesUnder(
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
  
  
  static AreaTile findCampingPoint(final Mission parent) {
    if (parent == null || parent.map() == null) return null;
    
    final Area map = parent.map();
    final AreaTile init = AreaTile.nearestOpenTile(parent.standPoint(), map);
    if (init == null) return null;
    
    final AreaTile temp[] = new AreaTile[9];
    final Pick <AreaTile> pick = new Pick(0);
    
    Flood <AreaTile> flood = new Flood <AreaTile> () {
      protected void addSuccessors(AreaTile front) {
        for (AreaTile n : Area.adjacent(front, temp, map)) {
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
    BaseCouncil council = parent.awayCity().council;
    
    Actor monarch = council.memberWithRole(Role.MONARCH);
    if (monarch != null && monarch.onMap()) return monarch;
    
    Actor minister = council.memberWithRole(Role.PRIME_MINISTER);
    if (minister != null && monarch.onMap()) return monarch;
    
    Actor consort = council.memberWithRole(Role.CONSORT);
    if (consort != null && consort.onMap()) return consort;
    
    return null;
  }
  
  
  
  static class SiegeSearch extends ActorPathSearch {
    
    AreaTile tempT[] = new AreaTile[9];
    
    public SiegeSearch(Area map, AreaTile init, AreaTile dest) {
      super(map, init, dest, -1);
    }
    
    protected Pathing[] adjacent(Pathing spot) {
      Area.adjacent((AreaTile) spot, tempT, map);
      return tempT;
    }
    
    protected boolean canEnter(Pathing spot) {
      if (super.canEnter(spot)) return true;
      Element above = ((AreaTile) spot).above;
      if (above != null && ! above.type().isNatural()) return true;
      return false;
    }
    
    protected float cost(Pathing prior, Pathing spot) {
      Element above = ((AreaTile) spot).above;
      if (above != null && ! above.type().isNatural()) return 20;
      return super.cost(prior, spot);
    }
  }
  
  
  
  static class Option { Object target; AreaTile secures; float rating; }
  
  static Option tacticalOptionFor(
    Object focus, Area map, Pathing pathFrom, boolean checkPathing
  ) {
    
    if (focus instanceof Mission) {
      Mission f = (Mission) focus;
      Target goes = f.pathGoes();
      if (f.away() || goes == null) return null;
      if (f.powerSum() == 0 || ! f.active()) return null;
      
      int power = f.powerSum();
      AreaTile secures = f.standPoint();
      if (power <= 0) return null;

      if (checkPathing) {
        boolean hasPath = map.pathCache.pathConnects(pathFrom, goes, true, true);
        if (! hasPath) return null;
      }
      
      float dist = Area.distance(goes, pathFrom);
      float rating = Area.distancePenalty(dist);
      
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
      
      AreaTile secures = e.centre();
      float dist = Area.distance(secures, pathFrom);
      float rating = Area.distancePenalty(dist);
      
      Option o = new Option();
      o.target  = focus;
      o.secures = secures;
      o.rating  = rating;
      return o;
    }
    
    return null;
  }
  
  
  static boolean updateTacticalTarget(Mission parent) {
    
    Area     map    = parent.map();
    Base     home   = parent.base();
    Pathing  from   = parent.pathFrom();
    AreaTile stands = parent.standPoint();
    Object   focus  = parent.focus();
    Base     sieges = parent.awayCity();
    boolean  envoy  = parent.escorted.size() > 0;
    
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
      AreaTile campPoint = findCampingPoint(parent);
      parent.setFocus(campPoint, parent.facing(), map);
      return true;
    }
    
    //
    //  If you're beaten, disband or turn around and go home:
    if (parent.casualtyLevel() > MAX_CASUALTIES / 100f) {
      BaseEvents.enterHostility(sieges, home, false, 1);
      BaseEvents.signalVictory(sieges, home, parent);
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
          AreaTile t = (AreaTile) p;
          Element above = t.above;
          int pathT = t.pathType();
          
          if (above != null && (pathT == Type.PATH_BLOCK || pathT == Type.PATH_WALLS)) {
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
      BaseEvents.enterHostility(sieges, home, true, 1);
      BaseEvents.imposeTerms(sieges, home, parent);
      BaseEvents.signalVictory(home, sieges, parent);
      parent.setFocus(home);
      return true;
    }
  }
  
  
  static void updateGuardPoints(final Mission parent) {
    
    //
    //  First, check to see if an update is due:
    final Target focus = (Target) parent.focus();
    final Area map = parent.map();
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
          if (Area.distance(front, focus) > MAX_DIST) return;
          
          for (Pathing p : front.adjacent(temp, map)) {
            if (p == null || p.pathType() != Type.PATH_WALLS) continue;
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
      Batch <AreaTile> touched = new Batch();
      parent.guardPoints.clear();
      
      for (Pathing p : covered) {
        if (p.flaggedWith() != null || ! p.isTile()) continue;
        for (Pathing n : p.adjacent(temp, map)) {
          if (n != null && n.isTile()) {
            n.flagWith(covered);
            touched.add((AreaTile) n);
          }
        }
        p.flagWith(covered);
        touched.add((AreaTile) p);
        parent.guardPoints.add((AreaTile) p);
      }
      
      for (AreaTile t : touched) t.flagWith(null);
    }
  }
  
  
  static AreaTile standingPointPatrol(Actor member, Mission parent) {
    
    int span = DAY_LENGTH, numRecruits = parent.recruits.size();
    int epoch = (parent.map().time / span) % numRecruits;
    
    int index = parent.recruits.indexOf(member);
    if (index == -1) return null;
    
    updateGuardPoints(parent);
    index = (index + epoch) % numRecruits;
    return parent.guardPoints.atIndex(index);
  }
  
  
  static AreaTile standingPointRanks(Actor member, Mission parent) {
    
    Area map = parent.map();
    AreaTile goes = parent.standPoint();
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
    
    AreaTile stands = map.tileAt(x, y);
    stands = AreaTile.nearestOpenTile(stands, map, AVG_FILE);
    return stands;
  }
  
  
}









