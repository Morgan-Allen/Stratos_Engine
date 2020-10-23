/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package graphics.common;
import util.*;



public class Lighting {
  
  
  private static boolean verbose = false;
  
  final Rendering rendering;
  final public Colour ambient = new Colour();
  final public Colour diffuse = new Colour();
  final public Vec3D direction = new Vec3D();
  final public float lightSum[] = new float[4];
  final public float lightDir[] = new float[3];
  
  
  Lighting(Rendering rendering) {
    this.rendering = rendering;
    setup(1, 1, 1);
  }
  
  
  /**  Initialises this light based on expected rgb values, ambience ratio,
    *  and whether ambient light should complement diffuse shading (to create
    *  the appearance of naturalistic shadows.)
    */
  public void setup(
    float r,
    float g,
    float b
  ) {
    if (verbose) I.add("\n\nRGB are: "+r+" "+g+" "+b);
    
    diffuse.set(r, g, b, 0.8f);
    diffuse.complement(ambient);
    ambient.a = 1 - diffuse.a;
    direction.set(0.25f, -0.5f, -0.5f).normalise();
    
    lightDir[0] = direction.x;
    lightDir[1] = direction.y;
    lightDir[2] = direction.z;
    
    final Colour d = diffuse, a = ambient;
    lightSum[0] = Nums.clamp((d.r * d.a) + (a.r * a.a), 0, 1);
    lightSum[1] = Nums.clamp((d.g * d.a) + (a.g * a.a), 0, 1);
    lightSum[2] = Nums.clamp((d.b * d.a) + (a.b * a.a), 0, 1);
    lightSum[3] = 1;
    
    if (verbose) {
      I.add("\nLight sum is: ");
      for (float f : lightSum) I.add(f+" ");
    }
  }
}







