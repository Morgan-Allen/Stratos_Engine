

package game;
import util.*;
import static game.Area.*;
import static game.GameConstants.*;



public class ActorPathSearch extends Search <Pathing> {
  
  Area map;
  Pathing dest;
  Pathing temp[] = new Pathing[9];
  Actor   client   = null;
  boolean getNear  = false;
  boolean stealthy = false;
  
  
  public ActorPathSearch(Actor w, Pathing dest) {
    this(w.map, w.at(), dest, -1);
    this.client = w;
  }
  
  
  public ActorPathSearch(
    Area map, Pathing init, Pathing dest, int maxDist
  ) {
    super(init, -1);
    this.map     = map;
    this.dest    = dest;
    this.maxCost = maxDist;
  }
  
  
  public void setProximate(boolean yes) {
    getNear = true;
  }
  
  
  public void setStealthy(boolean yes) {
    stealthy = yes;
  }
  
  
  public Search <Pathing> doSearch() {
    return super.doSearch();
  }
  
  
  protected Pathing[] adjacent(Pathing spot) {
    return spot.adjacent(temp, map);
  }
  
  
  protected boolean endSearch(Pathing best) {
    if (getNear) return distance(best, dest) <= 1.5f;
    return best == dest;
  }
  
  
  protected boolean canEnter(Pathing spot) {
    if (spot.isTile() && map.blocked((AreaTile) spot)) {
      return false;
    }
    if (client != null && ! spot.allowsEntry(client)) {
      return false;
    }
    return true;
  }
  
  
  protected float cost(Pathing prior, Pathing spot) {
    float dist = distance(prior, spot);
    int type = spot.pathType();
    if (type == Type.PATH_PAVE  ) dist *= 0.75f;
    if (type == Type.PATH_HINDER) dist *= 2.50f;
    if (stealthy) dist += map.fog.sightLevel(spot.at());
    return dist;
  }
  
  
  protected float estimate(Pathing spot) {
    return distance(spot, dest);
  }
  
  
  protected void setEntry(Pathing spot, Entry flag) {
    spot.flagWith(flag);
  }
  
  
  protected Entry entryFor(Pathing spot) {
    return (Entry) spot.flaggedWith();
  }
}



