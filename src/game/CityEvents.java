

package game;
import util.*;
import static game.City.*;



//  TODO:  Move things like conquest, diplomacy and discovery of foreign cities
//         in here from the Formation class.
/*
  The baseline chance of invasion, for a city of 1000 pop (40 walkers) in one
  year, by another bordering city with a city of 1000 people, assuming that the
  latter has a standing military of 200 people and the former is defenceless,
  when both have equal wealth, is 100%.
  
  If your militaries are equal, the chance drops to maybe 25%.  If you have
  double their strength, then the chance of invasion drops very low.
  
  If one city is especially wealthy in something the other needs, then the
  temptation to invade increases.
  
  
  So, invasion attraction =
  ((benefits of win * chance of win) + (benefits of loss * chance of loss)) -
  ((cost of loss * chance of loss) + (cost of win * chance of win))
  
  Straightforward enough.
  Benefits:  Killing enemy population
             Gaining tribute from enemy
  Costs:     Losing own population
             Angering enemy
  
  You can calculate that and modulate by personality easily enough.
  
  
  For now, we'll assume that the value of lives is calculated on an entirely
  cynical economic basis:
    Avg-lifespan * (avg-tax-yield + (avg-good-value / (make-time * 2))) / 2
  
  And for tribute-benefit:
    Avg-good-output * expected-tribute-duration * good-value * tribute-fraction.
  
  And for anger-cost:
    relative-military-strength * population * life-value.
  
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













