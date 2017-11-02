

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



public class TaskWander extends Task {
  
  
  static int dirs[] = new int[4];
  
  
  public TaskWander(Actor actor) {
    super(actor);
  }
  
  
  public TaskWander(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  Tile[] updatePathing() {
    Batch <Tile> walk = new Batch();
    
    CityMap map    = actor.map;
    Tile    next   = actor.at();
    int     facing = T_ADJACENT[Rand.index(4)];
    int     range  = Nums.max(4, Rand.index(MAX_WANDER_RANGE));
    
    while (walk.size() < range) {
      boolean prefPave = map.paved(next.x, next.y);
      int nx, ny, numDirs = 0;
      int backDir = (facing + 4) % 8;
      
      for (int dir : T_ADJACENT) {
        if (dir == backDir) continue;
        nx = next.x + T_X[dir];
        ny = next.y + T_Y[dir];
        if (prefPave && ! map.paved(nx, ny)) continue;
        if (map.blocked(nx, ny)) continue;
        if (map.tileAt(nx, ny).pathFlag != null) continue;
        dirs[numDirs] = dir;
        numDirs++;
      }
      if (numDirs == 0) {
        break;
      }
      else if (numDirs > 1) {
        facing = dirs[Rand.index(numDirs)];
      }
      else {
        facing = dirs[0];
      }
      next = map.tileAt(
        next.x + T_X[facing],
        next.y + T_Y[facing]
      );
      walk.add(next);
      next.pathFlag = walk;
    }
    
    for (Tile t : walk) t.pathFlag = null;
    return walk.toArray(Tile.class);
  }
  
  
}
