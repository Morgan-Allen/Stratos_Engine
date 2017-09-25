package game;


import util.*;
import static util.TileConstants.*;



public class PathSearch extends Search <Tile> {
  
  CityMap map;
  Walker moves;
  Tile dest;
  Tile temp[] = new Tile[8];
  boolean getNear = false;
  boolean paveOnly = true;
  
  
  public PathSearch(
    CityMap map, Walker moves, Tile init, Tile dest
  ) {
    super(init, -1);
    this.map   = map  ;
    this.moves = moves;
    this.dest  = dest ;
    getNear    = ! canEnter(dest);
  }
  
  
  public void setPaveOnly(boolean paveOnly) {
    this.paveOnly = paveOnly;
  }
  
  
  public static Tile[] adjacent(
    Tile spot, Tile temp[], CityMap map, boolean paveOnly
  ) {
    for (int dir : T_INDEX) {
      int x = spot.x + T_X[dir], y = spot.y + T_Y[dir];
      if (paveOnly) {
        if (map.paved(x, y)) temp[dir] = map.tileAt(x, y);
      }
      else {
        if (!map.blocked(x, y)) temp[dir] = map.tileAt(x, y);
      }
    }
    return temp;
  }
  
  
  public static float distance(Tile a, Tile b) {
    if (a == null || b == null) return 1000000000;
    float dist = Nums.max(Nums.abs(a.x - b.x), Nums.abs(a.y - b.y));
    if (a.x != b.x && a.y != b.y) dist += 0.25f;
    return dist;
  }
  
  
  protected Tile[] adjacent(Tile spot) {
    return adjacent(spot, temp, map, paveOnly);
  }
  
  
  protected boolean endSearch(Tile best) {
    if (getNear) return distance(best, dest) <= 1.5f;
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



