

package game;
import static game.CityMap.*;
import util.*;



public class ActorPathSearch extends Search <Tile> {
  
  CityMap map;
  Tile dest;
  Tile temp[] = new Tile[8];
  boolean getNear  = false;
  boolean paveOnly = false;
  boolean stealthy = false;
  

  public ActorPathSearch(Actor w, Tile dest) {
    this(w.map, w.at(), dest, -1);
  }
  
  
  public ActorPathSearch(CityMap map, Tile init, Tile dest, int maxDist) {
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
    float dist = distance(prior, spot);
    if (spot.paved) dist *= 0.75f;
    if (stealthy  ) dist += map.fog.sightLevel(spot);
    return dist;
  }
  
  
  protected float estimate(Tile spot) {
    return distance(spot, dest);
  }
  
  
  protected void setEntry(Tile spot, Entry flag) {
    spot.pathFlag = flag;
  }
  
  
  protected Entry entryFor(Tile spot) {
    return (Entry) spot.pathFlag;
  }
}



