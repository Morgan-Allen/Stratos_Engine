/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package graphics.sfx;
import graphics.common.*;
import util.*;
import java.io.*;
import com.badlogic.gdx.graphics.*;



public class ShieldFX extends SFX {
  
  
  /**  Fields, constants, setup and save/load methods-
    */
  final public static float
    BURST_FADE_INC  = 0.033f,
    MIN_ALPHA_GLOW  = 0.00f,
    MAX_BURST_ALPHA = 0.99f
  ;

  public static class Model extends graphics.common.ModelAsset {
    
    String texName, haloName;
    Texture burstTex;
    Texture haloTex;
    
    public Model(
      String modelName, Class modelClass,
      String texFileName, String haloFileName
    ) {
      super(modelClass, modelName);
      this.texName = texFileName;
      this.haloName = haloFileName;
    }
    
    
    protected State loadAsset() {
      burstTex = ImageAsset.getTexture(texName );
      haloTex  = ImageAsset.getTexture(haloName);
      if (burstTex != null && haloTex != null) return state = State.LOADED;
      else return state = State.ERROR;
    }
    
    
    protected State disposeAsset() {
      if (burstTex != null) burstTex.dispose();
      if (haloTex  != null) haloTex .dispose();
      return state = State.DISPOSED;
    }
    
    
    public Sprite makeSprite() { return new ShieldFX(this); }
    public Object sortingKey() { return haloTex; }
  }
  
  
  
  final Model model;
  
  class Burst { float angle, timer; }
  private Stack <Burst> bursts = new Stack <Burst> ();
  private float glowAlpha = 0.0f;
  private static Mat3D rotMat = new Mat3D();
  
  
  public ShieldFX(Model model) {
    super(PRIORITY_MIDDLE);
    this.model = model;
  }
  
  
  public ModelAsset model() { return model; }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out);
    out.writeFloat(glowAlpha);
    out.writeInt(bursts.size());
    for (Burst b : bursts) {
      out.writeFloat(b.angle);
      out.writeFloat(b.timer);
    }
  }
  
  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in);
    glowAlpha = in.readFloat();
    for (int i = in.readInt(); i-- > 0;) {
      final Burst b = new Burst();
      b.angle = in.readFloat();
      b.timer = in.readFloat();
      bursts.add(b);
    }
  }
  
  
  
  /**  Specialty methods for use by external clients-
    */
  public void attachBurstFromPoint(Vec3D point, boolean intense) {
    final Burst burst = new Burst();
    burst.angle = 270 - new Vec2D(
      position.x - point.x,
      position.y - point.y
    ).toAngle();
    //I.say("Angle is: "+burst.angle);
    burst.timer = intense ? 1 : 2;
    bursts.add(burst);
    glowAlpha = 1;
  }
  
  
  public void resetGlow() {
    glowAlpha = 1;
  }
  
  
  public Vec3D interceptPoint(Vec3D origin) {
    final Vec3D offset = new Vec3D().setTo(position).sub(origin);
    final float newLength = offset.length() - scale;
    offset.scale(newLength / offset.length());
    offset.add(origin);
    return offset;
  }
  
  
  public boolean visible() {
    return glowAlpha > MIN_ALPHA_GLOW;
  }
  
  
  
  /**  Actual rendering-
    */
  public void readyFor(Rendering rendering) {
    glowAlpha -= BURST_FADE_INC;
    if (glowAlpha < MIN_ALPHA_GLOW) glowAlpha = MIN_ALPHA_GLOW;
    for (Burst burst : bursts) {
      burst.timer -= BURST_FADE_INC;
      if (burst.timer <= 0) bursts.remove(burst);
    }
    
    super.readyFor(rendering);
  }
  
  
  protected void renderInPass(SFXPass pass) {
    //
    //  First, establish coordinates for the halo corners-
    final Vec3D flatPos = new Vec3D().setTo(position);
    pass.rendering.view.translateToScreen(flatPos);
    //
    //  Render the halo itself-
    final float r = scale * pass.rendering.view.screenScale();
    pass.compileQuad(
      model.haloTex, Colour.transparency(glowAlpha),
      true, flatPos.x - r, flatPos.y - r, r * 2,
      r * 2, 0, 0, 1,
      1, flatPos.z, true
    );
    //
    //  Then render each burst-
    for (Burst burst : bursts) renderBurst(pass, burst);
  }
  
  
  private void renderBurst(SFXPass pass, Burst burst) {
    final float s = burst.timer * 2;
    final float QV[] = SFXPass.QUAD_VERTS;
    int i = 0; for (Vec3D v : verts) {
      v.set(
        (QV[i++] - 0.5f) * s,
        (QV[i++] - 0.5f) * s,
         QV[i++] + 2
      );
    }
    
    rotMat.setIdentity();
    rotMat.rotateZ((float) Nums.toRadians(burst.angle));
    rotMat.rotateX((float) Nums.toRadians(90));
    for (Vec3D v : verts) {
      rotMat.trans(v);
      v.scale(scale / 2f);
      v.add(position);
    }
    
    pass.compileQuad(
      model.burstTex, Colour.transparency(burst.timer * MAX_BURST_ALPHA),
      true, verts, 0, 0, 1, 1
    );
  }
}






