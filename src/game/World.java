

package game;
import util.*;




public class World implements Session.Saveable {
  
  
  List <City> cities = new List();
  
  
  World() {
    return;
  }
  
  
  public World(Session s) throws Exception {
    s.cacheInstance(this);
    
    s.loadObjects(cities);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObjects(cities);
  }
  
  
  
  void updateFrom(CityMap map) {
    for (City city : cities) {
      city.updateFrom(map);
    }
  }
  
}















