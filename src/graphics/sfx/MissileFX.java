

package graphics.sfx;
import graphics.common.*;
import graphics.cutout.*;
import util.*;
import java.io.*;




public class MissileFX extends CutoutSprite {
  
  
  Vec3D origin = new Vec3D();
  Vec3D target = new Vec3D();
  float initTime = -1, duration, arcHigh;
  public float distance;
  
  
  public MissileFX(CutoutModel model) {
    super(model, 0);
  }
  
  protected void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in);
    origin.loadFrom(in);
    target.loadFrom(in);
    initTime = in.readFloat();
    duration = in.readFloat();
  }
  
  protected void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out);
    origin.saveTo(out);
    target.saveTo(out);
    out.writeFloat(initTime);
    out.writeFloat(duration);
  }
  
  
  
  
  public void attachMissile(Vec3D origin, Vec3D target, float duration, float arcHigh) {
    this.origin.setTo(origin);
    this.target.setTo(target);
    this.duration = duration;
    this.arcHigh  = arcHigh ;
    this.position.setTo(origin).add(target).scale(0.5f);
    this.distance = origin.distance(target);
  }
  

  public void readyFor(Rendering rendering) {
    
    final float time = Rendering.activeTime();
    if (initTime == -1) initTime = time;
    
    float progress = (time - initTime) / duration;
    position.setTo(target).sub(origin).scale(progress);
    float angle = (360 + 90 - Vec2D.toAngle(position.y, position.x)) % 360;
    position.add(origin);
    position.z += arcHigh * progress * (1 - progress) * 4;
    
    setFacing((int) (angle * 16 / 360f));
    super.readyFor(rendering);
  }
}













