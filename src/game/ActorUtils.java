

package game;
import util.*;
import static game.Area.*;
import static game.GameConstants.*;
import static game.TaskDelivery.*;



public class ActorUtils {
  
  
  /**  General migration utilities-
    */
  static AreaTile findTransitPoint(
    Area map, Base base, Base with, Actor client
  ) {
    ActorType type = client.type();
    int moveMode = type.moveMode;
    int size = Nums.max(type.wide, type.high);
    return findTransitPoint(map, base, with, moveMode, size);
  }
  
  
  static AreaTile findTransitPoint(
    Area map, Base base, Base with, int moveMode, int clientSize
  ) {
    if (map == null || base == null || with == null) return null;
    
    //  TODO:  Make sure there's a pathing connection to the main settlement
    //  here!
    
    boolean land = moveMode == Type.MOVE_LAND;
    
    if (land) {
      AreaTile current = map.transitPoints.get(with);
      if (current != null && ! map.blocked(current.x, current.y)) return current;
    }
    
    Pick <AreaTile> pick = new Pick();
    Vec2D cityDir = new Vec2D(
      with.locale.mapX - base.locale.mapX,
      with.locale.mapY - base.locale.mapY
    ).normalise(), temp = new Vec2D();
    
    //  Larger actors will need to start out further from the map edge...
    int maxPerim = map.size - (1 + clientSize);
    
    for (Coord c : Visit.perimeter(1, 1, maxPerim, maxPerim)) {
      if (land && map.blocked(c.x, c.y)) continue;
      
      temp.set(c.x - (map.size / 2), c.y - (map.size / 2)).normalise();
      float rating = 1 + temp.dot(cityDir);
      if (land && map.pathType(c) == Type.PATH_PAVE) rating *= 2;
      
      AreaTile u = map.tileAt(c.x, c.y);
      pick.compare(u, rating);
    }
    
    AreaTile point = pick.result();
    map.transitPoints.put(with, point);
    return point;
  }
  
  
  static float distanceRating(Base from, Base goes) {
    return AVG_CITY_DIST / (AVG_CITY_DIST + from.distance(goes, Type.MOVE_AIR));
  }
  
  
  
