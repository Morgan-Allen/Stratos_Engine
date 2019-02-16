

package game;
import static game.GameConstants.*;
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
  
  
  public float suppliesDue(Base other, Good g) {
    if (other != base.federation().capital) return 0;
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
      Base capital = base.federation().capital;
      
      if (doCheck && capital != null) for (Good g : suppliesDue.keys()) {
        float sent = BaseTrading.goodsSent(base, capital, g);
        float due  = suppliesDue.valueFor(g);
        if (sent < due) failedSupply = true;
      }
      
      if (failedSupply) {
        toggleRebellion(base.faction(), true);
      }
      else if (capital != null) {
        capital.relations.incBond(base, LOY_TRIBUTE_BONUS);
      }
    }
  }
  
}





