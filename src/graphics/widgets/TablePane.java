

package graphics.widgets;
import util.*;




public class TablePane extends UIGroup {
  
  
  final int numCols, numRows;
  
  
  public TablePane(HUD UI, int cols, int rows) {
    super(UI);
    this.numCols = cols;
    this.numRows = rows;
    
    for (Coord c : Visit.grid(0, 0, cols, rows, 1)) {
      final UIGroup entry = initEntry(UI, c.x, c.y);
      if (entry == null) continue;
      int y = rows - (1 + c.y);
      entry.alignAcross(c.x * 1f / cols, (c.x + 1) * 1f / cols);
      entry.alignDown  (y   * 1f / rows, (y   + 1) * 1f / rows);
      entry.attachTo(this);
    }
  }
  
  
  protected UIGroup initEntry(HUD UI, int col, int row) {
    return new UIGroup(UI);
  }
  
}