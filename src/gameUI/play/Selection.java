

package gameUI.play;
import game.*;
import graphics.common.*;
import util.*;



public class Selection {
  
  public static interface Focus extends Description.Clickable {
    boolean testSelection(PlayUI UI, Base base, Viewport port);
    boolean setSelected(PlayUI UI);
    boolean trackSelection();
    Vec3D trackPosition();
  }
  
  final PlayUI UI;
  private Focus    hovered;
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
  
  
  void updateSelection(Area stage, Base base) {
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
        if (e.indoors()) continue;
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
      UI.setDetailPane(null);
    }
    else {
      selected.setSelected(UI);
    }
  }
  
  
  public AreaTile hoverTile () { return hoverTile ; }
  public Actor    hoverActor() { return hoverActor; }
  public Building hoverVenue() { return hoverVenue; }
  public Focus    hovered   () { return hovered   ; }
  
}






