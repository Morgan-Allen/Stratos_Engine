

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
  boolean checkEntranceOkay(AreaTile e, int index) {
    if (super.checkEntranceOkay(e, index)) return true;
    if (tower && index > 0 && e.pathType() == Type.PATH_WALLS) return true;
    return false;
  }
  
  
  AreaTile[] selectEntrances() {
    int facing = facing();
    
    //  TODO:  Scrub any null entrances from these lists!
    
    if (tower) {
      AreaTile stair = tileAt(1, -1, facing);
      AreaTile left  = tileAt(-1, 0, facing);
      AreaTile right = tileAt(type().wide, 0, facing);
      return new AreaTile[] { stair, left, right };
    }
    if (gate) {
      AreaTile front = tileAt(1, -1, facing);
      AreaTile back  = tileAt(1, type().high, facing);
      return new AreaTile[] { front, back };
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
      assignTask(null, this);
    }
  }
  
  
  void updateReactions() {
    if (! map().world.settings.toggleReacts) return;
    if (! turret) return;
    
    if (! Task.inCombat(this)) {
      Series <Active> others = map.activeInRange(at(), sightRange());
      TaskCombat c = TaskCombat.nextReaction(this, this, this, false, others);
      if (c != null) assignTask(c, this);
    }
  }
  
  
  
  /**  Handling the Active contract-
    */
  public boolean mobile() {
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
  
  
  public void assignTask(Task task, Object source) {
    if (this.task != null) this.task.toggleFocus(false);
    this.task = task;
    if (this.task != null) this.task.toggleFocus(true);
  }
  
  
  public void performAttack(Element other, boolean melee) {
    TaskCombat.performAttack(this, other, melee);
  }
  
  
}







