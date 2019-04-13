

package game;
import static game.GameConstants.*;
import util.*;



public class RelationSet {

  
  final public static int
    
    BOND_CHILD   = 1 << 1,
    BOND_PARENT  = 1 << 2,
    BOND_MARRIED = 1 << 3,
    BOND_MASTER  = 1 << 4,
    BOND_SERVANT = 1 << 5,
    
    BOND_ENEMY   = 1 << 6,
    BOND_ALLY    = 1 << 7,
    BOND_VASSAL  = 1 << 8,
    BOND_LORD    = 1 << 9,
    BOND_NEUTRAL = 1 << 10,
    BOND_TRADING = 1 << 11,
    
    BOND_ANY     = 0
  ;
  final static int
    BOND_UPDATE_TIME = GameConstants.DAY_LENGTH
  ;
  
  
  public static interface Focus extends Session.Saveable {
    Type type();
  }
  
  
  public static class Bond {
    
    Focus with;
    float level;
    float impression;
    float novelty;
    int properties;
    
    public String toString() { return with+": "+level; }
  }
  
  
  final Focus focus;
  protected List <Bond> bonds = new List();
  
  
  
  RelationSet(Focus focus) {
    this.focus = focus;
  }
  
  
  void loadState(Session s) throws Exception {
    for (int n = s.loadInt(); n-- > 0;) {
      Bond b = new Bond();
      b.with       = (Focus) s.loadObject();
      b.level      = s.loadFloat();
      b.novelty    = s.loadFloat();
      b.impression = s.loadFloat();
      b.properties = s.loadInt();
      bonds.add(b);
    }
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveInt(bonds.size());
    for (Bond b : bonds) {
      s.saveObject(b.with);
      s.saveFloat(b.level);
      s.saveFloat(b.novelty);
      s.saveFloat(b.impression);
      s.saveInt(b.properties);
    }
  }
  
  
  
  
  /**  Handling bonds with other foci-
    */
  Bond bondWith(Focus with, boolean init) {
    for (Bond b : bonds) {
      if (b.with == with) return b;
    }
    if (init) {
      Bond b = new Bond();
      b.with    = with;
      b.novelty = INIT_NOVELTY / 100f;
      b.level   = INIT_BONDING / 100f;
      bonds.add(b);
      return b;
    }
    return null;
  }
  
  
  public void setBond(Focus with, float level, int... props) {
    Bond b = bondWith(with, true);
    b.level = level;
    b.properties = 0;
    for (int p : props) b.properties |= p;
  }
  
  
  public void incBond(Focus with, float inc) {
    incBond(with, inc, 1);
  }
  
  
  public void incBond(Focus with, float inc, float maxRange) {
    Bond b = bondWith(with, true);
    if (b.level > maxRange || b.level < -maxRange) return;
    b.level = Nums.clamp(b.level + inc, -maxRange, maxRange);
  }
  
  
  public void incNovelty(Focus with, float inc) {
    Bond b = bondWith(with, true);
    b.novelty  = Nums.clamp(b.novelty + inc, 0, 1);
  }
  
  
  public void deleteBond(Focus with) {
    Bond b = bondWith(with, false);
    if (b != null) bonds.remove(b);
  }
  
  
  public void setBondType(Focus with, int type) {
    Bond b = bondWith(with, true);
    b.properties = type;
  }
  
  
  public boolean hasBondType(Focus with, int type) {
    Bond b = bondWith(with, false);
    return b == null ? false : ((b.properties & type) != 0);
  }
  
  
  public int bondProperties(Focus with) {
    Bond b = bondWith(with, false);
    return b == null ? -1 : b.properties;
  }
  
  
  public float bondLevel(Focus with) {
    Bond b = bondWith(with, false);
    return b == null ? (INIT_BONDING / 100f) : b.level;
  }
  
  
  public float bondNovelty(Focus with) {
    Bond b = bondWith(with, false);
    return b == null ? (INIT_NOVELTY / 100f) : b.novelty;
  }
  
  
  public boolean hasBond(Focus with) {
    return bondWith(with, false) != null;
  }
  
  
  public Focus bondedWith(int type) {
    for (Bond b : bonds) if ((b.properties & type) != 0) return b.with;
    return null;
  }
  
  
  public Series <Focus> allBondedWith(int type) {
    return allBondedWith(-100, 100, type, false);
  }
  
  
  public Series <Focus> allBondedWith(
    float minVal, float maxVal,
    int type, boolean personOnly
  ) {
    Batch <Focus> all = new Batch();
    for (Bond b : bonds) {
      if (b.level < minVal || b.level > maxVal) continue;
      if (type != BOND_ANY && (b.properties & type) == 0) continue;
      if (personOnly && ! b.with.type().isPerson()) continue;
      all.add(b.with);
    }
    return all;
  }
  
}









