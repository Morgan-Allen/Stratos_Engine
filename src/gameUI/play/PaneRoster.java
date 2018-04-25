

package gameUI.play;
import game.*;
import static game.GameConstants.*;
import util.*;



public class PaneRoster extends DetailPane {
  
  
  final PlayUI UI;
  
  
  PaneRoster(PlayUI UI) {
    super(UI, null);
    this.UI = UI;
  }
  
  
  protected void updateState() {
    final Area area = UI.area;
    final Base base = UI.base;
    
    this.text.setText("");
    final Description d = this.text;
    d.append("\nPersonnel Roster: ");
    
    //  TODO:  Use portraits for this, and/or arrange in a simple grid order.
    //  You could give the player multiple options for sorting, in fact- by
    //  guild, by seniority, by class, et cetera.
    
    //  And you should give a visual indication of what each actor is up to and
    //  how healthy they are.  Then the player has an at-a-glance indication of
    //  how well everyone is doing.
    
    
    for (Building b : area.buildings()) if (b.base() == base) {
      if (b.workers().empty()) continue;
      
      d.appendAll("\n\n  ", b, ":");
      for (Actor a : b.workers()) {
        d.appendAll("\n    ", a);
      }
    }
    
    super.updateState();
  }
  
  
  
}







