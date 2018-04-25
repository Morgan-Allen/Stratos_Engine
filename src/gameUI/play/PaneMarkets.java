

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
    d.append("\nMarket Goods:          Buy / Sell");
    
    
    //  TODO:  It might help to set import/export options from here?
    
    for (Good g : base.world.goodTypes()) {
      if (g == CASH) continue;
      
      int amount  = (int) base.inventory().valueFor(g);
      int needs   = (int) base.needLevel(g);
      int produce = (int) base.prodLevel(g);
      
      float made = base.totalMade(g);
      float used = base.totalUsed(g);
      
      d.append("\n  "+I.padToLength(g.name, 10)+": ");
      d.append(I.padToLength(amount+"/"+needs, 9));
      
      if (home != null) {
        int priceI = (int) base.importPrice(g, home);
        int priceE = (int) base.exportPrice(g, home);
        d.append(I.padToLength(""+priceI, 6));
        d.append(I.padToLength(""+priceE, 6));
        //d.append("\n    Buy  for: "+priceI);
        //d.append("\n    Sell for: "+priceE);
      }
      
      //  Supply and demand!
      //  Production and consumption!
      //  Import/export allowance!
    }
    
    super.updateState();
  }
}






