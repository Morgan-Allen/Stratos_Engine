/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package graphics.cutout;
import util.*;
import graphics.common.*;
import java.io.*;
import com.badlogic.gdx.math.*;



public class CutoutSprite extends Sprite {
  
  
  protected CutoutModel model;
  protected int faceIndex = 0;
  protected float depth;
  protected CutoutSprite overlays[] = null;
  
  
  
  public CutoutSprite(CutoutModel model, int faceIndex) {
    this.model = model;
    this.faceIndex = faceIndex;
  }
  
  
  protected void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out);
    out.write(faceIndex);
  }
  
  
  protected void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in);
    faceIndex = in.read();
  }
  
  
  public CutoutModel model() {
    return model;
  }
  
  
  
  public void setAnimation(String animName, float progress, boolean loop) {
    
    final boolean attached = model.isOverlay();
    boolean turns = false;
    int start = 0, end = 1;
    CutoutModel.AnimRange range = model.animations.get(animName);
    
    if ((attached && range == null) || animName.equals(AnimNames.HIDE)) {
      faceIndex = -1;
      return;
    }
    if (range == null) {
      range = model.animations.get(AnimNames.STILL);
    }
    if (range != null) {
      start = range.start;
      end   = range.end;
      turns = range.turns;
    }
    
    if (turns) {
      int facing = ((int) (8f * (rotation + 360 + 22.5f) / 360)) % 8;
      int frames = (end + 1 - start) / 8;
      int frame = Nums.clamp((int) (frames * progress), frames);
      faceIndex = start + (frame * 8) + facing;
    }
    else {
      int frames = end + 1 - start;
      int frame = Nums.clamp((int) ((frames - 1) * progress), frames);
      faceIndex = start + frame;
    }
    
    if (model == null || model.allFaces == null) {
      ///I.say("?");
      return;
    }
    
    faceIndex = Nums.clamp(faceIndex, model.allFaces.length);
    
    if (overlays != null) for (CutoutSprite s : overlays) {
      s.setAnimation(animName, progress, loop);
    }
  }
  
  
  public void setFacing(int index) {
    if (index == -1) this.faceIndex = -1;
    else this.faceIndex = Nums.clamp(index, model.allFaces.length);
  }
  
  
  public void readyFor(Rendering rendering) {
    rendering.recordAsRendered(this);
    
    if (faceIndex == -1) return;
    if (passType == PASS_NORMAL && model.splat) passType = PASS_SPLAT;
    
    if (overlays != null) {
      rendering.cutoutsPass.register(this);
      
      for (CutoutSprite s : overlays) if (s.faceIndex != -1) {
        
        final float pixels = Viewport.DEFAULT_SCALE;
        Vector3 temp = new Vector3();
        
        temp.x -= model.screenWide / 2f;
        temp.x += s.model.overX / pixels;
        temp.x += s.model.screenWide / 2f;
        
        temp.y -= model.minScreenHigh * -1;
        temp.y += s.model.overY / pixels;
        temp.y += s.model.minScreenHigh * -1;
        
        Viewport.isometricInverted(temp, temp);
        Viewport.GLToWorld(temp, s.position);
        
        s.position.scale(scale);
        s.position.add(position);
        
        s.passType = passType;
        s.colour   = colour;
        s.fog      = fog;
        s.scale    = scale;
        
        rendering.cutoutsPass.register(s);
      }
    }
    else {
      rendering.cutoutsPass.register(this);
    }
  }
  
  
  
  /**  Some additional utility methods for convenience (used for rendering
    *  status FX, good amounts when hovering over industries, etc...)
    */
  public static void renderAbove(
    Vec3D point, float offX, float offY, float offZ, Rendering rendering,
    float spacing, float scale, Series <CutoutModel> models
  ) {
    final Batch <CutoutSprite> sprites = new Batch();
    for (CutoutModel m : models) {
      CutoutSprite s = m.makeSprite();
      s.scale = scale;
      sprites.add(s);
    }
    layoutAbove(point, offX, offY, offZ, rendering.view, spacing, sprites);
    for (CutoutSprite s : sprites) s.readyFor(rendering);
  }
  
  
  public static void layoutAbove(
    Vec3D point, float offX, float offY, float offZ, Viewport view,
    float spacing, Series <? extends Sprite> sprites
  ) {
    float
      offAcross = (sprites.size() - 1) / 2f,
      index     = 0;
    final Vec3D
      horiz = view.screenHorizontal().normalise(),
      vert  = view.screenVertical  ().normalise(),
      deep  = view.direction       ().normalise(),
      adds  = new Vec3D();
    
    for (Sprite s : sprites) {
      s.position.setTo(point);
      adds.setTo(horiz).scale(offX + ((index - offAcross) * spacing));
      s.position.add(adds);
      adds.setTo(vert).scale(offY);
      s.position.add(adds);
      adds.setTo(deep).scale(offZ);
      s.position.add(adds);
      index++;
      
      s.colour = Colour.glow(1);
    }
  }
}
















