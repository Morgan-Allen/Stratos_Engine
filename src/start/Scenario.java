


package start;
import game.*;
import gameUI.play.*;
import graphics.common.*;
import util.*;
import java.lang.reflect.Method;



public abstract class Scenario implements Session.Saveable {
  
  
  MainGame game;
  boolean setupDone = false;
  
  World world = null;
  Area  area  = null;
  Base  base  = null;
  
  private PlayUI UI;
  
  
  protected Scenario() {
    return;
  }
  
  
  public Scenario(Session s) throws Exception {
    s.cacheInstance(this);
    world = (World) s.loadObject();
    area  = (Area ) s.loadObject();
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
    
    world = null;
    area  = null;
    base  = null;
    UI    = null;
    world = createWorld();
    area  = createArea(world);
    base  = createBase(area, world);
    configScenario(world, area, base);
    
    setupDone = true;
  }
  
  
  protected void afterLoading(MainGame game) {
    this.game = game;
    setupDone = true;
  }
  
  
  protected abstract World createWorld();
  protected abstract Area createArea(World world);
  protected abstract Base createBase(Area map, World world);
  protected abstract void configScenario(World world, Area map, Base base);
  
  
  public float loadProgress() {
    return setupDone ? 1 : 0;
  }
  
  
  
  /**  Regular update and access methods-
    */
  public void updateScenario() {
    area.update(PlayLoop.UPDATES_PER_SECOND);
  }
  
  
  public Base  base () { return base ; }
  public Area  stage() { return area ; }
  public World world() { return world; }
  
  
  
  /**  Defining and evaluating objectives-
    */
  final public static int
    COMPLETE_NONE    = -1,
    COMPLETE_FAILED  =  0,
    COMPLETE_SUCCESS =  1
  ;

  public static class Objective extends Constant {
    
    String description;
    
    Class baseClass;
    Method checkMethod;
    
    
    public Objective(
      Class baseClass, String ID, String description, String checkMethod
    ) {
      super(null, ID, IS_STORY);
      
      this.baseClass = baseClass;
      try { this.checkMethod = baseClass.getDeclaredMethod(checkMethod); }
      catch (Exception e) { this.checkMethod = null; }
      
      this.description = description;
    }
    
    public int checkCompletion(Scenario scenario) {
      if (checkMethod == null) return COMPLETE_NONE;
      try { return (Integer) checkMethod.invoke(scenario); }
      catch (Exception e) { return COMPLETE_NONE; }
    }
    
    public String description() {
      return description;
    }
  }
  
  
  public Series <Objective> objectives() {
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





