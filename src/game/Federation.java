

package game;
import util.*;
import static game.GameConstants.*;
import static game.Mission.*;
import static game.ActorBonds.*;
import static game.BaseRelations.*;



public class Federation {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final public static int
    AI_OFF       = -1,
    AI_NORMAL    =  0,
    AI_COMPLIANT =  1,
    AI_DEFIANT   =  2,
    AI_PACIFIST  =  3,
    AI_WARLIKE   =  4
  ;
  
  public static enum GOVERNMENT {
    IMPERIAL, FEUDAL, BARBARIAN, REPUBLIC
  }
  
  
  final Faction faction;
  final World world;
  
  Base homeland;
  Base capital;

  final public FederationRelations relations;
  GOVERNMENT government = GOVERNMENT.FEUDAL;
  int typeAI = AI_NORMAL;
  
  Tally <WorldLocale> exploreLevels = new Tally();
  
  
  
  Federation(Faction faction, World world) {
    this.faction = faction;
    this.world   = world  ;
    this.relations = new FederationRelations(faction);
  }
  
  
  void loadState(Session s) throws Exception {
    
    homeland = (Base) s.loadObject();
    capital  = (Base) s.loadObject();
    
    relations.loadState(s);
    government = GOVERNMENT.values()[s.loadInt()];
    typeAI = s.loadInt();
    
    s.loadTally(exploreLevels);
  }
  
  
  void saveState(Session s) throws Exception {
    
    s.saveObject(homeland);
    s.saveObject(capital );
    
    relations.saveState(s);
    s.saveInt(government.ordinal());
    s.saveInt(typeAI);
    
    s.saveTally(exploreLevels);
  }
  
  
  
  /**  Regular updates-
    */
  void update(boolean playerOwned) {
    float presDrift = PRESTIGE_AVG * 1f / PRES_FADEOUT_TIME;
    float presDiff = PRESTIGE_AVG - relations.prestige();
    if (Nums.abs(presDiff) > presDrift) {
      presDiff = presDrift * (presDiff > 0 ? 1 : -1);
    }
    relations.incPrestige(presDiff);
    
    /*
    //
    //  And, finally, lose prestige based on any vassals in recent revolt-
    //  and if the time gone exceeds a certain threshold, end vassal status.
    if (world.time % YEAR_LENGTH == 0) {
      
      for (Base base : world.bases) {
        if (base.federation() != this) continue;
        if (! base.isLoyalVassalOf(capital)) continue;
        
        float years = base.relations.yearsSinceRevolt(faction);
        float maxT = AVG_TRIBUTE_YEARS, timeMult = (maxT - years) / maxT;
        
        if (years < AVG_TRIBUTE_YEARS) {
          relations.initPrestige(PRES_REBEL_LOSS * timeMult);
          relations.incBond(base, LOY_REBEL_PENALTY * 0.5f * timeMult);
        }
        else {
          relations.setBondType(base, BOND_ENEMY);
        }
      }
    }
    //*/
  }
  
  
  
  /**  Assigning homeworld and capital-
    */
  public Base homeland() {
    return homeland;
  }
  
  
  public Base capital() {
    return capital;
  }
  
  
  public void assignHomeland(Base home) {
    this.homeland = home;
  }
  
  
  public void assignCapital(Base capital) {
    this.capital = capital;
  }
  
  
  
  /**  Toggle membership of the council and handling personality-effects-
    */
  public void setGovernment(GOVERNMENT g) {
    this.government = g;
  }
  
  
  public GOVERNMENT government() {
    return government;
  }
  
  
  public void setTypeAI(int typeAI) {
    this.typeAI = typeAI;
  }
  
  
  public boolean hasTypeAI(int typeAI) {
    return this.typeAI == typeAI;
  }
  
  
  
  /**  Setting up initial postures between bases/federations-
    */
  //  TODO:  You have 2 cases here- setting relations between factions, and
  //  assigning base-ownership in the case of lord/vassal proposals.  Okay.
  
  /*
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
      setPosture(w.federation(f).relations, (Faction) a.focus, reverse, false, w);
    }
  }
  //*/
  
  public static void setPosture(Faction a, Faction b, int posture, World w) {
    if (posture <= 0) posture = BOND_NEUTRAL;

    int reverse = BOND_NEUTRAL;
    if (posture == BOND_TRADING) reverse = BOND_TRADING;
    if (posture == BOND_VASSAL ) reverse = BOND_LORD   ;
    if (posture == BOND_LORD   ) reverse = BOND_VASSAL ;
    if (posture == BOND_ALLY   ) reverse = BOND_ALLY   ;
    if (posture == BOND_ENEMY  ) reverse = BOND_ENEMY  ;
    
    a.relations(w).setBondType(b, posture);
    b.relations(w).setBondType(a, reverse);
  }
  
  
  
  /**  Levels of exploration-
    */
  public float exploreLevel(WorldLocale l) {
    return exploreLevels.valueFor(l);
  }
  
  public void setExploreLevel(WorldLocale l, float value) {
    exploreLevels.set(l, value);
  }
  
  
  
  /**  Graphical, interface and debug methods-
    */
  public String toString() {
    return "Federation: "+faction.name;
  }
}











