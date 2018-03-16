/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package start;
import graphics.common.*;
import graphics.widgets.*;
import util.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;



public final class PlayLoop implements ApplicationListener {
  
  
  /**  Fields and constant definitions-
    */
  public static boolean
    verbose = false;
  
  final public static String DEFAULT_INIT_PACKAGES[] = {
    "game", "gameUI", "content"
  };
  
  public final static int
    UPDATES_PER_SECOND = 10,
    FRAMES_PER_SECOND  = 60,
    
    MIN_SLEEP    = 10,
    SLEEP_MARGIN = 2;
  
  
  private static String initPackages[] = DEFAULT_INIT_PACKAGES;
  
  private static Rendering rendering;
  private static Playable playing;
  private static Playable prepared;
  private static Thread gdxThread;
  private static boolean loopChanged = false;
  
  private static long lastFrame, lastUpdate;
  private static float frameTime;
  private static long numStateUpdates = 0, numFrameUpdates = 0;
  private static float gameSpeed = 1.0f;
  
  private static boolean
    initDone   = false,
    shouldLoop = false,
    paused     = false,
    background = false,
    noInput    = false;
  
  
  
  /**  Returns the components of the current game state-
    */
  public static HUD currentUI() {
    if (playing == null) return null;
    return playing.UI(playing.isLoading());
  }
  
  public static Rendering rendering() {
    return rendering;
  }
  
  public static Playable played() {
    return playing;
  }
  
  public static boolean onMainThread() {
    return Thread.currentThread() == gdxThread;
  }
  
  public static boolean mainThreadBegun() {
    return gdxThread != null;
  }
  
  
  
  
  /**  New GDX method overrides-
    */
  public void create() {
    //
    //  NOTE:  We perform some extra diagnostic printouts here, since the
    //  GL context wasn't obtainable earlier:
    I.say(
      "Please send me this info"+
      "\n--- GL INFO -----------"+
      "\n   GL_VENDOR: "+Gdx.gl.glGetString(GL20.GL_VENDOR)+
      "\n GL_RENDERER: "+Gdx.gl.glGetString(GL20.GL_RENDERER)+
      "\n  GL_VERSION: "+Gdx.gl.glGetString(GL20.GL_VERSION)+
      "\nGLSL_VERSION: "+Gdx.gl.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION)+
      "\n-----------------------\n"
    );
    shouldLoop = true;
    
