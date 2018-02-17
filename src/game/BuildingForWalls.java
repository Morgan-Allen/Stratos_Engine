

package game;
import static game.AreaMap.*;
import static game.GameConstants.*;
import util.*;



public class BuildingForWalls extends Building implements Active {
  
  
  /**  Data fields, construction and save/load methods-
    */
  boolean tower, gate, turret;
  Task task;
  
  
  public BuildingForWalls(BuildType type) {
    super(type);
    tower  = type.hasFeature(IS_TOWER );
    gate   = type.hasFeature(IS_GATE  );
    turret = type.hasFeature(IS_TURRET);
  }
  
  
  public BuildingForWalls(Session s) throws Exception {
    super(s);
    tower  = s.loadBool();
    gate   = s.loadBool();
    turret = s.loadBool();
    task   = (Task) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveBool(tower );
    s.saveBool(gate  );
    s.saveBool(turret);
    s.saveObject(task);
  }
  
  
  
  /**  Entering and exiting the world-
    */
  public void enterMap(AreaMap map, int x, int y, float buildLevel, Base owns) {
    super.enterMap(map, x, y, buildLevel, owns);
    map.flagActive(this, centre(), true);
  }
  
  
  public void exitMap(AreaMap map) {
    map.flagActive(this, centre(), false);
    super.exitMap(map);
  }
  
  
  
  /**  Selecting and filtering entrances and visitors-
    */
  boolean checkEntranceOkay(Tile e, int index) {
    if (super.checkEntranceOkay(e, index)) return true;
    if (tower && index > 0 && e.pathType() == PATH_WALLS) return true;
    return false;
  }
  
  
  Tile[] selectEntrances() {
    int facing = facing();
    
    //  TODO:  Scrub any null entrances from these lists!
    
    if (tower) {
      Tile stair = tileAt(1, -1, facing);
      Tile left  = tileAt(-1, 0, facing);
      Tile right = tileAt(type().wide, 0, facing);
      return new Tile[] { stair, left, right };
    }
    if (gate) {
      Tile front = tileAt(1, -1, facing);
      Tile back  = tileAt(1, type().high, facing);
      return new Tile[] { front, back };
    }
    return super.selectEntrances();
  }
  
  
  public boolean allowsEntry(Actor a) {
    Base owner = base();
    if (a.base() != owner && a.guestBase() != owner) {
      return false;
    }
    return super.allowsEntry(a);
  }
  
  
  
  /**  Providing direct attack functions-
    */
  void update() {
    super.update();
    
    updateReactions();
    
    if (task != null && ! task.checkAndUpdateTask()) {
      assignTask(null);
    }
  }
  
  
  void updateReactions() {
    if (! map().world.settings.toggleReacts) return;
    if (! turret) return;
    
    if (! Task.inCombat(this)) {
      TaskCombat c = TaskCombat.nextReaction(this);
      if (c != null) assignTask(c);
    }
  }
  
  
  
  /**  Handling the Active contract-
    */
  public boolean isActor() {
    return false;
  }
  
  
  public Task.JOB jobType() {
    if (task == null) return Task.JOB.NONE;
    return task.type;
  }
  
  
  public Task task() {
    return task;
  }
  
  
  public Mission mission() {
    return null;
  }
  
  
  public void assignTask(Task task) {
    if (this.task != null) this.task.toggleFocus(false);
    this.task = task;
    if (this.task != null) this.task.toggleFocus(true);
  }
  
  
  public void performAttack(Element other, boolean melee) {
    TaskCombat.performAttack(this, other, melee);
  }
  
  
}







