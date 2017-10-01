

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class CityBorders {
  
  
  static Tile findTransitPoint(CityMap map, City with) {
    
    Tile current = map.transitPoints.get(with);
    if (current != null && ! map.blocked(current.x, current.y)) return current;
    
    Pick <Tile> pick = new Pick();
    Vec2D cityDir = new Vec2D(
      with.mapX - map.city.mapX,
      with.mapY - map.city.mapY
    ).normalise(), temp = new Vec2D();
    
    for (Coord c : Visit.perimeter(1, 1, map.size - 2, map.size - 2)) {
      if (map.blocked(c.x, c.y)) continue;
      
      temp.set(c.x - (map.size / 2), c.y - (map.size / 2)).normalise();
      float rating = 1 + temp.dot(cityDir);
      if (map.paved(c.x, c.y)) rating *= 2;
      
      Tile u = map.tileAt(c.x, c.y);
      pick.compare(u, rating);
    }
    
    Tile point = pick.result();
    map.transitPoints.put(with, point);
    return point;
  }
  
  
  static Tally <Good> configureCargo(
    Trader from, Trader goes, boolean cityOnly
  ) {
    Tally <Good> cargo = new Tally();
    boolean fromCity = from.tradeOrigin() == from;
    boolean goesCity = goes.tradeOrigin() == goes;
    
    if (from == null || goes == null        ) return cargo;
    if (cityOnly && ! (fromCity || goesCity)) return cargo;
    
    for (Good good : ALL_GOODS) {
      float amountO  = from.inventory ().valueFor(good);
      float demandO  = from.tradeLevel().valueFor(good);
      float amountD  = goes.inventory ().valueFor(good);
      float demandD  = goes.tradeLevel().valueFor(good);
      float surplus  = amountO - Nums.max(0, demandO);
      float shortage = Nums.max(0, demandD) - amountD;
      
      if (surplus > 0 && shortage > 0) {
        float size = Nums.min(surplus, shortage);
        cargo.set(good, size);
      }
    }
    
    return cargo;
  }
  
  
  static float distanceRating(Trader from, Trader goes) {
    
    City fromC = from.tradeOrigin(), goesC = goes.tradeOrigin();
    Integer distance = fromC.distances.get(goesC);
    float distRating = distance == null ? 100 : (1 + distance);
    
    if (
      from instanceof Building &&
      goes instanceof Building &&
      fromC == goesC
    ) {
      Building fromB = (Building) from, goesB = (Building) goes;
      float mapDist = CityMap.distance(fromB.entrance, goesB.entrance);
      distRating += mapDist / Walker.MAX_WANDER_TIME;
    }
    
    return distRating;
  }
  
  
}
