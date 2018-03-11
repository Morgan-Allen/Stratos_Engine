

package gameUI.play;
import game.*;
import graphics.common.*;
import graphics.widgets.*;
import util.*;
import com.badlogic.gdx.Input.Keys;



public class Tracking {
  
  
  final HUD UI;
  private Selection.Focus tracked = null;
  
  
  Tracking(HUD UI) {
    this.UI = UI;
  }
  
  
  public void loadState(Session s) throws Exception {
    tracked = (Selection.Focus) s.loadObject();
    UI.rendering.view.lookedAt.loadFrom(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(tracked);
    UI.rendering.view.lookedAt.saveTo(s.output());
  }
  
  
  public void zoomNow(Vec3D position) {
    UI.rendering.view.lookedAt.setTo(position);
  }
  
  
  
  /**  Updates general camera behaviour.
    */
  protected void updateTracking(AreaMap stage, Selection.Focus tracked) {
  
    if (tracked == null || ! tracked.trackSelection()) tracked = null;
    else trackSelection(tracked);
    this.tracked = tracked;
    
    if (pressed(Keys.UP   ) || pressed(Keys.W)) pushCamera( 1, -1, stage);
    if (pressed(Keys.DOWN ) || pressed(Keys.S)) pushCamera(-1,  1, stage);
    if (pressed(Keys.RIGHT) || pressed(Keys.D)) pushCamera( 1,  1, stage);
    if (pressed(Keys.LEFT ) || pressed(Keys.A)) pushCamera(-1, -1, stage);
  }
  
  
  private boolean pressed(int code) {
    return KeyInput.isPressed(code);
  }
  
  
  private void trackSelection(Selection.Focus selected) {
    final Viewport view = UI.rendering.view;
    //
    //  Ascertain the difference between the current camera position and the
    //  the target's position.
    final Vec3D
      viewPos  = new Vec3D(view.lookedAt),
      targPos  = new Vec3D(selected.trackPosition()),
      displace = targPos.sub(viewPos, new Vec3D());
    final float
      distance = displace.length(),
      drift = ((distance + 2) * 2) / (UI.rendering.frameRate() * distance);
    //
    //  If distance is too large, or drift would cause overshoot, just go
    //  straight to the point.  Otherwise, displace gradually-
    if (drift >= 1) viewPos.setTo(targPos);
    else viewPos.add(displace.scale(drift));
    view.lookedAt.setTo(viewPos);
  }
  
  
  private void pushCamera(int x, int y, AreaMap stage) {
    //
    //  First, we calculate a positional offset within the world and without
    //  exiting it's bounds:
    final Vec3D nextPos = new Vec3D(UI.rendering.view.lookedAt);
    nextPos.x = Nums.clamp(nextPos.x + x, 0, stage.size());
    nextPos.y = Nums.clamp(nextPos.y + y, 0, stage.size());
    UI.rendering.view.lookedAt.setTo(nextPos);
    if (tracked != null) PlayUI.pushSelection(null);
  }
  
  
}











