/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package graphics.widgets;
import util.*;
import util.Description.Clickable;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;

import graphics.common.*;



public class Button extends Image {
  
  
  /**  Data fields and basic constructors-
    */
  final public static ImageAsset
    DEFAULT_LIT = ImageAsset.fromImage(
      Button.class, "button_default_lit_tex",
      "media/GUI/iconLit.gif"
    ),
    CIRCLE_LIT = ImageAsset.fromImage(
      Button.class, "button_circle_lit_tex",
      "media/GUI/icon_lit_circle.png"
    ),
    CROSSHAIRS_LIT = ImageAsset.fromImage(
      Button.class, "button_crosshairs_lit_tex",
      "media/GUI/icon_lit_crosshairs.png"
    );
  
  
  protected Texture   highlit;
  protected String    info   ;
  protected Clickable links  ;
  
  public float
    hoverLit = DEFAULT_HOVER_ALPHA,
    pressLit = DEFAULT_PRESS_ALPHA;
  public boolean
    toggled = false;
  
  
  
  public Button(
    HUD UI, String widgetID, ImageAsset norm, String infoS
  ) {
    this(UI, widgetID, norm.asTexture(), DEFAULT_LIT.asTexture(), infoS);
  }
  

  public Button(
    HUD UI, String widgetID, ImageAsset norm, ImageAsset lit, String infoS
  ) {
    this(UI, widgetID, norm.asTexture(), lit.asTexture(), infoS);
  }
  
  
  public Button(
    HUD UI, String widgetID, Texture norm, String infoS
  ) {
    this(UI, widgetID, norm, DEFAULT_LIT.asTexture(), infoS);
  }
  
  
  public Button(
    HUD UI, String widgetID, Texture norm, Texture lit, String infoS
  ) {
    super(UI, norm);
    setWidgetID(widgetID);
    this.info     = infoS;
    this.highlit  = lit;
  }
  
  
  public void setLinks(Clickable links) {
    this.links = links;
    this.info  = links.fullName();
  }
  
  
  public void setHighlight(Texture h) {
    this.highlit = h;
  }
  
  
  protected String info() {
    if (! enabled) return info+"\n"+disableInfo();
    return info;
  }
  
  
  protected String disableInfo() {
    return "(Unavailable)";
  }
  
  
  public boolean equals(Object other) {
    if (! (other instanceof Button)) return false;
    final Button b = (Button) other;
    return this.toString().equals(b.toString());
  }
  
  
  public int hashCode() {
    if (links != null || info != null) return toString().hashCode();
    else return super.hashCode();
  }
  
  
  public String toString() {
    if      (links != null) return links.fullName()+" (button link)";
    else if (info  != null) return "Button "+I.shorten(info, 8);
    else                    return "Button "+hashCode();
  }
  
  
  
  /**  UI method overrides/implementations-
    */
  protected UINode selectionAt(Vec2D mousePos) {
    return (trueBounds().contains(mousePos.x, mousePos.y)) ? this : null;
    //  TODO:  Consider restoring multiple selection modes.
  }
  
  
  protected boolean toggled() {
    return toggled;
  }
  
  
  public void performAction() {
    this.whenClicked();
  }
  
  
  protected void whenClicked() {
    super.whenClicked();
    if (enabled && links != null) links.whenClicked(this);
  }
  
  
  protected void render(WidgetsPass pass) {
    super.render(pass);
    if (! enabled) return;
    if (toggled()) {
      super.renderTex(highlit, absAlpha, pass);
    }
    else if (amPressed() || amDragged() || amClicked()) {
      super.renderTex(highlit, pressLit * absAlpha, pass);
    }
    else if (amHovered()) {
      float alpha = absAlpha * hoverLit;
      alpha *= Nums.clamp(UI.timeHovered() / DEFAULT_FADE_TIME, 0, 1);
      super.renderTex(highlit, alpha, pass);
    }
  }
}



