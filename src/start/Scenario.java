


package start;
import game.*;
import gameUI.play.*;
import graphics.common.*;
import util.*;



public abstract class Scenario implements Session.Saveable {
  
  
  MainGame game;
  boolean setupDone = false;
  
  World world = null;
  AreaMap  area  = null;
  Base  base  = null;
  
  private PlayUI UI;
  
  
  protected Scenario() {
    return;
  }
  
  
  public Scenario(Session s) throws Exception {
    s.cacheInstance(this);
    world = (World) s.loadObject();
    area  = (AreaMap ) s.loadObject();
    base  = (Base ) s.loadObject();
    //UI = new PlayUI(PlayLoop.rendering());
    //UI.loadState(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(world);
    s.saveObject(area );
    s.saveObject(base );
    //UI.saveState(s);
  }
  
  
  
  /**  Initial setup methods-
    */
  public void initScenario(MainGame game) {
    setupDone = false;
    this.game = game;
    
    wipeScenario();
    
    world = createWorld();
    area  = createMap(world);
    base  = createBase(area, world);
    configScenario(world, area, base);
    
    setupDone = true;
  }
  
  
  public void wipeScenario() {
    world = null;
    area  = null;
    base  = null;
    UI    = null;
  }
  
  
  protected void afterLoading(MainGame game) {
    this.game = game;
    setupDone = true;
  }
  
  
  protected abstract World createWorld();
  protected abstract AreaMap createMap(World world);
  protected abstract Base createBase(AreaMap map, World world);
  protected abstract void configScenario(World world, AreaMap map, Base base);
  
  
  public float loadProgress() {
    return setupDone ? 1 : 0;
  }
  
  
  
  /**  Regular update and access methods-
    */
  public void updateScenario() {
    area.update(PlayLoop.UPDATES_PER_SECOND);
  }
  
  
  public Base  base () { return base ; }
  public AreaMap  area () { return area ; }
  public World world() { return world; }
  
  
  public Series <WorldScenario.Objective> objectives() {
    return new Batch();
  }
  
  
  
  /**  Graphical, UI and debug methods-
    */
  public void renderVisuals(Rendering rendering) {
    area.renderStage(rendering, base);
  }
  
  
  public PlayUI playUI() {
    if (UI == null) {
      UI = new PlayUI(PlayLoop.rendering());
      UI.assignParameters(area, base);
      UI.setLookPoint(base.headquarters());
    }
    return UI;
  }
}





