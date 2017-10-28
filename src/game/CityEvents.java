

package game;
import util.*;
import static game.City.*;
import static game.GameConstants.*;



public class CityEvents {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final City city;
  
  
  CityEvents(City city) {
    this.city = city;
  }
  
  
  void loadState(Session s) throws Exception {
    return;
  }
  
  
  void saveState(Session s) throws Exception {
    return;
  }
  
  
  
  /**  Evaluating the appeal and probability of invading other cities:
    */
  static class InvasionAssessment {
    
    City attackC, defendC;
    float attackPower, defendPower;
    Tally <Good> tribute = new Tally();
    
    float winChance, angerChance;
    float winKillsA, lossKillsA;
    float winKillsD, lossKillsD;
    float hateBonus, lovePenalty;
    
    float costs, benefits;
    float evaluatedAppeal;
  }
  
  
  void calculateTribute(InvasionAssessment a) {
    //
    //  We establish *reasonable* levels of tribute across a variety of goods,
    //  based on what the attacker is hungry for and the defender seems to have
    //  plenty of-
    for (Good g : ALL_GOODS) {
      float prodVal = 5 + a.defendC.inventory .valueFor(g);
      prodVal      += 0 + a.defendC.tradeLevel.valueFor(g);
      float consVal = 0 - a.attackC.tradeLevel.valueFor(g);
      consVal      -= 5 + a.attackC.inventory .valueFor(g);
      
      float grabVal = (prodVal + consVal) / 2f;
      if (grabVal <= 0) continue;
      
      grabVal *= AVG_TRIBUTE_PERCENT / 100f;
      grabVal = Nums.round(grabVal, 5, true);
      a.tribute.set(g, grabVal);
    }
  }
  
  
  void calculateChances(InvasionAssessment a, boolean random) {
    //
    //  First, we calculate a rule-of-thumb calculation for how likely you are
    //  to win or lose, and average casualties for both sides:
    float chance = 0, lossA = 0, lossD = 0;
    chance = a.attackPower / (a.attackPower + a.defendPower);
    chance = Nums.clamp((chance * 2) - 0.5f, 0, 1);
    lossA  = ((random ? Rand.num() : 0.5f) + 1 - chance) / 2;
    lossD  = ((random ? Rand.num() : 0.5f) +     chance) / 2;
    //
    //  And then calculate probable casualties for both sides in each case:
    a.winChance   = chance;
    a.angerChance = 1;
    a.winKillsA   = a.attackPower * Nums.clamp(lossA - 0.25f, 0, 1);
    a.lossKillsA  = a.attackPower * Nums.clamp(lossA + 0.25f, 0, 1);
    a.winKillsD   = a.attackPower * Nums.clamp(lossD + 0.25f, 0, 1);
    a.lossKillsD  = a.attackPower * Nums.clamp(lossD - 0.25f, 0, 1);
  }
  
  
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
  
  
  void calculateAppeal(InvasionAssessment a) {
    //
    //  We calculate the attractiveness of invasion using a relatively
    //  straightforward cost/benefit evaluation:
    float casValueA = casualtyValue(a.attackC);
    float casValueD = casualtyValue(a.defendC);
    float tribValue = 0;
    for (Good g : a.tribute.keys()) {
      tribValue += tributeValue(g, a.tribute.valueFor(g), a.attackC);
    }
    //
    //  We also weight the value of hostility, roughly speaking, based on the
    //  cost of retaliation by the opponent:
    float loseChance = 1 - a.winChance;
    float angerValue = a.defendC.population * (casValueA + casValueD) / 2f;
    angerValue /= 4 * POP_PER_CITIZEN;
    //
    //  And account for pre-existing hostility/loyalty:
    City.Relation r = a.attackC.relationWith(a.defendC);
    a.hateBonus   = Nums.min(0, r.loyalty) * angerValue;
    a.lovePenalty = Nums.max(0, r.loyalty) * angerValue;
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
  
  
  InvasionAssessment performAssessment(
    City attack, City defend,
    float commitLevel, boolean random
  ) {
    InvasionAssessment IA = new InvasionAssessment();
    IA.attackC     = attack;
    IA.defendC     = defend;
    IA.attackPower = attack.armyPower * commitLevel / POP_PER_CITIZEN;
    IA.defendPower = defend.armyPower               / POP_PER_CITIZEN;
    calculateTribute(IA);
    calculateChances(IA, false);
    calculateAppeal(IA);
    
    float appeal = 0;
    appeal += (random ? Rand.avgNums(2) : 0.5f) * IA.benefits;
    appeal -= (random ? Rand.avgNums(2) : 0.5f) * IA.costs;
    appeal /= (IA.benefits + IA.costs) / 2;
    appeal *= CityBorders.distanceRating(IA.attackC, IA.defendC);
    IA.evaluatedAppeal = appeal;
    
    return IA;
  }
  
  
  List <InvasionAssessment> updateInvasionChoices() {
    List <InvasionAssessment> choices = new List();
    //
    //  TODO:  Allow for multiple levels of force-commitment, since you don't
    //  want your own city to be vulnerable?
    
    for (City other : city.world.cities) {
      Integer distance = city.distances.get(other);
      if (distance == null || other == city) continue;
      if (other.isLoyalVassalOf   (city)   ) continue;
      if (other.isVassalOfSameLord(city)   ) continue;
      
      InvasionAssessment IA = performAssessment(city, other, 0.5f, true);
      choices.add(IA);
    }
    return choices;
  }
  
  
  Formation spawnInvasion(InvasionAssessment IA) {
    
    Formation force = new Formation();
    force.setupFormation(GARRISON, city);
    
    int n = 0;
    while (force.formationPower() < city.armyPower / 2) {
      Type  type   = (n++ % 4 == 0) ? SOLDIER : CITIZEN;
      Actor fights = (Actor) type.generate();
      fights.assignHomeCity(IA.attackC);
      force.toggleRecruit(fights, true);
    }
    
    return force;
  }
  
  
  void updateEvents() {
    //
    //  Once per month, otherwise, evaluate any likely prospects for invasion:
    if (city.world.time % MONTH_LENGTH == 0) {
      Pick <InvasionAssessment> pick = new Pick(0);
      for (InvasionAssessment IA : updateInvasionChoices()) {
        pick.compare(IA, IA.evaluatedAppeal);
      }
      //
      //  If any have positive appeal, launch the enterprise:
      InvasionAssessment IA = pick.result();
      if (IA != null) {
        Formation force = spawnInvasion(IA);
        World.Journey j = force.beginJourney(IA.attackC, IA.defendC);
        handleDeparture(force, IA.defendC, j);
      }
    }
  }
  
  
  boolean considerRevolt(City lord) {
    InvasionAssessment IA = performAssessment(city, lord, 0.5f, true);
    return IA.evaluatedAppeal > 0 && Rand.index(AVG_TRIBUTE_YEARS) == 0;
  }
  
  
  
  /**  Handling end-stage events:
    */
  static void handleDeparture(
    Formation formation, City goes, World.Journey journey
  ) {
    City belongs = formation.belongs;
    belongs.armyPower -= formation.formationPower();
    belongs.formations.add(formation);
  }
  
  
  static void handleInvasion(
    Formation formation, City goes, World.Journey journey
  ) {
    InvasionAssessment IA = new InvasionAssessment();
    City  from  = journey.from;
    IA.attackC     = from;
    IA.defendC     = goes;
    IA.attackPower = formation.formationPower() / POP_PER_CITIZEN;
    IA.defendPower = goes.armyPower / POP_PER_CITIZEN;
    
    goes.events.calculateChances(IA, true);
    
    float chance = IA.winChance, fromLost = 0, goesLost = 0;
    boolean victory = false;
    
    if (Rand.num() < chance) {
      fromLost = IA.winKillsA;
      goesLost = IA.winKillsD;
      victory  = true;
    }
    else {
      fromLost = IA.lossKillsA;
      goesLost = IA.winKillsD;
      victory  = false;
    }
    
    fromLost = inflictCasualties(formation, fromLost);
    goesLost = inflictCasualties(goes     , goesLost);
    
    I.say("\n"+formation+" CONDUCTED ACTION AGAINST "+goes);
    I.say("  Victorious:       "+victory );
    I.say("  Took losses:      "+fromLost);
    I.say("  Inflicted losses: "+goesLost);
    I.say("  Home city now:    "+from.posture(goes)+" of "+goes);
    
    //
    //  We assume/pretend that barbarian factions won't set up political ties-
    if (victory && from.government != GOVERNMENT.BARBARIAN) {
      inflictVassalStatus(goes, from, formation);
    }
    //
    //  TODO:  Handle recall of forces in a separate decision-pass?
    formation.stopSecuringPoint();
    goes.world.beginJourney(goes, from, formation);
  }
  
  
  static int inflictCasualties(Formation formation, float casualties) {
    int numFought = formation.recruits.size();
    if (numFought == 0) return 0;
    
    for (float i = Nums.min(numFought, casualties); i-- > 0;) {
      Actor lost = (Actor) Rand.pickFrom(formation.recruits);
      formation.toggleRecruit(lost, false);
    }
    return (int) casualties;
  }
  
  
  static int inflictCasualties(City defends, float casualties) {
    casualties = Nums.min(casualties, defends.armyPower);
    defends.armyPower  -= casualties * POP_PER_CITIZEN;
    defends.population -= casualties * POP_PER_CITIZEN;
    return (int) casualties;
  }
  
  
  static void inflictVassalStatus(
    City defends, City attacks, Formation formation
  ) {
    int tributeTime = defends.world.time + YEAR_LENGTH;
    setPosture(defends, attacks, formation.postureDemand);
    setSuppliesDue(defends, attacks, formation.tributeDemand, tributeTime);
  }
  
  
  static void handleReturn(
    Formation formation, City from, World.Journey journey
  ) {
    City belongs = formation.belongs;
    belongs.armyPower += formation.formationPower();
    belongs.formations.remove(formation);
  }
}













