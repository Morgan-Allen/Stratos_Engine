

package game;
import util.*;
import static game.City.*;
import static game.GameConstants.*;



public class CityEvents {
  
  
  /**  Handling end-stage events:
    */
  static void handleDeparture(Formation formation, City from, City goes) {
    City belongs = formation.belongs;
    belongs.armyPower -= formation.formationPower();
    belongs.formations.include(formation);
    formation.beginJourney(formation.belongs, goes);
  }
  
  
  static void handleInvasion(
    Formation formation, City goes, World.Journey journey
  ) {
    //
    //  We use the same math that estimates the appeal of invasion to play out
    //  the real event:
    //  TODO:  Consider where this should be placed, however?
    CityCouncil.InvasionAssessment IA = new CityCouncil.InvasionAssessment();
    City  from  = journey.from;
    World world = from.world;
    int   time  = world.time;
    IA.attackC     = from;
    IA.defendC     = goes;
    IA.attackPower = formation.formationPower() / POP_PER_CITIZEN;
    IA.defendPower = goes.armyPower / POP_PER_CITIZEN;
    from.council.calculateChances(IA, true);
    
    float chance = IA.winChance, fromLost = 0, goesLost = 0;
    boolean victory = false;
    
    if (Rand.num() < chance) {
      fromLost = IA.winKillsA;
      goesLost = IA.winKillsD;
      victory  = true;
    }
    else {
      fromLost = IA.lossKillsA;
      goesLost = IA.lossKillsD;
      victory  = false;
    }
    
    I.say("\n"+formation+" CONDUCTED ACTION AGAINST "+goes+", time "+time);
    I.say("  Victorious:    "+victory );
    I.say("  Attack power:  "+IA.attackPower);
    I.say("  Defend power:  "+IA.defendPower);
    I.say("  Taken losses:  "+fromLost);
    I.say("  Dealt losses:  "+goesLost);
    
    //
    //  We inflict the estimated casualties upon each party accordingly:
    fromLost = inflictCasualties(formation, fromLost);
    goesLost = inflictCasualties(goes     , goesLost);
    world.recordEvent("attacked", from, goes);
    
    //
    //  We assume/pretend that barbarian factions won't set up political ties.
    //  In either case, modify relations between the two cities accordingly-
    if (victory && from.government != GOVERNMENT.BARBARIAN) {
      inflictDemands(goes, from, formation);
    }
    else {
      becomeEnemies(goes, from);
    }
    if (victory) {
      signalVictory(from, goes, formation);
      //
      //  TODO:  Handle recall of forces in a separate decision-pass?
      formation.stopSecuringPoint();
      world.beginJourney(goes, from, formation);
    }
    else {
      signalVictory(goes, from, formation);
      formation.stopSecuringPoint();
      world.beginJourney(goes, from, formation);
    }
    incLoyalty(from, goes, victory ? LOY_CONQUER_PENALTY : LOY_ATTACK_PENALTY);
    
    I.say("  Adjusted loss: "+fromLost+"/"+goesLost);
    I.say("  "+from+" now: "+goes.posture(from)+" of "+goes);
  }
  
  
  static int inflictCasualties(Formation formation, float casualties) {
    int numFought = formation.recruits.size(), numLost = 0;
    if (numFought == 0) return 0;
    
    for (float i = Nums.min(numFought, casualties); i-- > 0;) {
      Actor lost = (Actor) Rand.pickFrom(formation.recruits);
      formation.toggleRecruit(lost, false);
      numLost += 1;
    }
    return numLost;
  }
  
  
  static int inflictCasualties(City defends, float casualties) {
    casualties = Nums.min(casualties, defends.armyPower);
    defends.armyPower  -= casualties * POP_PER_CITIZEN;
    defends.population -= casualties * POP_PER_CITIZEN;
    return (int) casualties;
  }
  
  
  
  /**  Note- these methods can also be called by formations on the map, so
    *  don't delete...
    */
  static void inflictDemands(
    City defends, City attacks, Formation formation
  ) {
    if (defends == null || attacks == null || formation == null) return;
    setPosture(attacks, defends, formation.postureDemand);
    setSuppliesDue(defends, attacks, formation.tributeDemand);
    incPrestige(attacks, PRES_VICTORY_GAIN);
    incPrestige(defends, PRES_DEFEAT_LOSS );
  }
  
  
  static void signalVictory(
    City victor, City losing, Formation formation
  ) {
    if (victor == null || losing == null || formation == null) return;
    incPrestige(victor, PRES_VICTORY_GAIN);
    incPrestige(losing, PRES_DEFEAT_LOSS );
  }
  
  
  static void becomeEnemies(City defends, City attacks) {
    setPosture(attacks, defends, POSTURE.ENEMY);
  }
  
  
  static void handleReturn(
    Formation formation, City from, World.Journey journey
  ) {
    City belongs = formation.belongs;
    belongs.armyPower += formation.formationPower();
    belongs.formations.remove(formation);
  }
}






