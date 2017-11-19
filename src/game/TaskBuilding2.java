

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;
import game.GameConstants.Target;



//  TODO:  You'll need to return to the store for building materials!

public class TaskBuilding2 extends Task {
  
  
  
  final static int MAX_TRIES = 100;
  
  Building store;
  int numTries = 0;
  
  
  public TaskBuilding2(Actor actor, Building store) {
    super(actor);
    this.store = store;
  }
  
  
  public TaskBuilding2(Session s) throws Exception {
    super(s);
    store = (Building) s.loadObject();
    numTries = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(store);
    s.saveInt(numTries);
  }
  

  
  static TaskBuilding2 configBuilding2(Building store, Actor a) {
    TaskBuilding2 task = new TaskBuilding2(a, store);
    Tile target = task.pickNextTarget(false);
    if (target == null) {
      return null;
    }
    if (task.configTask(store, null, target, JOB.BUILDING, 1) == null) {
      return null;
    }
    return task;
  }
  
  
  Tile pickNextTarget(boolean near) {
    CityMap map = actor.map;
    CityMapFlagging flagging = map.flagging.get(NEED_BUILD);
    if (flagging == null) {
      return null;
    }
    if (near) {
      int range = actor.type.sightRange;
      return flagging.findNearbyPoint(actor, range);
    }
    else {
      int maxRange = store.type.maxDeliverRange;
      return flagging.pickRandomPoint(store, maxRange);
    }
  }
  
  
  protected void onTarget(Target target) {
    buildTileElement((Tile) target);
  }
  
  
  void buildTileElement(Tile t) {
    CityMap map = actor.map;
    Type type = map.planning.objectAt(t);
    Element above = t.above;
    //
    //  Demolish the old structure or element...
    if (above != null && above.type != type) {
      above.exitMap(map);
    }
    //
    //  Plant a new element!
    else if (above == null) {
      above = (Element) type.generate();
      above.enterMap(map, t.x, t.y, 0);
      above = t.above;
    }
    //
    //  Advance construction, and check to see if we're done yet-
    if (above != null && above.type == type) {
      above.incBuildLevel(0.25f);
    }
    if (map.planning.checkNeedForBuilding(t)) {
      configTask(store, null, t, JOB.BUILDING, 1);
    }
    else if ((t = pickNextTarget(true)) != null) {
      configTask(store, null, t, JOB.BUILDING, 1);
    }
  }
  
  
  
  
  
}




