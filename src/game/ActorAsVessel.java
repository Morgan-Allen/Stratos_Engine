

package game;
import static game.GameConstants.*;
import util.*;



//  TODO:  Adapt this to the requirements of flight offworld or to distant
//  sectors of the same world...


public class ActorAsVessel extends Actor {
  
  
  
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
  
}