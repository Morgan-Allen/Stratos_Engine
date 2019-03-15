

package game;
import util.*;



//  TODO:  Move the event-history in here!

public class WorldEvents {
  
  
  final static int
    EVENTS_INTERVAL = 60 * 6
  ;
  
  public static class Event {
    
    String label;
    int time;
    Object[] involved;
  }
  
  public static interface Trouble {
    Faction faction();
    float troublePower();
    void generateTrouble(Area activeMap, float factionPower);
  }
  
  
  final World world;
  int lastEventTime;
  List <Event> history = new List();
  
  
  WorldEvents(World world) {
    this.world = world;
  }
  
  void loadState(Session s) throws Exception {
    this.lastEventTime = s.loadInt();

    for (int n = s.loadInt(); n-- > 0;) {
      Event e = new Event();
      e.time     = s.loadInt();
      e.label    = s.loadString();
      e.involved = s.loadObjectArray(Object.class);
      history.add(e);
    }
  }
  
  void saveState(Session s) throws Exception {
    s.saveInt(lastEventTime);
    
    s.saveInt(history.size());
    for (Event e : history) {
      s.saveString(e.label);
      s.saveInt(e.time);
      s.saveObjectArray(e.involved);
    }
  }
  
  
  
  void updateEvents(int time) {
    
    if (time - lastEventTime > EVENTS_INTERVAL) {
      lastEventTime = time;
    }
    else {
      return;
    }
    
    Area activeMap = world.activeBaseMap();
    
    for (Federation federation : world.federations()) {
      MissionAIUtils.updateCapital(federation, world);
      MissionAIUtils.considerMissionRecalls(federation, world);
    }
    
    for (Federation federation : world.federations()) {
      MissionAIUtils.generateLocalTrouble(federation, activeMap, true);
      MissionAIUtils.generateOffmapTrouble(federation, world, true);
    }
  }
  
  
  
  /**  Recording events:
    */
  public void recordEvent(String label, Object... involved) {
    Event e = new Event();
    e.label    = label;
    e.time     = world.time();
    e.involved = involved;
    history.add(e);
  }
  
  
  public void clearHistory() {
    history.clear();
  }
  
  
  public Batch <Event> eventsWithLabel(String label) {
    Batch <Event> matches = new Batch();
    for (Event e : history) if (e.label.equals(label)) matches.add(e);
    return matches;
  }
  
  
  public Series <Event> history() {
    return history;
  }
  
  
  public String descFor(Event e) {
    return e.label+" at time "+e.time+": "+I.list(e.involved);
  }
  
}









