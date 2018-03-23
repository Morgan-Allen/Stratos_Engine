

package game;
import static game.GameConstants.*;
import util.*;



//  TODO:  Adapt this to the requirements of flight offworld or to distant
//  sectors of the same world...


public class ActorAsVessel extends Actor implements Trader, Employer, Pathing {
  
  
  
  Actor pilot = null;
  List <Actor> passengers = new List();
  
  
  public ActorAsVessel(ActorType type) {
    super(type);
  }
  
  
  public ActorAsVessel(Session s) throws Exception {
    super(s);
    pilot = (Actor) s.loadObject();
    s.loadObjects(passengers);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(pilot);
    s.saveObjects(passengers);
  }
  
  
  
  /**  Regular updates and behaviour methods-
    */
  void beginNextBehaviour() {
    assignTask(null);
    
    if (idle() && work() != null && work().complete()) {
      assignTask(work().selectActorBehaviour(this));
    }
    if (idle()) {
      assignTask(TaskResting.configResting(this, home()));
    }
  }
  


  /**  Implementing Employer interface-
    */
  public Task selectActorBehaviour(Actor actor) {
    return null;
  }
  
  
  public void actorUpdates(Actor actor) {
    return;
  }
  
  
  public void actorPasses(Actor actor, Building other) {
    return;
  }
  
  
  public void actorTargets(Actor actor, Target other) {
    return;
  }
  
  
  public void actorVisits(Actor actor, Building visits) {
    return;
  }
  
  
  
  /**  Implementing Pathing interface-
    */
  public Pathing[] adjacent(Pathing[] temp, Area map) {
    // TODO Auto-generated method stub
    return null;
  }
  
  
  public boolean allowsEntry(Actor a) {
    // TODO Auto-generated method stub
    return false;
  }
  
  
  public void setInside(Actor a, boolean is) {
    // TODO Auto-generated method stub
    
  }
  
  
  public Series<Actor> allInside() {
    // TODO Auto-generated method stub
    return null;
  }
  
  

  /**  Implementing Trader interface-
    */
  //  TODO:  Sew these up properly.
  
  public Tally <Good> needLevels() {
    return null;
  }
  
  public Tally <Good> prodLevels() {
    return null;
  }
  
  public float importPrice(Good g, Base sells) {
    return 0;
  }
  
  public float exportPrice(Good g, Base buys) {
    return 0;
  }
  
  public boolean allowExport(Good g, Trader buys) {
    return true;
  }
}







