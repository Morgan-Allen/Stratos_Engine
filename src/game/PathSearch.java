

package game;
import static game.CityMap.*;
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
  
  
  protected Tile[] adjacent(Tile spot) {
    return CityMap.adjacent(spot, temp, map, paveOnly);
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



