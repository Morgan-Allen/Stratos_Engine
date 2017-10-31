

package game;
import static game.GameConstants.*;
import util.*;




public class World implements Session.Saveable {
  
  
  /**  Public interfaces-
    */
  static class Journey {
    City from;
    City goes;
    int startTime;
    int arriveTime;
    Batch <Journeys> going = new Batch();
  }
  
  static class Event {
    String label;
    int time;
    Object[] involved;
  }
  
  
  /**  Data fields, setup and save/load methods-
    */
  int time = 0;
  List <City> cities = new List();
  List <Journey> journeys = new List();
  List <Event> history = new List();
  
  //  Only used for graphical reference...
  int mapWide = 10, mapHigh = 10;
  
  
  World() {
    return;
  }
  
  
  public World(Session s) throws Exception {
    s.cacheInstance(this);
    
    time = s.loadInt();
    s.loadObjects(cities);
    
    for (int n = s.loadInt(); n-- > 0;) {
      Journey j = new Journey();
      j.from       = (City) s.loadObject();
      j.goes       = (City) s.loadObject();
      j.startTime  = s.loadInt();
      j.arriveTime = s.loadInt();
      s.loadObjects(j.going);
      journeys.add(j);
    }

    for (int n = s.loadInt(); n-- > 0;) {
      Event e = new Event();
      e.time     = s.loadInt();
      e.label    = s.loadString();
      e.involved = s.loadObjectArray(Object.class);
      history.add(e);
    }
    
    mapWide = s.loadInt();
    mapHigh = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveInt(time);
    s.saveObjects(cities);
    
    s.saveInt(journeys.size());
    for (Journey j : journeys) {
      s.saveObject(j.from);
      s.saveObject(j.goes);
      s.saveInt(j.startTime );
      s.saveInt(j.arriveTime);
      s.saveObjects(j.going);
    }
    
    s.saveInt(history.size());
    for (Event e : history) {
      s.saveString(e.label);
      s.saveInt(e.time);
      s.saveObjectArray(e.involved);
    }
    
    s.saveInt(mapWide);
    s.saveInt(mapHigh);
  }
  
  
  
  /**  Managing cities:
    */
  void addCity(City c) {
    this.cities.add(c);
  }
  
  
  City cityNamed(String n) {
    for (City c : cities) if (c.name.equals(n)) return c;
    return null;
  }
  
  
  CityMap activeCityMap() {
    for (City c : cities) if (c.map != null) return c.map;
    return null;
  }
  
  
  
  /**  Registering and updating journeys:
    */
  Journey beginJourney(City from, City goes, Journeys... going) {
    if (from == null || goes == null) return null;
    
    Integer distance = from.distances.get(goes);
    if (distance == null) {
      float dx = from.mapX - goes.mapX;
      float dy = from.mapY - goes.mapY;
      distance = (int) Nums.sqrt((dx * dx) + (dy * dy));
    }
    
    Journey j = new Journey();
    j.from       = from;
    j.goes       = goes;
    j.startTime  = time;
    j.arriveTime = j.startTime + (distance * TRADE_DIST_TIME);
    for (Journeys g : going) j.going.add(g);
    journeys.add(j);
    
    if (reports(j)) {
      I.say("\nBeginning journey: "+j.from+" to "+j.goes);
      I.say("  Embarked: "+j.going);
      I.say("  Time: "+time+", arrival: "+j.arriveTime);
    }
    
    return j;
  }

  
  Journey beginJourney(City from, City goes, Series <Journeys> going) {
    return beginJourney(from, goes, going.toArray(Journeys.class));
  }
  
  
  boolean completeJourney(Journey j) {
    journeys.remove(j);
    for (Journeys g : j.going) {
      g.onArrival(j.goes, j);
    }
    return true;
  }
  
  
  boolean isComplete(Journey j) {
    return time >= j.arriveTime;
  }
  
  
  Journey journeyFor(Journeys on) {
    for (Journey j : journeys) {
      if (j.going.includes(on)) return j;
    }
    return null;
  }
  
  
  
  /**  Recording events:
    */
  void recordEvent(String label, Object... involved) {
    Event e = new Event();
    e.label    = label;
    e.time     = time;
    e.involved = involved;
    history.add(e);
  }
  
  
  void clearHistory() {
    history.clear();
  }
  
  
  Batch <Event> eventsWithLabel(String label) {
    Batch <Event> matches = new Batch();
    for (Event e : history) if (e.label.equals(label)) matches.add(e);
    return matches;
  }
  
  
  
  /**  Regular updates-
    */
  void updateWithTime(int time) {
    this.time = time;
    
    for (City city : cities) {
      city.updateCity();
    }
    
    for (Journey j : journeys) {
      if (time >= j.arriveTime) {
        if (reports(j)) {
          I.say("\nCompleted journey: "+j.from+" to "+j.goes);
          I.say("  Embarked: "+j.going);
          I.say("  Time: "+time+", arrival: "+j.arriveTime);
        }
        completeJourney(j);
      }
    }
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  boolean reports(Journey j) {
    for (Journeys k : j.going) {
      if (k == I.talkAbout) return true;
    }
    return false;
  }
  
  
  Vec2D journeyPos(Journey j) {
    float timeGone = time - j.startTime;
    float a = timeGone / (j.arriveTime - j.startTime), i = 1 - a;
    
    float initX = j.from.mapX, initY = j.from.mapY;
    float destX = j.goes.mapX, destY = j.goes.mapY;
    
    Vec2D c = new Vec2D();
    c.x = (destX * a) + (initX * i);
    c.y = (destY * a) + (initY * i);
    return c;
  }
  
  
  String descFor(Event e) {
    return e.label+" at time "+e.time+": "+I.list(e.involved);
  }
  
  
  City onMap(int mapX, int mapY) {
    for (City city : cities) {
      int x = (int) city.mapX, y = (int) city.mapY;
      if (x == mapX && y == mapY) return city;
    }
    return null;
  }
  
}









