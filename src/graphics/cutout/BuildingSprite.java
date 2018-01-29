/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package graphics.cutout;
import graphics.common.*;
import graphics.sfx.*;
import util.*;

import java.io.*;
import com.badlogic.gdx.math.*;




public class BuildingSprite extends Sprite implements TileConstants {
  
  /**  Constant fields and model definitions-
    */
  final public static String
    STATE_FOUNDING  = "founding" ,
    STATE_UPGRADE_0 = "upgrade_0",
    STATE_LEVEL_1   = "level_1"  ,
    STATE_DAMAGED_1 = "damaged_1",
    STATE_UPGRADE_1 = "upgrade_1",
    STATE_LEVEL_2   = "level_2"  ,
    STATE_DAMAGED_2 = "damaged_2",
    STATE_UPGRADE_2 = "upgrade_2",
    STATE_LEVEL_3   = "level_3"  ,
    STATE_DAMAGED_3 = "damaged_3",
    
    STATE_LEVELS[] = { STATE_LEVEL_1, STATE_LEVEL_2, STATE_LEVEL_3 }
  ;
  
  public static class Model extends ModelAsset {
    
    final Table <String, ModelAsset> stateModels;
    final ModelAsset defaultModel;
    
    
    public Model(
      Class sourceClass, String modelID,
      int size, int height,
      String basePath, Table <String, String> forStates
    ) {
      super(sourceClass, modelID);
      this.stateModels = new Table();
      ModelAsset first = null;
      
      for (String state : forStates.keySet()) {
        final String path = forStates.get(state);
        final CutoutModel model = CutoutModel.fromImage(
          sourceClass, modelID+"_"+state,
          basePath+path, size, height
        );
        if (first == null) first = model;
        stateModels.put(state, model);
      }
      
      ModelAsset level1 = stateModels.get(STATE_LEVEL_1);
      this.defaultModel = level1 == null ? first : level1;
    }
    
    
    public Object sortingKey() {
      return this;
    }
    
    
    public Sprite makeSprite() {
      return new BuildingSprite(this);
    }
    
    
    protected State loadAsset() {
      for (ModelAsset asset : stateModels.values()) {
        if (! asset.stateLoaded()) Assets.loadNow(asset);
      }
      return state = State.LOADED;
    }
    
    
    protected State disposeAsset() {
      for (ModelAsset asset : stateModels.values()) {
        if (! asset.stateDisposed()) Assets.disposeOf(asset);
      }
      return state = State.DISPOSED;
    }
  }
  
  
  public static String animForLevel(int level) {
    return STATE_LEVELS[Nums.clamp(level - 1, STATE_LEVELS.length)];
  }
  
  
  
  /**  Data fields, construction and setup methods-
    */
  final Model model;
  ModelAsset stateModel;
  Sprite stateSprite;
  float animProgress;
  
  
  public BuildingSprite(Model model) {
    this.model        = model;
    this.stateModel   = model.defaultModel;
    this.stateSprite  = stateModel.makeSprite();
    this.animProgress = 0;
  }
  
  
  public ModelAsset model() {
    return model;
  }
  
  
  
  /**  State modulation-
    */
  public void setAnimation(String anim, float progress, boolean loop) {
    final ModelAsset forAnim = model.stateModels.get(anim);
    if (forAnim == null || forAnim == stateModel) return;
    
    this.stateModel   = forAnim;
    this.animProgress = progress;
    this.stateSprite  = stateModel.makeSprite();
  }
  
  
  public void readyFor(Rendering rendering) {
    if (stateSprite == null) return;
    stateSprite.matchTo(this);
    stateSprite.readyFor(rendering);
  }
  
}



