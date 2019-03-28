

package game;
import static game.BaseRelations.*;
import util.*;



public class FederationRelations extends RelationSet {
  
  
  float prestige = PRESTIGE_AVG;
  
  
  FederationRelations(Faction faction) {
    super(faction);
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveFloat(prestige);
  }
  
  public void loadState(Session s) throws Exception {
    super.loadState(s);
    prestige = s.loadFloat();
  }
  

  
  
  
  
  /*
  public void setBond(Focus with, float level, int... props) {
    // TODO Auto-generated method stub
    
    Faction belongs = (Faction) focus;
    
    if (level < 0 && with == belongs) {
      I.say("???");
    }
    
    super.setBond(with, level, props);
  }
  
  
  public void incBond(Focus with, float inc, float maxRange) {
    // TODO Auto-generated method stub
    
    Faction belongs = (Faction) focus;
    
    if (inc < 0 && with == belongs) {
      I.say("???");
    }
    
    super.incBond(with, inc, maxRange);
  }
  //*/
  

  
  /**  TODO:  This might be moved into the Federation class itself?
    */
  public void initPrestige(float level) {
    this.prestige = level;
  }
  
  
  public float prestige() {
    return prestige;
  }
  
  
  public void incPrestige(float inc) {
    prestige = Nums.clamp(prestige + inc, PRESTIGE_MIN, PRESTIGE_MAX);
  }
  
  
  public Series <Base> vassalsInRevolt(World world) {
    Batch <Base> all = new Batch();
    for (Base b : world.bases) if (b.faction() == focus) {
      if (b.relations.yearsSinceRevolt(b.faction()) > 0) all.add(b);
    }
    return all;
  }
  
}








