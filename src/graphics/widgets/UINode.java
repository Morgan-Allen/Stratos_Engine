/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package graphics.widgets;
import graphics.common.*;
import util.*;



public abstract class UINode {
  
  
  /**  Data fields, constructors and accessor methods-
    */
  final public static byte
    HOVERED = 0,
    CLICKED = 1,
    PRESSED = 2,
    DRAGGED = 3;
  final public static float
    DEFAULT_FADE_TIME   = 0.25f,
    SLOW_FADE_TIME      = 1.0f,
    DEFAULT_HOVER_ALPHA = 0.5f,
    DEFAULT_PRESS_ALPHA = 0.75f;
  
  final protected Box2D
    relBound = new Box2D(),
    absBound = new Box2D();
  public float
    relDepth = 0,
    absDepth = 0;
  final protected Box2D
    bounds = new Box2D();
  public float
    relAlpha = 1;
  protected float
    absAlpha = 1;
  public boolean
    hidden  = false,
    fadeout = false,
    stretch = true ;
  
  
  final protected HUD UI;
  private UINode parent;
  private ListEntry <UINode> kidEntry;
  private String widgetID;
  
  
  public UINode(HUD UI) {
    this.UI = UI;
  }
  
  
  protected UINode selectionAt(Vec2D mousePos) {
    return (bounds.contains(mousePos.x, mousePos.y)) ? this : null;
  }
  
  
  public int xpos() { return (int) bounds.xpos(); }
  public int ypos() { return (int) bounds.ypos(); }
  public int xdim() { return (int) bounds.xdim(); }
  public int ydim() { return (int) bounds.ydim(); }
  
  public Box2D trueBounds() { return bounds; }
  public Box2D absBound() { return absBound; }
  public Box2D relBound() { return relBound; }
  
  
  
  public void setWidgetID(String ID) {
    this.widgetID = ID;
  }
  
  public String widgetID() {
    return widgetID;
  }
  
  protected String info() {
    return widgetID;
  }
  
  public String toString() {
    if (widgetID == null) return getClass().getSimpleName();
    return widgetID;
  }
  
  
  
  /**  Methods intended for implementation by subclasses-
    */
  protected abstract void render(WidgetsPass pass);

  
  public void setToPreferredSize() {
    return;
  }
  
  
  
  /**  Methods to control the order of rendering for UINodes.
    */
  public void attachTo(UINode parent) {
    if (this.parent != null) {
      I.say("WARNING: ALREADY ATTACHED TO A DIFFERENT PARENT: "+this);
      detach();
    }
    this.parent = parent;
    if (parent instanceof UIGroup) {
      kidEntry = ((UIGroup) parent).kids.addLast(this);
    }
  }
  
  
  public void detach() {
    if (parent instanceof UIGroup) {
      ((UIGroup) parent).kids.removeEntry(kidEntry);
    }
    kidEntry = null;
    parent   = null;
  }
  
  
  public boolean attached() {
    return parent != null;
  }
  
  
  public UINode parent() {
    return parent;
  }
  
  
  
  /**  Convenience methods for setting relative and absolute bounds in a more
    *  human-legible manner-
    */
  public void alignLeft(float margin, int width) {
    relBound.setX(0, 0);
    absBound.setX(margin, width);
  }
  
  public void alignRight(float margin, int width) {
    relBound.setX(1, 0);
    absBound.setX(0 - (margin + width), width);
  }
  
  public void alignHorizontal(int marginLeft, int marginRight) {
    relBound.setX(0, 1);
    absBound.setX(marginLeft, 0 - (marginLeft + marginRight));
  }
  
  public void alignHorizontal(float relative, float width, float offset) {
    relBound.setX(relative, 0);
    absBound.setX(offset - (width / 2f), width);
  }
  
  public void alignAcross(float relMin, float relMax) {
    relBound.setX(relMin, relMax - relMin);
    absBound.setX(0, 0);
  }
  
  public void alignAcross(float relMin, float relMax, int offX, int incX) {
    relBound.setX(relMin, relMax - relMin);
    absBound.setX(offX, incX - offX);
  }
  
  
  public void alignBottom(float margin, int height) {
    relBound.setY(0, 0);
    absBound.setY(margin, height);
  }
  
  public void alignTop(float margin, int height) {
    relBound.setY(1, 0);
    absBound.setY(0 - (margin + height), height);
  }
  
  public void alignVertical(int marginBottom, int marginTop) {
    relBound.setY(0, 1);
    absBound.setY(marginBottom, 0 - (marginBottom + marginTop));
  }
  
  public void alignVertical(float relative, float height, float offset) {
    relBound.setY(relative, 0);
    absBound.setY(offset - (height / 2f), height);
  }
  
