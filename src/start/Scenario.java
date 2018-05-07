


package start;
import game.*;
import gameUI.play.*;
import graphics.common.*;



public abstract class Scenario implements Session.Saveable {
  
  
  MainGame game;
  boolean setupDone = false;
  
  World world = null;
  Area  area  = null;
  Base  base  = null;
  
  PlayUI UI;
  
  
  protected Scenario() {
    return;
  }
  
  
  public Scenario(Session s) throws Exception {
    s.cacheInstance(this);
    world = (World) s.loadObject();
    area  = (Area ) s.loadObject();
    base  = (Base ) s.loadObject();
    UI = new PlayUI(PlayLoop.rendering());
    UI.loadState(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(world);
    s.saveObject(area );
    s.saveObject(base );
    UI.saveState(s);
  }
  
  
  
  /**  Initial setup methods-
    */
  protected void initScenario(MainGame game) {
    setupDone = false;
    this.game = game;
    
    world = null;
    area  = null;
    base  = null;
    UI    = null;
    world = createWorld();
    area  = createArea(world);
    base  = createBase(area, world);
    UI    = new PlayUI(PlayLoop.rendering());
    
    UI.assignParameters(area, base);
    configScenario(world, area, base);
    
    setupDone = true;
  }
  
  
  protected void afterLoading(MainGame game) {
    this.game = game;
    setupDone = true;
  }
  
  
  protected abstract String savePath();
  protected abstract World createWorld();
  protected abstract Area createArea(World world);
  protected abstract Base createBase(Area map, World world);
  protected abstract void configScenario(World world, Area map, Base base);
  
  
  public float loadProgress() {
    return setupDone ? 1 : 0;
  }
  
  
  public void updateScenario() {
    area.update(PlayLoop.UPDATES_PER_SECOND);
  }
  
  
  public Base  base () { return base ; }
  public Area  stage() { return area ; }
  public World world() { return world; }
  
  
  public void renderVisuals(Rendering rendering) {
    area.renderStage(rendering, base);
  }
  
  
  public PlayUI playUI() {
    return UI;
  }
}





