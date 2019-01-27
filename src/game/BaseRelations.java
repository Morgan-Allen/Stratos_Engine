

package game;
import static game.BaseRelations.LOY_CIVIL;
import static game.BaseRelations.LOY_FADEOUT_TIME;
import static game.BaseRelations.LOY_TRIBUTE_BONUS;
import static game.GameConstants.*;

import game.GameConstants.Good;
import util.*;



public class BaseRelations extends RelationSet {
  
  
  final public static float
    LOY_DEVOTED  =  1.0F,
    LOY_FRIENDLY =  0.5F,
    LOY_CIVIL    =  0.0F,
    LOY_STRAINED = -0.5F,
    LOY_NEMESIS  = -1.0F,
    LOYALTIES[]  = { 1f, 0.5f, 0, -0.5f, -1 }
  ;
  final static String LOYALTY_DESC[] = {
    "Devoted", "Friendly", "Civil", "Strained", "Nemesis"
  };
  final public static float
    LOY_ATTACK_PENALTY  = -0.25f,
    LOY_CONQUER_PENALTY = -0.50f,
    LOY_REBEL_PENALTY   = -0.25f,
    LOY_TRIBUTE_BONUS   =  0.05f,
    LOY_FADEOUT_TIME    =  AVG_TRIBUTE_YEARS * YEAR_LENGTH * 2,
    
    PRESTIGE_MAX        =  100,
    PRESTIGE_AVG        =  50,
    PRESTIGE_MIN        =  0,
    
    PRES_VICTORY_GAIN   =  25,
    PRES_DEFEAT_LOSS    = -15,
    PRES_REBEL_LOSS     = -10,
    PRES_FADEOUT_TIME   =  AVG_TRIBUTE_YEARS * YEAR_LENGTH * 2
  ;
  
  
  
  final Base base;
  
  int madeVassalDate = -1;
  int lastRebelDate  = -1;
  
  Tally <Good> suppliesDue = new Tally();
  
  
  BaseRelations(Base base) {
    super(base);
    this.base = base;
  }
  
  
  void loadState(Session s) throws Exception {
    super.loadState(s);
    
    madeVassalDate = s.loadInt();
    lastRebelDate  = s.loadInt();
    s.loadTally(suppliesDue);
  }
  
  
  void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveInt(madeVassalDate);
    s.saveInt(lastRebelDate);
    s.saveTally(suppliesDue);
  }
  
  
  public void toggleRebellion(Faction lord, boolean is) {
    if (lord != base.faction()) return;
    
    if (is) {
      lastRebelDate = base.world.time();
      incBond(lord, LOY_REBEL_PENALTY / 2);
    }
    else {
      lastRebelDate = -1;
    }
  }
  
  
  public void setSuppliesDue(Faction lord, Tally <Good> suppliesDue) {
    if (lord != base.faction()) return;
    if (suppliesDue == null) suppliesDue = new Tally();
    this.suppliesDue = suppliesDue;
  }
  
  
  public Tally <Good> suppliesDue(Faction lord) {
    if (lord != base.faction()) return new Tally();
    return suppliesDue;
  }
  
  
  public float suppliesDue(Base capital, Good g) {
    if (capital != base.council().capital) return 0;
    return suppliesDue.valueFor(g);
  }
  
  
  public boolean isVassalOfSameLord(Base o) {
    if (! o.relations.isLoyalVassal()) return false;
    if (! this       .isLoyalVassal()) return false;
    return o.faction() == base.faction();
  }
  
  
  public boolean isLoyalVassalOf(Faction f) {
    return f == base.faction() && isLoyalVassal();
  }
  
  
  public boolean isLoyalVassal() {
    return lastRebelDate == -1;
  }
  
  
  public float yearsSinceRevolt(Faction lord) {
    if (base.faction() != lord || lastRebelDate == -1) return -1;
    return (base.world.time() - lastRebelDate) * 1f / YEAR_LENGTH;
  }
  
  
  void updateRelations(int interval) {
    for (Bond b : this.bonds) {
      float diff = LOY_CIVIL - b.level;
      diff *= interval * 1f / LOY_FADEOUT_TIME;
      b.level += diff;
    }
  }
  
  
  void updateTribute(int interval) {
    
    if (isLoyalVassal()) {
      int timeAsVassal = base.world.time - madeVassalDate;
      boolean failedSupply = false, doCheck = timeAsVassal >= YEAR_LENGTH;
      Base capital = base.council().capital;
      
      if (doCheck && capital != null) for (Good g : suppliesDue.keys()) {
        float sent = BaseTrading.goodsSent(base, capital, g);
        float due  = suppliesDue.valueFor(g);
        if (sent < due) failedSupply = true;
      }
      
      if (failedSupply) {
        toggleRebellion(base.faction(), true);
      }
      else {
        base.council().relations.incBond(base, LOY_TRIBUTE_BONUS);
      }
    }
  }
}



