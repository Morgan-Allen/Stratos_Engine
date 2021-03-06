

package game;
import util.*;
import static game.AreaMap.*;
import static game.GameConstants.*;



public class ActorPathSearch extends Search <Pathing> {
  
  AreaMap map;
  AreaFog fog;
  AreaDanger danger;
  Pathing dest;
  Actor   client   = null;
  boolean getNear  = false;
  boolean stealthy = false;
  boolean flight   = false;
  boolean oneTile  = true;
  
  static Pathing  temp [] = new Pathing [9];
  static AreaTile tempT[] = new AreaTile[9];
  
  
  public ActorPathSearch(Actor w, Pathing init, Pathing dest) {
    this(w.map, init, dest, -1);
    
    this.client = w;
    this.danger = map.dangerMap(client);
    this.flight = w.type().moveMode == Type.MOVE_AIR;
    
    ActorType type = w.type();
    this.oneTile = type.wide == 1 && type.high == 1;
  }
  
  
  public ActorPathSearch(
    AreaMap map, Pathing init, Pathing dest, int maxDist
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
    if (yes && client != null) fog = map.fogMap(client);
    else fog = null;
  }
  
  
  public Search <Pathing> doSearch() {
    return super.doSearch();
  }
  
  
  protected Pathing[] adjacent(Pathing spot) {
    if (flight && spot.isTile()) {
      return AreaMap.adjacent((AreaTile) spot, tempT, map);
    }
    return spot.adjacent(temp, map);
  }
  
  
  protected boolean endSearch(Pathing best) {
    if (getNear) return distance(best, dest) <= 1.5f;
    return best == dest;
  }
  
  
  protected boolean canEnter(Pathing spot) {
    if (flight) {
      return true;
    }
    else if (oneTile) {
      if (spot.isTile()) {
        AreaTile t = (AreaTile) spot;
        if (map.blocked(t)) return false;
        //if (client != null && client.at() != t && ! t.allInside().empty()) return false;
      }
      if (client != null && ! spot.allowsEntry(client)) {
        return false;
      }
    }
    else {
      if (spot.isTile()) {
        AreaTile at = (AreaTile) spot;
        Type type = client.type();
        for (AreaTile t : map.tilesUnder(at.x, at.y, type.wide, type.high)) {
          if (map.blocked(t)) return false;
        }
      }
      if (client != null && ! spot.allowsEntry(client)) {
        return false;
      }
    }
    
    return true;
  }
  
  
  protected float cost(Pathing prior, Pathing spot) {
    float dist = distance(prior, spot);
    
    if (! flight) {
      int type = spot.pathType();
      if (type == Type.PATH_PAVE  ) dist *= 0.75f;
      if (type == Type.PATH_HINDER) dist *= 2.50f;
    }
    /*
    if (stealthy && fog != null) {
      dist += fog.sightLevel(spot.at());
    }
    //*/
    if (danger != null) {
      AreaTile at = spot.at();
      dist += danger.fuzzyLevel(at.x, at.y) / AreaMap.FLAG_AREA;
    }
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



