

package game;
import static game.GameConstants.*;
import util.*;



public class TestLoop {
  

  static int graphic[][] = null;
  static boolean paused = false;
  static Coord   hover  = new Coord(-1, -1);
  static Fixture above  = null;
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
        for (Walker w : map.walkers) if (w.inside == null) {
          int fill = WALKER_COLOR;
          if (w.home != null) fill = w.home.type.tint;
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
  
  
  private static String baseReport(CityMap map, boolean paused) {
    String report = "";
    if (map.city != null) {
      report += "Funding: "+map.city.currentFunds;
      report += "\n";
    }
    report += "Paused: "+paused;
    return report;
  }
  
  
  private static String reportFor(Building b) {
    
    String report = ""+b+"\n";
    
    if (b.resident.size() > 0) {
      report += "\nWalkers:";
      for (Walker w : b.resident) {
        report += "\n  "+w+" ("+w.jobType()+")";
      }
    }
    
    if (b.visitors.size() > 0) {
      report += "\nVisitors:";
      for (Walker w : b.visitors) {
        report += "\n  "+w+" ("+w.jobType()+")";
      }
    }
    
    if (b.formation() != null && b.formation().recruits.size() > 0) {
      report += "\nRecruits:";
      for (Walker w : b.formation().recruits) {
        report += "\n  "+w;
      }
    }
    
    if (b.buildLevel < 1) {
      report += "\nBuild level:\n  "+I.percent(b.buildLevel);
    }
    
    if (b.craftProgress > 0) {
      report += "\nCraft progress:\n  "+I.percent(b.craftProgress);
    }
    
    if (b.inventory.size() > 0) {
      report += "\nGoods:";
      for (Good g : ALL_GOODS) {
        float amount = b.inventory.valueFor(g);
        if (amount <= 0) continue;
        report += "\n  "+g+": "+amount;
      }
    }
    
    return report;
  }
  
  
}




