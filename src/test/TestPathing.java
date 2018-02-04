

package test;
import game.*;
import content.*;
import util.*;
import static content.GameContent.*;
import static game.GameConstants.*;



public class TestPathing extends Test {
  
  
  
  public static void main(String args[]) {
    testPathing(true);
  }
  
  
  static boolean testPathing(boolean graphics) {
    Test test = new TestPathing();
    
    boolean testOkay = true;
    
    City base = setupTestCity(32, ALL_GOODS, false);
    CityMap map = base.activeMap();
    World world = map.world;
    world.settings.toggleFog     = false;
    world.settings.toggleHunger  = false;
    world.settings.toggleHunger  = false;
    world.settings.toggleMigrate = false;
    
    //
    //  First, place a variety of structures on the map and test to
    //  ensure that pathing-checks function correctly between various
    //  points on the ground, walls, between gates, et cetera:
    
    CityMapPlanning.placeStructure(SHIELD_WALL, base, true, 4, 4, 20, 20);
    CityMapPlanning.markDemolish(map, true, 6, 6, 16, 16);
    
    Building gate = (Building) BLAST_DOOR.generate();
    gate.setFacing(TileConstants.N);
    gate.enterMap(map, 14, 22, 1, base);
    
    Building tower1 = (Building) TURRET.generate();
    tower1.setFacing(TileConstants.W);
    tower1.enterMap(map, 4, 14, 1, base);
    
    Building tower2 = (Building) TURRET.generate();
    tower2.setFacing(TileConstants.E);
    tower2.enterMap(map, 22, 14, 1, base);
    
    Building home = (Building) HOLDING.generate();
    home.enterMap(map, 10, 10, 1, base);
    
    Tile cornerWall   = map.tileAt(4 , 4 );
    Tile endWall      = map.tileAt(20, 4 );
    Tile cornerGround = map.tileAt(6 , 6 );
    Tile innerGround  = map.tileAt(9 , 12);
    Tile outerGround  = map.tileAt(16, 31);
    Tile oppGround    = map.tileAt(20, 14);
    
    Pathing p1[], p2[];
    p1 = cornerWall  .adjacent(null, map);
    p2 = cornerGround.adjacent(null, map);
    
    for (Pathing n : p1) {
      if (n != null && map.above(((Tile) n)) == null) {
        I.say("\nWALL TILES SHOULD ONLY BORDER OTHER WALL TILES!");
        testOkay = false;
        if (! graphics) return false;
      }
    }
    for (Pathing n : p2) {
      if (n != null && map.above(((Tile) n)) != null) {
        I.say("\nGROUND TILES SHOULD ONLY BORDER OTHER GROUND TILES!");
        testOkay = false;
        if (! graphics) return false;
      }
    }
    
    p1 = tower1.entrances()[0].adjacent(null, map);
    p2 = tower1.entrances()[1].adjacent(null, map);
    
    if (! Visit.arrayIncludes(p1, tower1)) {
      I.say("\nENTRANCES MUST LEAD TO BUILDING");
      testOkay = false;
      if (! graphics) return false;
    }
    if (! Visit.arrayIncludes(p2, tower1)) {
      I.say("\nENTRANCES MUST LEAD TO BUILDING");
      testOkay = false;
      if (! graphics) return false;
    }
    
    p1 = checkPathingOkay(cornerWall, endWall    , map);
    p2 = checkPathingOkay(cornerWall, innerGround, map);
    
    if (p1 == null) {
      I.say("\nPATHING ALONG WALL WAS NOT POSSIBLE");
      testOkay = false;
      if (! graphics) return false;
    }
    if (! Visit.arrayIncludes(p2, tower1)) {
      I.say("\nWALL-TO-GROUND PATHS SHOULD GO BY TOWER");
      testOkay = false;
      if (! graphics) return false;
    }
    
    p1 = checkPathingOkay(innerGround, outerGround, map);
    p2 = checkPathingOkay(oppGround  , endWall    , map);
    
    if (! Visit.arrayIncludes(p1, gate)) {
      I.say("\nEXITING COURTYARD SHOULD GO BY GATE");
      testOkay = false;
      if (! graphics) return false;
    }
    if (! Visit.arrayIncludes(p2, tower2)) {
      I.say("\nGROUND-TO-WALL PATHS SHOULD GO BY TOWER");
      testOkay = false;
      if (! graphics) return false;
    }
    
    //
    //  Next, introduce a number of different actors and ensure they
    //  can actually travel between a variety of different locations-
    
    Batch <Actor> actors = new Batch();
    Table <Actor, Pathing> destinations = new Table();
    Tally <Actor> numInside = new Tally();
    
    Tile initPoints[] = { cornerWall, innerGround, outerGround };
    
    for (int n = 3; n-- > 0;) {
      Actor a = (Actor) Vassals.PYON.generate();
      Tile point = initPoints[n];
      a.enterMap(map, point.x, point.y, 1, base);
      actors.add(a);
      destinations.put(a, a.at());
    }
    
    boolean insideWrong = false;
    boolean pathWrong   = false;
    boolean pathingDone = false;
    int numReachedDest = 0, shouldReach = actors.size() * 10;
    
    while (map.time() < 1000 || graphics) {
      numInside.clear();
      
      for (Tile t : map.allTiles()) {
        for (Actor i : t.inside()) {
          numInside.add(1, i);
          if (t != i.at()) {
            I.say("\n"+i+" REGISTERED IN WRONG TILE!");
            I.say("  At: "+i.at()+", found in: "+t);
            insideWrong = true;
          }
        }
      }
      
      for (Actor a : actors) {
        
        if (! a.alive()) {
          I.say("\n"+a+" IS DEAD, SOMEHOW?");
          pathWrong = true;
        }
        
        if (numInside.valueFor(a) != 1) {
          I.say("\n"+a+" NOT REGISTERED IN CORRECT NUMBER OF TILES!");
          I.say("  Inside: "+numInside.valueFor(a)+", expected 1");
          insideWrong = true;
        }
        
        Pathing dest = destinations.get(a);
        if (dest == a.at() || dest == a.inside()) {
          numReachedDest += 1;
          
          Pathing goes;
          if (a.inside() != home && ! home.hasFocus()) {
            goes = home;
            a.assignTask(a.visitTask(home, 0, Task.JOB.RETURNING, null));
          }
          else {
            goes = map.tileAt(Rand.index(map.size()), Rand.index(map.size()));
            a.assignTask(a.targetTask(goes, 0, Task.JOB.EXPLORING, null));
          }
          destinations.put(a, goes);
          
          Pathing path[] = a.task() == null ? null : a.currentPath();
          //I.say("\nSending "+a+" to "+goes);
          //I.say("  Path: "+I.list(path));
          if (! Task.verifyPath(path, dest, goes, map)) {
            I.say("\n"+a+" CONSTRUCTED INVALID PATH-");
            I.say("  From: "+a.at()+", Goes: "+goes+"\n  ");
            I.add(I.list(path));
            pathWrong = true;
          }
        }
      }
      
      if (insideWrong || pathWrong) {
        I.say("Inside wrong: "+insideWrong);
        I.say("Path wrong:   "+pathWrong  );
        if (! graphics) break;
      }
      
      if ((! pathingDone) && numReachedDest >= shouldReach && testOkay) {
        pathingDone = true;
        if (pathingDone) {
          I.say("\nPATHING TEST CONCLUDED SUCCESSFULLY!");
          if (! graphics) return true;
        }
      }
      
      test.runLoop(base, 1, graphics, "saves/test_pathing.tlt");
    }
    
    I.say("\nPATHING TEST FAILED!");
    I.say("  Current time: "+map.time());
    I.say("  Reached destination: "+numReachedDest+"/"+shouldReach);
    for (Actor a : actors) if (a.dead()) I.say("  "+a+" is dead!");
    
    return false;
  }
  
  
  private static Pathing[] checkPathingOkay(
    Pathing from, Pathing goes, CityMap map
  ) {
    ActorPathSearch search = new ActorPathSearch(map, from, goes, -1);
    search.doSearch();
    Pathing path[] = search.fullPath(Pathing.class);
    if (! Task.verifyPath(path, from, goes, map)) {
      I.say("\nPATH IS INVALID-");
      I.say("  From: "+from+", Goes: "+goes+"\n  ");
      I.add(I.list(path));
      return null;
    }
    return path;
  }
  
  
}














