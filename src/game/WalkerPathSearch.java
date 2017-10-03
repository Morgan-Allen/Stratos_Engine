

package game;
import static game.CityMap.*;
import util.*;



public class WalkerPathSearch extends Search <Tile> {
  
  CityMap map;
  Tile dest;
  Tile temp[] = new Tile[8];
  boolean getNear = false;
  boolean paveOnly = true;
  

  public WalkerPathSearch(Walker w, Tile dest) {
    this(w.map, w.at, dest, -1);
  }
  
  
  public WalkerPathSearch(CityMap map, Tile init, Tile dest, int maxDist) {
    super(init, -1);
    this.map     = map;
    this.dest    = dest;
    this.maxCost = maxDist;
    this.getNear = ! canEnter(dest);
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



