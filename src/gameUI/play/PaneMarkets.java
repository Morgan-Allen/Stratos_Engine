

package gameUI.play;
import game.*;
import static game.GameConstants.*;
import graphics.widgets.*;
import util.*;



public class PaneMarkets extends DetailPane {
  
  
  final PlayUI UI;
  
  
  PaneMarkets(PlayUI UI) {
    super(UI, null);
    this.UI = UI;
  }
  

  protected void updateText(Text text) {
    text.setText("");
    final Description d = text;
    
    final AreaMap area = UI.area;
    final World world = UI.base.world;
    final Base  base  = UI.base;
    final Base  home  = base.federation().homeland();
    
    d.append("\nMarket Goods:          Buy / Sell");
    //  TODO:  It might help to set import/export options from here?
    
    for (Good g : base.world.goodTypes()) {
      if (g == CASH) continue;
      
      int amount  = (int) base.inventory().valueFor(g);
      int needs   = (int) base.trading.needLevel(g);
      int produce = (int) base.trading.prodLevel(g);
      
      float made = base.trading.totalMade(g);
      float used = base.trading.totalUsed(g);
      
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
      
      //  TODO:  Allow toggling of import/export functions here.
      /*
      d.append(new Description.Link("Hire "+w.name+" ("+cost+" Cr)") {
        public void whenClicked(Object context) {
          ActorUtils.generateMigrant(w, built, true);
        }
      });
      //*/
      
      //  Supply and demand!
      //  Production and consumption!
      //  Import/export allowance!
    }
    
    ActorAsVessel trader = home == null ? null : home.trading.traderFor(base);
    if (trader != null) {
      Area offmap = trader.offmap();
      int ETA = world.arriveTime(trader, base);
      World.Journey j = world.journeyFor(trader);
      
      d.append("\n\n");
      d.appendAll("Current trader: ", trader);
      if (j != null) {
        d.appendAll("\n  Travelling from ", j.from()+" to "+j.goes());
      }
      if (ETA < 0) {
        d.appendAll(" (On "+offmap+")");
      }
      else {
        d.appendAll(" (ETA "+ETA+")");
      }
    }
  }
}





