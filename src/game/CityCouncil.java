

package game;
import util.*;
import static game.ActorAsPerson.*;
import static game.City.*;
import static game.GameConstants.*;



public class CityCouncil {
  
  
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
  
  
  final City city;
  private int typeAI = AI_NORMAL;
  
  private List <Actor> members = new List();
  private Table <Actor, Role> roles = new Table();
  
  private List <Mission> petitions = new List();
  
  
  CityCouncil(City city) {
    this.city = city;
  }
  
  
  void loadState(Session s) throws Exception {
    typeAI = s.loadInt();
    
    for (int n = s.loadInt(); n-- > 0;) {
      Actor a = (Actor) s.loadObject();
      Role r = (Role) s.loadEnum(Role.values());
      members.add(a);
      roles.put(a, r);
    }
    
    s.loadObjects(petitions);
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveInt(typeAI);
    
    s.saveInt(members.size());
    for (Actor a : members) {
      s.saveObject(a);
      s.saveEnum(roles.get(a));
    }
    
    s.saveObjects(petitions);
  }
  
  
  
  /**  Toggle membership of the council and handling personality-effects-
    */
  public void setTypeAI(int typeAI) {
    this.typeAI = typeAI;
  }
  
  
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
      avg += m.levelOf(trait);
      count += 1;
    }
    return avg / Nums.max(1, count);
  }
  
  
  public float membersBondAvg(Actor with) {
    float avg = 0, count = 0;
    for (Actor m : members) {
      avg += m.bondLevel(with);
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
    petition.setTermsAccepted(true);
    petitions.remove(petition);
  }
  
  
  public void rejectTerms(Mission petition) {
    petition.setTermsAccepted(false);
    petitions.remove(petition);
  }
  
  
  
  /**  Regular updates-
    */
  public static class MissionAssessment {
    
    City fromC, goesC;
    float fromPower, goesPower;
    
    int objective;
    POSTURE      postureDemand  = null;
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
    
    public City from() { return fromC; }
    public City goes() { return goesC; }
    public float appeal() { return evaluatedAppeal; }
  }
  
  
  void updateCouncil(boolean onMap) {
    //
    //  Check on any current heirs and/or marriage status-
    Actor monarch = memberWithRole(Role.MONARCH);
    if (monarch != null) {
      for (Actor consort : monarch.allBondedWith(BOND_MARRIED)) {
        toggleMember(consort, Role.CONSORT, true);
      }
      for (Actor heir : monarch.allBondedWith(BOND_CHILD)) {
        toggleMember(heir, Role.HEIR, true);
      }
    }
    //
    //  For now, we'll assume succession is determined by quasi-hereditary-
    //  male-line-primogeniture with a dash of popularity contest.  Might allow
    //  for customisation later.
    if (monarch == null || monarch.dead()) {
      toggleMember(monarch, Role.MONARCH, false);
      Pick <Actor> pick = new Pick();
      for (Actor a : members) {
        Role r = roles.get(a);
        float rating = 1 + membersBondAvg(a);
        if (r == Role.HEIR) rating *= 3;
        if (a.woman()) rating /= 2;
        rating *= 1 + (a.ageYears() / AVG_RETIREMENT);
        pick.compare(a, rating);
      }
      //
      //  The king is dead, long live the king-
      Actor newMonarch = pick.result();
      if (newMonarch != null) toggleMember(newMonarch, Role.MONARCH, true);
    }
    //
    //  And remove any other dead council-members:
    for (Actor a : members) {
      Role r = roles.get(a);
      if (a.dead()) toggleMember(a, r, false);
    }
    //
    //  We annul any independent decision-making if AI is toggled off-
    if (typeAI == AI_OFF || onMap) return;
    //
    //  Once per month, otherwise, evaluate any major independent decisions-
    if (city.world.time % (MONTH_LENGTH / 2) == 0) {
      updateCouncilAI();
    }
  }
  
  
  void updateCouncilAI() {
    
    for (Mission petition : petitions) {
      float appeal = appealOfTerms(
        petition.homeCity(), city,
        petition.postureDemand,
        petition.actionDemand,
        petition.marriageDemand,
        petition.tributeDemand
      );
      if (Rand.num() < appeal) {
        petition.setTermsAccepted(true);
      }
      else {
        petition.setTermsAccepted(false);
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
      Mission force = spawnFormation(IA);
      CityEvents.handleDeparture(force, IA.fromC, IA.goesC);
    }
  }
  
  
  
  /**  Evaluating the appeal and probability of invading other cities:
    */
  float casualtyValue(City city) {
    //
    //  For now, we'll assume that the value of lives is calculated on an
    //  entirely cynical economic basis:
    float value = 0;
    value += AVG_GOOD_VALUE * (LIFESPAN_LENGTH / 2f) / AVG_CRAFT_TIME;
    value += AVG_TAX_VALUE  * (LIFESPAN_LENGTH / 2f) / TAX_INTERVAL  ;
    return value;
  }
  
  
  float tributeValue(Good good, float perYear, City city) {
    //
    //  The value of tribute is calculated based on relative supply/demand
    //  levels for a particular good:
    float value = 1;
    if (city.tradeLevel.valueFor(good) > 0) value /= 2;
    if (city.tradeLevel.valueFor(good) < 0) value *= 2;
    value *= good.price * perYear * AVG_TRIBUTE_YEARS;
    return value;
  }
  
  
  Tally <Good> calculateTribute(MissionAssessment a) {
    //
    //  We establish *reasonable* levels of tribute across a variety of goods,
    //  based on what the attacker is hungry for and the defender seems to have
    //  plenty of-
    Tally <Good> tribute = new Tally();
    for (Good g : city.world.goodTypes) {
      float prodVal = 5 + a.goesC.inventory .valueFor(g);
      prodVal      += 0 + a.goesC.tradeLevel.valueFor(g);
      float consVal = 0 - a.fromC.tradeLevel.valueFor(g);
      consVal      -= 5 + a.fromC.inventory .valueFor(g);
      
      float grabVal = (prodVal + consVal) / 2f;
      if (grabVal <= 0) continue;
      
      grabVal *= AVG_TRIBUTE_PERCENT / 100f;
      grabVal = Nums.round(grabVal, 5, true);
      tribute.set(g, grabVal);
    }
    return tribute;
  }
  
  
  void calculateChances(MissionAssessment a, boolean random) {
    //
    //  First, we calculate a rule-of-thumb calculation for how likely you are
    //  to win or lose, and average casualties for both sides.  (We include
    //  the prestige of each city in this calculation, as a reputation for
    //  victory can intimidate opponents.)
    float bravery = membersTraitAvg(TRAIT_BRAVERY);
    float chance = 0, lossA = 0, lossD = 0, presDiff = 0, wallDiff;
    presDiff = (a.fromC.prestige - a.goesC.prestige) / PRESTIGE_MAX;
    wallDiff = a.goesC.wallsLevel() > 0 ? 1 : 0;
    chance   = a.fromPower / (a.fromPower + a.goesPower);
    chance   = chance + (presDiff / 4);
    chance   = chance + (bravery  / 4);
    chance   = chance - (wallDiff / 4);
    chance   = Nums.clamp((chance * 2) - 0.5f, 0, 1);
    lossA    = ((random ? Rand.num() : 0.5f) + 1 - chance) / 2;
    lossD    = ((random ? Rand.num() : 0.5f) +     chance) / 2;
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
    float diligence  = membersTraitAvg(TRAIT_DILIGENCE );
    float compassion = membersTraitAvg(TRAIT_COMPASSION);
    float casValueA  = casualtyValue(a.fromC);
    float casValueD  = casualtyValue(a.goesC);
    float tribValue  = 0;
    for (Good g : a.tributeDemand.keys()) {
      tribValue += tributeValue(g, a.tributeDemand.valueFor(g), a.fromC);
    }
    //
    //  We also weight the value of hostility, roughly speaking, based on the
    //  cost of retaliation by the opponent:
    float loseChance = 1 - a.winChance;
    float angerValue = a.goesC.population() * (casValueA + casValueD) / 2f;
    angerValue /= 4 * POP_PER_CITIZEN;
    //
    //  And account for pre-existing hostility/loyalty:
    Relation r       = a.fromC.relationWith(a.goesC);
    boolean isLord   = a.goesC.isLordOf    (a.fromC);
    boolean isVassal = a.goesC.isVassalOf  (a.fromC);
    a.hateBonus   = Nums.min(0, 0 - r.loyalty) * angerValue;
    a.lovePenalty = Nums.max(0, 0 + r.loyalty) * angerValue;
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
  
  
  public MissionAssessment invasionAssessment(
    City attack, City defend,
    float commitLevel, boolean random
  ) {
    MissionAssessment MA = new MissionAssessment();
    
    MA.objective = Mission.OBJECTIVE_CONQUER;
    MA.fromC     = attack;
    MA.goesC     = defend;
    MA.fromPower = attack.armyPower() * commitLevel / POP_PER_CITIZEN;
    MA.goesPower = defend.armyPower()               / POP_PER_CITIZEN;
    
    MA.postureDemand = POSTURE.VASSAL;
    MA.tributeDemand = calculateTribute(MA);
    
    calculateChances(MA, false);
    calculateInvasionAppeal(MA);
    
    if (typeAI == AI_WARLIKE ) MA.costs    = 0;
    if (typeAI == AI_PACIFIST) MA.benefits = 0;
    
    float appeal = 0;
    appeal += (random ? Rand.avgNums(2) : 0.5f) * MA.benefits;
    appeal -= (random ? Rand.avgNums(2) : 0.5f) * MA.costs;
    //appeal /= (MA.benefits + MA.costs) / 2;
    appeal *= CityBorders.distanceRating(MA.fromC, MA.goesC);
    MA.evaluatedAppeal = appeal;
    
    return MA;
  }
  
  
  
  //  TODO:  Appeal of a trade-relationship depends on whether mutual trade
  //  would be attractive.
  
  float appealOfTerms(
    City from, City goes,
    POSTURE      posture ,
    Mission      action  ,
    Actor        marriage,
    Tally <Good> tribute
  ) {
    if (typeAI == AI_WARLIKE ) return 0;
    if (typeAI == AI_PACIFIST) return 1000000;
    
    //  TODO:  Merge this with the assessment code below.
    
    float synergyVal = 0, dot = 0, count = 0;
    for (City c : goes.relationsWith()) {
      float valueF = from.loyalty(c);
      float valueG = goes.loyalty(c);
      synergyVal += dot = valueF * valueG;
      count += (Nums.abs(dot) + 1) / 2;
    }
    synergyVal /= Nums.max(1, count);
    
    float tradeVal = 0;
    for (Good g : city.world.goodTypes) {
      float perYear = from.tradeLevel.valueFor(g);
      if (perYear <= 0) continue;
      tradeVal += tributeValue(g, perYear, city);
    }
    tradeVal *= AVG_ALLIANCE_YEARS * 1f / AVG_TRIBUTE_YEARS;
    
    float powerVal = from.idealArmyPower() / POP_PER_CITIZEN;
    powerVal *= casualtyValue(city);
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
    City from, City goes, boolean random
  ) {
    MissionAssessment MA = new MissionAssessment();
    
    MA.objective = Mission.OBJECTIVE_DIALOG;
    MA.fromC = from;
    MA.goesC = goes;
    
    if (typeAI == AI_WARLIKE) {
      MA.evaluatedAppeal = -1;
      return MA;
    }
    
    //  See if it's possible to arrange a marriage as well.
    
    Actor monarch = goes.council.memberWithRole(CityCouncil.Role.MONARCH);
    Pick <Actor> pickM = new Pick();
    
    for (Actor a : from.council.allMembersWithRole(CityCouncil.Role.HEIR)) {
      Actor spouse = a.allBondedWith(BOND_MARRIED).first();
      if (spouse != null) continue;
      if (monarch.sexData == a.sexData) continue;
      
      float rating = 1.0f;
      if (random) rating += Rand.num() - 0.5f;
      pickM.compare(a, rating);
    }
    Actor marries = pickM.result();
    
    MA.postureDemand  = POSTURE.ALLY;
    MA.marriageDemand = marries;
    
    //  Appeal of alliance depends on whether you have a good existing
    //  relationship and/or share similar enemies.
    
    //I.say("\nGetting synergy between "+from+" and "+goes);
    float synergyVal = 0, dot = 0, count = 0;
    for (City c : goes.relationsWith()) {
      float valueF = from.loyalty(c);
      float valueG = goes.loyalty(c);
      //I.say("  "+c+": "+valueF+" * "+valueG);
      if (valueF == -100 || valueG == -100) continue;
      synergyVal += dot = valueF * valueG;
      count += (Nums.abs(dot) + 1) / 2;
    }
    synergyVal /= Nums.max(1, count);
    //I.say("  Total value: "+synergyVal);
    
    float tradeVal = 0;
    for (Good g : city.world.goodTypes) {
      float exports = from.tradeLevel.valueFor(g);
      if (exports > 0) {
        tradeVal += tributeValue(g, exports, goes);
      }
      float imports = goes.tradeLevel.valueFor(g);
      if (imports > 0) {
        tradeVal += tributeValue(g, exports, from);
      }
    }
    tradeVal *= AVG_ALLIANCE_YEARS * 1f / AVG_TRIBUTE_YEARS;
    
    //I.say("  Trade value: "+tradeVal);
    
    float powerVal = from.idealArmyPower() / POP_PER_CITIZEN;
    powerVal *= casualtyValue(city);
    powerVal *= AVG_ALLIANCE_YEARS * 1f / AVG_RETIREMENT;
    
    float marriageCost = 0;
    if (marries != null) {
      marriageCost = casualtyValue(from) * MARRIAGE_VALUE_MULT;
      marriageCost *= (1 + from.council.membersBondAvg(marries)) / 2;
    }
    
    float relationsVal = 0;
    relationsVal = (goes.loyalty(from) + from.loyalty(goes)) / 2;
    
    MA.benefits += tradeVal;
    MA.benefits += powerVal;
    MA.benefits *= 1 + synergyVal + relationsVal;
    MA.costs    += marriageCost;
    
    float appeal = 0;
    appeal += (random ? Rand.avgNums(2) : 0.5f) * MA.benefits;
    appeal -= (random ? Rand.avgNums(2) : 0.5f) * MA.costs;
    //appeal /= (MA.benefits + MA.costs) / 2;
    appeal *= CityBorders.distanceRating(MA.fromC, MA.goesC);
    MA.evaluatedAppeal = appeal;
    
    return MA;
  }
  
  
  List <MissionAssessment> updateMissionChoices() {
    List <MissionAssessment> choices = new List();
    //
    //  This is something of a hack at the moment, but it helps prevent some
    //  of the more bitty exchanges...
    if (city.armyPower() < AVG_ARMY_POWER / 2) {
      return choices;
    }
    //
    //  TODO:  Allow for multiple levels of force-commitment, since you don't
    //  want your own city to be vulnerable?  And multiple options for terms
    //  during diplomacy?
    for (City other : city.world.cities) if (other != city) {
      Integer distance = city.locale.distances.get(other.locale);
      if (distance == null) continue;
      
      if (! (
        other.isLoyalVassalOf(city) ||
        other.isVassalOfSameLord(city)
      )) {
        MissionAssessment IA = invasionAssessment(city, other, 0.5f, true);
        choices.add(IA);
      }
      
      if (! (
        other.isLordOf(city) ||
        other.isAllyOf(city)
      )) {
        MissionAssessment DA = dialogAssessment(city, other, true);
        choices.add(DA);
      }
      
    }
    return choices;
  }
  
  
  public Mission spawnFormation(MissionAssessment IA) {
    Mission force = new Mission(IA.objective, city, true);
    
    Type citizen = (Type) Visit.first(city.world.citizenTypes);
    Type soldier = (Type) Visit.first(city.world.soldierTypes);
    Type noble   = (Type) Visit.first(city.world.nobleTypes  );
    
    int n = 0;
    while (force.powerSum() < city.armyPower() / 2) {
      Type  type   = (n++ % 4 == 0) ? soldier : citizen;
      Actor fights = (Actor) type.generate();
      fights.assignHomeCity(IA.fromC);
      force.toggleRecruit(fights, true);
    }
    
    if (city.government == GOVERNMENT.BARBARIAN) {
      //  Only non-barbarian governments will set up permanent command-fx or
      //  attempt diplomacy...
    }
    else {
      force.assignTerms(
        IA.postureDemand,
        IA.actionDemand,
        IA.marriageDemand,
        IA.tributeDemand
      );
      if (IA.marriageDemand != null) {
        Actor marries = IA.marriageDemand;
        marries.assignHomeCity(city);
        force.toggleEscorted(marries, true);
      }
      if (IA.objective == Mission.OBJECTIVE_DIALOG) {
        while (force.escorted.size() < 2) {
          Actor talks = (Actor) noble.generate();
          talks.assignHomeCity(city);
          force.toggleEscorted(talks, true);
        }
      }
    }
    
    return force;
  }
  
  
  boolean considerRevolt(City lord, int period) {
    if (typeAI == AI_DEFIANT  ) return true ;
    if (typeAI == AI_COMPLIANT) return false;
    if (typeAI == AI_OFF      ) return false;
    
    MissionAssessment IA = invasionAssessment(city, lord, 0.5f, true);
    float chance = period * 1f / AVG_TRIBUTE_YEARS;
    return IA.evaluatedAppeal > 0 && Rand.num() < chance;
  }
  
}




