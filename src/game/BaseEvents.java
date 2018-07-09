

package game;
import util.*;



public class BaseEvents {
  
  
  final static int
    EVENTS_INTERVAL = 60 * 6
  ;
  
  
  public static interface Trouble {
    Faction faction();
    float troublePower();
    void generateTrouble(Area activeMap, float factionPower);
  }
  
  
  Area area;
  int lastEventTime;
  
  
  
  void updateEvents() {
    
    if (area.time() - lastEventTime > EVENTS_INTERVAL) {
      lastEventTime = area.time();
    }
    else {
      return;
    }
    
    //Pick <Trouble> pickTrouble = new Pick();
    
    List  <Trouble> trouble = new List ();
    Pick  <Trouble> pick    = new Pick ();
    Tally <Faction> powers  = new Tally();
    
    for (Building b : area.buildings()) if (b instanceof Trouble) {
      trouble.add((Trouble) b);
    }
    for (Base b : area.world.bases()) if (b instanceof Trouble) {
      trouble.add((Trouble) b);
    }
    for (Trouble t : trouble) {
      Faction faction = t.faction();
      float power = t.troublePower();
      powers.add(power, faction);
      pick.compare(t, power * (0.5f + Rand.num()));
    }
    
    //  The most important thing here is to space out events so that the player
    //  isn't under continual siege.  The actual size of the raids is less
    //  important.  (In fact, it might make sense to launch massed raids from
    //  several sources on a particular target at once.)
    
    if (! pick.empty()) {
      Trouble source = pick.result();
      Faction faction = source.faction();
      float power = powers.valueFor(faction);
      source.generateTrouble(area, power);
    }
  }
  
}









