

package game;
import util.*;



public class BuildingForMilitary extends BuildingForCrafts {
  
  
  /**  Data fields, setup and save/load methods-
    */
  Formation formation = null;
  
  
  public BuildingForMilitary(ObjectType type) {
    super(type);
  }
  
  
  public BuildingForMilitary(Session s) throws Exception {
    super(s);
    formation = (Formation) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(formation);
  }
  
  
  
  /**  World entry and exit operations-
    */
  void enterMap(CityMap map, int x, int y, float buildLevel) {
    super.enterMap(map, x, y, buildLevel);
  }
  
  
  void exitMap(CityMap map) {
    if (formation != null) formation.disband();
    super.exitMap(map);
  }
  
  
  void updateOnPeriod(int period) {
    if (formation == null && map.city != null) {
      this.formation = new Formation();
      formation.setupFormation(this.type, map.city);
    }
    super.updateOnPeriod(period);
  }
  
  
  void assignFormation(Formation f) {
    this.formation = f;
  }
  
  
  Formation formation() {
    return this.formation;
  }
  
  
  
  /**  Regular updates and active service-
    */
  boolean eligible(Walker walker) {
    if (workers.includes(walker)) return false;
    return walker.formation == null;
  }
  
  
  public void setWorker(Walker w, boolean is) {
    super.setWorker(w, is);
    if (is) formation.toggleRecruit(w, true);
  }
  
  
  public void selectWalkerBehaviour(Walker walker) {
    if (formation == null) return;
    Pick <Walker> pick = new Pick();
    
    if (formation.recruits.size() < type.maxRecruits) {
      for (Building b : map.buildings) {
        for (Walker w : b.residents) {
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
    for (Walker w : other.residents) if (eligible(w)) {
      formation.toggleRecruit(w, true);
    }
  }
  
  
}





