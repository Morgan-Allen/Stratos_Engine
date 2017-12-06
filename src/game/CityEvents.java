

package game;
import util.*;
import static game.City.*;
import static game.GameConstants.*;



public class CityEvents {
  
  
  /**  Handling end-stage events:
    */
  static void handleDeparture(Formation formation, City from, City goes) {
    City belongs = formation.homeCity();
    belongs.armyPower -= formation.powerSum();
    belongs.formations.include(formation);
    formation.beginSecuring(goes);
  }
  
  
  static void handleInvasion(
    Formation formation, City goes, World.Journey journey
  ) {
    //
    //  Gather some details first:
    City    from   = journey.from;
    World   world  = from.world;
    int     time   = world.time;
    CityMap map    = world.activeCityMap();
    boolean report = map != null && map.settings.reportBattle;
    //
    //  We use the same math that estimates the appeal of invasion to play out
    //  the real event, and report accordingly:
    //  TODO:  Use separate math for the purpose?
    CityCouncil.InvasionAssessment IA = new CityCouncil.InvasionAssessment();
    IA.attackC     = from;
    IA.defendC     = goes;
    IA.attackPower = formation.powerSum() / POP_PER_CITIZEN;
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
    
    if (report) {
      I.say("\n"+formation+" CONDUCTED ACTION AGAINST "+goes+", time "+time);
      I.say("  Victorious:    "+victory );
      I.say("  Attack power:  "+IA.attackPower);
      I.say("  Defend power:  "+IA.defendPower);
      I.say("  Taken losses:  "+fromLost);
      I.say("  Dealt losses:  "+goesLost);
    }
    //
    //  We inflict the estimated casualties upon each party, and adjust posture
    //  and relations.  (We assume/pretend that 'barbarian' factions won't set
    //  up political ties.
    //  TODO:  Handle recall of forces in a separate decision-pass?
    fromLost = inflictCasualties(formation, fromLost);
    goesLost = inflictCasualties(goes     , goesLost);
    world.recordEvent("attacked", from, goes);
    enterHostility(goes, from, victory, 1);
    
    if (victory && from.government != GOVERNMENT.BARBARIAN) {
      inflictDemands(goes, from, formation);
    }
    if (victory) {
      signalVictory(from, goes, formation);
      formation.beginSecuring(from);
    }
    else {
      signalVictory(goes, from, formation);
      formation.beginSecuring(from);
    }
    //
    //  Either way, report the final outcome:
    if (report) {
      I.say("  Adjusted loss: "+fromLost+"/"+goesLost);
      I.say("  "+from+" now: "+goes.posture(from)+" of "+goes);
    }
  }
  
  
  static int inflictCasualties(Formation formation, float casualties) {
    int numFought = formation.recruits.size(), numLost = 0;
    if (numFought == 0) return 0;
    
    for (float i = Nums.min(numFought, casualties); i-- > 0;) {
      Actor lost = (Actor) Rand.pickFrom(formation.recruits);
      lost.setAsKilled("casualty of war");
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
    setPosture(attacks, defends, formation.postureDemand, true);
    setSuppliesDue(defends, attacks, formation.tributeDemand);
    defends.toggleRebellion(attacks, false);
    
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
  
  
  static void enterHostility(
    City defends, City attacks, boolean victory, float weight
  ) {
    if (defends == null) return;
    
    setPosture(attacks, defends, POSTURE.ENEMY, true);
    float hate = (victory ? LOY_CONQUER_PENALTY : LOY_ATTACK_PENALTY) * weight;
    incLoyalty(defends, attacks, hate);
    
    //  TODO:  It should ideally take time for the news of a given assault to
    //  reach more distant cities?
    enterHostility(defends.currentLord(), attacks, victory, weight / 2);
  }
  
  
  static void handleReturn(
    Formation formation, City from, World.Journey journey
  ) {
    City belongs = formation.homeCity();
    belongs.armyPower += formation.powerSum();
    formation.disbandFormation();
  }
}






