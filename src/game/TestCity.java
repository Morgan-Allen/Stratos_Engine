

package game;
import static game.Goods.*;
import static game.BuildingSet.*;
import util.*;



public class TestCity {
  
  
  public static void main(String args[]) {
    
    City map = new City();
    map.performSetup(20);
    
    Tile.applyPaving(map, 3, 8, 12, 1 , true);
    Tile.applyPaving(map, 8, 2, 1 , 16, true);

    Building palace = new HomeBuilding(PALACE    );
    Building house1 = new HomeBuilding(HOUSE     );
    Building house2 = new HomeBuilding(HOUSE     );
    Building court  = new Building    (BALL_COURT);
    
    palace.enterMap(map, 3 , 3 );
    house1.enterMap(map, 9 , 6 );
    house2.enterMap(map, 12, 6 );
    court .enterMap(map, 9 , 9 );

    Building quarry = new CraftBuilding(QUARRY_PIT);
    Building kiln1  = new CraftBuilding(KILN      );
    Building kiln2  = new CraftBuilding(KILN      );
    Building market = new CraftBuilding(MARKET    );
    
    quarry.enterMap(map, 4 , 15);
    kiln1 .enterMap(map, 9 , 17);
    kiln2 .enterMap(map, 9 , 14);
    market.enterMap(map, 4 , 9 );
    
    quarry.inventory.add(2, CLAY   );
    market.inventory.add(3, POTTERY);
    
    
    int graphic[][] = new int[map.size][map.size];
    boolean paused = false;
    Coord   hover  = null ;
    boolean click  = false;
    Fixture above  = null ;
    
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
      if (hover != null) try { graphic[hover.x][hover.y] = FILL_COLOR; }
      catch (Exception e) {}
      
      I.present(graphic, "City Map", 400, 400);
      
      hover = I.getDataCursor("City Map", false);
      click = I.checkMouseClicked("City Map");
      above = map.above(hover.x, hover.y);
      
      if (above instanceof Building) {
        I.presentInfo(reportFor((Building) above), "City Map");
      }
      if (click) {
        paused = ! paused;
      }
      
      if (! paused) map.update();
      
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









