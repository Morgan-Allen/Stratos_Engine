

package start;
import game.*;
import gameUI.play.*;
import graphics.common.*;
import graphics.widgets.*;
import util.*;



public class MainGame implements Playable {
  
  final static int
    DO_PLAY      = -1,
    DO_SAVE      =  0,
    DO_SAVE_EXIT =  1,
    DO_LOAD      =  2,
    DO_RESTART   =  3
  ;
  
  static MainGame current;
  int nextOp = DO_PLAY;
  
  CityMapScenario scenario;
  
  
  
  /**  Static setup methods for convenience-
    */
  static MainGame mainGame() {
    if (current != null) return current;
    PlayLoop.setupAndLoop(current = new MainGame());
    return current;
  }
  
  
  public static void playScenario(CityMapScenario s) {
    mainGame();
    current.scenario = s;
    current.nextOp   = DO_LOAD;
  }
  
  
  public static CityMapScenario currentScenario() {
    if (current == null || current.scenario == null) return null;
    return current.scenario;
  }
  
  
  public static void scheduleSave() {
    if (currentScenario() == null) return;
    current.nextOp = DO_SAVE;
  }
  
  
  public static void scheduleReload() {
    if (currentScenario() == null) return;
    current.nextOp = DO_LOAD;
  }
  
  
  public static PlayUI playUI() {
    if (currentScenario() == null) return null;
    return current.scenario.playUI();
  }
  
  
  protected boolean restartScenario() {
    if (current == null || current.scenario == null) return false;
    current.scenario.initScenario(this);
    return true;
  }
  
  
  protected boolean loadScenario(String savePath) {
    if (Assets.exists(savePath)) try {
      Session s = Session.loadSession(savePath, true);
      CityMapScenario loaded = (CityMapScenario) s.loaded()[0];
      scenario = loaded;
      scenario.afterLoading(this);
      
      I.say("Scenario loading done");
      return true;
    }
    catch (Exception e) { I.report(e); }
    
    scenario.initScenario(this);
    
    I.say("Scenario setup done");
    return true;
  }
  
  
  protected boolean saveScenario() {
    if (scenario != null) try {
      I.say("\nWill save game...");
      long initTime = I.getTime();
      Session.saveSession(scenario.savePath(), scenario);
      long timeSpent = I.getTime() - initTime;
      I.say("  Saving done: "+timeSpent+" ms.");
      return true;
    }
    catch (Exception e) { I.report(e); }
    return false;
  }
  
  
  
  /**  PlayLoop implementation-
    */
  public void beginGameSetup() {
    //  TODO:  Set up main menu and other auxiliary data.
  }
  
  
  public void updateGameState() {
    if (scenario == null) {
    }
    else if (scenario.loadProgress() < 1) {
    }
    else {
      scenario.updateScenario();
    }
  }
  
  
  public void renderVisuals(Rendering rendering) {
    if (scenario == null) {
    }
    else if (scenario.loadProgress() < 1) {
    }
    else {
      scenario.renderVisuals(rendering);
    }
  }
  
  
  public float loadProgress() {
    if (scenario != null) {
      return scenario.loadProgress();
    }
    else {
      return 1;
    }
  }
  
  
  public HUD UI(boolean loading) {
    if (loading) {
      return null;
    }
    else if (scenario == null) {
      return null;
    }
    else if (scenario.loadProgress() < 1) {
      return null;
    }
    else {
      return scenario.playUI();
    }
  }
  
  
  public boolean shouldExitLoop() {
    if (nextOp == DO_SAVE_EXIT) {
      return saveScenario();
    }
    if (nextOp == DO_SAVE) {
      saveScenario();
    }
    if (nextOp == DO_RESTART) {
      restartScenario();
    }
    if (nextOp == DO_LOAD && scenario != null) {
      loadScenario(scenario.savePath());
    }
    nextOp = DO_PLAY;
    return false;
  }
  
  
  public boolean isLoading() {
    return false;
  }
  
  
  public boolean wipeAssetsOnExit() {
    return false;
  }
}



