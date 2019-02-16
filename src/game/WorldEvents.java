

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
      BaseCouncilUtils.generateTrouble(federation, activeMap);
    }
    
    /*
    if (area.time() - lastEventTime > EVENTS_INTERVAL) {
      lastEventTime = area.time();
    }
    else {
      return;
    }
    
    List  <Trouble> trouble = new List ();
    Pick  <Trouble> pick    = new Pick ();
    Tally <Faction> powers  = new Tally();
    
    for (Building b : area.buildings()) if (b instanceof Trouble) {
      trouble.add((Trouble) b);
    }
    for (Base b : area.world.bases()) if (b instanceof Trouble) {
      trouble.add((Trouble) b);
    }
    for (Trouble t : trouble) {
      Faction faction = t.faction();
      float power = t.troublePower();
      powers.add(power, faction);
      pick.compare(t, power * (0.5f + Rand.num()));
    }
    
    //  The most important thing here is to space out events so that the player
    //  isn't under continual siege.  The actual size of the raids is less
    //  important.  (In fact, it might make sense to launch massed raids from
    //  several sources on a particular target at once.)
    
    if (! pick.empty()) {
      Trouble source = pick.result();
      Faction faction = source.faction();
      float power = powers.valueFor(faction);
      source.generateTrouble(area, power);
    }
    //*/
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









