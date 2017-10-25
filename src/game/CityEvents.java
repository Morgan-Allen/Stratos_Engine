

package game;
import util.*;
import static game.City.*;
import static game.GameConstants.*;


/*
  The baseline chance of invasion, for a city of 1000 pop (40 walkers) in one
  year, by another bordering city with a city of 1000 people, assuming that the
  latter has a standing military of 200 people and the former is defenceless,
  when both have equal wealth, is 100%.
  
  If your militaries are equal, the chance drops to maybe 25%.  If you have
  double their strength, then the chance of invasion drops very low.
  
  If one city is especially wealthy in something the other needs, then the
  temptation to invade increases.
//*/


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
    
    float costs, benefits;
    float evaluatedAppeal;
  }
  
  
  void calculateTribute(InvasionAssessment a) {
    final int AVG_TRIBUTE_PERCENT = 25;
    //
    //  We establish *reasonable* levels of tribute across a variety of goods,
    //  based on what the attacker is hungry for and the defender seems to have
    //  plenty of-
    for (Good g : ALL_GOODS) {
      float prodVal = 5 + a.defendC.inventory .valueFor(g);
      prodVal      += 0 + a.defendC.tradeLevel.valueFor(g);
      float consVal = 0 - a.attackC.tradeLevel.valueFor(g);
      consVal      -= 5 + a.attackC.inventory .valueFor(g);
      
      float grabVal = Nums.max(prodVal, consVal);
      if (grabVal <= 0) continue;
      
      grabVal *= AVG_TRIBUTE_PERCENT / 100f;
      grabVal = Nums.round(grabVal, 5, true);
      a.tribute.set(g, grabVal);
    }
  }
  
  
  void calculateChances(InvasionAssessment a) {
    //
    //  First, we calculate a rule-of-thumb calculation for how likely you are
    //  to win or lose, and average casualties for both sides:
    float chance = 0, lossA = 0, lossD = 0;
    chance = a.attackPower / (a.attackPower + a.defendPower);
    chance = Nums.clamp((chance * 2) - 0.5f, 0, 1);
    lossA  = (0.5f + 1 - chance) / 2;
    lossD  = (0.5f +     chance) / 2;
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
    final int AVG_TRIBUTE_YEARS = 10;
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
    //  cost of retaliation by the opponent's army:
    float loseChance = 1 - a.winChance;
    float angerValue = a.defendC.armyPower * (casValueA + casValueD) / 4;
    //
    //  Then simply tally up the pros and cons, and return:
    a.costs    += a.winChance   * a.winKillsA  * casValueA;
    a.costs    += loseChance    * a.lossKillsA * casValueA;
    a.costs    += a.angerChance * angerValue;
    a.benefits += a.winChance   * a.winKillsD  * casValueD;
    a.benefits += loseChance    * a.lossKillsD * casValueD;
    a.benefits += a.winChance   * tribValue;
  }
  
  
  InvasionAssessment performAssessment(
    City attack, City defend,
    float commitLevel
  ) {
    InvasionAssessment IA = new InvasionAssessment();
    IA.attackC     = attack;
    IA.defendC     = defend;
    IA.attackPower = attack.armyPower * commitLevel;
    IA.defendPower = defend.armyPower;
    calculateTribute(IA);
    calculateChances(IA);
    calculateAppeal (IA);
    return IA;
  }
  
  
  List <InvasionAssessment> updateInvasionChoices() {
    List <InvasionAssessment> choices = new List();
    //
    //  TODO:  Allow for multiple levels of force-commitment, since you don't
    //  want your own city to be vulnerable.
    //  TODO:  Also, take travel times into account- that attenuates the
    //  benefit to be gained.
    
    for (City other : city.world.cities) {
      if (other == city) continue;
      InvasionAssessment IA = performAssessment(city, other, 0.5f);
      
      float appeal = 0;
      appeal += Rand.avgNums(2) * IA.benefits;
      appeal -= Rand.avgNums(2) * IA.costs;
      
      IA.evaluatedAppeal = appeal;
      choices.add(IA);
    }
    return choices;
  }
  
  
  
  /**  Handling end-stage events:
    */
  static void handleInvasion(
    Formation formation, City goes, World.Journey journey
  ) {
    City  from      = journey.from;
    float power     = formation.formationPower();
    float cityPower = goes.armyPower;
    
    float chance = 0, casualties = 0;
    boolean victory = false;
    chance     = power / (power + cityPower);
    chance     = Nums.clamp((chance * 2) - 0.5f, 0, 1);
    casualties = (Rand.num() + (1 - chance)) / 2;
    
    if (Rand.num() < chance) {
      setRelations(from, RELATION.LORD, goes, RELATION.VASSAL);
      casualties -= 0.25f;
      victory = true;
    }
    else {
      casualties += 0.25f;
      victory = false;
    }
    
    int numLost = inflictCasualties(formation, casualties);
    
    I.say("\n"+formation+" CONDUCTED ACTION AGAINST "+goes);
    I.say("  Victorious:    "+victory);
    I.say("  Casualties:    "+numLost / formation.recruits.size());
    I.say("  Home city now: "+goes.relations.get(from)+" of "+goes);
    
    //  TODO:  Handle recall of forces in a separate decision-pass...
    
    formation.stopSecuringPoint();
    goes.world.beginJourney(goes, from, formation);
  }
  
  
  static int inflictCasualties(Formation formation, float casualties) {
    int numFought = formation.recruits.size();
    if (numFought == 0) return 0;
    
    casualties *= numFought;
    for (float i = Nums.min(numFought, casualties); i-- > 0;) {
      Actor lost = (Actor) Rand.pickFrom(formation.recruits);
      formation.toggleRecruit(lost, false);
    }
    return (int) casualties;
  }
}













