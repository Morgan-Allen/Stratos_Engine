/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package game;
import static game.GameConstants.*;
import util.*;



public class TaskWaiting extends Task {
  
  
  /**  Data fields, setup and save/load functions-
    */
  private static boolean
    evalVerbose  = false,
    stepsVerbose = false;
  
  public static int
    TYPE_VIP_STAY  = 1,
    TYPE_OVERSIGHT = 2,
    TYPE_DOMESTIC  = 3,
    TYPE_INVENTORY = 4
  ;
  
  
  final Pathing venue;
  final int type;
  
  private Session.Saveable worksOn = null;
  private float beginTime = -1;
  
  
  private TaskWaiting(Actor actor, Pathing supervised, int stayType) {
    super(actor);
    this.venue = supervised;
    this.type  = stayType  ;
  }
  
  
  public TaskWaiting(Session s) throws Exception {
    super(s);
    this.venue     = (Pathing) s.loadObject();
    this.type      = s.loadInt();
    this.beginTime = s.loadFloat();
    this.worksOn   = s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(venue    );
    s.saveInt   (type     );
    s.saveFloat (beginTime);
    s.saveObject(worksOn  );
  }
  
  
  
  /**  Factory methods-
    */
  public static TaskWaiting configWaiting(Actor actor, Building venue) {
    return configWaiting(actor, venue, TYPE_OVERSIGHT, venue);
  }
  
  
  public static TaskWaiting configWaiting(
    Actor actor, Pathing venue, int stayType, Employer e
  ) {
    TaskWaiting task = new TaskWaiting(actor, venue, stayType);
    return (TaskWaiting) task.configTask(e, venue, null, JOB.WAITING, 10);
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected float successPriority() {
    Actor actor = (Actor) active;
    float diligence = (actor.traits.levelOf(TRAIT_DILIGENCE) + 1) / 2;
    
    if (type == TYPE_DOMESTIC || type == TYPE_INVENTORY) {
      return Task.ROUTINE * (0.5f + diligence);
    }
    else {
      return Task.CASUAL * (0.5f + diligence);
    }
  }
  
  
  protected void onVisit(Pathing visits) {
    Actor actor = (Actor) active;
    
    final boolean report = I.talkAbout == actor && stepsVerbose;
    if (report) {
      I.say("\nGetting next supervision step: "+actor);
    }
    
    //  TODO:  Fill these in later...
    
    if (type == TYPE_VIP_STAY) {
      return;
    }
    
    if (type == TYPE_OVERSIGHT) {
      return;
    }
    
    if (type == TYPE_DOMESTIC) {
      return;
    }
    
    if (type == TYPE_INVENTORY) {
      return;
    }
  }
  
  
  public String toString() {
    return "Supervising "+venue;
  }
}


