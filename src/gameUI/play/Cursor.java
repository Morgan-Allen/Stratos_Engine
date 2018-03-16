

package gameUI.play;
import graphics.common.*;
import graphics.widgets.*;
import com.badlogic.gdx.math.Vector2;



public class Cursor extends Image {
  
  
  final PlayUI UI;
  
  
  public Cursor(PlayUI UI, ImageAsset tex) {
    super(UI, tex);
    this.UI = UI;
  }

  
  protected void updateRelativeParent() {
    absBound.xpos(UI.mouseX());
    absBound.ypos(UI.mouseY() - absBound.ydim());
    super.updateRelativeParent();
  }
  
  
  protected UINode selectionAt(Vector2 mousePos) {
    return null;
  }
  
}