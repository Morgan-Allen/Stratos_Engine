

package game;
import static game.GameConstants.*;



public class TaskDelivery extends Task {
  
  
  Building from;
  Building goes;
  Good carried;
  float amount;
  
  
  public TaskDelivery(Walker actor) {
    super(actor);
  }
  
  
  public TaskDelivery(Session s) throws Exception {
    super(s);
    from    = (Building) s.loadObject();
    goes    = (Building) s.loadObject();
    carried = (Good) s.loadObject();
    amount  = s.loadFloat();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(from);
    s.saveObject(goes);
    s.saveObject(carried);
    s.saveFloat(amount);
  }
  
  
  
  TaskDelivery configDelivery(
    Building from, Building goes, Task.JOB jobType,
    Good carried, float amount, Employer e
  ) {
    this.from    = from;
    this.goes    = goes;
    this.carried = carried;
    this.amount  = amount;
    return (TaskDelivery) configTask(e, from, null, jobType, 0);
  }
  
  
  protected void onVisit(Building visits) {
    if (visits == from) {
      actor.pickupGood(carried, amount, from);
      this.configTask(origin, goes, null, type, 0);
    }
    if (visits == goes) {
      actor.offloadGood(carried, goes);
    }
  }
  
}



