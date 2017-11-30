

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class TestPathing extends Test {
  
  
  
  public static void main(String args[]) {
    testPathing(true);
  }
  
  
  static boolean testPathing(boolean graphics) {
    Test test = new TestPathing();
    
    CityMap map = setupTestCity(32);
    map.settings.toggleFog     = false;
    map.settings.toggleHunger  = false;
    map.settings.toggleHunger  = false;
    map.settings.toggleMigrate = false;
    
    Batch <Actor> actors = new Batch();
    Table <Actor, Pathing> destinations = new Table();
    Tally <Actor> numInside = new Tally();
    
    for (int n = 3; n-- > 0;) {
      Actor a = (Actor) CITIZEN.generate();
      a.enterMap(map, Rand.index(map.size), Rand.index(map.size), 1);
      actors.add(a);
      destinations.put(a, a.at());
    }
    
    Building home = (Building) HOUSE.generate();
    home.enterMap(map, 10, 10, 1);
    
    //  TODO:  You also have to test pathing to and from buildings (and
    //  possibly through gatehouses and over walls, et cetera.)
    
    //  For now, all we want to test is that actors can, in fact, get from
    //  point A to point B, and that they are present in one and only one tile
    //  at any given time.
    
    boolean insideWrong = false;
    boolean pathWrong   = false;
    boolean pathingDone = false;
    int numReachedDest = 0;
    
    while (map.time < 1000 || graphics) {
      
      numInside.clear();
      
      for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
        Tile t = map.tileAt(c);
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
        if (dest == a.at() || dest == a.inside) {
          numReachedDest += 1;
          
          Pathing goes;
          if (a.inside != home && ! home.hasFocus()) {
            goes = home;
            a.embarkOnVisit(home, 0, Task.JOB.WANDERING, null);
          }
          else {
            goes = map.tileAt(Rand.index(map.size), Rand.index(map.size));
            a.embarkOnTarget(goes, 0, Task.JOB.WANDERING, null);
          }
          destinations.put(a, goes);
          
          Pathing path[] = a.task == null ? null : a.task.path;
          
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
      
      if ((! pathingDone) && numReachedDest == actors.size() * 3) {
        pathingDone = true;
        
        if (pathingDone) {
          I.say("\nPATHING TEST CONCLUDED SUCCESSFULLY!");
          if (! graphics) return true;
        }
      }
      
      map = test.runLoop(map, 1, graphics, "saves/test_pathing.tlt");
    }
    
    I.say("\nPATHING TEST FAILED!");
    I.say("  Current time: "+map.time);
    I.say("  Reached destination: "+numReachedDest+"/"+actors.size());
    for (Actor a : actors) if (a.dead()) I.say("  "+a+" is dead!");
    
    return false;
  }
  
}














