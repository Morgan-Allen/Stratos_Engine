

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
    //d.append("\n\n  <Under construction!>");
    
    d.append("\n\nDiplomatic Offers:");
    
    for (final Mission petition : base.council.petitions()) {
      d.appendAll("\n  ", petition);
      d.append("\n  Posture: "+petition.terms.postureDemand());
      
      d.append("\n  Accept terms? ");
      d.append(new Description.Link("YES") {
        public void whenClicked(Object context) {
          base.council.acceptTerms(petition);
        }
      });
      d.append("  ");
      d.append(new Description.Link("NO") {
        public void whenClicked(Object context) {
          base.council.rejectTerms(petition);
        }
      });
    }
  }
  
}


