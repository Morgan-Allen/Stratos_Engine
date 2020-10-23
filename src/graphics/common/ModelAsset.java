/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package graphics.common;
import util.*;
import graphics.cutout.CutoutModel;

import java.io.*;
import java.awt.Color;



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
  
  
  public abstract Object sortingKey();
  public abstract Sprite makeSprite();
  
  
  
  
  
  /**  Assorted dummy methods that it's useful to declare here and allow
    *  subclasses to override as required-
    */
  public void attachAnimRange(String name, int start, int end, boolean turns) {
    return;
  }
  
  public void attachAnimRange(String name, boolean turns) {
    return;
  }
  
  public void attachOverlay(CutoutModel media, float x, float y, String... names) {
    return;
  }
  
  public boolean hasAnimation(String name) {
    return false;
  }
  
  public Color colourTint() {
    return Color.BLACK;
  }
}







