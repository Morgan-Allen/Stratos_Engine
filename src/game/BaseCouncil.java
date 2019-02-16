

package game;
import static game.GameConstants.*;
import static game.Mission.*;
import static game.ActorBonds.*;
import static game.BaseRelations.*;
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
  /*
  public static class MissionAssessment {
    
    Base rulesC;
    Base fromC;
    Base goesC;
    float fromPower, goesPower;
    
    int objective;
    int          postureDemand  = -1;
    Mission      actionDemand   = null;
    Actor        marriageDemand = null;
    Tally <Good> tributeDemand  = null;
    
    float winChance, angerChance;
    float winKillsA, lossKillsA;
    float winKillsD, lossKillsD;
    float hateBonus, lovePenalty;
    float dutyBonus, dutyPenalty;
    
    float costs, benefits;
    float evaluatedAppeal;
    
    //public Base rules() { return rulesC; }
    
    public Base rules() { return rulesC; }
    public Base from() { return fromC; }
    public Base goes() { return goesC; }
    public float appeal() { return evaluatedAppeal; }
  }
  //*/
  
  
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
    if (base.federation().hasTypeAI(AI_OFF) && ! playerOwned) {
      if (base.world.time % (DAY_LENGTH / 2) == 0) {
        updateCouncilAI();
      }
    }
  }
  
  
  void updateCouncilAI() {
    
    //  TODO:  Revisit this!
    
    /*
    //
    //  See if any of the current petitions are worth responding to-
    for (Mission petition : petitions) {
      float appeal = appealOfTerms(
        petition.base(), petition.localBase,
        petition.terms.postureDemand,
        petition.terms.actionDemand,
        petition.terms.marriageDemand,
        petition.terms.tributeDemand
      );
      if (Rand.num() < appeal) {
        petition.terms.setAccepted(true);
      }
      else {
        petition.terms.setAccepted(false);
      }
      petitions.remove(petition);
    }
    //
    //  Put together a list of possible missions:
    Pick <MissionAssessment> pickI = new Pick(0);
    for (MissionAssessment IA : updateMissionChoices()) {
      pickI.compare(IA, IA.evaluatedAppeal);
    }
    //
    //  And if any have positive appeal, launch the enterprise:
    MissionAssessment IA = pickI.result();
    if (IA != null) {
      Mission force = spawnFormation(IA, IA.fromC);
      force.beginMission(IA.fromC);
    }
    //*/
  }
  
  
  
  /*
  void calculateChances(MissionAssessment a, boolean random) {
    //  TODO:  I need to restore these calculations!
  }
  //*/
  
  
  
  /**  Evaluating the appeal and probability of invading other cities:
    */
  /*
  float casualtyValue(Base city) {
    //
    //  For now, we'll assume that the value of lives is calculated on an
    //  entirely cynical economic basis:
    float value = 0;
    value += AVG_GOOD_VALUE * (LIFESPAN_LENGTH / 2f) / GOOD_CRAFT_TIME;
    value += AVG_TAX_VALUE  * (LIFESPAN_LENGTH / 2f) / TAX_INTERVAL;
    return value;
  }
  
  
  float tributeValue(Good good, float perYear, Base city) {
    //
    //  The value of tribute is calculated based on relative supply/demand
    //  levels for a particular good:
    float value = 1;
    float need = city.trading.needLevel(good);
    float prod = city.trading.prodLevel(good);
    if (need + prod == 0) return 0;
    value *= 4 * need / (need + prod + AVG_MAX_STOCK);
    value *= good.price * perYear * AVG_TRIBUTE_YEARS;
    return value;
  }
  
  
  Tally <Good> calculateTribute(MissionAssessment a) {
    //
    //  We establish *reasonable* levels of tribute across a variety of goods,
    //  based on what the attacker is hungry for and the defender seems to have
    //  plenty of-
    Tally <Good> tribute = new Tally();
    for (Good g : base.world.goodTypes) {
      float prodVal = 5 + a.goesC.trading.inventory(g);
      prodVal      += 0 + a.goesC.trading.prodLevel(g);  //  Use prod-level!
      float consVal = 0 + a.fromC.trading.needLevel(g);
      consVal      -= 5 + a.fromC.trading.inventory(g);
      
      float grabVal = (prodVal + consVal) / 2f;
      if (grabVal <= 0) continue;
      
      grabVal *= AVG_TRIBUTE_PERCENT / 100f;
      grabVal = Nums.round(grabVal, 5, true);
      tribute.set(g, grabVal);
    }
    return tribute;
  }
  
  
  public MissionAssessment invasionAssessment(
    float attackForce, Base from, Base defend, boolean random
  ) {
    MissionAssessment MA = new MissionAssessment();
    
    MA.objective     = Mission.OBJECTIVE_STRIKE;
    MA.rulesC        = base.federation().capital;
    MA.fromC         = from;
    MA.goesC         = defend;
    MA.fromPower     = attackForce;
    MA.goesPower     = defend.armyPower() / POP_PER_CITIZEN;
    MA.postureDemand = BOND_VASSAL;
    MA.tributeDemand = calculateTribute(MA);
    
    calculateChances(MA, false);
    calculateInvasionAppeal(MA);
    
    int typeAI = base.federation().typeAI;
    if (typeAI == AI_WARLIKE ) MA.costs    = 0;
    if (typeAI == AI_PACIFIST) MA.benefits = 0;
    
    float appeal = 0;
    appeal += (random ? Rand.avgNums(2) : 0.5f) * MA.benefits;
    appeal -= (random ? Rand.avgNums(2) : 0.5f) * MA.costs;
    //appeal /= (MA.benefits + MA.costs) / 2;
    
    appeal *= ActorUtils.distanceRating(MA.rulesC, MA.goesC);
    MA.evaluatedAppeal = appeal;
    
    return MA;
  }
  
  void calculateChances(MissionAssessment a, boolean random) {
    //
    //  First, we calculate a rule-of-thumb calculation for how likely you are
    //  to win or lose, and average casualties for both sides.  (We include
    //  the prestige of each city in this calculation, as a reputation for
    //  victory can intimidate opponents.)
    float bravery = membersTraitAvg(TRAIT_BRAVERY);
    float chance = 0, lossA = 0, lossD = 0, presDiff = 0, wallDiff;
    presDiff = (
      a.rulesC.federation().relations.prestige -
      a.goesC .federation().relations.prestige
    ) / PRESTIGE_MAX;
    wallDiff = a.goesC.wallsLevel() > 0 ? 1 : 0;
    chance   = a.fromPower / (a.fromPower + a.goesPower);
    chance   = chance + (presDiff / 4);
    chance   = chance + (bravery  / 4);
    chance   = chance - (wallDiff / 4);
    chance   = Nums.clamp((chance * 2) - 0.5f, 0, 1);
    lossA    = ((random ? Rand.num() : 0.5f) + 1 - chance) / 2;
    lossD    = ((random ? Rand.num() : 0.5f) +     chance) / 2;
    //
    //  And then calculate probable casualties for both sides in each case:
    a.winChance   = chance;
    a.angerChance = 1;
    a.winKillsA   = a.fromPower * Nums.clamp(lossA - 0.25f, 0, 1);
    a.lossKillsA  = a.fromPower * Nums.clamp(lossA + 0.25f, 0, 1);
    a.winKillsD   = a.goesPower * Nums.clamp(lossD + 0.25f, 0, 1);
    a.lossKillsD  = a.goesPower * Nums.clamp(lossD - 0.25f, 0, 1);
  }
  
  
  void calculateInvasionAppeal(MissionAssessment a) {
    //
    //  We calculate the attractiveness of invasion using a relatively
    //  straightforward cost/benefit evaluation:
    float diligence  = membersTraitAvg(TRAIT_DILIGENCE);
    float compassion = membersTraitAvg(TRAIT_EMPATHY  );
    float casValueA  = casualtyValue(a.rulesC);
    float casValueD  = casualtyValue(a.goesC);
    float tribValue  = 0;
    for (Good g : a.tributeDemand.keys()) {
      tribValue += tributeValue(g, a.tributeDemand.valueFor(g), a.rulesC);
    }
    //
    //  We also weight the value of hostility, roughly speaking, based on the
    //  cost of retaliation by the opponent:
    float loseChance = 1 - a.winChance;
    float angerValue = a.goesC.population() * (casValueA + casValueD) / 2f;
    angerValue /= 4 * POP_PER_CITIZEN;
    //
    //  And account for pre-existing hostility/loyalty:
    float   loyalty  = base.relations.bondLevel(a.goesC.faction());
    boolean isLord   = a.goesC.isLordOf  (a.rulesC);
    boolean isVassal = a.goesC.isVassalOf(a.rulesC);
    a.hateBonus   = Nums.min(0, 0 - loyalty) * angerValue;
    a.lovePenalty = Nums.max(0, 0 + loyalty) * angerValue;
    a.hateBonus   *= 1 - (compassion / 2);
    a.lovePenalty *= 1 + (compassion / 2);
    if (isLord  ) a.lovePenalty *= 1 + (diligence / 2);
    if (isVassal) a.hateBonus   *= 1 - (diligence / 2);
    //
    //  Then simply tally up the pros and cons-
    a.costs    += a.winChance   * a.winKillsA  * casValueA;
    a.costs    += loseChance    * a.lossKillsA * casValueA;
    a.costs    += a.angerChance * angerValue;
    a.costs    += a.winChance   * a.lovePenalty;
    a.benefits += a.winChance   * a.winKillsD  * casValueD;
    a.benefits += loseChance    * a.lossKillsD * casValueD;
    a.benefits += a.winChance   * tribValue;
    a.benefits += a.winChance   * a.hateBonus;
  }
  
  
  
  
  //  TODO:  Appeal of a trade-relationship depends on whether mutual trade
  //  would be attractive.
  
  float appealOfTerms(
    Base from, Base goes,
    int          posture ,
    Mission      action  ,
    Actor        marriage,
    Tally <Good> tribute
  ) {
    int typeAI = base.federation().typeAI;
    if (typeAI == AI_WARLIKE ) return 0;
    if (typeAI == AI_PACIFIST) return 1000000;
    
    //  TODO:  Merge this with the assessment code below.
    
    float synergyVal = 0, dot = 0, count = 0;
    for (Focus c : goes.relations.allBondedWith(0)) {
      float valueF = from.relations.bondLevel(c);
      float valueG = goes.relations.bondLevel(c);
      synergyVal += dot = valueF * valueG;
      count += (Nums.abs(dot) + 1) / 2;
    }
    synergyVal /= Nums.max(1, count);
    
    float tradeVal = 0;
    for (Good g : base.world.goodTypes) {
      float perYear = from.trading.prodLevel(g);
      if (perYear <= 0) continue;
      tradeVal += tributeValue(g, perYear, goes);
    }
    tradeVal *= AVG_ALLIANCE_YEARS * 1f / AVG_TRIBUTE_YEARS;
    
    float powerVal = from.idealArmyPower() / POP_PER_CITIZEN;
    powerVal *= casualtyValue(goes);
    powerVal *= AVG_ALLIANCE_YEARS * 1f / AVG_RETIREMENT;
    
    float marriageVal = 0;
    if (marriage != null) {
      marriageVal = casualtyValue(goes) * MARRIAGE_VALUE_MULT;
    }
    
    float appeal = 0;
    appeal += tradeVal;
    appeal += powerVal;
    appeal += marriageVal;
    appeal *= 1 + synergyVal;
    
    return appeal;
  }
  
  
  public MissionAssessment dialogAssessment(
    Base from, Base goes, boolean random
  ) {
    MissionAssessment MA = new MissionAssessment();
    int typeAI = base.federation().typeAI;
    
    MA.objective = Mission.OBJECTIVE_CONTACT;
    MA.fromC = from;
    MA.goesC = goes;
    
    if (typeAI == AI_WARLIKE) {
      MA.evaluatedAppeal = -1;
      return MA;
    }
    
    //  See if it's possible to arrange a marriage as well.
    
    Actor monarch = goes.council.memberWithRole(BaseCouncil.Role.MONARCH);
    Pick <Actor> pickM = new Pick();
    
    for (Actor a : from.council.allMembersWithRole(BaseCouncil.Role.HEIR)) {
      Actor spouse = (Actor) a.bonds.bondedWith(BOND_MARRIED);
      if (spouse != null) continue;
      if (monarch.health.man() == a.health.man()) continue;
      
      float rating = 1.0f;
      if (random) rating += Rand.num() - 0.5f;
      pickM.compare(a, rating);
    }
    Actor marries = pickM.result();
    
    MA.postureDemand  = BOND_ALLY;
    MA.marriageDemand = marries;
    
    //  Appeal of alliance depends on whether you have a good existing
    //  relationship and/or share similar enemies.
    
    //I.say("\nGetting synergy between "+from+" and "+goes);
    float synergyVal = 0, dot = 0, count = 0;
    for (Focus f : goes.relations.allBondedWith(0)) {
      //if (! f.type().isFaction()) continue;
      float valueF = from.relations.bondLevel(f);
      float valueG = goes.relations.bondLevel(f);
      //I.say("  "+c+": "+valueF+" * "+valueG);
      if (valueF == -100 || valueG == -100) continue;
      synergyVal += dot = valueF * valueG;
      count += (Nums.abs(dot) + 1) / 2;
    }
    synergyVal /= Nums.max(1, count);
    //I.say("  Total value: "+synergyVal);
    
    float tradeVal = 0;
    for (Good g : base.world.goodTypes) {
      float exports = from.trading.prodLevel(g);
      if (exports > 0) {
        tradeVal += tributeValue(g, exports, goes);
      }
      float imports = goes.trading.needLevel(g);
      if (imports > 0) {
        tradeVal += tributeValue(g, exports, from);
      }
    }
    tradeVal *= AVG_ALLIANCE_YEARS * 1f / AVG_TRIBUTE_YEARS;
    
    //I.say("  Trade value: "+tradeVal);
    
    float powerVal = from.idealArmyPower() / POP_PER_CITIZEN;
    powerVal *= casualtyValue(goes);
    powerVal *= AVG_ALLIANCE_YEARS * 1f / AVG_RETIREMENT;
    
    float marriageCost = 0;
    if (marries != null) {
      marriageCost = casualtyValue(from) * MARRIAGE_VALUE_MULT;
      marriageCost *= (1 + from.council.membersBondAvg(marries)) / 2;
    }
    
    float relationsVal = (
      goes.relations.bondLevel(from.faction()) +
      from.relations.bondLevel(goes.faction())
    ) / 2;
    
    MA.benefits += tradeVal;
    MA.benefits += powerVal;
    MA.benefits *= 1 + synergyVal + relationsVal;
    MA.costs    += marriageCost;
    
    float appeal = 0;
    appeal += (random ? Rand.avgNums(2) : 0.5f) * MA.benefits;
    appeal -= (random ? Rand.avgNums(2) : 0.5f) * MA.costs;
    //appeal /= (MA.benefits + MA.costs) / 2;
    appeal *= ActorUtils.distanceRating(MA.rulesC, MA.goesC);
    MA.evaluatedAppeal = appeal;
    
    return MA;
  }
  
  
  List <MissionAssessment> updateMissionChoices() {
    List <MissionAssessment> choices = new List();
    //
    //  This is something of a hack at the moment, but it helps prevent some
    //  of the more bitty exchanges...
    //float power = factionPower(), idealPower = idealFactionPower();
    float power = base.armyPower(), idealPower = base.idealArmyPower();
    if (power < idealPower / 3) {
      return choices;
    }
    
    //  TODO:  Ideally, you should be spreading out your forces over a wider
    //  front- enough so that on whatever map the player is located, they can
    //  expect to see some resistance.
    
    //  And then you can have occasional incursions against other off-map
    //  bases.
    Faction faction = base.faction();
    Base capital = base.federation().capital;
    
    //
    //  Then check where to send an invasion force-
    for (Base other : base.world.bases) if (other.faction() != faction) {
      
      //  TODO:  You need to check for path-access!
      //float distance = base.distance(other, Type.MOVE_LAND);
      //if (distance < 0) continue;
      
      MissionAssessment IA = invasionAssessment(power, capital, other, true);
      choices.add(IA);
      
      MissionAssessment DA = dialogAssessment(capital, other, true);
      choices.add(DA);
    }
    return choices;
  }
  
  
  /*
  float factionPower() {
    float sum = 0;
    for (Base b : world.bases) if (b.faction() == faction) {
      sum += b.armyPower();
    }
    return sum;
  }
  
  
  float idealFactionPower() {
    float sum = 0;
    for (Base b : world.bases) if (b.faction() == faction) {
      sum += b.idealArmyPower();
    }
    return sum;
  }
  //*/
  
  
  /*
  public Mission spawnFormation(MissionAssessment IA, Base base) {
    
    Mission force = null;
    if (IA.objective == OBJECTIVE_STRIKE ) force = new MissionForStrike (base);
    if (IA.objective == OBJECTIVE_SECURE ) force = new MissionForSecure (base);
    if (IA.objective == OBJECTIVE_RECON  ) force = new MissionForRecon  (base);
    if (IA.objective == OBJECTIVE_CONTACT) force = new MissionForContact(base);
    if (force == null) return null;
    
    Type soldier = (Type) Visit.first(base.world.soldierTypes);
    Type noble   = (Type) Visit.first(base.world.nobleTypes  );
    
    while (MissionForStrike.powerSum(force.recruits(), null) < base.armyPower() / 2) {
      Actor fights = (Actor) soldier.generate();
      fights.assignBase(IA.rulesC);
      force.toggleRecruit(fights, true);
    }
    
    GOVERNMENT government = base.federation().government;
    if (government == GOVERNMENT.BARBARIAN) {
      //  Only non-barbarian governments will set up permanent command-fx or
      //  attempt diplomacy...
    }
    else {
      force.terms.assignTerms(
        IA.postureDemand,
        IA.actionDemand,
        IA.marriageDemand,
        IA.tributeDemand
      );
      if (IA.marriageDemand != null) {
        Actor marries = IA.marriageDemand;
        marries.assignBase(base);
        force.toggleEnvoy(marries, true);
      }
      if (IA.objective == Mission.OBJECTIVE_CONTACT) {
        while (force.envoys.size() < 2) {
          Actor talks = (Actor) noble.generate();
          talks.assignBase(base);
          force.toggleEnvoy(talks, true);
        }
      }
    }
    
    force.setWorldFocus(IA.goesC);
    
    return force;
  }
  
  
  boolean considerRevolt(Faction faction, int period, Base base) {
    int typeAI = base.federation().typeAI;
    if (typeAI == AI_DEFIANT  ) return true ;
    if (typeAI == AI_COMPLIANT) return false;
    if (typeAI == AI_OFF      ) return false;
    
    Base capital = base.federation().capital;
    MissionAssessment IA = invasionAssessment(base.armyPower(), base, capital, true);
    float chance = period * 1f / AVG_TRIBUTE_YEARS;
    return IA.evaluatedAppeal > 0 && Rand.num() < chance;
  }
  //*/
  

  
  //  TODO:  Restore this...
  
  boolean considerRevolt(Faction faction, int period, Base base) {
    return false;
  }
  
}













