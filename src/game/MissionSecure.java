

package game;
import static game.Area.*;
import static game.GameConstants.*;
import game.World.Journey;
import util.*;




public class MissionSecure extends Mission {
  
  
  final static int
    DEFAULT_GUARD_PERIOD = DAY_LENGTH * 1
  ;
  
  int guardPeriod = DEFAULT_GUARD_PERIOD;
  boolean autoRenew = false;
  int beginTime = -1;
  
  List <AreaTile> guardPoints = new List();
  int lastUpdateTime = -1;
  
  
  
  
  public MissionSecure(Base belongs) {
    super(OBJECTIVE_SECURE, belongs);
  }
  
  
  public MissionSecure(Session s) throws Exception {
    super(s);
    
    guardPeriod = s.loadInt();
    autoRenew   = s.loadBool();
    beginTime   = s.loadInt();
    
    Area map = (Area) s.loadObject();
    for (int n = s.loadInt(); n-- > 0;) {
      AreaTile point = Area.loadTile(map, s);
      guardPoints.add(point);
    }
    lastUpdateTime = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveInt(guardPeriod);
    s.saveBool(autoRenew);
    s.saveInt(beginTime);
    
    Area map = localMap();
    s.saveObject(map);
    s.saveInt(guardPoints.size());
    for (AreaTile t : guardPoints) Area.saveTile(t, map, s);
    s.saveInt(lastUpdateTime);
  }
  

  public boolean allowsFocus(Object newFocus) {
    if (newFocus instanceof Building) return true;
    if (newFocus instanceof AreaTile) return true;
    return false;
  }
  
  
  public void setGuardPeriod(int period, boolean autoRenew) {
    this.guardPeriod = period;
    this.autoRenew   = autoRenew;
  }
  
  
  
  
  public void beginMission(Base localBase) {
    super.beginMission(localBase);
    beginTime = localBase.world.time();
  }


  void update() {
    super.update();
    
    if (! complete()) {
      if (worldFocus() != null && ! onWrongMap()) {
        //  TODO:  Renew your target here, and check for completion-criteria...
      }
      else if (localFocus() != null) {
        int time = localMap().time();
        boolean isElement = localFocus() instanceof Element;
        
        if (isElement && ((Element) localFocus()).destroyed()) {
          setMissionComplete(false);
        }
        else if (time - beginTime >= guardPeriod) {
          int periodCash = rewards.cashReward();
          if (autoRenew && homeBase().funds() >= periodCash) {
            beginTime = time;
            rewards.dispenseRewards();
            rewards.setAsBounty(periodCash);
          }
          else setMissionComplete(true);
        }
      }
    }
  }
  

  void beginJourney(Base from, Base goes) {
    super.beginJourney(from, goes);
    for (Actor a : recruits) a.assignGuestBase(null);
    guardPoints.clear();
    lastUpdateTime = -1;
  }
  

  public void onArrival(Base goes, World.Journey journey) {
    if (goes != homeBase()) for (Actor a : recruits) a.assignGuestBase(goes);
    super.onArrival(goes, journey);
  }
  
  
  public Task nextLocalMapBehaviour(Actor actor) {
    
    Target stands = standLocation(actor);
    Target anchor = stands == null ? localFocus() : stands;

    //  TODO:  Don't stray too far from the original guard-point.
    TaskCombat taskC = Task.inCombat(actor) ? null :
      TaskCombat.nextReaction(actor, anchor, this, AVG_FILE)
    ;
    if (taskC != null) return taskC;
    
    if (stands == null) {
      Task patrol = TaskPatrol.protectionFor(actor, localFocus(), Task.ROUTINE);
      if (patrol != null) return patrol;
    }
    else {
      Task taskS = actor.targetTask(stands, 1, Task.JOB.MILITARY, this);
      if (taskS != null) return taskS;
    }
    
    return null;
  }
  
  
  void handleOffmapArrival(Base goes, World.Journey journey) {
    BaseEvents.handleGarrison(this, goes, journey);
  }
  
  
  void handleOffmapDeparture(Base from, Journey journey) {
    return;
  }
  
  
  
  /**  Other utility methods-
    */
  public AreaTile standLocation(Actor actor) {
    boolean doPatrol = localFocus().type().isWall;
    if (doPatrol) {
      return standingPointPatrol(actor, this);
    }
    else {
      return null;
    }
  }
  
  
  public boolean assembled() {
    for (Actor a : recruits) if (a.at() != standLocation(a)) return false;
    return true;
  }
  
  
  static AreaTile standingPointPatrol(Actor member, MissionSecure parent) {
    
    int span = SHIFT_LENGTH;
    int numRecruits = Nums.max(1, parent.recruits.size());
    int epoch = (parent.homeBase.world.time / span) % numRecruits;
    
    int index = parent.recruits.indexOf(member);
    if (index == -1) return null;
    
    updateGuardPoints(parent);
    index = (index + epoch) % numRecruits;
    return parent.guardPoints.atIndex(index);
  }
  
  
  static float rateCampingPoint(AreaTile t, Mission parent) {
    if (t == null || parent == null) return -1;
    
    Area map = parent.localBase.activeMap();
    Pathing from = parent.transitTile;
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
    if (parent == null) return null;
    
    final Area map = parent.localBase.activeMap();
    final AreaTile init = AreaTile.nearestOpenTile(parent.transitTile, map);
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
  
  
  
  static void updateGuardPoints(final MissionSecure parent) {
    
    //
    //  First, check to see if an update is due:
    final Target focus = parent.localFocus();
    final Area map = parent.localBase.activeMap();
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
  
  
  static AreaTile standingPointRanks(
    Actor member, Mission parent, Target fromFocus
  ) {
    if (fromFocus == null) return null;
    
    Area map = parent.localBase.activeMap();
    AreaTile goes = fromFocus.at();
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
    
    AreaTile stands = map.tileAt(x, y);
    stands = AreaTile.nearestOpenTile(stands, map, AVG_FILE);
    return stands;
  }
  
}


