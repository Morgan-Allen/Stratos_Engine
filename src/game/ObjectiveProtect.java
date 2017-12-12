


package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class ObjectiveProtect extends Formation.Objective {
  
  
  CityMap map;
  int lastUpdateTime = -1;
  List <Tile> guardPoints = new List();
  
  
  public ObjectiveProtect() {
    super();
  }
  
  
  public ObjectiveProtect(Session s) throws Exception {
    super(s);
    
    map = (CityMap) s.loadObject();
    lastUpdateTime = s.loadInt();
    for (int n = s.loadInt(); n-- > 0;) {
      guardPoints.add(CityMap.loadTile(map, s));
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveObject(map);
    s.saveInt(lastUpdateTime);
    s.saveInt(guardPoints.size());
    for (Tile t : guardPoints) CityMap.saveTile(t, map, s);
  }
  
  
  
  void updateGuardPoints(final Formation parent) {
    
    //
    //  First, check to see if an update is due:
    final Target focus = (Target) parent.secureFocus;
    final CityMap map = parent.map;
    int nextUpdate = lastUpdateTime >= 0 ? (lastUpdateTime + 10) : 0;
    
    if (focus == null || map == null) {
      guardPoints.clear();
      return;
    }
    if (parent.securePoint == focus.at() && map.time < nextUpdate) {
      return;
    }
    
    parent.securePoint = focus.at();
    lastUpdateTime = map.time;
    Type type = focus.type();
    
    
    //
    //  In the case we're defending a wall, try to spread out across
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
      guardPoints.clear();
      
      for (Pathing p : covered) {
        //if (p.facing() == CENTRE   ) continue;
        if (p.flaggedWith() != null || ! p.isTile()) continue;
        for (Pathing n : p.adjacent(temp, map)) {
          if (n != null && n.isTile()) {
            n.flagWith(covered);
            touched.add((Tile) n);
          }
        }
        p.flagWith(covered);
        touched.add((Tile) p);
        guardPoints.add((Tile) p);
      }
      
      for (Tile t : touched) t.flagWith(null);
    }
    
    else if (type.isActor()) {
      
    }
    else {
      
    }
  }
  
  
  
  boolean updateTacticalTarget(Formation parent) {
    
    //  TODO:  In the case of local garrisons being assigned to defend...
    //  look for a nearby wall to garrison, close to an invading army's
    //  entry point?
    
    return false;
  }
  
  

  Tile standLocation(Actor member, Formation parent) {
    this.updateGuardPoints(parent);
    int index = parent.recruits.indexOf(member);
    if (index == -1) return null;
    return guardPoints.atIndex(index);
  }
  
  
  
  void selectActorBehaviour(Actor a, Formation parent) {
    
    //  TODO:  Don't attack targets if that means abandoning your post!
    
    TaskCombat taskC = TaskCombat.actorCombat(a, parent);
    if (taskC != null) {
      a.assignTask(taskC);
      return;
    }
    
    Tile stands = standLocation(a, parent);
    if (stands != null) {
      a.embarkOnTarget(stands, 10, Task.JOB.MILITARY, parent);
      return;
    }
  }
  
  
  void actorUpdates(Actor a, Formation parent) {
    
    //  TODO:  Don't attack targets if that means abandoning your post!
    
    TaskCombat taskC = TaskCombat.actorCombat(a, parent);
    if (taskC != null) {
      a.assignTask(taskC);
      return;
    }
  }
  
  
  void actorTargets(Actor a, Target other, Formation parent) {
    return;
  }
  
}








