/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package graphics.common;
import util.*;
import java.io.*;
import com.badlogic.gdx.graphics.*;
import java.lang.reflect.Field;



public abstract class Sprite {

  final public static float
    WHITE_BITS = Color.WHITE.toFloatBits(),
    GREEN_BITS = Color.GREEN.toFloatBits(),
    RED_BITS   = Color.RED  .toFloatBits(),
    BLUE_BITS  = Color.BLUE .toFloatBits(),
    BLACK_BITS = Color.BLACK.toFloatBits(),
    CLEAR_BITS = new Color(0, 0, 0, 0).toFloatBits();
  
  final public static int
    PASS_NONE    = -1,
    PASS_SPLAT   =  0,
    PASS_NORMAL  =  1,
    PASS_PREVIEW =  2,
    PASS_PLACING =  3;
  
  final public Vec3D position = new Vec3D();
  public float scale = 1, rotation = 0;
  public float fog = 1;
  public Colour colour = null;
  public int passType = PASS_NONE;
  
  
  protected void saveTo(DataOutputStream out) throws Exception {
    //  TODO:  FILL THESE IN
  }
  
  protected void loadFrom(DataInputStream in) throws Exception {
    //  TODO:  FILL THESE IN
  }
  
  
  public void matchTo(Sprite s) {
    position.setTo(s.position);
    scale  = s.scale ;
    fog    = s.fog   ;
    colour = s.colour;
  }
  
  
  public Vec3D attachPoint(String function, Vec3D v) {
    if (v == null) v = new Vec3D();
    v.setTo(position);
    v.z += 0.5f;
    return v;
  }
  
  
  /**  Checking for valid animation names-
    */
  private static Table<String, String> validAnimNames = null;
  
  public static boolean isValidAnimName(String animName) {
    if (validAnimNames == null) {
      validAnimNames = new Table<String, String>(100);
      for (Field field : AnimNames.class.getFields())
        try {
          if (field.getType() != String.class)
            continue;
          final String value = (String) field.get(null);
          validAnimNames.put(value, value);
        } catch (Exception e) {
      }
    }
    return validAnimNames.get(animName) != null;
  }
  
  
  
  public abstract ModelAsset model();
  public abstract void setAnimation(String anim, float progress, boolean loop);
  public abstract void setFacing(int faceIndex);
  public abstract void readyFor(Rendering rendering);
}






