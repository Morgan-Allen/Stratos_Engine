

package start;
import game.*;
import gameUI.main.*;
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
  
  
  MainScreen mainScreen;
  PlayUI playUI;
  
  World world;
  Scenario scenario;
  
  
  MainGame() {
    return;
  }
  
  
  
  /**  Static setup methods for convenience-
    */
  public static MainGame mainGame() {
    if (current != null) return current;
    PlayLoop.setupAndLoop(current = new MainGame());
    return current;
  }
  
  
  public static void playScenario(Scenario s, World w) {
    mainGame();
    current.scenario = null;
    current.world    = null;
    current.scenario = s;
    current.world    = w;
  }
  
  
  public static Scenario currentScenario() {
    if (current == null) return null;
    return current.scenario;
  }
  
  
  public static World currentWorld() {
    if (current == null) return null;
    return current.world;
  }
  
  
  public static void scheduleSave() {
    if (currentScenario() == null) return;
    current.nextOp = DO_SAVE;
  }
  
  
  public static void scheduleReload() {
    if (currentScenario() == null) return;
    current.nextOp = DO_LOAD;
  }
  
  
  public static MainScreen mainScreen() {
    if (current == null) return null;
    return current.mainScreen;
  }
  
  
  public static PlayUI playUI() {
    if (currentScenario() == null) return null;
    return current.scenario.playUI();
  }
  
  
  public static boolean restartScenario() {
    if (current == null || current.scenario == null) return false;
    current.scenario.initScenario(mainGame());
    return true;
  }
  
  
  public static boolean loadGameState(String savePath) {
    if (Assets.exists(savePath)) try {
      Session s = Session.loadSession(savePath, true);
      Scenario scenario = (Scenario) s.loaded()[0];
      World    world    = (World   ) s.loaded()[1];
      
      scenario.afterLoading(mainGame());
      
      current.scenario = scenario;
      current.world    = world   ;
      
      I.say("Scenario loading done");
      return true;
    }
    catch (Exception e) { I.report(e); }
    return false;
  }
  
  
  public static boolean saveGameState() {
    try {
      I.say("\nWill save game...");
      
      long initTime = I.getTime();
      Scenario scenario = current.scenario;
      World    world    = current.world   ;
      
      String savePath = world.savePath();
      Session.saveSession(savePath, scenario, world);
      
      long timeSpent = I.getTime() - initTime;
      I.say("  Saving done: "+timeSpent+" ms.");
      return true;
    }
    catch (Exception e) { I.report(e); }
    return false;
  }
  
  
  /*
  public static Scenario loadScenario(String savePath) {
    if (Assets.exists(savePath)) try {
      Session s = Session.loadSession(savePath, true);
      Scenario loaded = (Scenario) s.loaded()[0];
      loaded.afterLoading(mainGame());
      I.say("Scenario loading done");
      return loaded;
    }
    catch (Exception e) { I.report(e); }
    return null;
  }
  
  
  public static boolean saveScenario(Scenario scenario) {
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
  //*/
  
  
  
  /**  PlayLoop implementation-
    */
  public void updateGameState() {
    if (scenario == null) {
      return;
    }
    else if (scenario.loadProgress() < 1) {
      return;
    }
    else {
      scenario.updateScenario();
    }
  }
  
  
  public void renderVisuals(Rendering rendering) {
    if (scenario == null) {
      return;
    }
    else if (scenario.loadProgress() < 1) {
      return;
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
  
  
  public HUD UI() {
    if (mainScreen == null) {
      mainScreen = new MainScreen(PlayLoop.rendering());
    }
    if (scenario == null) {
      return mainScreen;
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
      if (saveGameState()) return true;
    }
    if (nextOp == DO_SAVE) {
      saveGameState();
    }
    if (nextOp == DO_RESTART) {
      restartScenario();
    }
    if (nextOp == DO_LOAD && scenario != null) {
      String savePath = world.savePath();
      scenario = null;
      world    = null;
      loadGameState(savePath);
      if (scenario != null) world = scenario.world();
    }
    nextOp = DO_PLAY;
    return false;
  }
  
  
  public boolean wipeAssetsOnExit() {
    return false;
  }
}




