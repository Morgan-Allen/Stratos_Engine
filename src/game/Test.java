

package game;
import static game.GameConstants.*;
import util.*;



public class Test {
  

  static int graphic[][] = null;
  static boolean paused = false;
  static Coord   hover  = new Coord(-1, -1);
  static Element above  = null;
  static Series <Character> pressed = new Batch();
  
  
  static CityMap runGameLoop(
    CityMap map, int numUpdates, boolean graphics
  ) {
    
    while (true) {
      
      if (graphics) {
        if (graphic == null || graphic.length != map.size) {
          graphic = new int[map.size][map.size];
        }
        
        for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
          int fill = BLANK_COLOR;
          CityMap.Tile at = map.tileAt(c.x, c.y);
          if      (at.above != null  ) fill = at.above.type.tint;
          else if (at.paved          ) fill = PAVE_COLOR;
          else if (at.terrain != null) fill = at.terrain.tint;
          graphic[c.x][c.y] = fill;
        }
        for (Actor w : map.walkers) if (w.inside == null) {
          int fill = WALKER_COLOR;
          if      (w.work != null) fill = w.work.type.tint;
          else if (w.home != null) fill = w.home.type.tint;
          graphic[w.at.x][w.at.y] = fill;
        }
        try { graphic[hover.x][hover.y] = WHITE_COLOR; }
        catch (Exception e) {}
        
        I.present(graphic, "City Map", 400, 400);
        hover   = I.getDataCursor("City Map", false);
        pressed = I.getKeysPressed("City Map");
        above   = map.above(hover.x, hover.y);
        
        if (above instanceof Building) {
          I.presentInfo(reportFor((Building) above), "City Map");
          I.talkAbout = above;
        }
        else {
          I.presentInfo(baseReport(map, paused), "City Map");
          I.talkAbout = null;
        }
        
        if (pressed.includes('p')) {
          paused = ! paused;
          I.say("\nPAUSING: "+paused);
        }
        
        if (pressed.includes('s')) try {
          I.say("\nWILL SAVE CURRENT MAP...");
          Session.saveSession("test_save.tlt", map);
        }
        catch (Exception e) { I.report(e); }
        
        if (pressed.includes('l')) try {
          I.say("\nWILL LOAD SAVED MAP...");
          Session s = Session.loadSession("test_save.tlt", true);
          map = (CityMap) s.loaded()[0];
          if (map == null) throw new Exception("No map loaded!");
        }
        catch (Exception e) { I.report(e); }
      }
      
      if (! paused) {
        map.update();
        if (numUpdates > 0 && --numUpdates == 0) break;
      }
      
      if (graphics) {
        try { Thread.sleep(100); }
        catch (Exception e) {}
      }
    }
    
    return map;
  }
  
  
  
  /**  Other setup utilities:
    */
  static CityMap setupTestCity(int size) {
    World   world = new World();
    City    city  = new City(world);
    CityMap map   = new CityMap(city);
    map.performSetup(size);
    return map;
  }
  
  
  static void fillAllVacancies(CityMap map) {
    for (Building b : map.buildings) {
      fillWorkVacancies(b);
      for (Actor w : b.workers) CityBorders.findHome(map, w);
    }
  }
  
  
  static void fillWorkVacancies(Building b) {
    for (Type t : b.type.workerTypes) {
      while (b.numWorkers(t) < b.maxWorkers(t)) {
        spawnWalker(b, t, false);
      }
    }
  }
  
  
  static void fillHomeVacancies(Building b, Type... types) {
    for (Type t : types) {
      while (b.numResidents(t.socialClass) < b.maxResidents(t.socialClass)) {
        spawnWalker(b, t, true);
      }
    }
  }
  
  
  static Actor spawnWalker(Building b, Type type, boolean resident) {
    
    Actor walker = (Actor) type.generate();
    type.initAsMigrant(walker);
    walker.enterMap(b.map, b.at.x, b.at.y, 1);
    walker.inside = b;
    
    if (resident) b.setResident(walker, true);
    else          b.setWorker  (walker, true);
    b.visitors.add(walker);
    
    return walker;
  }
  
  
  
  /**  UI outputs:
    */
  private static String baseReport(CityMap map, boolean paused) {
    String report = "";
    if (map.city != null) {
      report += "Funding: "+map.city.currentFunds;
      report += "\n";
    }
    report += "Time: "+map.time;
    report += "\nPaused: "+paused;
    return report;
  }
  
  
  private static String reportFor(Building b) {
    
    StringBuffer report = new StringBuffer(""+b+"\n");
    
    if (b.workers.size() > 0) {
      report.append("\nWorkers:");
      for (Actor w : b.workers) {
        report.append("\n  "+w+" ("+w.jobType()+")");
      }
    }
    
    if (b.residents.size() > 0) {
      report.append("\nResidents:");
      for (Actor w : b.residents) {
        report.append("\n  "+w+" ("+w.jobType()+")");
      }
    }
    
    if (b.visitors.size() > 0) {
      report.append("\nVisitors:");
      for (Actor w : b.visitors) {
        report.append("\n  "+w+" ("+w.jobType()+")");
      }
    }
    
    if (b.formation() != null && b.formation().recruits.size() > 0) {
      report.append("\nRecruits:");
      for (Actor w : b.formation().recruits) {
        report.append("\n  "+w);
      }
    }
    
    if (b.buildLevel < 1) {
      report.append("\nBuild level:\n  "+I.percent(b.buildLevel));
    }
    
    if (b.craftProgress() > 0) {
      report.append("\nCraft progress:\n  "+I.percent(b.craftProgress()));
    }
    
    List <String> goodRep = new List();
    for (Good g : ALL_GOODS) {
      float amount = b.inventory.valueFor(g);
      float demand = b.demandFor(g) + amount;
      if (amount <= 0 && demand <= 0) continue;
      report.append("\n  "+g+": "+I.shorten(amount, 1)+"/"+I.shorten(demand, 1));
    }
    
    if (! goodRep.empty()) {
      report.append("\nGoods:");
      for (String s : goodRep) report.append(s);
    }
    
    return report.toString();
  }
  
  
}






