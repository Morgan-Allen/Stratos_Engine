

package game;
import static game.GameConstants.*;
import util.*;



//  TODO:  Adapt this to the requirements of flight offworld or to distant
//  sectors of the same world...


public class ActorAsVessel extends Actor implements Trader, Employer {
  
  
  
  List <Actor> passengers = new List();
  
  
  public ActorAsVessel(ActorType type) {
    super(type);
  }
  
  
  public ActorAsVessel(Session s) throws Exception {
    super(s);
    s.loadObjects(passengers);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(passengers);
  }
  
  

  void beginNextBehaviour() {
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
  
  
  
  /**  Implementing Trader interface-
    */
  //  TODO:  Sew these up properly.
  
  public Tally <Good> needLevels() {
    return null;
  }
  
  
  public Tally <Good> prodLevels() {
    return null;
  }
  
  
  public Tally<Good> inventory() {
    return null;
  }
  
  
  public float importPrice(Good g, Base sells) {
    return 0;
  }
  
  
  public float exportPrice(Good g, Base buys) {
    return 0;
  }
  
  
  
}







