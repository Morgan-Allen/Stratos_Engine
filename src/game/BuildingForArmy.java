

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class BuildingForArmy extends BuildingForCrafts {
  
  
  /**  Data fields, setup and save/load methods-
    */
  List <Actor> recruits = new List();
  
  
  public BuildingForArmy(Type type) {
    super(type);
  }
  
  
  public BuildingForArmy(Session s) throws Exception {
    super(s);
    s.loadObjects(recruits);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjects(recruits);
  }
  
  
  
  /**  Regular updates and worker assignments-
    */
  void updateOnPeriod(int period) {
    super.updateOnPeriod(period);
    
    for (Actor a : recruits) if (! eligible(a, true)) {
      recruits.remove(a);
    }
  }
  
  
  public void selectActorBehaviour(Actor actor) {
    Pick <Building> pick = new Pick(0);
    
    //  TODO:  On larger maps you will need a more efficient protocol
    //  here-
    if (recruits.size() < type().maxRecruits) {
      for (Building b : map.buildings) {
        float rating = CityMap.distancePenalty(this, b);
        if (b == this) rating *= 10;
        float sumE = 0;
        for (Actor a : b.workers) if (eligible(a, false)) {
          sumE += 1;
        }
        for (Actor a : b.residents) if (eligible(a, false)) {
          sumE += 1;
        }
        pick.compare(b, rating * sumE);
      }
    }
    
    Building goes = pick.result();
    if (goes != null) {
      actor.embarkOnVisit(goes, 2, Task.JOB.VISITING, this);
    }
    if (actor.idle()) {
      super.selectActorBehaviour(actor);
    }
  }
  
  
  public void actorVisits(Actor actor, Building other) {
    for (Actor o : other.workers) {
      if (eligible(o, false)) toggleRecruit(o, true);
    }
    for (Actor o : other.residents) {
      if (eligible(o, false)) toggleRecruit(o, true);
    }
  }
  
  
  
  /**  Handling recruitment and deployment-
    */
  public boolean eligible(Actor actor, boolean isMember) {
    if (actor.woman() || actor.dead()          ) return false;
    if (actor.type().socialClass == CLASS_NOBLE) return false;
    if (actor.recruiter != null && ! isMember  ) return false;
    return true;
  }
  
  
  public void toggleRecruit(Actor actor, boolean is) {
    if (is) {
      recruits.include(actor);
      actor.recruiter = this;
    }
    else {
      recruits.remove(actor);
      if (actor.recruiter == this) actor.recruiter = null;
    }
  }
  
  
  public Series <Actor> recruits() {
    return recruits;
  }
  
  
  public void deployInFormation(Formation f, boolean is) {
    for (Actor a : recruits) if (eligible(a, true)) {
      f.toggleRecruit(a, is);
    }
  }
  
}


