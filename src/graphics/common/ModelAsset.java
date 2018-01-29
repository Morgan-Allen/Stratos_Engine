/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package graphics.common;
import util.*;

import java.io.*;


//
//  This is intended for use by external simulation classes, so that they can
//  maintain a static/constant reference to a graphical resource (i.e, a sprite
//  model) while it's still loading on another thread, and without blocking
//  any main-loop code that refers to the class.
//
//  This should also provide some convenience methods for saving/loading
//  sprites of a given model type, and caching the model in case of multiple
//  references.

public abstract class ModelAsset extends Assets.Loadable {
  
  
  protected ModelAsset(Class sourceClass, String modelName) {
    super(modelName, sourceClass, false);
  }
  
  public abstract Object sortingKey();
  
  
  public abstract Sprite makeSprite();
  
  
  public static void saveSprite(
      Sprite sprite, DataOutputStream out
  ) throws Exception {
    if (sprite == null) { Assets.saveReference(null, out); return; }
    final ModelAsset model = sprite.model();
    if (model == null) I.complain("Sprite must have model!");
    Assets.saveReference(model, out);
    sprite.saveTo(out);
  }
  
  
  public static Sprite loadSprite(
      DataInputStream in
  ) throws Exception {
    final ModelAsset model = (ModelAsset) Assets.loadReference(in);
    if (model == null) return null;
    final Sprite sprite = model.makeSprite();
    sprite.loadFrom(in);
    return sprite;
  }
}





