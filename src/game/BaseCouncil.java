

package game;
import static game.GameConstants.*;
import static game.ActorBonds.*;
import static game.Federation.*;
import util.*;




public class BaseCouncil {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public static enum Role {
    MONARCH          ,
    CONSORT          ,
    HEIR             ,
    PRIME_MINISTER   ,
    HIGH_PRIEST      ,
    MINISTER_WAR     ,
    MINISTER_TRADE   ,
    MINISTER_LEARNING,
    MINISTER_ARTS    ,
  };
  
  
  final Base base;
  
  private List <Actor> members = new List();
  private Table <Actor, Role> roles = new Table();
  
  private List <Mission> petitions = new List();
  
  
  
  BaseCouncil(Base base) {
    this.base = base;
  }
  
  
  void loadState(Session s) throws Exception {
    
    for (int n = s.loadInt(); n-- > 0;) {
      Actor a = (Actor) s.loadObject();
      Role r = (Role) s.loadEnum(Role.values());
      members.add(a);
      roles.put(a, r);
    }
    
    s.loadObjects(petitions);
  }
  
  
  void saveState(Session s) throws Exception {
    
    s.saveInt(members.size());
    for (Actor a : members) {
      s.saveObject(a);
      s.saveEnum(roles.get(a));
    }
    
    s.saveObjects(petitions);
  }
  
  
  
  /**  Toggle membership of the council and handling personality-effects-
    */
  public void toggleMember(Actor actor, Role role, boolean yes) {
    if (yes) {
      members.include(actor);
      roles.put(actor, role);
    }
    else {
      members.remove(actor);
      roles.remove(actor);
    }
  }
  
  
  public Actor memberWithRole(Role role) {
    for (Actor a : members) if (roles.get(a) == role) return a;
    return null;
  }
  
  
  public Series <Actor> members() {
    return members;
  }
  
  
  public Series <Actor> allMembersWithRole(Role role) {
    Batch <Actor> all = new Batch();
    for (Actor a : members) if (roles.get(a) == role) all.add(a);
    return all;
  }
  

  public float membersTraitAvg(Trait trait) {
    float avg = 0, count = 0;
    for (Actor m : members) {
      avg += m.traits.levelOf(trait);
      count += 1;
    }
    return avg / Nums.max(1, count);
  }
  
  
  public float membersBondAvg(Actor with) {
    float avg = 0, count = 0;
    for (Actor m : members) {
      avg += m.bonds.bondLevel(with);
      count += 1;
    }
    return avg / Nums.max(1, count);
  }
  
  
  
  /**  Handling agreements or terms submitted by other cities-
    */
  public void receiveTerms(Mission petition) {
    petitions.include(petition);
  }
  
  
  public Series <Mission> petitions() {
    return petitions;
  }
  
  
  public void acceptTerms(Mission petition) {
    petition.terms.setAccepted(true);
    petitions.remove(petition);
  }
  
  
  public void rejectTerms(Mission petition) {
    petition.terms.setAccepted(false);
    petitions.remove(petition);
  }
  
  
  
  /**  Regular updates-
    */
  void updateCouncil(boolean playerOwned) {
    //
    //  Check on any current heirs and/or marriage status-
    Actor monarch = memberWithRole(Role.MONARCH);
    if (monarch != null) {
      for (Focus f : monarch.bonds.allBondedWith(BOND_MARRIED)) {
        Actor consort = (Actor) f;
        if (! consort.health.alive()) continue;
        toggleMember(consort, Role.CONSORT, true);
      }
      for (Focus f : monarch.bonds.allBondedWith(BOND_CHILD)) {
        Actor heir = (Actor) f;
        if (! heir.health.alive()) continue;
        toggleMember(heir, Role.HEIR, true);
      }
    }
    //
    //  For now, we'll assume succession is determined by quasi-hereditary-
    //  male-line-primogeniture with a dash of popularity contest.  Might allow
    //  for customisation later.
    
    //  TODO:  Vary the selection algorithm based on government style.
    
    if (monarch == null || monarch.health.dead()) {
      toggleMember(monarch, Role.MONARCH, false);
      Pick <Actor> pick = new Pick();
      for (Actor a : members) {
        Role r = roles.get(a);
        float rating = 1 + membersBondAvg(a);
        if (r == Role.HEIR  ) rating *= 3;
        if (a.health.woman()) rating /= 2;
        rating *= 1 + (a.health.ageYears() / AVG_RETIREMENT);
        pick.compare(a, rating);
      }
      //
      //  The king is dead, long live the king-
      Actor newMonarch = pick.result();
      if (newMonarch != null) {
        toggleMember(newMonarch, Role.MONARCH, true);
      }
    }
    //
    //  And remove any other dead council-members:
    for (Actor a : members) {
      Role r = roles.get(a);
      if (a.health.dead()) {
        toggleMember(a, r, false);
      }
    }
    //
    //  Once per month, otherwise, evaluate any major independent decisions-
    if ((! base.federation().hasTypeAI(AI_OFF)) && ! playerOwned) {
      //
      //  See if any of the current petitions are worth responding to-
      for (Mission petition : petitions) {
        float appeal = MissionAIUtils.appealOfTerms(base, petition);
        if (Rand.num() < appeal) {
          petition.terms.setAccepted(true);
        }
        else {
          petition.terms.setAccepted(false);
        }
        petitions.remove(petition);
      }
    }
  }
  

  
  //  TODO:  Restore this...
  
  boolean considerRevolt(Faction faction, int period, Base base) {
    return false;
  }
  
}













