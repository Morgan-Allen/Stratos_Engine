

package game;
import static game.GameConstants.*;
import game.World.Journey;
import util.*;



public class MissionForStrike extends Mission {
  
  final static Object
    LOSS    = new Object(),
    VICTORY = new Object();
  
  
  List <Actor> casualties = new List();
  
  
  public MissionForStrike(Base belongs) {
    super(OBJECTIVE_STRIKE, belongs);
  }
  
  
  public MissionForStrike(Session s) throws Exception {
    super(s);
    s.loadObjects(casualties);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(casualties);
  }
  
  
  public boolean allowsFocus(Object newFocus) {
    if (newFocus instanceof Building) return true;
    if (newFocus instanceof Actor   ) return true;
    return false;
  }
  
  
  void update() {
    super.update();
    
    if (! complete()) {
      
      if (worldFocus() != null && ! onWrongMap()) {
        Base sieges = worldFocus(), home = homeBase();
        final Target focus = localFocus();
        Object update = updateTacticalTarget(focus);
        
        if (update == LOSS) {
          WorldEvents.enterHostility(sieges, home, false, 1);
          WorldEvents.signalVictory(sieges, home, this);
          setMissionComplete(false);
        }
        else if (update == VICTORY) {
          WorldEvents.enterHostility(sieges, home, true, 1);
          WorldEvents.imposeTerms(sieges, home, this);
          WorldEvents.signalVictory(home, sieges, this);
          setMissionComplete(true);
        }
        else {
          setLocalFocus((Target) update);
        }
      }
      
      else if (localFocus() instanceof Element) {
        Element e = (Element) localFocus();
        if (TaskCombat.beaten(e)) setMissionComplete(true);
        else if (! e.onMap()) setMissionComplete(false);
      }
    }
  }
  
  
  void handleOffmapArrival(Base goes, World.Journey journey) {
    if (goes == worldFocus()) {
      WorldEvents.handleInvasion(this, goes, journey);
    }
  }
  
  
  void handleOffmapDeparture(Base from, Journey journey) {
    return;
  }
  
  
  public Task nextLocalMapBehaviour(Actor actor) {
    if (localFocus() == null) return null;
    
    boolean  haveTerms = terms.hasTerms() && ! envoys.empty();
    boolean  isEnvoy   = isEnvoy(actor);
    Pathing  camp      = transitPoint(actor);
    AreaTile stands    = MissionForSecure.standingPointRanks(actor, this, camp);
    
    if (haveTerms && isEnvoy && ! terms.sent()) {
      Actor offersTerms = MissionForContact.findTalkSubject(this, actor, true, true);
      Task t = actor.targetTask(offersTerms, 1, Task.JOB.DIALOG, this);
      if (t != null) return t;
    }
    
    if (terms.rejected() || terms.expired() || ! haveTerms) {
      TaskCombat taskC = (Task.inCombat(actor) || isEnvoy) ? null :
        TaskCombat.nextReaction(actor, stands, this, false, actor.seen())
      ;
      if (taskC != null) return taskC;
      
      //  TODO:  Use mission-utils to set intermediate targets and get through
      //  walls and buildings, et cetera...
      
      TaskCombat taskS = (Task.inCombat(actor) || isEnvoy) ? null :
        TaskCombat.nextSieging(actor, this, localFocus())
      ;
      if (taskS != null) return taskS;
    }
    
    Task standT = actor.targetTask(stands, 1, Task.JOB.MILITARY, this);
    if (standT != null) return standT;
    
    return null;
  }
  
  
  public float casualtyLevel() {
    float live = recruits.size(), dead = casualties.size();
    if (dead + live == 0) return 0;
    return dead / (dead + live);
  }
  
  
  public static int powerSum(Series <Actor> recruits, Area mapOnly) {
    float sumStats = 0;
    for (Actor a : recruits) {
      if (mapOnly != null && a.map != mapOnly) continue;
      sumStats += TaskCombat.attackPower(a);
    }
    return (int) (sumStats * POP_PER_CITIZEN);
  }
  
  
  public static int powerSum(Mission mission) {
    return powerSum(mission.recruits(), mission.localMap());
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
  
  
  static class Option { Target target; AreaTile secures; float rating; }
  
  Option tacticalOptionFor(
    Target focus, Area map, Pathing pathFrom, boolean checkPathing
  ) {
    
    if (focus instanceof Element) {
      Element e = (Element) focus;
      if (TaskCombat.beaten(e) || ! homeBase().isEnemyOf(e.base())) return null;
      
      if (e.type().isActor()) {
        if (TaskCombat.attackPower((Actor) e) <= 0) return null;
      }
      if (e.type().isBuilding()) {
        if (! e.type().isMilitaryBuilding()) return null;
      }
      
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
  
  
  Object updateTacticalTarget(Target current) {
    //
    //  Basic sanity checks first-
    Base    home   = base();
    Area    map    = localMap();
    Pathing from   = transitTile;
    Base    sieges = worldFocus();
    if (sieges == null || from == null || map == null) return null;
    //
    //  If your terms were delivered and accepted in a timely manner, win-
    if (terms.accepted) {
      return VICTORY;
    }
    //
    //  If you're beaten, disband or turn around and go home:
    if (casualtyLevel() > MAX_CASUALTIES / 100f) {
      return LOSS;
    }
    if (powerSum(recruits(), null) == 0) {
      return LOSS;
    }
    //
    //  Determine whether your current target has been beaten yet-
    if (tacticalOptionFor(current, map, from, true) != null) {
      return current;
    }
    //
    //  Otherwise, look for either an enemy formation to engage, or
    //  a building to tear down:
    Pick <Option> pick = new Pick();
    
    //  TODO:  Look at individual actors instead...
    /*
    for (Base c : map.bases) for (Mission f : c.missions) {
      Option o = tacticalOptionFor(f, map, from, false);
      if (o != null && o.secures != null) pick.compare(o, o.rating);
    }
    //*/
    
    for (Building b : map.buildings) {
      Option o = tacticalOptionFor(b, map, from, false);
      if (o != null && o.secures != null) pick.compare(o, o.rating);
    }
    
    if (! pick.empty()) {
      Option o = pick.result();
      
      if (tacticalOptionFor(o.target, map, from, true) != null) {
        return o.target;
      }
      else {
        SiegeSearch search = new SiegeSearch(map, from.at(), o.secures);
        search.doSearch();
        Pathing path[] = (Pathing[]) search.fullPath(Pathing.class);
        for (Pathing p : path) {
          AreaTile t = (AreaTile) p;
          Element above = t.above;
          int pathT = t.pathType();
          
          if (above != null && (pathT == Type.PATH_BLOCK || pathT == Type.PATH_WALLS)) {
            return above;
          }
        }
        return null;
      }
    }
    
    //
    //  If there are no targets left here, declare victory and go home:
    else {
      return VICTORY;
    }
  }
}








