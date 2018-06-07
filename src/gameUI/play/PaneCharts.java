

package gameUI.play;
import game.*;
import graphics.widgets.*;
import util.*;



public class PaneCharts extends DetailPane {
  
  
  final PlayUI UI;
  
  
  PaneCharts(PlayUI UI) {
    super(UI, null);
    this.UI = UI;
  }
  
  
  protected void updateText(Text text) {
    
    text.setText("");
    final Description d = text;
    
    final Area area = UI.area;
    final Base base = UI.base;
    
    d.append("\nSector Charts: ");
    
    d.append("\n\n  <Under construction!>");
    
    //  TODO:  Fill this in...
    
  }
  

}


