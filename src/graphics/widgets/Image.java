/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package graphics.widgets;
import graphics.common.*;
import util.*;
import static graphics.common.Colour.*;
import com.badlogic.gdx.graphics.*;




/**  Note- images set their own dimensions to match that of their texture (
  *  times scale.)  If you wish to disable this behaviour, set scale to zero.
  */
public class Image extends UINode {
  
  
  /**  Data fields, constructors and setup methods-
    */
  final public static ImageAsset
    TRANSLUCENT_WHITE = ImageAsset.withColor(15, SOFT_WHITE, Image.class),
    TRANSLUCENT_GREY  = ImageAsset.withColor(16, SOFT_GREY , Image.class),
    TRANSLUCENT_BLACK = ImageAsset.withColor(16, SOFT_BLACK, Image.class),
    SOLID_WHITE       = ImageAsset.withColor(16, WHITE     , Image.class),
    SOLID_BLACK       = ImageAsset.withColor(16, BLACK     , Image.class),
    FULL_TRANSPARENCY = ImageAsset.withColor(16, NONE      , Image.class);
  
  public boolean
    lockToPixels = false,
    blocksSelect = false,
    enabled      = true ;
  
  protected ImageAsset texture;
  protected Texture customTex;
  protected ImageAsset greyOut = TRANSLUCENT_BLACK;
  protected Batch <Texture> overlaid = null;
  
  
  
  
  public Image(HUD myHUD, ImageAsset t) {
    super(myHUD);
    this.texture = t;
  }
  
  
  public void setBaseTexture(ImageAsset t) {
    this.texture = t;
  }
  
  
  public void setCustomTexture(Texture t) {
    this.customTex = t;
  }
  
  
  public void setDisabledOverlay(ImageAsset g) {
    this.greyOut = g;
  }
  
  
  public void addOverlay(ImageAsset g) {
    if (overlaid == null) overlaid = new Batch();
    overlaid.add(g.asTexture());
  }
  
  
  public void setOverlays(ImageAsset... g) {
    overlaid = new Batch();
    for (ImageAsset a : g) overlaid.add(a.asTexture());
  }
  
  
  public void setToPreferredSize() {
    expandToTexSize(1, false);
  }
  
  
  public int texWide() {
    if (customTex != null) return customTex.getWidth();
    return texture.asTexture().getWidth();
  }
  
  
  public int texHigh() {
    if (customTex != null) return customTex.getHeight();
    return texture.asTexture().getHeight();
  }
  
  
  public void expandToTexSize(float scale, boolean centre) {
    absBound.xdim(texWide() * scale);
    absBound.ydim(texHigh() * scale);
    if (centre) {
      absBound.xpos(absBound.xpos() - (absBound.xdim() / 2));
      absBound.ypos(absBound.ypos() - (absBound.ydim() / 2));
    }
  }
  
  
  /**  Rendering and feedback methods-
    */
  protected UINode selectionAt(Vec2D mousePos) {
    if (blocksSelect) return super.selectionAt(mousePos);
    else return null;
  }
  

  protected void setUnstretched(
    float x, float y, float wide, float high, Box2D bounds
  ) {
    final float
      texWide = texWide(),
      texHigh = texHigh(),
      scale   = Nums.min(wide / texWide, high / texHigh);
    bounds.set(x, y, texWide * scale, texHigh * scale);
  }
  
  
  protected void render(WidgetsPass pass) {
    if (customTex != null) {
      renderTex(customTex, absAlpha, pass);
    }
    else if (texture != null) {
      renderTex(texture.asTexture(), absAlpha, pass);
    }
    if (greyOut != null && ! enabled) {
      renderTex(greyOut.asTexture(), absAlpha, pass);
    }
    if (overlaid != null) for (Texture t : overlaid) {
      renderTex(t, absAlpha, pass);
    }
  }
  
  
  protected void renderTex(Texture tex, float alpha, WidgetsPass pass) {
    final Box2D drawn = new Box2D(bounds);
    if (lockToPixels) {
      drawn.xpos((int) drawn.xpos());
      drawn.ypos((int) drawn.ypos());
      drawn.xdim((int) drawn.xdim());
      drawn.ydim((int) drawn.ydim()); 
    }
    pass.draw(
      tex, Colour.transparency(alpha),
      drawn.xpos(), drawn.ypos(), drawn.xdim(), drawn.ydim(),
      0.0f, 1.0f, 1.0f, 0.0f
    );
  }
}




