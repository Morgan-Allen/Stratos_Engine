

package game;
import util.*;



public class BaseEvents {
  
  
  final static int
    EVENTS_INTERVAL = 60 * 6
  ;
  
  
  public static interface Trouble {
  }
  
  
  Area area;
  int lastEventTime;
  
  
  
  void updateEvents() {
    
    if (area.time() - lastEventTime > EVENTS_INTERVAL) {
      lastEventTime = area.time();
    }
    else {
      return;
    }
    
    //Pick <Trouble> pickTrouble = new Pick();
    Tally <Trouble> troubleSources = new Tally();
    
    //  TODO:  Get a rating of the 'force magnitude' associated with each
    //  trouble-source, and a unique key (base, faction, nest-complex, etc.)
    
    //  Compare that against your own base's force-magnitude, and then see
    //  which trouble-source actually generates an event.
    
    //  Then delegate the actual mission-generation to the source of the 
    //  trouble...
    
    
    for (Building b : area.buildings()) if (b instanceof Trouble) {
    }
    
    for (Base b : area.world.bases()) if (b instanceof Trouble) {
    }
    
  }
  
}




