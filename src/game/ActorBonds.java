

package game;
import static game.GameConstants.*;
import util.*;



public class ActorBonds {
  
  
  final public static int
    BOND_CHILD   = 1 << 1,
    BOND_PARENT  = 1 << 2,
    BOND_MARRIED = 1 << 3,
    BOND_MASTER  = 1 << 4,
    BOND_SERVANT = 1 << 5,
    BOND_ANY     = 0
  ;
  final static int
    BOND_UPDATE_TIME = GameConstants.DAY_LENGTH
  ;
  
  
  static class Bond {
    Actor with;
    float level;
    float novelty;
    int properties;
    
    public String toString() { return with+": "+level; }
  }
  
  
  final Actor actor;
  
  private List <Bond> bonds = new List();
  private Base guestBase;
  private Base baseLoyal;
  int updateCounter = 0;
  
  
  
  ActorBonds(Actor actor) {
    this.actor = actor;
  }
  
  
  void loadState(Session s) throws Exception {
    for (int n = s.loadInt(); n-- > 0;) {
      Bond b = new Bond();
      b.with       = (Actor) s.loadObject();
      b.level      = s.loadFloat();
      b.novelty    = s.loadFloat();
      b.properties = s.loadInt();
      bonds.add(b);
    }
    guestBase = (Base) s.loadObject();
    baseLoyal = (Base) s.loadObject();
    updateCounter = s.loadInt();
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveInt(bonds.size());
    for (Bond b : bonds) {
      s.saveObject(b.with);
      s.saveFloat(b.level);
      s.saveFloat(b.novelty);
      s.saveInt(b.properties);
    }
    s.saveObject(guestBase);
    s.saveObject(baseLoyal);
    s.saveInt(updateCounter);
  }
  
  
  
  /**  Regular updates-
    */
  public void updateBonds() {
    int updateLimit = BOND_UPDATE_TIME * actor.map().ticksPerSecond();
    if (updateCounter++ >= updateLimit) {
      updateCounter = 0;
      for (Bond b : bonds) {
        b.novelty += BOND_UPDATE_TIME * 1f / BOND_NOVEL_TIME;
        b.novelty = Nums.clamp(b.novelty, 0, 1);
      }
      makeLoyaltyCheck();
    }
  }
  
  
  public void makeLoyaltyCheck() {
    
    Tally <Base> loyalties = new Tally();
    Pick <Base> toJoin = new Pick();
    Base current = baseLoyal();
    float stubborn = actor.traits.levelOf(TRAIT_DILIGENCE) / 2f;
    
    for (Bond b : bonds) {
      loyalties.add(b.level, b.with.bonds.baseLoyal());
    }
    
    for (Base b : loyalties.keys()) {
      float rating = loyalties.valueFor(b);
      if (b == current) rating *= 1 + stubborn;
      toJoin.compare(b, rating);
    }
    
    if (toJoin.result() != current) {
      assignBaseLoyal(toJoin.result());
    }
  }
  
  

  /**  Supplementary base-allegiance settings-
    */
  public void assignGuestBase(Base city) {
    this.guestBase = city;
  }
  
  
  public Base guestBase() {
    return guestBase;
  }
  
  
  public void assignBaseLoyal(Base base) {
    if (base == actor.base()) base = null;
    this.baseLoyal = base;
  }
  
  
  public Base baseLoyal() {
    if (baseLoyal == null) return actor.base();
    return baseLoyal;
  }
  
  
  
  /**  Handling bonds with other actors-
    */
  Bond bondWith(Actor with, boolean init) {
    for (Bond b : bonds) {
      if (b.with == with) return b;
    }
    if (init) {
      Bond b = new Bond();
      b.with    = with;
      b.novelty = INIT_NOVELTY / 100f;
      b.level   = INIT_BONDING;
      bonds.add(b);
      return b;
    }
    return null;
  }
  
  
  public static void setBond(
    Actor a, Actor b, int roleA, int roleB, float level
  ) {
    if (a != null && b != null) a.bonds.setBond(b, level, roleB);
    if (b != null && a != null) b.bonds.setBond(a, level, roleA);
  }
  
  
  public void setBond(Actor with, float level, int... props) {
    Bond b = bondWith(with, true);
    b.level = level;
    b.properties = 0;
    for (int p : props) b.properties |= p;
  }
  
  
  public void incBond(Actor with, float inc, float maxRange) {
    Bond b = bondWith(with, true);
    if (b.level > maxRange || b.level < -maxRange) return;
    b.level = Nums.clamp(b.level + inc, -maxRange, maxRange);
  }
  
  
  public void incNovelty(Actor with, float inc) {
    Bond b = bondWith(with, true);
    b.novelty  = Nums.clamp(b.novelty + inc, 0, 1);
  }
  
  
  public void deleteBond(Actor with) {
    Bond b = bondWith(with, false);
    if (b != null) bonds.remove(b);
  }
  
  
  public boolean hasBondType(Actor with, int type) {
    Bond b = bondWith(with, false);
    return b == null ? false : ((b.properties & type) != 0);
  }
  
  
  public float bondLevel(Actor with) {
    Bond b = bondWith(with, false);
    return b == null ? INIT_BONDING : b.level;
  }
  
  
  public float bondNovelty(Actor with) {
    Bond b = bondWith(with, false);
    return b == null ? (INIT_NOVELTY / 100f) : b.novelty;
  }
  
  
  public boolean hasBond(Actor with) {
    return bondWith(with, false) != null;
  }
  
  
  public Actor bondedWith(int type) {
    for (Bond b : bonds) if ((b.properties & type) != 0) return b.with;
    return null;
  }
  
  
  public Series <Actor> allBondedWith(int type) {
    Batch <Actor> all = new Batch();
    for (Bond b : bonds) {
      if (type != BOND_ANY && (b.properties & type) == 0) continue;
      all.add(b.with);
    }
    return all;
  }
  
  
  public float solitude() {
    float bondTotal = 0;
    for (Bond b : bonds) {
      bondTotal += Nums.max(0, b.level);
    }
    bondTotal = Nums.clamp(bondTotal / AVG_NUM_BONDS, 0, 1);
    return 1 - bondTotal;
  }
  
  
}













