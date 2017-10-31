

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class TestPathing extends Test {
  
  
  
  public static void main(String args[]) {
    testPathing(true);
  }
  
  
  static boolean testPathing(boolean graphics) {
    CityMap map = setupTestCity(32);
    
    Batch <Actor> actors = new Batch();
    Table <Actor, Tile> destinations = new Table();
    Tally <Actor> numInside = new Tally();
    
    for (int n = 3; n-- > 0;) {
      Actor a = (Actor) CITIZEN.generate();
      a.enterMap(map, Rand.index(map.size), Rand.index(map.size), 1);
      actors.add(a);
      destinations.put(a, a.at());
    }
    
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
        
        if (a.at() == destinations.get(a)) {
          numReachedDest += 1;
          Tile goes = map.tileAt(Rand.index(map.size), Rand.index(map.size));
          destinations.put(a, goes);
          
          a.embarkOnTarget(goes, 0, Task.JOB.WANDERING, null);
          Tile path[] = a.task == null ? null : a.task.path;
          
          if (! Task.verifyPath(path, a.at(), goes)) {
            I.say("\n"+a+"CONSTRUCTED INVALID PATH-");
            I.say("  From: "+a.at()+", Goes: "+goes+"\n  ");
            I.add(I.list(path));
            pathWrong = true;
          }
        }
      }
      
      if (insideWrong || pathWrong) {
        I.say("Inside wrong: "+insideWrong);
        I.say("Path wrong:   "+pathWrong  );
        break;
      }
      
      if ((! pathingDone) && numReachedDest == actors.size() * 3) {
        pathingDone = true;
        
        if (pathingDone) {
          I.say("\nPATHING TEST CONCLUDED SUCCESSFULLY!");
          if (! graphics) return true;
        }
      }
      
      Test.runGameLoop(map, 1, graphics, "saves/test_pathing.tlt");
    }
    
    I.say("\nPATHING TEST FAILED!");
    I.say("  Current time: "+map.time);
    I.say("  Reached destination: "+numReachedDest+"/"+actors.size());
    
    return false;
  }
  
}














