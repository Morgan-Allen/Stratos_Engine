

package gameUI.play;
import graphics.common.*;
import graphics.widgets.*;
import util.Vec2D;



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
  
  
  protected UINode selectionAt(Vec2D mousePos) {
    return null;
  }
  
}