

package game;
import static game.BaseRelations.*;
import util.*;



public class FactionRelations extends RelationSet {
  
  
  
  float prestige = PRESTIGE_AVG;
  
  
  FactionRelations(Faction faction) {
    super(faction);
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    prestige = s.loadFloat();
  }
  
  public void loadState(Session s) throws Exception {
    super.loadState(s);
    s.saveFloat(prestige);
  }
  
  
  

  
  
  public static void setPosture(Faction a, Faction b, int posture, World w) {
    setPosture(w.factionCouncil(a).relations, b, posture, true, w);
  }
  
  
  static void setPosture(RelationSet a, Faction f, int posture, boolean symmetric, World w) {
    if (posture <= 0) posture = BOND_NEUTRAL;
    //
    //  You cannot have more than one Lord at a time, so break relations with
    //  any former master-
    if (posture == BOND_LORD && a.focus instanceof Base) {
      Base base = (Base) a.focus;
      Faction oldLord = base.faction();
      if (oldLord == f) return;
      if (oldLord != null) a.setBondType(oldLord, BOND_NEUTRAL);
      base.assignFaction(f);
      base.relations.madeVassalDate = base.world.time();
    }
    a.setBondType(f, posture);
    //
    //  If you're enforcing symmetry, make sure the appropriate posture is
    //  reflected in the other city-
    if (symmetric && a.focus instanceof Faction) {
      int reverse = BOND_NEUTRAL;
      if (posture == BOND_TRADING) reverse = BOND_TRADING;
      if (posture == BOND_VASSAL ) reverse = BOND_LORD   ;
      if (posture == BOND_LORD   ) reverse = BOND_VASSAL ;
      if (posture == BOND_ALLY   ) reverse = BOND_ALLY   ;
      if (posture == BOND_ENEMY  ) reverse = BOND_ENEMY  ;
      setPosture(w.factionCouncil(f).relations, (Faction) a.focus, reverse, false, w);
    }
  }

  
  
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








