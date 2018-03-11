


package start;
import game.*;
import gameUI.play.*;
import graphics.common.*;
import graphics.widgets.*;
import util.*;



public abstract class CityMapScenario implements Session.Saveable {
  
  
  MainGame game;
  boolean setupDone = false;
  
  World   verse = null;
  AreaMap stage = null;
  Base    base  = null;
  
  PlayUI UI;
  
  
  protected CityMapScenario() {
  }
  
  
  public CityMapScenario(Session s) throws Exception {
    s.cacheInstance(this);
    verse = (World  ) s.loadObject();
    stage = (AreaMap) s.loadObject();
    base  = (Base   ) s.loadObject();
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
  protected void initScenario(MainGame game) {
    setupDone = false;
    this.game = game;
    
    verse = null;
    stage = null;
    base  = null;
    UI    = null;
    verse = createWorld();
    stage = createStage(verse);
    base  = createBase(stage, verse);
    UI    = new PlayUI(PlayLoop.rendering());
    
    UI.assignParameters(stage, base);
    configScenario(verse, stage, base);
    
    setupDone = true;
  }
  
  
  protected void afterLoading(MainGame game) {
    this.game = game;
    setupDone = true;
  }
  
  
  protected abstract String savePath();
  protected abstract World createWorld();
  protected abstract AreaMap createStage(World world);
  protected abstract Base createBase(AreaMap map, World world);
  protected abstract void configScenario(World world, AreaMap map, Base base);
  
  
  public float loadProgress() {
    return setupDone ? 1 : 0;
  }
  
  
  public void updateScenario() {
    stage.update();
    //stage.updateStage(PlayLoop.UPDATES_PER_SECOND);
  }
  
  
  public Base    base () { return base ; }
  public AreaMap stage() { return stage; }
  public World   verse() { return verse; }
  
  
  public void renderVisuals(Rendering rendering) {
    stage.renderStage(rendering, base);
  }
  
  
  public PlayUI playUI() {
    return UI;
  }
}





