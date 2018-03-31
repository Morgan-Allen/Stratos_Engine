

package game;
import static game.GameConstants.*;
import util.*;



public class MissionRewards {
  
  
  final Mission mission;
  boolean isBounty = false;
  int cashReward = -1;
  
  
  MissionRewards(Mission mission) {
    this.mission = mission;
  }
  
  
  void loadState(Session s) throws Exception {
    isBounty = s.loadBool();
    cashReward = s.loadInt();
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveBool(isBounty);
    s.saveInt(cashReward);
  }
  
  
  

  public void setAsBounty(int cashReward) {
    this.isBounty = true;
    this.cashReward = 0;
    incReward(cashReward);
  }
  
  
  public boolean incReward(int inc) {
    if (! isBounty) return false;
    
    if (inc > mission.homeBase.funds()) return false;
    if (inc < 0 - cashReward) inc = 0 - cashReward;
    
    mission.homeBase.incFunds(0 - inc);
    this.cashReward += inc;
    return true;
  }
  
  
  public boolean isBounty() {
    return isBounty;
  }
  
  
  public int cashReward() {
    return this.cashReward;
  }
  
  
  void dispenseRewards() {
    Series <Actor> recruits = mission.recruits();
    if (isBounty && ! recruits.empty()) {
      int split = this.cashReward / recruits.size();
      int rem   = this.cashReward % recruits.size();
      int index = 0;
      for (Actor r : recruits) {
        r.outfit.incCarried(CASH, split + (index++ < rem ? 1 : 0));
      }
    }
  }
  
}




