

package game;
import util.*;
import static game.ActorAsPerson.*;
import static game.City.*;
import static game.CityCouncil.*;
import static game.GameConstants.*;



public class CityEvents {
  
  
  /**  Rendering, debug and interface methods-
    */
  static boolean reportEvents(CityMap map) {
    if (map == null) return false;
    return map.world.settings.reportBattle;
  }
  
  
  /**  Handling end-stage events:
    */
  public static void handleDeparture(
    Mission mission, City from, City goes
  ) {
    City belongs = mission.homeCity();
    belongs.incArmyPower(0 - mission.powerSum());
    belongs.missions.include(mission);
    mission.setFocus(goes);
  }
  
  
  public static void handleInvasion(
    Mission mission, City goes, World.Journey journey
  ) {
    //
    //  Gather some details first:
    City    from   = journey.from;
    World   world  = from.world;
    int     time   = world.time;
    CityMap map    = world.activeCityMap();
    boolean report = reportEvents(map);
    //
    //  We use the same math that estimates the appeal of invasion to play out
    //  the real event, and report accordingly:
    //  TODO:  Use separate math for the purpose?
    CityCouncil.MissionAssessment IA = new CityCouncil.MissionAssessment();
    IA.fromC     = from;
    IA.goesC     = goes;
    IA.fromPower = mission.powerSum() / POP_PER_CITIZEN;
    IA.goesPower = goes.armyPower() / POP_PER_CITIZEN;
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
      I.say("\n"+mission+" CONDUCTED ACTION AGAINST "+goes+", time "+time);
      I.say("  Victorious:    "+victory );
      I.say("  Attack power:  "+IA.fromPower);
      I.say("  Defend power:  "+IA.goesPower);
      I.say("  Taken losses:  "+fromLost);
      I.say("  Dealt losses:  "+goesLost);
    }
    //
    //  We inflict the estimated casualties upon each party, and adjust posture
    //  and relations.  (We assume/pretend that 'barbarian' factions won't set
    //  up political ties.
    //  TODO:  Handle recall of forces in a separate decision-pass?
    fromLost = inflictCasualties(mission, fromLost);
    goesLost = inflictCasualties(goes   , goesLost);
    world.recordEvent("attacked", from, goes);
    enterHostility(goes, from, victory, 1);
    
    if (victory && from.government != GOVERNMENT.BARBARIAN) {
      imposeTerms(goes, from, mission);
    }
    if (victory) {
      signalVictory(from, goes, mission);
    }
    else {
      signalVictory(goes, from, mission);
    }
    //
    //  Either way, report the final outcome:
    if (report) {
      I.say("  Adjusted loss: "+fromLost+"/"+goesLost);
      I.say("  "+from+" now: "+goes.posture(from)+" of "+goes);
    }
  }
  
  
  static int inflictCasualties(Mission mission, float casualties) {
    int numFought = mission.recruits.size(), numLost = 0;
    if (numFought == 0) return 0;
    
    for (float i = Nums.min(numFought, casualties); i-- > 0;) {
      Actor lost = (Actor) Rand.pickFrom(mission.recruits);
      lost.setAsKilled("casualty of war");
      mission.toggleRecruit(lost, false);
      numLost += 1;
    }
    return numLost;
  }
  
  
  static int inflictCasualties(City defends, float casualties) {
    casualties = Nums.min(casualties, defends.armyPower());
    defends.incArmyPower (0 - casualties * POP_PER_CITIZEN);
    defends.incPopulation(0 - casualties * POP_PER_CITIZEN);
    return (int) casualties;
  }
  
  
  static void handleGarrison(
    Mission mission, City goes, World.Journey journey
  ) {
    //  TODO:  Implement this?
    return;
  }
  
  
  static void handleDialog(
    Mission mission, City goes, World.Journey journey
  ) {
    mission.dispatchTerms(goes);
  }
  
  
  static void handleReturn(
    Mission mission, City from, World.Journey journey
  ) {
    City belongs = mission.homeCity();
    belongs.incArmyPower(mission.powerSum());
    mission.disbandFormation();
  }
  
  
  
  /**  Note- these methods can also be called by formations on the map, so
    *  don't delete...
    */
  static void imposeTerms(
    City upon, City from, Mission mission
  ) {
    if (upon == null || from == null || mission == null) return;
    setPosture(from, upon, mission.postureDemand, true);
    setSuppliesDue (upon, from, mission.tributeDemand );
    arrangeMarriage(upon, from, mission.marriageDemand);
  }
  
  
  static void arrangeMarriage(City city, City other, Actor marries) {
    Actor monarch = city.council.memberWithRole(Role.MONARCH);
    if (monarch == null || monarch.dead()) return;
    
    setBond(monarch, marries, BOND_MARRIED, BOND_MARRIED, 0);
    marries.assignHomeCity(monarch.homeCity());
    if (monarch.home() != null) monarch.home().setResident(marries, true);
    
    Mission party = marries.mission;
    if (party != null) {
      party.toggleRecruit (marries, false);
      party.toggleEscorted(marries, false);
    }
  }
  
  
  static void signalVictory(
    City victor, City losing, Mission mission
  ) {
    if (victor == null || losing == null || mission == null) return;
    losing.toggleRebellion(victor, false);
    incPrestige(victor, PRES_VICTORY_GAIN);
    incPrestige(losing, PRES_DEFEAT_LOSS );
    mission.setMissionComplete(mission.homeCity() == victor);
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
}






