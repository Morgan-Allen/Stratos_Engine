

package game;
import static game.Area.*;
import static game.GameConstants.*;
import game.World.Journey;
import util.*;




public class MissionForSecure extends Mission {
  
  
  final static int
    DEFAULT_GUARD_PERIOD = DAY_LENGTH * 1,
    FOCUS_CHECK_PERIOD = 10,
    GUARD_CHECK_PERIOD = 10
  ;
  
  int guardPeriod = DEFAULT_GUARD_PERIOD;
  boolean autoRenew = false;
  int beginTime = -1;
  
  List <AreaTile> guardPoints = new List();
  int lastFocusEvalTime = -1;
  float lastFocusRating = -1;
  int lastGuardEvalTime = -1;
  
  
  
  
  public MissionForSecure(Base belongs) {
    super(OBJECTIVE_SECURE, belongs);
  }
  
  
  public MissionForSecure(Session s) throws Exception {
    super(s);
    
    guardPeriod = s.loadInt();
    autoRenew   = s.loadBool();
    beginTime   = s.loadInt();
    
    Area map = (Area) s.loadObject();
    for (int n = s.loadInt(); n-- > 0;) {
      AreaTile point = Area.loadTile(map, s);
      guardPoints.add(point);
    }
    lastFocusEvalTime = s.loadInt();
    lastGuardEvalTime = s.loadInt();
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
    s.saveInt(lastFocusEvalTime);
    s.saveInt(lastGuardEvalTime);
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
  
  
  
  public void beginMission(WorldLocale locale) {
    super.beginMission(locale);
    beginTime = homeBase().world.time();
    Base goes = (Base) worldFocus();
    for (Actor a : recruits()) a.bonds.assignGuestBase(goes);
  }
  
  
  public void disbandMission() {
    for (Actor a : recruits()) a.bonds.assignGuestBase(null);
    super.disbandMission();
  }
  

  void beginJourney(WorldLocale from, WorldLocale goes) {
    super.beginJourney(from, goes);
    guardPoints.clear();
    lastGuardEvalTime = -1;
  }


  void update() {
    super.update();
    
    if (! complete()) {
      //
      //  Iterate across all structures on the map belonging to the world-focus,
      //  and pick whichever seems to be in most danger (especially if it's a
      //  wall/tower or other defensible or essential structure.)
      if (worldFocus() != null && ! onWrongMap()) {
        
        int currentTime = this.homeBase().world.time();
        int updateTime  = this.lastFocusEvalTime;
        int nextUpdate  = updateTime >= 0 ? (updateTime + FOCUS_CHECK_PERIOD) : 0;

        if (currentTime >= nextUpdate) {
          this.lastFocusEvalTime = currentTime;
          
          Pick <Building> pickDefend = new Pick();
          Base client = (Base) worldFocus();
          Area map = localMap();
          Target lastFocus = localFocus();
          AreaDanger danger = map.dangerMap(client.faction(), true);
          
          for (Building b : map.buildings()) if (b.base() == client) {
            float rating = 1.0f;
            AreaTile at = b.centre();
            
            rating *= danger.fuzzyLevel(at.x, at.y);
            if (b.type().isWall) rating *= 1.5f;
            if (b == client.headquarters()) rating *= 1.5f;
            
            if (b == lastFocus) rating *= 1.25f;
            else if (lastFocus != null) rating *= Area.distancePenalty(lastFocus, b);
            
            pickDefend.compare(b, rating);
          }
          
          setLocalFocus(pickDefend.result());
        }
      }
      //
      //  
      else if (localFocus() != null) {
        int time = localMap().time();
        boolean isElement = localFocus() instanceof Element;
        
        if (isElement && TaskCombat.killed((Element) localFocus())) {
          setMissionComplete(false);
        }
        else if (time - beginTime >= guardPeriod) {
          int periodCash = rewards.cashReward();
          int funds = homeBase().funds();
          boolean canRenew = periodCash == 0 || funds >= periodCash;
          
          if (autoRenew && canRenew) {
            beginTime = time;
            rewards.dispenseRewards();
            if (periodCash > 0) rewards.setAsBounty(periodCash);
          }
          else setMissionComplete(true);
        }
      }
    }
  }
  
  
  public Task nextLocalMapBehaviour(Actor actor) {
    
    AreaTile stands = standLocation(actor);
    Target anchor = stands == null ? localFocus() : stands;
    
    //  TODO:  Don't stray too far from the original guard-point...
    TaskCombat taskC = Task.inCombat(actor) ? null :
      TaskCombat.nextReaction(actor, anchor, this, true, actor.seen())
    ;
    if (taskC != null) return taskC;
    
    if (stands == null) {
      Task patrol = TaskPatrol.protectionFor(actor, localFocus(), this);
      if (patrol != null) return patrol;
    }
    else {
      Task sentry = TaskPatrol.sentryDutyFor(actor, stands, this);
      if (sentry != null) return sentry;
    }
    
    return null;
  }
  
  
  void handleOffmapArrival(WorldLocale goes, World.Journey journey) {
    Base focus = worldFocusBase();
    MissionUtils.handleGarrisonArrive(this, focus, journey);
  }
  
  
  void handleOffmapDeparture(WorldLocale from, Journey journey) {
    Base focus = worldFocusBase();
    MissionUtils.handleGarrisonDepart(this, focus, journey);
  }
  
  
  
  /**  Other utility methods-
    */
  public AreaTile standLocation(Actor actor) {
    if (localFocus().type().isWall) {
      return standingPointPatrol(actor, this);
    }
    else if (localFocus().isTile()) {
      return standingPointRanks(actor, this, localFocus());
    }
    else {
      return null;
    }
  }
  
  
  public boolean assembled() {
    for (Actor a : recruits) if (a.at() != standLocation(a)) return false;
    return true;
  }
  
  
  static AreaTile standingPointPatrol(Actor member, MissionForSecure parent) {
    
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
    
    Area map = parent.localMap();
    Pathing from = parent.transitTile();
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
    
    final Area map = parent.localMap();
    final AreaTile init = AreaTile.nearestOpenTile(parent.transitTile(), map);
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
  
  
  
  static void updateGuardPoints(final MissionForSecure parent) {
    
    //
    //  First, check to see if an update is due:
    final Target focus = parent.localFocus();
    final Area map = parent.localMap();
    final int updateTime = parent.lastGuardEvalTime;
    int nextUpdate = updateTime >= 0 ? (updateTime + GUARD_CHECK_PERIOD) : 0;
    
    if (focus == null || map == null) {
      parent.guardPoints.clear();
      return;
    }
    if (map.time < nextUpdate) {
      return;
    }
    
    parent.lastGuardEvalTime = map.time;
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
    
    Area map = parent.localMap();
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



