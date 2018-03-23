

package game;
import util.*;
import static game.Area.*;
import static game.GameConstants.*;

import game.Task.JOB;
import graphics.common.*;



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
  
  
  
  static TaskWander configWandering(Actor actor) {
    Task t = new TaskWander(actor);
    t.updatePathing();
    if (t.path == null) return null;
    t = t.configTask(null, null, (Pathing) Visit.last(t.path), JOB.WANDERING, 0);
    return (TaskWander) t;
  }
  
  
  protected float successPriority() {
    return Task.IDLE;
  }
  
  
  boolean updatePathing() {
    Actor actor = (Actor) this.active;
    Batch <Pathing> walk = new Batch();
    
    Area map    = actor.map();
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
    
    for (Pathing t : walk) t.flagWith(null);
    this.path = walk.toArray(Pathing.class);
    return true;
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  String animName() {
    return AnimNames.LOOK;
  }
}





