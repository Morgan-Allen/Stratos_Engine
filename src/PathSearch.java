

import util.*;
import static util.TileConstants.*;



public class PathSearch extends Search <Tile> {
  
  City map;
  Walker moves;
  Tile dest;
  Tile temp[] = new Tile[8];
  boolean getNear = false;
  
  
  public PathSearch(City map, Walker moves, Tile init, Tile dest) {
    super(init, -1);
    this.map   = map  ;
    this.moves = moves;
    this.dest  = dest ;
    getNear = ! canEnter(dest);
  }
  
  
  public static Tile[] adjacent(Tile spot, Tile temp[], City map) {
    for (int dir : T_INDEX) {
      int x = spot.x + T_X[dir], y = spot.y + T_Y[dir];
      if (map.paved(x, y)) temp[dir] = map.tileAt(x, y);
    }
    return temp;
  }
  
  
  public static float distance(Tile a, Tile b) {
    float dist = Nums.max(Nums.abs(a.x - b.x), Nums.abs(a.y - b.y));
    if (a.x != b.x && a.y != b.y) dist += 0.25f;
    return dist;
  }
  
  
  protected Tile[] adjacent(Tile spot) {
    return adjacent(spot, temp, map);
  }
  
  
  protected boolean endSearch(Tile best) {
    if (getNear) return Visit.arrayIncludes(adjacent(best), dest);
    return best == dest;
  }
  
  
  protected boolean canEnter(Tile spot) {
    if (map.blocked(spot.x, spot.y)) return false;
    return true;
  }
  
  
  protected float cost(Tile prior, Tile spot) {
    return distance(prior, spot);
  }
  
  
  protected float estimate(Tile spot) {
    return distance(spot, dest);
  }
  
  
  protected void setEntry(Tile spot, Entry flag) {
    spot.flag = flag;
  }
  
  
  protected Entry entryFor(Tile spot) {
    return (Entry) spot.flag;
  }
}



