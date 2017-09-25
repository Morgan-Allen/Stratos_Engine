

package game;
import static game.BuildingSet.*;
import util.*;



public class TestLoop {
  
  
  static void runGameLoop(CityMap map) {
    
    int graphic[][] = new int[map.size][map.size];
    boolean paused = false;
    Coord   hover  = new Coord(-1, -1);
    boolean click  = false;
    Fixture above  = null ;
    Series <Character> pressed = new Batch();
    
    
    while (true) {
      
      for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
        int fill = BLANK_COLOR;
        Tile at = map.tileAt(c.x, c.y);
        if      (at.above != null) fill = at.above.type.tint;
        else if (at.paved        ) fill = PAVE_COLOR;
        graphic[c.x][c.y] = fill;
      }
      for (Walker w : map.walkers) if (w.inside == null) {
        int fill = WALKER_COLOR;
        if (w.home != null) fill = w.home.type.tint;
        graphic[w.x][w.y] = fill;
      }
      try { graphic[hover.x][hover.y] = WHITE_COLOR; }
      catch (Exception e) {}
      
      
      I.present(graphic, "City Map", 400, 400);
      hover   = I.getDataCursor("City Map", false);
      click   = I.checkMouseClicked("City Map");
      pressed = I.getKeysPressed("City Map");
      above   = map.above(hover.x, hover.y);
      
      
      if (above instanceof Building) {
        I.presentInfo(reportFor((Building) above), "City Map");
      }
      else {
        I.presentInfo("Paused: "+paused, "City Map");
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
      }
      catch (Exception e) { I.report(e); }
      
      
      if (! paused) {
        map.update();
      }
      try { Thread.sleep(100); }
      catch (Exception e) {}
    }
  }
  
  
  private static String reportFor(Building b) {
    String report = ""+b.type.name+"\n";
    
    if (b.walkers.size() > 0) {
      report += "\nWalkers:";
      for (Walker w : b.walkers) {
        report += "\n  "+w;
      }
    }
    
    if (b.visitors.size() > 0) {
      report += "\nVisitors:";
      for (Walker w : b.visitors) {
        report += "\n  "+w;
      }
    }
    
    if (b.craftProgress > 0) {
      report += "\nCraft progress:\n  "+b.craftProgress;
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




