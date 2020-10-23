/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package graphics.common;
//import stratos.start.*;
import util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;

import graphics.cutout.*;
import graphics.sfx.*;
import graphics.solids.*;
import graphics.terrain.*;
import graphics.widgets.*;




//  NOTE:  This class should not be instantiated until the LibGdx engine has
//  invoked the create() method for the ApplicationListener.
//
public class Rendering {
  
  
  final public Viewport view;
  final public Lighting lighting;
  public Colour backColour = null, foreColour = null;
  
  private int frameRate;
  private static float activeTime, frameAlpha;
  
  //  first terrain, then cutouts, then solids, then sfx, then the UI.
  final public TerrainPass terrainPass;
  final public SolidsPass  solidsPass ;
  final public CutoutsPass cutoutsPass;
  final public SFXPass     sfxPass    ;
  final public WidgetsPass widgetsPass;
  final Batch <Sprite> rendered = new Batch(100);
  
  
  public Rendering() {
    lighting = new Lighting(this);
    view = new Viewport();
    
    terrainPass = new TerrainPass(this);
    solidsPass  = new SolidsPass(this);
    cutoutsPass = new CutoutsPass(this);
    sfxPass     = new SFXPass    (this);
    widgetsPass = new WidgetsPass(this);
  }
  
  
  public void dispose() {
    terrainPass.dispose();
    solidsPass .dispose();
    cutoutsPass.dispose();
    sfxPass    .dispose();
    widgetsPass.dispose();
  }
  
  
  public void clearAll() {
    terrainPass.clearAll();
    cutoutsPass.clearAll();
    solidsPass .clearAll();
    sfxPass    .clearAll();
  }
  
  
  public Camera camera() { return view.camera; }
  public int frameRate() { return frameRate; }
  public static float activeTime() { return activeTime; }
  public static float frameAlpha() { return frameAlpha; }
  
  
  public void updateViews(float worldTime, float frameTime) {
    Rendering.activeTime = worldTime;
    Rendering.frameAlpha = frameTime;
    view.update();
  }
  
  
  public void recordAsRendered(Sprite s) {
    rendered.add(s);
  }
  
  
  public void renderDisplay(int frameRate) {
    this.frameRate = frameRate;
    ///I.say("World and frame time are:"+worldTime+"/"+frameTime);
    
    //Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    //Gdx.gl.glEnable(GL20.GL_BLEND);
    //Gdx.gl.glDepthMask(true);
    
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
    Gdx.gl.glEnable(GL20.GL_TEXTURE);
    
    final Colour BC = backColour == null ? Colour.DARK_GREY : backColour;
    Gdx.gl.glClearColor(BC.r, BC.g, BC.b, BC.a);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    
    Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
    Gdx.gl.glDepthMask(false);
    
    terrainPass.performPass();
    cutoutsPass.performSplatPass();
    terrainPass.performOverlayPass();

    Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    Gdx.gl.glDepthMask(true);
    Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
    cutoutsPass.performNormalPass();
    
    Gdx.gl.glDepthMask(false);
    sfxPass.performPass();
    
    Gdx.gl.glDepthMask(true);
    Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
    cutoutsPass.performPreviewPass();
    
    for (Sprite s : rendered) s.passType = Sprite.PASS_NONE;
    rendered.clear();
  }
  
  
  public void renderUI(HUD UI) {
    widgetsPass.begin();
    if (UI != null) UI.renderHUD(this);
    widgetsPass.end();
  }
}




