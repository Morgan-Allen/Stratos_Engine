

package game;
import util.*;



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
  boolean eligible(Walker walker) {
    return walker.formation == null;
  }
  
  
  public void selectWalkerBehaviour(Walker walker) {
    Pick <Walker> pick = new Pick();
    
    for (Building b : map.buildings) {
      for (Walker w : b.walkers) {
        if (eligible(w) && w.inside == b) {
          float dist = CityMap.distance(b.entrance, entrance);
          float rating = 10 / (10 + dist);
          pick.compare(w, rating);
        }
      }
    }
    
    Walker drafts = pick.result();
    if (drafts != null) {
      walker.embarkOnVisit(drafts.inside, 2, Walker.JOB_VISITING, this);
    }
    else {
      super.selectWalkerBehaviour(walker);
    }
  }
  
  
  public void walkerExits(Walker walker, Building enters) {
    for (Walker w : enters.visitors) if (eligible(w)) {
      w.formation = this.formation;
    }
  }
  
  
}





