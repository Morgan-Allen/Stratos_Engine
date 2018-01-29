

package gameUI.play;
import game.*;
import gameUI.debug.*;
import graphics.widgets.*;
import util.*;
import com.badlogic.gdx.Input.Keys;



public class InstallPane extends DetailPane {
  
  
  final PlayUI UI;
  
  
  InstallPane(PlayUI UI) {
    super(UI, null);
    this.UI = UI;
  }
  
  
  
  protected void updateState() {
    
    this.text.setText("");
    final Description d = this.text;
    
    d.append("\nFacilities: ");
    for (final Blueprint type : UI.base.faction().canBuild()) {
      if (! type.canBuildManually(UI.base)) continue;
      d.append("\n  ");
      d.append(type.name, new Description.Link() {
        public void whenClicked(Object context) {
          beginInstallTask(type);
        }
      });
      d.append(" ("+type.buildCost+" Cr)");
    }
    
    super.updateState();
  }
  
  
  private void beginInstallTask(Blueprint type) {
    final Venue placed = type.sampleFor(UI.base.faction());
    final Stage stage  = UI.stage;
    
    final PlayTask task = new PlayTask() {
      public void doTask(PlayUI UI) {
        final Spot puts = UI.selection.hoverSpot();
        final Account reasons = new Account();
        boolean canPlace = false;
        
        if (puts != null) {
          placed.setPosition(stage, puts.x + 0.5f, puts.y + 0.5f);
          if (placed.canPlace(reasons)) canPlace = true;
          placed.renderPreview(UI.rendering, canPlace);
        }
        
        if (UI.mouseClicked()) {
          if (canPlace) {
            placed.enterStage(stage, puts.x + 0.5f, puts.y + 0.5f, false);
            UI.base.incCredits(0 - placed.blueprint().buildCost);
          }
          UI.assignTask(null);
        }
        
        if (KeyInput.wasTyped(Keys.ESCAPE)) {
          UI.assignTask(null);
        }
      }
    };
    UI.assignTask(task);
  }
  
  
}








