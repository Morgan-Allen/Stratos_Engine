

//*

package start;
//import game.stage.*;
//import game.verse.*;
import gameUI.play.*;
import graphics.common.*;
import graphics.widgets.*;
import util.*;


/*

public abstract class Scenario implements Session.Saveable {
  
  
  MainGame game;
  boolean setupDone = false;
  
  Verse verse = null;
  Stage stage = null;
  Base  base  = null;
  
  PlayUI UI;
  
  
  protected Scenario() {
  }
  
  
  public Scenario(Session s) throws Exception {
    s.cacheInstance(this);
    verse = (Verse) s.loadObject();
    stage = (Stage) s.loadObject();
    base  = (Base ) s.loadObject();
    UI = new PlayUI(PlayLoop.rendering());
    UI.loadState(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(verse);
    s.saveObject(stage);
    s.saveObject(base );
    UI.saveState(s);
  }
  
  
  
  /**  Initial setup methods-
    */
/*
  protected void initScenario(MainGame game) {
    setupDone = false;
    this.game = game;
    
    verse = null;
    stage = null;
    base  = null;
    
    verse = createVerse();
    stage = createStage(verse);
    base  = createBase(stage, verse);
    
    UI = new PlayUI(PlayLoop.rendering());
    UI.assignPlayer(stage, base);
    
    verse.assignStage(stage);
    stage.setupLocale(base.location(), verse, base);
    configScenario(verse, stage, base);
    
    setupDone = true;
  }
  
  
  protected void afterLoading(MainGame game) {
    this.game = game;
    setupDone = true;
  }
  
  
  protected abstract String savePath();
  protected abstract Verse createVerse();
  protected abstract Stage createStage(Verse verse);
  protected abstract Base createBase(Stage stage, Verse verse);
  protected abstract void configScenario(Verse verse, Stage stage, Base base);
  
  
  public float loadProgress() {
    return setupDone ? 1 : 0;
  }
  
  
  public void updateScenario() {
    stage.updateStage(PlayLoop.UPDATES_PER_SECOND);
  }
  
  
  public void renderVisuals(Rendering rendering) {
    stage.renderStage(rendering, base);
  }
  
  
  public PlayUI playUI() {
    return UI;
  }
}
//*/





