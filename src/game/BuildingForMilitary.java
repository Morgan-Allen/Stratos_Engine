

package game;
import util.*;



public class BuildingForMilitary extends BuildingForDelivery {
  
  
  /**  Data fields, setup and save/load methods-
    */
  List <Walker> recruits = new List();
  Formation formation = null;
  
  
  public BuildingForMilitary(ObjectType type) {
    super(type);
  }
  
  
  public BuildingForMilitary(Session s) throws Exception {
    super(s);
    s.loadObjects(recruits);
    formation = (Formation) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(recruits);
    s.saveObject(formation);
  }
  
  
  
  /**  World entry and exit operations-
    */
  void enterMap(CityMap map, int x, int y) {
    super.enterMap(map, x, y);
    
    formation = new Formation();
    formation.garrison = this;
    formation.map      = map ;
  }
  
  
  
  /**  Regular updates and active service-
    */
  boolean eligible(Walker walker) {
    if (resident.includes(walker)) return false;
    return walker.formation == null;
  }
  
  
  protected Walker addWalker(ObjectType type) {
    Walker w = super.addWalker(type);
    recruits.include(w);
    return w;
  }
  
  
  public void selectWalkerBehaviour(Walker walker) {
    Pick <Walker> pick = new Pick();
    
    if (recruits.size() < type.maxRecruits) {
      for (Building b : map.buildings) {
        for (Walker w : b.resident) {
          if (eligible(w)) {
            float dist = CityMap.distance(b.entrance, entrance);
            float rating = 10 / (10 + dist);
            pick.compare(w, rating);
          }
        }
      }
    }
    
    Walker drafts = pick.result();
    if (drafts != null) {
      //I.say("\nWILL DRAFT: "+drafts);
      walker.embarkOnVisit(drafts.home, 2, Walker.JOB.VISITING, this);
    }
    else {
      formation.selectWalkerBehaviour(walker);
    }
    if (walker.job == null) {
      super.selectWalkerBehaviour(walker);
    }
  }
  
  
  public void walkerVisits(Walker walker, Building other) {
    for (Walker w : other.resident) if (eligible(w)) {
      //I.say("\nDRAFTING: "+w);
      w.formation = this.formation;
      recruits.include(w);
    }
  }
  
  
}





