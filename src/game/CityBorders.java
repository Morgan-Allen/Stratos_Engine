

package game;
import util.*;
import static game.AreaMap.*;
import static game.GameConstants.*;



public class CityBorders {
  
  
  /**  General migration utilities-
    */
  static Tile findTransitPoint(AreaMap map, Base base, Base with) {
    
    //  TODO:  Make sure there's a pathing connection to the main settlement
    //  here!
    
    Tile current = map.transitPoints.get(with);
    if (current != null && ! map.blocked(current.x, current.y)) return current;
    
    Pick <Tile> pick = new Pick();
    Vec2D cityDir = new Vec2D(
      with.locale.mapX - base.locale.mapX,
      with.locale.mapY - base.locale.mapY
    ).normalise(), temp = new Vec2D();
    
    for (Coord c : Visit.perimeter(1, 1, map.size - 2, map.size - 2)) {
      if (map.blocked(c.x, c.y)) continue;
      
      temp.set(c.x - (map.size / 2), c.y - (map.size / 2)).normalise();
      float rating = 1 + temp.dot(cityDir);
      if (map.pathType(c) == PATH_PAVE) rating *= 2;
      
      Tile u = map.tileAt(c.x, c.y);
      pick.compare(u, rating);
    }
    
    Tile point = pick.result();
    map.transitPoints.put(with, point);
    return point;
  }
  
  
  static float distanceRating(Base from, Base goes) {
    return AVG_CITY_DIST / (AVG_CITY_DIST + from.distance(goes));
  }
  
  
  
  /**  Finding migrants in and out-
    */
  static class Assessment {
    float jobsTotal = 0, jobsFilled = 0;
    float homeTotal = 0, homeFilled = 0;
    float jobsCrowding, homeCrowding;
    
    Tally <Type> jobsDemand = new Tally();
    Tally <Type> jobsSupply = new Tally();
    
    Assessment(AreaMap map) {
      int MR, NR, MW, NW;
      
      for (Building b : map.buildings) {
        boolean report = b.reports();
        if (report) {
          I.say("\nGetting job assessment for "+b);
        }
        for (int socialClass : ALL_CLASSES) {
          homeTotal  += MR = b.maxResidents(socialClass);
          homeFilled += NR = b.numResidents(socialClass);
          if (report) I.say("  Class "+socialClass+": "+NR+"/"+MR);
        }
        for (ActorType t : b.type().workerTypes.keys()) {
          jobsTotal  += MW = b.maxWorkers(t);
          jobsFilled += NW = b.numWorkers(t);
          jobsDemand.add(MW, t);
          jobsSupply.add(NW, t);
          if (report) I.say("  Job "+t+": "+NW+"/"+MW);
        }
      }
      
      jobsCrowding = jobsTotal == 0 ? 1 : (jobsFilled / jobsTotal);
      homeCrowding = homeTotal == 0 ? 1 : (homeFilled / homeTotal);
    }
  }
  
  
  public static Actor generateMigrant(
    ActorType jobType, Building employs, boolean payHireCost
  ) {
    if (! jobType.isPerson()) return null;
    
    //  TODO:  Consider a wider variety of cities to source from!
    
    AreaMap map  = employs.map();
    Base    from = map.locals;
    Base    goes = employs.base();
    int     cost = employs.hireCost(jobType);
    
    if (payHireCost) goes.incFunds(0 - cost);
    
    ActorAsPerson migrant = (ActorAsPerson) jobType.generate();
    jobType.initAsMigrant(migrant);
    migrant.assignHomeCity(goes);
    employs.setWorker(migrant, true);
    map.world.beginJourney(from, goes, migrant);
    
    return migrant;
  }
  
  
  static void findWork(AreaMap map, Actor migrant) {
    
    class Opening { Building b; ActorType position; }
    Tile from = migrant.at();
    Base homeC = migrant.base();
    final Pick <Opening> pick = new Pick();
    
    for (Building b : map.buildings) if (b.accessible()) {
      if (homeC != null && b.base() != homeC) continue;
      
      for (ActorType t : b.type().workerTypes.keys()) {
        int space = b.maxWorkers(t) - b.numWorkers(t);
        if (space <= 0) continue;
        
        float fitness = 2;
        for (Trait skill : t.initTraits.keys()) {
          fitness += migrant.levelOf(skill) * t.initTraits.valueFor(skill);
        }
        
        float near = AreaMap.distancePenalty(from, b);
        Opening o = new Opening();
        o.b = b;
        o.position = t;
        pick.compare(o, near * fitness);
      }
    }
    
    Opening o = pick.result();
    if (o != null) {
      Building b = o.b;
      int SC = o.position.socialClass;
      
      migrant.assignType(o.position);
      b.setWorker(migrant, true);
      if (b.maxResidents(SC) > b.numResidents(SC)) b.setResident(migrant, true);
    }
  }
  
  
  static void findHome(AreaMap map, Actor migrant) {
    int socialClass = migrant.type().socialClass;
    Tile from = migrant.at();
    final Pick <Building> pick = new Pick();
    
    for (Building b : map.buildings) if (b.accessible()) {
      int max   = b.maxResidents(socialClass);
      int space = max - b.numResidents(socialClass);
      if (space <= 0) continue;
      
      float near = 10 / (10f + AreaMap.distance(from, b));
      pick.compare(b, space * near);
    }
    
    Building home = pick.result();
    if (home != null) home.setResident(migrant, true);
  }
  
  
  
}