    for (String assetPackage : initPackages) {
      Assets.compileAssetList(assetPackage);
    }
    rendering = new Rendering();
  }
  
  
  public void resize(int width, int height) {
  }
  
  
  public void dispose() {
    disposeLoop();
  }
  
  
  public void pause() {
    background = true;
  }
  
  
  public void resume() {
    background = false;
  }
  
  
  public void render() {
    gdxThread = Thread.currentThread();
    if (! shouldLoop) {
      if (verbose) I.say("should not be looping...");
      return;
    }
    
    final boolean okay = advanceLoop();
    
    if (! okay) {
      if (verbose) I.say("Loop does not want to advance!");
      exitLoop();
    }
  }
  
  
  
  /**  The big static setup, run and exit methods-
    */
  public static void setupAndLoop(Playable scenario) {
    setupAndLoop(scenario, DEFAULT_INIT_PACKAGES);
  }
  
  
  public static void setupAndLoop(
    Playable scenario, String... initPackages
  ) {
    PlayLoop.initPackages    = initPackages;
    PlayLoop.loopChanged     = true;
    PlayLoop.prepared        = scenario;
    PlayLoop.numStateUpdates = 0;
    PlayLoop.numFrameUpdates = 0;
    PlayLoop.gameSpeed       = 1.0f;
    
    if (verbose) {
      I.say("ASSIGNED NEW PLAYABLE: "+scenario);
      I.reportStackTrace();
    }
    
    if (! initDone) {
      initDone = true;
      
      //  TODO:  You'll have to pipe this through to the DesktopLauncher (or
      //  something else platform-appropriate) before something can happen!
      
      /*
      new LwjglApplication(new ApplicationListener() {
      }, getConfig());
      //*/
    }
  }
  
  
  public static void sessionStateWipe() {
    I.talkAbout = null;
    playing     = null;
    Assets.disposeSessionAssets();
    
    if (rendering != null) rendering.clearAll();
  }
  
  
  public static void exitLoop() {
    if (verbose) I.say("EXITING PLAY LOOP");
    shouldLoop = false;
    Gdx.app.exit();
  }
  
  
  private static void disposeLoop() {
    rendering.dispose();
    Assets.disposeGameAssets();
  }
  
  
  private static boolean advanceLoop() {
    
    final long time = timeMS(), frameGap = time - lastFrame, updateGap;
    final int FRAME_INTERVAL  = 1000 / FRAMES_PER_SECOND;
    final int UPDATE_INTERVAL = (int) (
      1000 / (UPDATES_PER_SECOND * gameSpeed)
    );
    final boolean freeze = paused || background;
    
    if (freeze || (time - lastUpdate) > UPDATE_INTERVAL * 10) {
      lastUpdate = time;
      updateGap = 0;
    }
    else {
      updateGap = time - lastUpdate;
      frameTime = (updateGap - 0) * 1.0f / UPDATE_INTERVAL;
      frameTime = Nums.clamp(frameTime, 0, 1);
    }
    
    if (playing != prepared) {
      if (playing != null && playing.wipeAssetsOnExit()) {
        PlayLoop.sessionStateWipe();
      }
      playing = prepared;
    }
    loopChanged = false;
    float worldTime = (numStateUpdates + frameTime) / UPDATES_PER_SECOND;
    rendering.updateViews(worldTime, frameTime);
    
    if (verbose) {
      I.say("\nAdvancing play loop, time: "+time);
      I.say("  Last frame/last update: "+lastFrame+"/"+lastUpdate);
      I.say("  Frame/update gap: "+frameGap+"/"+updateGap);
      I.say("  FRAME/UPDATE INTERVAL: "+FRAME_INTERVAL+"/"+UPDATE_INTERVAL);
    }
    
    if (Assets.loadProgress() < 1) {
      if (verbose) {
        I.say("  Loading assets!");
        I.say("  Loading progress: "+Assets.loadProgress());
      }
      
      Assets.advanceAssetLoading(FRAME_INTERVAL - (SLEEP_MARGIN * 2));
      
      rendering.renderDisplay(FRAMES_PER_SECOND);
      rendering.renderUI(null);
      return true;
    }
    
    if (loopChanged) {
      if (verbose) I.say("  Loop changed!  Will return");
      return true;
    }
    if (playing != null && playing.loadProgress() < 1) {
      if (verbose) {
        I.say("  Loading simulation: "+playing);
        I.say("  Is loading?         "+playing.isLoading());
        I.say("  Loading progress:   "+playing.loadProgress());
      }
      if (playing.shouldExitLoop()) {
        if (verbose) I.say("  Exiting loop!  Will return");
        return false;
      }
      
      if (! playing.isLoading()) {
        if (verbose) I.say("  Beginning simulation setup...");
        playing.beginGameSetup();
      }
      
      rendering.renderDisplay(FRAMES_PER_SECOND);
      rendering.renderUI(playing.UI(true));
      lastUpdate = lastFrame = time;
      return true;
    }

    //  TODO:  I'm updating graphics as fast as possible for the moment, since
    //  I get occasional flicker problems otherwise.  Still seems wasteful,
    //  mind...
    if (loopChanged) {
      if (verbose) I.say("  Loop changed!  Will return");
      return true;
    }
    if (frameGap >= FRAME_INTERVAL || true) {
      if (verbose) I.say("  Rendering graphics.");
      
      if (playing != null) {
        playing.renderVisuals(rendering);
      }
      
      final HUD UI = playing == null ? null : playing.UI(false);
      if (UI != null) {
        UI.updateInput();
        UI.renderWorldFX();
      }
      
      rendering.renderDisplay(FRAMES_PER_SECOND);
      rendering.renderUI(UI);
      KeyInput.updateInputs();
      
      lastFrame = time;
      numFrameUpdates++;
      
      I.used60Frames = numFrameUpdates % 60 == 0;
    }
    
    //  Now we essentially 'pretend' that updates were occurring once every
    //  UPDATE_INTERVAL milliseconds:
    if (playing != null) {
      final int numUpdates = Nums.min(
        (int) (updateGap / UPDATE_INTERVAL),
        (1 + (FRAME_INTERVAL / UPDATE_INTERVAL))
      );
      if (playing.shouldExitLoop()) {
        if (verbose) I.say("  Exiting loop!  Will return");
        return false;
      }
      
      if (verbose) I.say("  No. of updates: "+numUpdates);
      if (! freeze) for (int n = numUpdates; n-- > 0;) {
        
        if (loopChanged) {
          if (verbose) I.say("  Loop changed!  Will return");
          return true;
        }
        if (verbose) I.say("  Updating simulation.");
        playing.updateGameState();
        numStateUpdates++;
        lastUpdate += UPDATE_INTERVAL;
      }
    }
    
    return true;
  }
  
  
  private static long timeMS() {
    return java.lang.System.nanoTime() / 1000000;
  }
  
  
  
  /**  Pausing the loop, exiting the loop, and setting simulation speed and
    *  frame rate.
    */
  public static float frameTime() {
    return frameTime;
  }
  
  
  public static long frameUpdates() {
    return numFrameUpdates;
  }
  
  
  public static long stateUpdates() {
    return numStateUpdates;
  }
  
  
  public static boolean isFrameIncrement(int unit) {
    return (numFrameUpdates % unit) == 0;
  }
  
  
  public static boolean paused() {
    return paused;
  }
  
  
  public static float gameSpeed() {
    return gameSpeed;
  }
  
  
  public static void setGameSpeed(float mult) {
    gameSpeed = Nums.max(0, mult);
  }
  
  
  public static void setPaused(boolean p) {
    paused = p;
  }
  
  
  public static void setNoInput(boolean n) {
    noInput = n;
  }
}




