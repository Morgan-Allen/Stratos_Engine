

package game;
import static game.GameConstants.*;
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
  
  
  public static interface Postured {
    Faction faction();
    BaseRelations relations(World world);
  }
  
  
  public static class Relation {
    
    Postured with;
    POSTURE posture;
    float   loyalty;
    
    int madeVassalDate = -1;
    int lastRebelDate  = -1;
    
    //  TODO:  Move these out to base-trading!
    
    Tally <Good> suppliesDue  = new Tally();
    Tally <Good> suppliesSent = new Tally();
  }
  
  
  final Object base;
  final World world;
  
  float prestige = PRESTIGE_AVG;
  final Table <Postured, Relation> relations = new Table();
  
  
  
  BaseRelations(Faction base, World world) {
    this.base  = base;
    this.world = world;
  }
  
  BaseRelations(Base base) {
    this.base  = base;
    this.world = base.world;
  }
  
  
  void loadState(Session s) throws Exception {
    
    prestige = s.loadFloat();

    for (int n = s.loadInt(); n-- > 0;) {
      Relation r = new Relation();
      r.with    = (Postured) s.loadObject();
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
  Relation relationWith(Postured other) {
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
  
  //  There is also the slight problem that factions can't have an attitude
  //  toward their own bases right now.
  
  
  public static void setPosture(Faction a, Faction b, POSTURE p, World w) {
    w.factionCouncil(a).relations.setPosture(b, p, true);
  }
  
  
  public void setPosture(Postured f, POSTURE p, boolean symmetric) {
    if (p == null) p = POSTURE.NEUTRAL;
    //
    //  You cannot have more than one Lord at a time, so break relations with
    //  any former master-
    if (p == POSTURE.LORD) {
      Faction oldLord = this.currentLord();
      if (oldLord == f) return;
      if (oldLord != null) setPosture(oldLord, POSTURE.NEUTRAL, true);
      relationWith(f).madeVassalDate = world.time;
    }
    //
    //  Impose the relation itself-
    if (p == POSTURE.LORD && base instanceof Base) {
      ((Base) base).assignFaction(f.faction());
    }
    relationWith(f).posture = p;
    //
    //  If you're enforcing symmetry, make sure the appropriate posture is
    //  reflected in the other city-
    if (symmetric) {
      POSTURE reverse = POSTURE.NEUTRAL;
      if (p == POSTURE.TRADING) reverse = POSTURE.TRADING;
      if (p == POSTURE.VASSAL ) reverse = POSTURE.LORD   ;
      if (p == POSTURE.LORD   ) reverse = POSTURE.VASSAL ;
      if (p == POSTURE.ALLY   ) reverse = POSTURE.ALLY   ;
      if (p == POSTURE.ENEMY  ) reverse = POSTURE.ENEMY  ;
      
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
      if (r.posture == POSTURE.LORD) return (Faction) r.with;
    }
    return null;
  }
  
  
  public void toggleRebellion(Faction lord, boolean is) {
    Relation r = relationWith(lord);
    if (r.posture != POSTURE.LORD) return;
    
    if (is) {
      r.lastRebelDate = world.time();
      r.suppliesSent.clear();
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
  //*/
  
  
  public boolean isLoyalVassalOf(Faction o) {
    Relation r = relationWith(o);
    if (r == null || r.posture != POSTURE.LORD) return false;
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
    if (r.posture != POSTURE.LORD         ) return -1;
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
  
  
  
}














