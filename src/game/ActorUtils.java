

package game;
import util.*;
import static game.AreaMap.*;
import static game.GameConstants.*;
import static game.TaskDelivery.*;



public class ActorUtils {
  
  
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
  public static Actor generateMigrant(
    ActorType jobType, Building employs, boolean payHireCost
  ) {
    //  TODO:  Consider a wider variety of cities to source from!
    
    AreaMap map  = employs.map();
    Base    from = map.locals;
    Base    goes = employs.base();
    int     cost = employs.hireCost(jobType);
    
    if (payHireCost) goes.incFunds(0 - cost);
    
    Actor migrant = (Actor) jobType.generate();
    if (jobType.isPerson()) {
      jobType.initAsMigrant((ActorAsPerson) migrant);
    }
    
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
        
        float fitness = 0, sumWeights = 0;
        for (Trait skill : t.initTraits.keys()) {
          float level = t.initTraits.valueFor(skill);
          fitness += migrant.levelOf(skill) / level;
          sumWeights += level;
        }
        fitness /= Nums.max(1, sumWeights);
        if (fitness <= 0.5f) continue;
        
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
    //  Each citizen prompts the search based on proximity to their place of
    //  work, proximity to needed services, and safety of the location (they
    //  prefer being behind walls, for example- either that, or you have a
    //  high safety rating.)
    
    if (migrant.work() == null) return;
    if (migrant.home() != null) return;
    if (! migrant.type().isPerson()) return;
    
    class SumPos extends Vec2D {
      float sumWeights;
      
      void addPos(Building b, float weight) {
        if (b == null) return;
        x += b.at().x * weight;
        y += b.at().y * weight;
        sumWeights += weight;
      }
    }
    SumPos sumPos = new SumPos();
    int maxRange = MAX_WANDER_RANGE * 2;
    
    Building work = migrant.work();
    sumPos.addPos(work, 1);
    
    Building refuge = findNearestWithFeature(IS_REFUGE, maxRange, work);
    sumPos.addPos(refuge, 1);
    
    Building market = findNearestWithFeature(IS_VENDOR, maxRange, work);
    sumPos.addPos(market, 1);
    
    sumPos.scale(1f / sumPos.sumWeights);
    
    final Tile from = map.tileAt(sumPos.x, sumPos.y);
    final int socialClass = migrant.type().socialClass;
    
    
    Pick <Building> pick = new Pick <Building> () {
      public void compare(Building b, float rating) {
        int max   = b.maxResidents(socialClass);
        int space = max - b.numResidents(socialClass);
        if (space <= 0) return;
        float near = 10 / (10f + AreaMap.distance(from, b));
        super.compare(b, rating * space * near);
      }
    };
    
    //  TODO:  Check the actor's work venue first to see if that will allow
    //  residence?
    //  TODO:  This could get very computationally-intensive on large maps.
    //  Try to improve on that?
    
    for (Building b : map.buildings) if (b.accessible()) {
      pick.compare(b, 1);
    }
    for (Element e : map.planning.toBuild) {
      if (e.type().isHomeBuilding() && ! e.onMap()) {
        Building b = (Building) e;
        pick.compare(b, 1);
      }
    }
    
    Building home = pick.result();
    if (home != null) {
      home.setResident(migrant, true);
    }
    else if (map.world.settings.toggleAutoBuild) {
      Type baseHomeType = migrant.type().nestType();
      home = (Building) baseHomeType.generate();
      Tile goes = findEntryPoint(home, map, from, maxRange / 2);
      
      if (! home.canPlace(map, goes.x, goes.y, 1)) {
        I.say("???");
        home.canPlace(map, goes.x, goes.y, 1);
      }
      
      if (goes != null) {
        home.assignBase(migrant.base());
        home.setLocation(goes, map);
        map.planning.placeObject(home);
        home.setResident(migrant, true);
      }
    }
  }
  
  
  static Tile findEntryPoint(
    final Building enters, final AreaMap map, Target from, int maxRange
  ) {
    final Vars.Ref <Tile> result = new Vars.Ref();
    final Tile temp[] = new Tile[9];
    
    Flood <Tile> flood = new Flood <Tile> () {
      protected void addSuccessors(Tile front) {
        if (result.value != null) return;
        
        for (Tile n : AreaMap.adjacent(front, temp, map)) {
          if (n == null || n.flaggedWith() != null) continue;
          ///I.say("Check at: "+n);
          
          if (enters.canPlace(map, n.x, n.y, 1)) {
            result.value = n;
            return;
          }
          else {
            tryAdding(n);
          }
        }
      }
    };
    
    flood.floodFrom(from.at());
    return result.value;
  }
  
  
  
  
  public static Tile pickRandomTile(Target t, float range, AreaMap map) {
    final float angle = Rand.num() * Nums.PI * 2;
    final float dist = (Rand.num() * range) + 1, max = map.size - 1;
    final Tile at = t.at();
    return map.tileAt(
      Nums.clamp(at.x + (float) (Nums.cos(angle) * dist), 0, max),
      Nums.clamp(at.y + (float) (Nums.sin(angle) * dist), 0, max)
    );
  }
  
}







/*
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
//*/


