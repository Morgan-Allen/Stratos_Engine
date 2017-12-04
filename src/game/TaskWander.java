

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



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
  
  
  
  Pathing[] updatePathing() {
    Batch <Pathing> walk = new Batch();
    
    CityMap map    = actor.map;
    Pathing next   = pathOrigin(actor);
    int     range  = Nums.max(4, Rand.index(MAX_WANDER_RANGE));
    Pathing temp[] = new Pathing[9];
    Pathing adj [] = new Pathing[9];
    
    next.flagWith(walk);
    walk.add(next);
    
    while (walk.size() < range) {
      boolean prefPave = next.pathType() == PATH_PAVE;
      
      int numA = 0;
      for (Pathing n : next.adjacent(temp, map)) {
        if (n == null || n.flaggedWith() != null) continue;
        if (prefPave && n.pathType() != PATH_PAVE) continue; 
        adj[numA] = n;
        numA += 1;
      }
      if (numA == 0) break;
      
      next = adj[Rand.index(numA)];
      next.flagWith(walk);
      walk.add(next);
    }
    
    for (Pathing t : walk) t.flagWith(null);
    return walk.toArray(Pathing.class);
  }
  
  
}

