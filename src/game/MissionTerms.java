

package game;
import static game.GameConstants.*;
import static game.Base.*;
import util.*;



public class MissionTerms {
  
  
  final Mission mission;
  
  Base.POSTURE postureDemand  = null;
  Mission      actionDemand   = null;
  Actor        marriageDemand = null;
  Tally <Good> tributeDemand  = new Tally();
  
  int timeTermsSent = -1;
  boolean accepted = false;
  boolean rejected = false;
  
  
  MissionTerms(Mission mission) {
    this.mission = mission;
  }
  
  
  void loadState(Session s) throws Exception {
    postureDemand  = (Base.POSTURE) s.loadEnum(Base.POSTURE.values());
    actionDemand   = (Mission     ) s.loadObject();
    marriageDemand = (Actor       ) s.loadObject();
    s.loadTally(tributeDemand);
    
    timeTermsSent = s.loadInt();
    accepted = s.loadBool();
    rejected = s.loadBool();
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveEnum  (postureDemand );
    s.saveObject(actionDemand  );
    s.saveObject(marriageDemand);
    s.saveTally (tributeDemand );
    
    s.saveInt(timeTermsSent);
    s.saveBool(accepted);
    s.saveBool(rejected);
  }
  

  
  public void assignTerms(
    Base.POSTURE posture,
    Mission actionTaken,
    Actor toMarry,
    Tally <Good> tribute
  ) {
    this.postureDemand  = posture;
    this.actionDemand   = actionTaken;
    this.marriageDemand = toMarry;
    this.tributeDemand  = tribute == null ? new Tally() : tribute;
  }
  
  
  public POSTURE      postureDemand () { return postureDemand ; }
  public Mission      actionDemand  () { return actionDemand  ; }
  public Actor        marriageDemand() { return marriageDemand; }
  public Tally <Good> tributeDemand () { return tributeDemand ; }
  
  
  public boolean hasTerms() {
    boolean haveTerms = false;
    haveTerms |= marriageDemand != null;
    haveTerms |= actionDemand   != null;
    haveTerms |= postureDemand  != null;
    haveTerms |= tributeDemand  != null && ! tributeDemand.empty();
    return haveTerms;
  }
  

  void sendTerms(Base goes) {
    goes.council.receiveTerms(mission);
    timeTermsSent = goes.world.time;
  }
  
  
  public boolean sent() {
    return timeTermsSent != -1;
  }
  
  
  public boolean expired() {
    if (! sent()) return false;
    int time = mission.homeBase.world.time();
    if ((time - timeTermsSent     ) >= DAY_LENGTH    ) return true;
    if ((time - mission.arriveTime) >= DAY_LENGTH * 2) return true;
    return false;
  }
  
  
  public void setAccepted(boolean accepted) {
    if (accepted) {
      
      Base focus = mission.worldFocus();
      if (focus == null) focus = ((Element) mission.localFocus()).base();
      this.accepted = true;
      
      BaseEvents.imposeTerms(focus, mission.homeBase(), mission);
      mission.setMissionComplete(true);
    }
    else {
      this.rejected = true;
    }
  }
  
  
  public boolean rejected() {
    return rejected;
  }
  
  
  public boolean accepted() {
    return accepted;
  }
  
  
  public boolean answered() {
    return accepted || rejected;
  }
}








