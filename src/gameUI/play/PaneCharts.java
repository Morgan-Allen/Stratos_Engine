

package gameUI.play;
import game.*;
import static game.GameConstants.*;
import util.*;



public class PaneCharts extends DetailPane {
  
  
  final PlayUI UI;
  
  
  PaneCharts(PlayUI UI) {
    super(UI, null);
    this.UI = UI;
  }
  
  
  protected void updateState() {
    final Area area = UI.area;
    final Base base = UI.base;
    
    this.text.setText("");
    final Description d = this.text;
    d.append("\nSector Charts: ");
    
    d.append("\n\n  <Under construction!>");
    
    //  TODO:  Fill this in...
    
    super.updateState();
  }
  

}


