

package gameUI.play;
import game.*;
import static game.GameConstants.*;
import util.*;



public class PaneMarkets extends DetailPane {
  
  
  final PlayUI UI;
  
  
  PaneMarkets(PlayUI UI) {
    super(UI, null);
    this.UI = UI;
  }
  
  
  protected void updateState() {
    final Area area = UI.area;
    final Base base = UI.base;
    final Base home = base.homeland();
    
    this.text.setText("");
    final Description d = this.text;
    d.append("\nMarket Goods: ");
    
    
    //  TODO:  It might help to set import/export options from here?
    
    for (Good g : base.world.goodTypes()) {
      if (g == CASH) continue;
      
      int amount  = (int) base.inventory().valueFor(g);
      int needs   = (int) base.needLevel(g);
      int produce = (int) base.prodLevel(g);
      
      float made = base.totalMade(g);
      float used = base.totalUsed(g);
      
      d.append("\n  "+I.padToLength(g.name, 10)+": "+amount+"/"+needs);
      
      if (home != null) {
        float priceI = base.importPrice(g, home);
        float priceE = base.exportPrice(g, home);
      }
      
      //  Supply and demand!
      //  Production and consumption!
      //  Import/export allowance!
    }
    
    super.updateState();
  }
}






