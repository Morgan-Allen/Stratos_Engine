

package gameUI.play;
import game.*;
import graphics.common.*;
import graphics.widgets.*;
import util.*;



public class Selection {
  
  public static interface Focus extends Description.Clickable {
    boolean testSelection(PlayUI UI, City base, Viewport port);
    boolean setSelected(PlayUI UI);
    boolean trackSelection();
    Vec3D trackPosition();
  }
  
  final PlayUI UI;
  private Focus    hovered   ;
  private Tile     hoverSpot ;
  private Actor    hoverActor;
  private Building hoverVenue;
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
  
  
  void updateSelection(CityMap stage, City base) {
    if (UI.selected() != null) {
      hovered    = null;
      hoverSpot  = null;
      hoverVenue = null;
      hoverActor = null;
    }
    else {
      final Viewport port = UI.rendering.view;
      final Vec3D groundPoint = pickedGroundPoint();
      hoverSpot = stage.tileAt(groundPoint.x, groundPoint.y);
      
      final Element owner = stage.above(hoverSpot);
      if (owner != null && owner.canRender(base, port) && owner.isVenue()) {
        hoverVenue = (Building) owner;
      }
      else hoverVenue = null;
      
      //  TODO:  Use a more efficient technique here?
      final Pick <Actor> pickA = new Pick();
      for (Actor e : stage.actors()) {
        trySelection(e, base, port, pickA);
      }
      hoverActor = pickA.result();
      
      //  TODO:  Restore this shortly...
      /*
      final Pick <Mission> pickO = new Pick();
      for (Base b : stage.allBases()) {
        for (Mission m : b.missions()) {
          trySelection(m, port, base, pickO);
        }
      }
      hoverOther = pickO.result();
      //*/
      
      if      (hoverOther != null) hovered = hoverOther;
      else if (hoverActor != null) hovered = hoverActor;
      else if (hoverVenue != null) hovered = hoverVenue;
      else hovered = hoverSpot;
      
      if (UI.mouseClicked()) presentSelectionPane(hovered);
    }
    
    I.talkAbout = PlayUI.selectionFocus();
  }
  
  
  private void trySelection(
    Selection.Focus e, City base, Viewport port, Pick pick
  ) {
    if (! e.testSelection(UI, base, port)) return;
    final float dist = port.translateToScreen(e.trackPosition()).z;
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
      UI.setOptionList(null);
    }
    else {
      selected.setSelected(UI);
    }
  }
  
  
  public Tile     hoverSpot () { return hoverSpot ; }
  public Actor    hoverActor() { return hoverActor; }
  public Building hoverVenue() { return hoverVenue; }
  
}








