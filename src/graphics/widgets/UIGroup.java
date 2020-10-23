/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package graphics.widgets;
import util.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;



public class UIGroup extends UINode {
  
  
  /**  Data fields, constructors, and basic access/setter methods-
    */
  final List <UINode> kids = new List <UINode> () {
    protected float queuePriority(UINode r) {
      return 0 - r.relDepth;
    }
  };
  public boolean clipKids = false;
  
  
  public UIGroup(HUD UI) {
    super(UI);
    if (UI == null && ! (this instanceof HUD)) I.complain("No HUD!");
  }
  
  
  public Series <UINode> kids() {
    return kids;
  }
  
  
  
  /**  Overrides for general UI functions-
    */
  protected void render(WidgetsPass pass) {
    if (hidden) return;
    
    final boolean doClip = clipKids;
    if (doClip) {
      pass.flush();
      Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
      Gdx.gl.glScissor((int) xpos(), (int) ypos(), (int) xdim(), (int) ydim());
    }
    
    for (UINode kid : kids) if (! kid.hidden) {
      kid.render(pass);
    }
    
    if (doClip) {
      pass.flush();
      Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }
  }
  
  
  protected UINode selectionAt(Vec2D mousePos) {
    UINode selected = null;
    for (UINode kid : kids) if (! kid.hidden) {
      final UINode kidSelect = kid.selectionAt(mousePos);
      if (kidSelect != null) selected = kidSelect;
    }
    //  Return children, if possible.
    return selected;
  }

  
  public void updateAsBase(Box2D bound) {
    updateState();
    updateRelativeParent(bound);
    updateAbsoluteBounds(bound);
  }
  
  
  protected void updateState() {
    super.updateState();
    for (UINode kid : kids) kid.updateState();
  }
  
  
  void updateRelativeParent(Box2D base) {
    super.updateRelativeParent(base);
    for (UINode kid : kids) if (! kid.hidden) kid.updateRelativeParent();
  }
  
  
  void updateAbsoluteBounds(Box2D base) {
    super.updateAbsoluteBounds(base);
    for (UINode kid : kids) if (! kid.hidden) kid.updateAbsoluteBounds();
  }
  
  
  
  /**  Other utility methods related to display-
    */
  protected void sortKidsByDepth() {
    kids.queueSort();
  }
}











