/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package graphics.widgets;
import util.*;
import com.badlogic.gdx.graphics.*;
import graphics.common.*;



public class Bordering extends UIGroup {
  
  
  final ImageAsset borderTex;
  final public UIGroup inside;
  public int
    left   = 10, right = 10,
    bottom = 10, top   = 10;
  public float
    leftU   = 0.33f, rightU = 0.33f,
    bottomV = 0.33f, topV   = 0.33f;
  
  
  public Bordering(HUD UI, ImageAsset tex) {
    super(UI);
    this.borderTex = tex;
    this.inside = new UIGroup(UI);
    inside.attachTo(this);
  }
  
  
  public void setInsets(int l, int r, int b, int t) {
    left   = l;
    right  = r;
    bottom = b;
    top    = t;
  }
  
  
  public void setUV(float l, float r, float b, float t) {
    leftU   = l;
    rightU  = r;
    bottomV = b;
    topV    = t;
  }
  
  
  public void attachAndSurround(UINode parent) {
    attachTo(parent);
    surround(parent);
  }
  
  
  public void attachAndFitWithin(UINode parent) {
    attachTo(parent);
    alignToFill();
  }
  
  
  public void surround(UINode other) {
    if (other == this.parent()) {
      alignToFill();
    }
    else if (other.parent() == this.parent()) {
      alignToMatch(other);
    }
    else {
      I.complain("MUST HAVE COMMON REFERENCE FRAME!");
      return;
    }
    
    absBound.incX(0 - left);
    absBound.incWide(left + right);
    absBound.incY(0 - bottom);
    absBound.incHigh(bottom + top);
  }
  
  
  protected void updateState() {
    inside.relBound.set(0, 0, 1, 1);
    inside.absBound.set(left, bottom, 0 - (left + right), 0 - (top + bottom));
    super.updateState();
  }
  
  
  protected void render(WidgetsPass pass) {
    
    int innerPanHigh = (int) (bounds.ydim() - (top + bottom));
    int innerPanWide = (int) (bounds.xdim() - (left + right));
    int innerTexHigh = (int) (borderTex.pixelHigh() * (1 - (topV + bottomV)));
    int innerTexWide = (int) (borderTex.pixelWide() * (1 - (leftU + rightU)));
    
    innerTexHigh = Nums.max(1, innerTexHigh);
    innerTexWide = Nums.max(1, innerTexWide);
    
    float numRepsV = innerPanHigh * 1f / innerTexHigh;
    float numRepsH = innerPanWide * 1f / innerTexWide;
    
    int px = 0, py = 0, nx = 0, ny = 0;
    float u = 0, v = 0, nu = 0, nv = 0;
    
    Colour c = Colour.transparency(absAlpha);
    
    for (int x = -1; x < numRepsH + 1; x++) {
      
      if (x == -1) {
        px = 0;
        nx = left;
        u  = 0;
        nu = leftU;
      }
      else if (x < numRepsH) {
        px = nx;
        nx = Nums.min(nx + innerTexWide, left + innerPanWide);
        u  = leftU;
        nu = 1 - rightU;
        nu = u + ((nu - u) * (nx - px) / innerTexWide);
      }
      else {
        nx = (int) bounds.xdim();
        px = nx - right;
        u = 1 - rightU;
        nu = 1;
      }
      
      ///I.say("\nX is: "+x);
      
      for (int y = -1; y < numRepsV + 1; y++) {
        
        if (y == -1) {
          py = 0;
          ny = bottom;
          nv = 1 - topV;
          v  = 1;
        }
        else if (y < numRepsV) {
          py = ny;
          ny = Nums.min(ny + innerTexHigh, bottom + innerPanHigh);
          nv = bottomV;
          v  = 1 - topV;
          v  = nv + ((v - nv) * (ny - py) / innerTexHigh);
        }
        else {
          ny = (int) bounds.ydim();
          py = ny - top;
          nv = 0;
          v  = bottomV;
        }
        
        ///I.say("  Y: "+y+ ", corner is: "+px+" "+py+" "+nx+" "+ny);
        
        pass.draw(
          borderTex.asTexture(), c,
          px + bounds.xpos(), py + bounds.ypos(), nx - px, ny - py,
          u, v, nu, nv
        );
      }
    }
    
    super.render(pass);
  }
  
  
  
  /**  Public implementation for the convenience of other widgets-
    */
  final static float
    coordX[] = new float[4],
    coordY[] = new float[4],
    coordU[] = new float[4],
    coordV[] = new float[4]
  ;
  
  
  public static void renderBorder(
    WidgetsPass pass, Box2D area,
    int left, int right, int bottom, int top,
    float LU, float RU, float BV, float TV,
    Texture borderTex, Colour c
  ) {
    
    coordX[0] = 0;
    coordX[1] = left;
    coordX[2] = area.xdim() - right;
    coordX[3] = area.xdim();
    
    coordY[0] = 0;
    coordY[1] = bottom;
    coordY[2] = area.ydim() - top;
    coordY[3] = area.ydim();
    
    coordU[0] = 0;
    coordU[1] = LU;
    coordU[2] = 1 - RU;
    coordU[3] = 1;
    
    coordV[0] = 0;
    coordV[1] = BV;
    coordV[2] = 1 - TV;
    coordV[3] = 1;
    
    for (int i = 4; i-- > 0;) {
      coordX[i] += area.xpos();
      coordY[i] = area.ymax() - coordY[i];
    }
    
    for (int x = 3; x-- > 0;) for (int y = 3; y-- > 0;) {
      pass.draw(
        borderTex, c,
        coordX[x],
        coordY[y],
        coordX[x + 1] - coordX[x],
        coordY[y + 1] - coordY[y],
        coordU[x],
        coordV[y],
        coordU[x + 1],
        coordV[y + 1]
      );
    }
  }
}








