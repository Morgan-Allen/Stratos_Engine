

package game;
import static game.GameConstants.*;

import game.GameConstants.Good;
import util.*;




public class BaseRelations {
  
  public static enum POSTURE {
    ENEMY  ,
    ALLY   ,
    VASSAL ,
    LORD   ,
    NEUTRAL,
    TRADING,
  };
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
  
  
  public static class Relation {
    Base    with;
    POSTURE posture;
    float   loyalty;
    
    int madeVassalDate = -1;
    int lastRebelDate  = -1;
    
    Tally <Good> suppliesDue  = new Tally();
    Tally <Good> suppliesSent = new Tally();
  }
  
  
  final Base base;

  Base homeland = null;
  float prestige = PRESTIGE_AVG;
  final Table <Base, Relation> relations = new Table();
  
  
  
  BaseRelations(Base base) {
    this.base = base;
  }
  
  
  void loadState(Session s) throws Exception {
    
    homeland = (Base) s.loadObject();
    prestige = s.loadFloat();

    for (int n = s.loadInt(); n-- > 0;) {
      Relation r = new Relation();
      r.with    = (Base) s.loadObject();
      r.posture = POSTURE.values()[s.loadInt()];
      r.loyalty = s.loadFloat();
      r.madeVassalDate = s.loadInt();
      r.lastRebelDate  = s.loadInt();
      s.loadTally(r.suppliesDue );
      s.loadTally(r.suppliesSent);
      relations.put(r.with, r);
    }
  }
  
  
  void saveState(Session s) throws Exception {
    
    s.saveObject(homeland);
    s.saveFloat(prestige);

    s.saveInt(relations.size());
    for (Relation r : relations.values()) {
      s.saveObject(r.with);
      s.saveInt(r.posture.ordinal());
      s.saveFloat(r.loyalty);
      s.saveInt(r.madeVassalDate);
      s.saveInt(r.lastRebelDate );
      s.saveTally(r.suppliesDue );
      s.saveTally(r.suppliesSent);
    }
  }
  
  
  
  
  /**  Initial setup and general query methods-
    */
  
  public void setHomeland(Base homeland) {
    this.homeland = homeland;
  }
  
  
  public Base homeland() {
    return homeland;
  }
  
  
  Relation relationWith(Base other) {
    if (other == null) return null;
    Relation r = relations.get(other);
    if (r == null) {
      relations.put(other, r = new Relation());
      r.with    = other;
      r.posture = POSTURE.NEUTRAL;
      r.loyalty = LOY_CIVIL;
    }
    return r;
  }
  
  
  public POSTURE posture(Base other) {
    if (other == null) return null;
    return relationWith(other).posture;
  }
  
  
  public float loyalty(Base other) {
    if (other == null) return 0;
    if (other == base) return 1;
    return relationWith(other).loyalty;
  }
  
  
  public static void setPosture(Base a, Base b, POSTURE p, boolean symmetric) {
    if (p == null) p = POSTURE.NEUTRAL;
    //
    //  You cannot have more than one Lord at a time, so break relations with
    //  any former master-
    if (p == POSTURE.LORD) {
      Base formerLord = a.relations.currentLord();
      if (formerLord == b) return;
      if (formerLord != null) setPosture(a, formerLord, POSTURE.NEUTRAL, true);
      a.relations.relationWith(b).madeVassalDate = a.world.time;
    }
    
    a.relations.relationWith(b).posture = p;
    
    //
    //  If you're enforcing symmetry, make sure the appropriate posture is
    //  reflected in the other city-
    if (symmetric) {
      POSTURE reverse = POSTURE.NEUTRAL;
      if (p == POSTURE.TRADING) reverse = POSTURE.TRADING;
      if (p == POSTURE.VASSAL ) reverse = POSTURE.LORD  ;
      if (p == POSTURE.LORD   ) reverse = POSTURE.VASSAL;
      if (p == POSTURE.ALLY   ) reverse = POSTURE.ALLY  ;
      if (p == POSTURE.ENEMY  ) reverse = POSTURE.ENEMY ;
      setPosture(b, a, reverse, false);
    }
  }
  
  
  public void toggleRebellion(Base lord, boolean is) {
    Relation r = relationWith(lord);
    if (r.posture != POSTURE.LORD) return;
    
    if (is) {
      r.lastRebelDate = base.world.time();
      r.suppliesSent.clear();
      incLoyalty(lord, base, LOY_REBEL_PENALTY / 2);
    }
    else {
      r.lastRebelDate = -1;
    }
  }
  
  
  public boolean isVassalOf(Base o) { return posture(o) == POSTURE.LORD  ; }
  public boolean isLordOf  (Base o) { return posture(o) == POSTURE.VASSAL; }
  public boolean isEnemyOf (Base o) { return posture(o) == POSTURE.ENEMY ; }
  public boolean isAllyOf  (Base o) { return posture(o) == POSTURE.ALLY  ; }
  
  
  public static void incLoyalty(Base a, Base b, float inc) {
    Relation r = a.relations.relationWith(b);
    float loyalty = Nums.clamp(r.loyalty + inc, -1, 1);
    r.loyalty = loyalty;
  }
  
  
  public void initPrestige(float level) {
    this.prestige = level;
  }
  
  
  public float prestige() {
    return prestige;
  }
  
  
  public static void incPrestige(Base c, float inc) {
    c.relations.prestige = Nums.clamp(
      c.relations.prestige + inc,
      PRESTIGE_MIN, PRESTIGE_MAX
    );
  }
  
  
  public Base currentLord() {
    for (Relation r : relations.values()) {
      if (r.posture == POSTURE.LORD) return r.with;
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
  
  
  public boolean isLoyalVassalOf(Base o) {
    Relation r = relationWith(o);
    if (r == null || r.posture != POSTURE.LORD) return false;
    return r.lastRebelDate == -1;
  }
  
  
  public Iterable <Relation> relations() {
    return relations.values();
  }
  
  
  public Series <Base> relationsWith() {
    Batch <Base> all = new Batch();
    for (Base c : relations.keySet()) all.add(c);
    return all;
  }
  
  
  public Series <Base> vassalsInRevolt() {
    Batch <Base> all = new Batch();
    for (Relation r : relations.values()) {
      if (r.with.relations.yearsSinceRevolt(base) > 0) all.add(r.with);
    }
    return all;
  }
  
  
  public float yearsSinceRevolt(Base lord) {
    Relation r = relationWith(lord);
    if (r.posture != POSTURE.LORD         ) return -1;
    if (r == null || r.lastRebelDate == -1) return -1;
    return (base.world.time() - r.lastRebelDate) * 1f / YEAR_LENGTH;
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
  
  
  
}














