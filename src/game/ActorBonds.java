

package game;
import static game.GameConstants.*;
import util.*;



public class ActorBonds extends RelationSet {
  
  
  
  final Actor actor;
  private Base guestBase;
  private Base baseLoyal;
  int updateCounter = 0;
  
  
  
  ActorBonds(Actor actor) {
    super(actor);
    this.actor = actor;
  }
  
  
  void loadState(Session s) throws Exception {
    super.loadState(s);
    guestBase = (Base) s.loadObject();
    baseLoyal = (Base) s.loadObject();
    updateCounter = s.loadInt();
  }
  
  
  void saveState(Session s) throws Exception {
    super.saveState(s);
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
    Pick <Base> toJoin = new Pick(0);
    Base current = baseLoyal();
    float stubborn = actor.traits.levelOf(TRAIT_DILIGENCE) / 2f;
    
    for (Bond b : bonds) {
      if (! b.with.type().isActor()) continue;
      Actor a = (Actor) b.with;
      loyalties.add(b.level, a.bonds.baseLoyal());
    }
    
    //  TODO:  Divide by average?  What's the real threshold here?
    
    for (Base b : loyalties.keys()) {
      float rating = loyalties.valueFor(b);
      if (b == current) rating *= 1 + stubborn;
      toJoin.compare(b, rating);
    }
    Base joins = toJoin.result();
    
    if (joins != current && joins != null) {
      assignBaseLoyal(joins);
      
      I.say(actor+" has become sympathiser for "+joins+"!");
      
      checkBuildingLoyalty(actor.work());
      checkBuildingLoyalty(actor.home());
    }
  }
  
  
  public static void checkBuildingLoyalty(Employer e) {
    
    if (! (e instanceof Building)) return;
    Building building = (Building) e;
    
    Tally <Base> loyalties = new Tally();
    Pick <Base> toJoin = new Pick(0);
    Base current = building.base();
    float stubborn = 0.5f;
    
    for (Actor a : building.workers()) {
      loyalties.add(1, a.bonds.baseLoyal());
    }
    for (Actor a : building.residents()) if (a.work() != building) {
      loyalties.add(1, a.bonds.baseLoyal());
    }
    
    for (Base b : loyalties.keys()) {
      float rating = loyalties.valueFor(b);
      if (b == current) rating *= 1 + stubborn;
      toJoin.compare(b, rating);
    }
    Base joins = toJoin.result();
    
    if (joins != current && joins != null) {
      
      I.say(building+" has defected to "+joins+"!");
      
      for (Actor a : building.workers()) if (a.base() != joins) {
        if (a.bonds.baseLoyal() == joins) a.assignBase(joins);
        else building.setWorker(a, false);
      }
      for (Actor a : building.residents()) if (a.base() != joins) {
        if (a.bonds.baseLoyal() == joins) a.assignBase(joins);
        else building.setResident(a, false);
      }
      building.assignBase(joins);
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
  public static void setBond(
    Actor a, Actor b, int roleA, int roleB, float level
  ) {
    if (a != null && b != null) a.bonds.setBond(b, level, roleB);
    if (b != null && a != null) b.bonds.setBond(a, level, roleA);
  }
  
  
  public float solitude() {
    float bondTotal = 0;
    for (Bond b : bonds) {
      bondTotal += Nums.max(0, b.level);
    }
    bondTotal = Nums.clamp(bondTotal / AVG_NUM_BONDS, 0, 1);
    return 1 - bondTotal;
  }
  
  
  public Series <Actor> friendly() {
    return (Series) allBondedWith(0.25f, 100, BOND_ANY, true);
  }
  
  
  public Series <Actor> unfriendly() {
    return (Series) allBondedWith(0, -0.25f, BOND_ANY, true);
  }
  
  
}
