/*
public class BaseRelations {
  
  
  
  /**  Initial setup and general query methods-
    */
  /*
  Relation relationWith(Postured other) {
    if (other == null) return null;
    Relation r = relations.get(other);
    if (r == null) {
      relations.put(other, r = new Relation());
      r.with    = other;
      r.posture = BOND_NEUTRAL;
      r.loyalty = LOY_CIVIL;
    }
    return r;
  }
  
  
  public POSTURE posture(Postured other) {
    if (other == null) return null;
    return relationWith(other).posture;
  }
  
  
  public float loyalty(Postured other) {
    if (other == null) return 0;
    if (other == base) return 1;
    return relationWith(other).loyalty;
  }
  
  
  
  //  TODO:  All of this has to be considered carefully.  You're setting a
  //  relationship between two things, one of whom must be a faction.
  
  
  public static void setPosture(Faction a, Faction b, POSTURE p, World w) {
    w.factionCouncil(a).relations.setPosture(b, p, true);
  }
  
  
  public void setPosture(Postured f, POSTURE p, boolean symmetric) {
    if (p == null) p = BOND_NEUTRAL;
    //
    //  You cannot have more than one Lord at a time, so break relations with
    //  any former master-
    if (p == BOND_LORD) {
      Faction oldLord = this.currentLord();
      if (oldLord == f) return;
      if (oldLord != null) setPosture(oldLord, BOND_NEUTRAL, true);
      relationWith(f).madeVassalDate = world.time;
    }
    //
    //  Impose the relation itself-
    if (p == BOND_LORD && base instanceof Base) {
      ((Base) base).assignFaction(f.faction());
    }
    relationWith(f).posture = p;
    //
    //  If you're enforcing symmetry, make sure the appropriate posture is
    //  reflected in the other city-
    if (symmetric) {
      POSTURE reverse = BOND_NEUTRAL;
      if (p == BOND_TRADING) reverse = BOND_TRADING;
      if (p == BOND_VASSAL ) reverse = BOND_LORD   ;
      if (p == BOND_LORD   ) reverse = BOND_VASSAL ;
      if (p == BOND_ALLY   ) reverse = BOND_ALLY   ;
      if (p == BOND_ENEMY  ) reverse = BOND_ENEMY  ;
      
      BaseRelations r = f.relations(world);
      r.setPosture((Faction) base, reverse, false);
    }
  }
  
  
  public void incLoyalty(Postured b, float inc) {
    Relation r = relationWith(b);
    float loyalty = Nums.clamp(r.loyalty + inc, -1, 1);
    r.loyalty = loyalty;
  }
  
  
  public Faction currentLord() {
    for (Relation r : relations.values()) {
      if (r.posture == BOND_LORD) return (Faction) r.with;
    }
    return null;
  }
  
  
  public void toggleRebellion(Faction lord, boolean is) {
    Relation r = relationWith(lord);
    if (r.posture != BOND_LORD) return;
    
    if (is) {
      r.lastRebelDate = world.time();
      //r.suppliesSent.clear();
      incLoyalty(lord, LOY_REBEL_PENALTY / 2);
    }
    else {
      r.lastRebelDate = -1;
    }
  }
  
  
  public void initPrestige(float level) {
    this.prestige = level;
  }
  
  
  public float prestige() {
    return prestige;
  }
  
  
  public static void incPrestige(Faction c, float inc, World w) {
    BaseRelations relations = w.factionCouncil(c).relations;
    relations.prestige = Nums.clamp(
      relations.prestige + inc,
      PRESTIGE_MIN, PRESTIGE_MAX
    );
  }
  
  
  /*
  public Base currentLord() {
    for (Relation r : relations.values()) {
      if (r.posture == BOND_LORD) return r.with;
    }
    return null;
  }
  
  
  public Base capitalLord() {
    Base c = base;
    while (true) {
      Base l = c.relations.currentLord();
      if (l == null) return c;
      else c = l;
    }
  }
  
  
  public boolean isVassalOfSameLord(Base o) {
    Base lord = capitalLord();
    return lord != null && o.relations.capitalLord() == lord;
  }
  //*/
  
  /*
  
  public boolean isLoyalVassalOf(Faction o) {
    Relation r = relationWith(o);
    if (r == null || r.posture != BOND_LORD) return false;
    return r.lastRebelDate == -1;
  }
  
  
  public Iterable <Relation> relations() {
    return relations.values();
  }
  
  
  public Series <Faction> relationsWith() {
    Batch <Faction> all = new Batch();
    for (Postured c : relations.keySet()) if (c instanceof Faction) {
      all.add((Faction) c);
    }
    return all;
  }
  
  
  public Series <Base> vassalsInRevolt() {
    Batch <Base> all = new Batch();
    for (Base b : world.bases) if (b.faction() == base) {
      if (b.relations.yearsSinceRevolt(b.faction()) > 0) all.add(b);
    }
    return all;
  }
  
  
  public float yearsSinceRevolt(Faction lord) {
    Relation r = relationWith(lord);
    if (r.posture != BOND_LORD         ) return -1;
    if (r == null || r.lastRebelDate == -1) return -1;
    return (world.time() - r.lastRebelDate) * 1f / YEAR_LENGTH;
  }
  
  
  
  /**  Regular updates-
    */
  /*
  void updateRelations() {
    
  }
  
  
  void updateRelationsOffmap() {
    Base lord = currentLord();
    
    if (isLoyalVassalOf(lord)) {
      if (council.considerRevolt(lord, UPDATE_GAP)) {
        toggleRebellion(lord, true);
      }
      else if (lord.activeMap() == null) {
        Relation r = relationWith(lord);
        for (Good g : r.suppliesDue.keys()) {
          float sent = r.suppliesDue.valueFor(g) * usageInc * 1.1f;
          r.suppliesSent.add(sent, g);
        }
      }
    }
  }
  //*/
  
  
  
//}














