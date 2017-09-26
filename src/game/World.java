

package game;
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
  
  static interface Journeys {
    void onArrival(City goes, Journey journey);
  }
  
  
  
  /**  Data fields, setup and save/load methods-
    */
  int time = 0;
  List <City> cities = new List();
  List <Journey> journeys = new List();
  
  
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
  }
  
  
  
  /**  Registering and updating journeys:
    */
  void beginJourney(City from, City goes, Journeys... going) {
    if (from == null || goes == null) return;
    
    Integer distance = from.distances.get(goes);
    if (distance == null) return;
    
    Journey j = new Journey();
    j.from       = from;
    j.goes       = goes;
    j.startTime  = time;
    j.arriveTime = j.startTime + (distance * Walker.TRADE_DIST_TIME);
    for (Journeys g : going) j.going.add(g);
    journeys.add(j);
  }
  
  
  
  /**  Regular updates-
    */
  void updateFrom(CityMap map) {
    
    this.time = map.time;
    
    for (City city : cities) {
      city.updateFrom(map);
    }
    
    for (Journey j : journeys) {
      I.say("\nUpdating journey: "+j.from+" to "+j.goes);
      I.say("  Embarked: "+j.going);
      I.say("  Time: "+map.time+", arrives: "+j.arriveTime);
      
      if (map.time >= j.arriveTime) {
        journeys.remove(j);
        for (Journeys g : j.going) {
          g.onArrival(j.goes, j);
        }
      }
    }
  }
  
}






