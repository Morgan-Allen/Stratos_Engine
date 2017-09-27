

package game;
import static game.CityMap.*;
import util.*;



//  NOTE:  The common barracks structure would actually train the entire
//  male adult populace.  Standing professional soldiers would be a minority.



public class BuildingForMilitary extends BuildingForDelivery {
  
  
  /**  Data fields, setup and save/load methods-
    */
  List <Walker> enlisted = new List();
  Formation formation = null;
  
  
  public BuildingForMilitary(ObjectType type) {
    super(type);
  }
  
  
  public BuildingForMilitary(Session s) throws Exception {
    super(s);
    s.loadObjects(enlisted);
    formation = (Formation) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(enlisted);
    s.saveObject(formation);
  }
  
  
  
  /**  World entry and exit operations-
    */
  void enterMap(CityMap map, int x, int y) {
    super.enterMap(map, x, y);
    
    formation = new Formation();
    formation.garrison = this    ;
    formation.map      = map     ;
    formation.belongs  = map.city;
  }
  
  
  /**  Regular updates and active service-
    */
  
  
}








