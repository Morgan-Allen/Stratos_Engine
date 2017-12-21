

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class CityBorders {
  
  
  /**  General migration utilities-
    */
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
      if (map.pathType(c) == PATH_PAVE) rating *= 2;
      
      Tile u = map.tileAt(c.x, c.y);
      pick.compare(u, rating);
    }
    
    Tile point = pick.result();
    map.transitPoints.put(with, point);
    return point;
  }
  
  
  
  /**  Finding migrants in and out-
    */
  static class Assessment {
    float jobsTotal = 0, jobsFilled = 0;
    float homeTotal = 0, homeFilled = 0;
    float jobsCrowding, homeCrowding;
    
    Tally <Type> jobsDemand = new Tally();
    Tally <Type> jobsSupply = new Tally();
    
    Assessment(CityMap map) {
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
        for (Type t : b.type.workerTypes) {
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
  
  
  static void spawnMigrants(CityMap map, int period) {
    if (! map.settings.toggleMigrate) return;
    Assessment a = new Assessment(map);
    
    float crowding = Nums.max(a.jobsCrowding, a.homeCrowding);
    float spaces = a.jobsTotal + a.homeTotal - (a.jobsFilled + a.homeFilled);
    if (crowding >= 1) return;
    
    //
    //  Tally up the number of migrants available to arrive this month-
    //  TODO:  Do this with each neighbouring city, based on their own
    //    population/crowding levels, current relations, and proximity.
    City from = map.city;
    Batch <Actor> migrants = new Batch();
    float months   = period * 1f / MONTH_LENGTH;
    float numSpawn = ((1 - crowding) * MIGRANTS_PER_1KM * months);
    numSpawn = Nums.min(numSpawn, spaces);
    
    //
    //  And put together a profile of which jobs are in greatest demand-
    Type  jobTypes[] = a.jobsDemand.keysToArray(Type.class);
    float jobNeeds[] = new float[jobTypes.length];
    for (int i = jobTypes.length; i-- > 0;) {
      Type j = jobTypes[i];
      float need = a.jobsDemand.valueFor(j) - a.jobsSupply.valueFor(j);
      jobNeeds[i] = Nums.max(0, need);
    }
    
    //
    //  Then spawn actors randomly generated to fit that profile-
    
    //  TODO:  There should always be a chance of getting random, unskilled
    //  workers, and higher-skill positions might not be trivially filled.
    
    while (numSpawn-- > 0) {
      Type job = (Type) Rand.pickFrom(jobTypes, jobNeeds);
      ActorAsPerson w = (ActorAsPerson) job.generate();
      w.type.initAsMigrant(w);
      migrants.add(w);
    }
    map.city.world.beginJourney(from, map.city, (Batch) migrants);
  }
  
  
  static void findWork(CityMap map, Actor migrant) {
    
    class Opening { Building b; Type position; }
    Tile from = migrant.at();
    final Pick <Opening> pick = new Pick();
    
    //  TODO:  You'll want a more sophisticated measure of which jobs you
    //  could reasonably fill.
    
    for (Building b : map.buildings) if (b.accessible()) {
      for (Type t : b.type.workerTypes) {
        int space = b.maxWorkers(t) - b.numWorkers(t);
        if (space <= 0) continue;

        float near = 10 / (10f + CityMap.distance(from, b));
        Opening o = new Opening();
        o.b = b;
        o.position = t;
        pick.compare(o, space * near);
      }
    }
    
    Opening o = pick.result();
    if (o != null) {
      Building b = o.b;
      int SC = o.position.socialClass;
      
      migrant.type = o.position;
      b.setWorker(migrant, true);
      if (b.maxResidents(SC) > b.numResidents(SC)) b.setResident(migrant, true);
    }
  }
  
  
  static void findHome(CityMap map, Actor migrant) {
    int socialClass = migrant.type.socialClass;
    Tile from = migrant.at();
    final Pick <Building> pick = new Pick();
    
    for (Building b : map.buildings) if (b.accessible()) {
      int max   = b.maxResidents(socialClass);
      int space = max - b.numResidents(socialClass);
      if (space <= 0) continue;
      
      float near = 10 / (10f + CityMap.distance(from, b));
      pick.compare(b, space * near);
    }
    
    Building home = pick.result();
    if (home != null) home.setResident(migrant, true);
  }
  
  
  
  /**  Trading utilities-
    */
  //  TODO:  MOVE THIS TO THE TASK-TRADING CLASS!
  
  
  static Tally <Good> configureCargo(
    Trader from, Trader goes, boolean cityOnly
  ) {
    Tally <Good> cargo = new Tally();
    boolean fromCity = from.homeCity() == from;
    boolean goesCity = goes.homeCity() == goes;
    
    if (from == null || goes == null        ) return cargo;
    if (cityOnly && ! (fromCity || goesCity)) return cargo;
    City.Relation fromR = goes.homeCity().relationWith(from.homeCity());
    City.Relation goesR = from.homeCity().relationWith(goes.homeCity());
    
    for (Good good : ALL_GOODS) {
      float amountO = from.inventory ().valueFor(good);
      float demandO = from.tradeLevel().valueFor(good);
      float amountD = goes.inventory ().valueFor(good);
      float demandD = goes.tradeLevel().valueFor(good);
      
      if (fromCity) {
        demandO = Nums.max(demandO, fromR.suppliesDue.valueFor(good));
      }
      if (goesCity) {
        demandD = Nums.max(demandD, goesR.suppliesDue.valueFor(good));
      }
      
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
    
    City fromC = from.homeCity(), goesC = goes.homeCity();
    Integer distance = fromC.distances.get(goesC);
    float distRating = distance == null ? MAX_TRADER_RANGE : distance;
    
    if (
      from instanceof Building &&
      goes instanceof Building &&
      fromC == goesC
    ) {
      float mapDist = CityMap.distance(
        ((Building) from).mainEntrance(),
        ((Building) goes).mainEntrance()
      );
      distRating += mapDist / MAX_WANDER_RANGE;
    }
    
    return AVG_CITY_DIST / (AVG_CITY_DIST + distRating);
  }
  
  
}