  /**  Finding migrants in and out-
    */
  public static Actor generateMigrant(
    ActorType jobType, Element employs, boolean payHireCost
  ) {
    Base  goes  = employs.base();
    World world = goes.world;
    int   cost  = goes.hireCost(jobType);
    
    if (! world.settings.toggleMigrate) return null;
    if (payHireCost) goes.incFunds(0 - cost);
    
    Actor migrant = (Actor) jobType.generate();
    migrant.assignBase(goes);
    ((Employer) employs).setWorker(migrant, true);
    if (jobType.isPerson()) jobType.initAsMigrant((ActorAsPerson) migrant);
    
    if (employs.onMap()) {
      //  TODO:  Consider a wider variety of cities to source from...
      if (world.settings.toggleShipping) {
        Base homeland = goes.homeland();
        if (homeland != null && homeland.traderFor(goes) != null) {
          homeland.addMigrant(migrant);
        }
        else {
          return null;
        }
      }
      else {
        Area map  = employs.map();
        Base from = map.locals;
        map.world.beginJourney(from, goes, Type.MOVE_AIR, migrant);
      }
    }
    else {
      migrant.setInside((Pathing) employs, true);
    }
    
    return migrant;
  }
  
  
  static void findWork(Area map, Actor migrant) {
    
    class Opening { Building b; ActorType position; }
    AreaTile from = migrant.at();
    Base homeC = migrant.base();
    final Pick <Opening> pick = new Pick();
    
    for (Building b : map.buildings) if (b.complete()) {
      if (homeC != null && b.base() != homeC) continue;
      
      for (ActorType t : b.type().workerTypes.keys()) {
        int space = b.maxWorkers(t) - b.numWorkers(t);
        if (space <= 0) continue;
        
        float fitness = 0, sumWeights = 0;
        for (Trait skill : t.initTraits.keys()) {
          float level = t.initTraits.valueFor(skill);
          fitness += migrant.traits.levelOf(skill) / level;
          sumWeights += level;
        }
        fitness /= Nums.max(1, sumWeights);
        if (fitness <= 0.5f) continue;
        
        float near = Area.distancePenalty(from, b);
        Opening o = new Opening();
        o.b = b;
        o.position = t;
        pick.compare(o, near * fitness);
      }
    }
    
    Opening o = pick.result();
    if (o != null) {
      Building b = o.b;
      migrant.assignType(o.position);
      b.setWorker(migrant, true);
      if (b.allowsResidence(migrant)) b.setResident(migrant, true);
    }
  }
  
  
  static void findHome(Area map, final Actor migrant) {
    
    //  Each citizen prompts the search based on proximity to their place of
    //  work, proximity to needed services, and safety of the location (they
    //  prefer being behind walls, for example- either that, or you have a
    //  high safety rating.)
    
    //  TODO:  You should, ideally, be making this check periodically to see if
    //  a better opening for a home is available.
    
    if (migrant.work() == null) return;
    if (migrant.home() != null) return;
    if (! migrant.type().isPerson()) return;
    
    class SumPos extends Vec2D {
      float sumWeights;
      
      void addPos(Pathing b, float weight) {
        if (b == null) return;
        x += b.at().x * weight;
        y += b.at().y * weight;
        sumWeights += weight;
      }
    }
    SumPos sumPos = new SumPos();
    int maxRange = MAX_WANDER_RANGE * 2;
    
    Pathing work = (Pathing) migrant.work();
    sumPos.addPos(work, 1);
    
    Building market = findNearestWithFeature(IS_VENDOR, maxRange, work);
    sumPos.addPos(market, 1);
    
    Building refuge = findNearestWithFeature(IS_REFUGE, maxRange, work);
    sumPos.addPos(refuge, 1);
    
    if (market == null) return;
    if (refuge == null) return;
    
    sumPos.scale(1f / sumPos.sumWeights);
    
    final AreaTile from = map.tileAt(sumPos.x, sumPos.y);
    Pick <Building> pick = new Pick <Building> () {
      public void compare(Building b, float rating) {
        if (! b.allowsResidence(migrant)) return;
        
        float numR = b.numResidents(migrant.type().socialClass);
        if (b == migrant.home()) numR = 0;
        
        float near = 10 / (10f + Area.distance(from, b));
        float comfort = b.type().homeComfortLevel * 1f / AVG_HOME_COMFORT;
        
        super.compare(b, rating * near * (1 + comfort) / (2 + numR));
      }
    };
    
    //  TODO:  This could get very computationally-intensive on large maps.
    //  Try to improve on that?
    
    for (Building b : map.buildings) if (b.complete()) {
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
      AreaTile goes = findEntryPoint(home, map, from, maxRange / 2);
      
      if (goes != null) {
        home.assignBase(migrant.base());
        home.setLocation(goes, map);
        map.planning.placeObject(home);
        home.setResident(migrant, true);
      }
    }
  }
  
  
  static AreaTile findEntryPoint(
    final Element enters, final Area map,
    final Target from, final int maxRange
  ) {
    final Vars.Ref <AreaTile> result = new Vars.Ref();
    final AreaTile temp[] = new AreaTile[9];
    
    Flood <AreaTile> flood = new Flood <AreaTile> () {
      protected void addSuccessors(AreaTile front) {
        if (result.value != null) return;
        
        for (AreaTile n : Area.adjacent(front, temp, map)) {
          if (n == null || n.flaggedWith() != null) continue;
          if (maxRange > 0 && Area.distance(from, n) > maxRange) continue;
          
          enters.setLocation(n, map);
          if (enters.canPlace(map)) {
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
  
  
  
  public static AreaTile pickRandomTile(Target t, float range, Area map) {
    final float angle = Rand.num() * Nums.PI * 2;
    final float dist = (Rand.num() * range) + 1, max = map.size - 1;
    final AreaTile at = t.at();
    return map.tileAt(
      Nums.clamp(at.x + (float) (Nums.cos(angle) * dist), 0, max),
      Nums.clamp(at.y + (float) (Nums.sin(angle) * dist), 0, max)
    );
  }
  

  
  /**  More utility methods for scenario setup-
    */
  public static void fillAllWorkVacancies(Area map) {
    for (Building b : map.buildings) if (b.complete()) {
      fillWorkVacancies(b);
      for (Actor w : b.workers) ActorUtils.findHome(map, w);
    }
  }
  
  
  public static void fillWorkVacancies(Building b) {
    for (ActorType t : b.type().workerTypes.keys()) {
      while (b.numWorkers(t) < b.maxWorkers(t)) {
        spawnActor(b, t, false);
      }
    }
  }
  
  
  public static Actor spawnActor(Building b, ActorType type, boolean resident) {
    
    Actor actor = (Actor) type.generate();
    AreaTile at = b.centre();
    
    if (type.isPerson()) {
      type.initAsMigrant((ActorAsPerson) actor);
    }
    
    if (resident) b.setResident(actor, true);
    else          b.setWorker  (actor, true);
    
    if (b.complete()) {
      actor.enterMap(b.map, at.x, at.y, 1, b.base());
      actor.setInside(b, true);
    }
    else {
      at = randomTileNear(at, b.radius(), b.map, true);
      actor.enterMap(b.map, at.x, at.y, 1, b.base());
    }
    
    return actor;
  }
  
  
  public static AreaTile randomTileNear(
    AreaTile at, float range, Area map, boolean open
  ) {
    int x = (int) (at.x + (range * Rand.range(-1, 1)));
    int y = (int) (at.y + (range * Rand.range(-1, 1)));
    AreaTile t = map.tileAt(Nums.clamp(x, map.size), Nums.clamp(y, map.size));
    if (open) t = AreaTile.nearestOpenTile(t, map, (int) range);
    return t;
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