  public void alignDown(float relMin, float relMax) {
    relBound.setY(relMin, relMax - relMin);
    absBound.setY(0, 0);
  }
  
  public void alignDown(float relMin, float relMax, int offY, int incY) {
    relBound.setY(relMin, relMax - relMin);
    absBound.setY(offY, incY - offY);
  }
  
  
  public void alignToFill() {
    relBound.set(0, 0, 1, 1);
    absBound.set(0, 0, 0, 0);
  }
  
  public void alignToCentre() {
    relBound.set(0.5f, 0.5f, 0, 0);
    absBound.set(0   , 0   , 0, 0);
  }
  
  public void alignToArea(int x, int y, int w, int h) {
    relBound.set(0, 0, 0, 0);
    absBound.set(x, y, w, h);
  }
  
  public void alignToMatch(UINode other) {
    relBound.setTo(other.relBound);
    absBound.setTo(other.absBound);
  }
  
  public void alignToMatch(UINode other, int padX, int padY) {
    relBound.setTo(other.relBound);
    absBound.setTo(other.absBound);
    absBound.incX   (0 - padX);
    absBound.incWide(padX * 2);
    absBound.incY   (0 - padY);
    absBound.incHigh(padY * 2);
  }
  
  public void alignBottomLeft(int x, int y, ImageAsset asset) {
    alignLeft  (x, asset.pixelWide());
    alignBottom(y, asset.pixelHigh());
  }
  
  public void alignBottomRight(int x, int y, ImageAsset asset) {
    alignRight (x, asset.pixelWide());
    alignBottom(y, asset.pixelHigh());
  }
  
  public void alignTopLeft(int x, int y, ImageAsset asset) {
    alignLeft(x, asset.pixelWide());
    alignTop (y, asset.pixelHigh());
  }
  
  public void alignTopRight(int x, int y, ImageAsset asset) {
    alignRight(x, asset.pixelWide());
    alignTop  (y, asset.pixelHigh());
  }
  
  
  
  /**  Internal methods for calibrating position based off parent coordinates-
    */
  protected void updateState() {
    absDepth = relDepth + (parent == null ? 0 : parent.absDepth);
    absAlpha = relAlpha * (parent == null ? 1 : parent.absAlpha);
  }
  
  
  protected void updateRelativeParent() {
    if (parent == null) updateRelativeParent(new Box2D());
    else updateRelativeParent(parent.bounds);
  }
  
  
  void updateRelativeParent(Box2D base) {
    //
    //  Firstly, obtain some default dimensions-
    float
      wide = absBound.xdim() + (base.xdim() * relBound.xdim()),
      high = absBound.ydim() + (base.ydim() * relBound.ydim()),
      x    = absBound.xpos(),
      y    = absBound.ypos();
    
    if (stretch) bounds.set(x, y, wide, high);
    else setUnstretched(x, y, wide, high, bounds);
  }
  
  
  protected void setUnstretched(
    float x, float y, float wide, float high, Box2D bounds
  ) {
    //  In this case we shrink either width or height to maintain a constant
    //  aspect ratio.
    final float
      oldWide = wide, absWide = Nums.max(1, absBound.xdim()),
      oldHigh = high, absHigh = Nums.max(1, absBound.ydim()),
      aspW    = wide / absWide,
      aspH    = high / absHigh,
      scale   = Nums.min(aspW, aspH);
    wide = absWide * scale;
    high = absHigh * scale;
    x += (oldWide - wide) / 2;
    y += (oldHigh - high) / 2;
    bounds.set(x, y, wide, high);
  }
  
  
  protected void updateAbsoluteBounds() {
    if (parent == null) updateAbsoluteBounds(new Box2D());
    else updateAbsoluteBounds(parent.bounds);
  }
  
  
  void updateAbsoluteBounds(Box2D base) {
    final float
      x = bounds.xpos() + base.xpos() + (relBound.xpos() * base.xdim()),
      y = bounds.ypos() + base.ypos() + (relBound.ypos() * base.ydim());
    bounds.xpos(x);
    bounds.ypos(y);
  }
  
  
  
  /**  Blank feedback methods for override by subclasses-
    */
  protected void whenHovered() {}
  protected void whenClicked() {}
  protected void whenPressed() {}
  protected void whenDragged() {}
  protected boolean amHovered() { return UI.amSelected(this, HOVERED); }
  protected boolean amClicked() { return UI.amSelected(this, CLICKED); }
  protected boolean amPressed() { return UI.amSelected(this, PRESSED); }
  protected boolean amDragged() { return UI.amSelected(this, DRAGGED); }
}













