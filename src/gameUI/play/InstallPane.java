


package gameUI.play;
import game.*;
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
    final AreaMap stage = UI.stage;
    final Base    base  = UI.base;
    
    this.text.setText("");
    final Description d = this.text;
    d.append("\nFacilities: ");
    
    for (final Type type : UI.base.buildTypes()) {
      if (! type.rulerCanBuild(base, stage)) continue;
      d.append("\n  ");
      d.append(type.name, new Description.Link() {
        public void whenClicked(Object context) {
          beginInstallTask(type);
        }
      });
    }
    
    super.updateState();
  }
  
  
  private void beginInstallTask(Type type) {
    final Building placed = (Building) type.generate();
    final AreaMap  stage  = UI.stage;
    final Base     base   = UI.base;
    
    final PlayTask task = new PlayTask() {
      public void doTask(PlayUI UI) {
        final Tile puts = UI.selection.hoverSpot();
        //final Account reasons = new Account();
        boolean canPlace = false;
        
        if (puts != null) {
          if (placed.canPlace(stage, puts.x, puts.y, 1)) canPlace = true;
          placed.renderPreview(UI.rendering, canPlace, puts);
        }
        
        if (UI.mouseClicked()) {
          if (canPlace) {
            placed.enterMap(stage, puts.x, puts.y, 0.0f, base);
            stage.planning.placeObject(placed);
            //UI.base.incCredits(0 - placed.blueprint().buildCost);
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








