

package gameUI.play;
import game.*;
import static game.GameConstants.*;
import graphics.common.*;
import graphics.sfx.*;
import util.*;



public class Selection {
  
  public static interface Focus extends Description.Clickable {
    boolean testSelection(PlayUI UI, Base base, Viewport port);
    boolean setSelected(PlayUI UI);
    boolean trackSelection();
    Vec3D trackPosition();
    void renderSelection(Rendering rendering, boolean hovered);
  }
  
  final PlayUI UI;
  private Focus    hovered;
  private Focus    selected;
  private AreaTile hoverTile;
  private Actor    hoverActor;
  private Building hoverVenue;
  private Mission  hoverMission;
  private Focus    hoverOther;
  
  
  Selection(PlayUI UI) {
    this.UI = UI;
  }
  
  
  public void loadState(Session s) throws Exception {
    final Focus selected = (Focus) s.loadObject();
    presentSelectionPane(selected);
    I.talkAbout = selected;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(PlayUI.selectionFocus());
  }
  
  
  void updateSelection(AreaMap stage, Base base) {
    if (UI.selected() != null) {
      hovered    = null;
      hoverTile  = null;
      hoverVenue = null;
      hoverActor = null;
    }
    else {
      final Viewport port = UI.rendering.view;
      final Vec3D groundPoint = pickedGroundPoint();
      hoverTile = stage.tileAt(groundPoint.x, groundPoint.y);
      
      final Element owner = stage.above(hoverTile);
      
      //if (I.used60Frames) I.say("Hovering above: "+owner);
      
      if (
        owner != null &&
        owner.canRender(base, port) &&
        owner.type().isBuilding()
      ) {
        hoverVenue = (Building) owner;
      }
      else hoverVenue = null;
      
      //  TODO:  Use a more efficient technique here?
      final Pick <Actor> pickA = new Pick();
      for (Actor e : stage.actors()) {
        trySelection(e, base, port, pickA);
      }
      hoverActor = pickA.result();
      
      final Pick <Mission> pickM = new Pick();
      for (Mission m : base.missions()) {
        trySelection(m, base, port, pickM);
      }
      hoverMission = pickM.result();
      
      if      (hoverOther   != null) hovered = hoverOther;
      else if (hoverMission != null) hovered = hoverMission;
      else if (hoverActor   != null) hovered = hoverActor;
      else if (hoverVenue   != null) hovered = hoverVenue;
      else hovered = hoverTile;
      
      if (I.used60Frames) {
        //I.say("\nHovering over: "+hovered);
      }
      
      if (UI.mouseClicked() && UI.currentTask() == null) {
        presentSelectionPane(hovered);
      }
    }
    
    I.talkAbout = PlayUI.selectionFocus();
  }
  
  
  private void trySelection(
    Selection.Focus e, Base base, Viewport port, Pick pick
  ) {
    if (! e.testSelection(UI, base, port)) return;
    final float dist = port.translateToScreen(e.trackPosition()).z;
    ///if (I.used60Frames) I.say("  Trying selection: "+e+", depth: "+dist);
    pick.compare(e, 0 - dist);
  }
  
  
  private Vec3D pickedGroundPoint() {
    //
    //  Here, we find the point of intersection between the line-of-sight
    //  underneath the mouse cursor, and the plane of the ground-
    final Viewport view = UI.rendering.view;
    final Vec3D origin = new Vec3D(UI.mouseX(), UI.mouseY(), 0);
    view.translateFromScreen(origin);
    final Vec3D vector = view.direction();
    vector.scale(0 - origin.z / vector.z);
    origin.add(vector);
    return origin;
  }
  
  
  void presentSelectionPane(Focus selected) {
    if (selected == null) {
      this.selected = null;
      UI.setDetailPane(null);
    }
    else {
      this.selected = selected;
      selected.setSelected(UI);
    }
  }
  
  
  public AreaTile hoverTile () { return hoverTile ; }
  public Actor    hoverActor() { return hoverActor; }
  public Building hoverVenue() { return hoverVenue; }
  public Focus    hovered   () { return hovered   ; }
  
  
  
  /**  Assorted rendering utilities-
    */
  protected void renderWorldFX(Rendering rendering) {
    if (hovered != null && hovered != selected) {
      hovered.renderSelection(rendering, true);
    }
    if (selected != null) {
      selected.renderSelection(rendering, false);
    }
  }
  
  
  final public static PlaneFX.Model
    CIRCLE_SELECT_MODEL = PlaneFX.imageModel(
      "select_circle_fx", Selection.class,
      "media/GUI/selectCircle.png", 1, 0, 0, false, false
    ),
    SQUARE_SELECT_MODEL = PlaneFX.imageModel(
      "select_square_fx", Selection.class,
      "media/GUI/selectSquare.png", 1, 0, 0, false, false
    );
  final public static ImageAsset
    SELECT_CIRCLE = ImageAsset.fromImage(
      Selection.class, "selection_tex_circle", "media/GUI/selectCircle.png"
    ),
    SELECT_SQUARE = ImageAsset.fromImage(
      Selection.class, "selection_tex_square", "media/GUI/selectSquare.png"
    ),
    SELECT_OVERLAY = ImageAsset.fromImage(
      Selection.class, "selection_tex_overlay", "media/GUI/selectOverlay.png"
    ),
    PLACE_OVERLAY = ImageAsset.fromImage(
      Selection.class, "place_tex_overlay", "media/GUI/placeOverlay.png"
    );
  
  
  public static void renderSimpleCircle(
    Target target, Vec3D pos, Rendering r, Colour c
  ) {
    final PlaneFX ring = (PlaneFX) CIRCLE_SELECT_MODEL.makeSprite();
    ring.colour = c;
    ring.scale = target.radius() / 2;
    ring.position.setTo(pos);
    ring.passType = Sprite.PASS_SPLAT;
    ring.readyFor(r);
  }
  
  
  public static void renderSelectSquare(
    Target target, Box2D area, Rendering r, Colour c
  ) {
    final PlaneFX square = (PlaneFX) SQUARE_SELECT_MODEL.makeSprite();
    square.colour = c;
    Vec2D mid = area.centre();
    square.position.set(mid.x + 0.5f, mid.y + 0.5f, 0);
    square.scale = (target.type().wide + 2) / 2;
    square.passType = Sprite.PASS_SPLAT;
    square.readyFor(r);
  }
}













