

package game;
import static game.AreaMap.*;
import static game.GameConstants.*;
import graphics.common.*;
import util.*;



public class TaskWander extends Task {
  
  
  public TaskWander(Actor actor) {
    super(actor);
  }
  
  
  public TaskWander(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  static TaskWander nextWandering(Actor actor) {
    TaskWander t = new TaskWander(actor);
    AreaTile goes = t.wanderTarget();
    t = (TaskWander) t.configTask(null, null, goes, JOB.WANDERING, 0);
    return (TaskWander) t;
  }
  
  
  AreaTile wanderTarget() {
    Actor actor = (Actor) this.active;
    Batch <Pathing> walk = new Batch();
    
    AreaMap map    = actor.map();
    Pathing next   = pathOrigin(actor);
    int     range  = Nums.max(4, Rand.index(MAX_WANDER_RANGE));
    Pathing temp[] = new Pathing[9];
    Pathing adj [] = new Pathing[9];
    
    next.flagWith(walk);
    walk.add(next);
    
    boolean prefPave = Rand.yes() && next.pathType() == Type.PATH_PAVE;
    
    while (walk.size() < range) {
      
      int numA = 0;
      for (Pathing n : next.adjacent(temp, map)) {
        if (n == null || n.flaggedWith() != null) continue;
        if (prefPave && n.pathType() != Type.PATH_PAVE) continue; 
        adj[numA] = n;
        numA += 1;
      }
      if (numA == 0) break;
      
      next = adj[Rand.index(numA)];
      next.flagWith(walk);
      walk.add(next);
    }
    
    AreaTile goes = null;
    for (Pathing t : walk) {
      t.flagWith(null);
      if (t.isTile()) goes = (AreaTile) t;
    }
    return goes;
  }
  
  
  protected float successPriority() {
    Actor actor = (Actor) this.active;
    float curiosity = (actor.traits.levelOf(TRAIT_CURIOSITY) + 2) / 2;
    return IDLE * (0.5f + curiosity);
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  String animName() {
    return AnimNames.LOOK;
  }
}





