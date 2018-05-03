


package gameUI.play;
import game.*;
import static game.GameConstants.*;
import graphics.common.*;
import graphics.widgets.*;
import util.*;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Texture;



public class PaneBuildOptions extends DetailPane {
  
  
  final PlayUI UI;
  private Element placed = null;
  
  
  PaneBuildOptions(PlayUI UI) {
    super(UI, null);
    this.UI = UI;
  }
  
  
  protected void updateState() {
    final Area area = UI.area;
    final Base base = UI.base;
    
    this.text.setText("");
    final Description d = this.text;
    d.append("\nBuilding Types: ");
    
    for (final Type type : UI.base.buildTypes()) {
      if (! type.rulerCanBuild(base, area)) continue;
      d.append("\n  ");
      d.append(type.name, new Description.Link() {
        public void whenClicked(Object context) {
          beginInstallTask(type);
        }
      });
    }
    
    if (placed != null) {
      Type type = placed.type();
      
      int cashCost = 0;
      d.append("\n  (");
      for (Good g : type.builtFrom) if (g != VOID) {
        int amount = (int) type.buildNeed(g);
        Text.appendColour(amount+" "+g+" ", Colour.LITE_GREY, d);
        cashCost += g.price * amount;
      }
      d.append(cashCost+" credits");
      d.append(")");
    }
    
    super.updateState();
  }
  
  
  private void beginInstallTask(Type type) {
    
    placed = (Element) type.generate();
    final Area area = UI.area;
    final Base base = UI.base;
    
    final PlayTask task = new PlayTask() {
      public void doTask(PlayUI UI) {
        final AreaTile puts = UI.selection.hoverTile();
        //final Account reasons = new Account();
        boolean canPlace = false;
        
        if (puts != null) {
          placed.setLocation(puts, area);
          if (placed.canPlace(area)) canPlace = true;
          placed.renderPreview(UI.rendering, canPlace, puts);
        }
        
        if (UI.mouseClicked()) {
          if (canPlace) {
            placed.enterMap(area, puts.x, puts.y, 0.0f, base);
            area.planning.placeObject(placed);
            placed = null;
            //UI.base.incCredits(0 - placed.blueprint().buildCost);
          }
          UI.assignTask(null);
        }
        
        if (KeyInput.wasTyped(Keys.ESCAPE)) {
          UI.assignTask(null);
        }
      }
      
      public Texture cursor() {
        return null;
      }
    };
    UI.assignTask(task);
  }
  
  
}








