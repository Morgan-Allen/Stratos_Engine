

package game;
import util.*;
import static game.ActorAsPerson.*;
import static game.Base.*;
import static game.BaseCouncil.*;
import static game.GameConstants.*;



public class BaseEvents {
  
  
  /**  Rendering, debug and interface methods-
    */
  static boolean reportEvents(Area map) {
    if (map == null) return false;
    return map.world.settings.reportBattle;
  }
  
  
  /**  Handling end-stage events:
    */
  public static void handleDeparture(
    Mission mission, Base from, Base goes
  ) {
    Base belongs = mission.base();
    belongs.incArmyPower(0 - MissionStrike.powerSum(mission.recruits(), null));
  }
  
  
  public static void handleInvasion(
    Mission mission, Base goes, World.Journey journey
  ) {
    //
    //  Gather some details first:
    Base    from   = journey.from;
    World   world  = from.world;
    int     time   = world.time;
    Area    map    = world.activeBaseMap();
    boolean report = reportEvents(map);
    //
    //  We use the same math that estimates the appeal of invasion to play out
    //  the real event, and report accordingly:
    //  TODO:  Use separate math for the purpose?
    BaseCouncil.MissionAssessment IA = new BaseCouncil.MissionAssessment();
    IA.fromC     = from;
    IA.goesC     = goes;
    IA.fromPower = MissionStrike.powerSum(mission.recruits(), null) / POP_PER_CITIZEN;
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
      lost.health.setAsKilled("casualty of war");
      mission.toggleRecruit(lost, false);
      numLost += 1;
    }
    return numLost;
  }
  
  
  static int inflictCasualties(Base defends, float casualties) {
    casualties = Nums.min(casualties, defends.armyPower());
    defends.incArmyPower (0 - casualties * POP_PER_CITIZEN);
    defends.incPopulation(0 - casualties * POP_PER_CITIZEN);
    return (int) casualties;
  }
  
  
  static void handleGarrison(
    Mission mission, Base goes, World.Journey journey
  ) {
    //  TODO:  Implement this?
    return;
  }
  
  
  static void handleDialog(
    Mission mission, Base goes, World.Journey journey
  ) {
    mission.terms.sendTerms(goes);
  }
  
  
  static void handleReturn(
    Mission mission, Base goes, World.Journey journey
  ) {
    goes.incArmyPower(MissionStrike.powerSum(mission.recruits(), null));
    mission.disbandMission();
  }
  
  
  
  /**  Note- these methods can also be called by formations on the map, so
    *  don't delete...
    */
  static void imposeTerms(
    Base upon, Base from, Mission mission
  ) {
    if (upon == null || from == null || mission == null) return;
    setPosture(from, upon, mission.terms.postureDemand, true);
    setSuppliesDue (upon, from, mission.terms.tributeDemand );
    arrangeMarriage(upon, from, mission.terms.marriageDemand);
  }
  
  
  static void arrangeMarriage(Base city, Base other, Actor marries) {
    Actor monarch = city.council.memberWithRole(Role.MONARCH);
    if (monarch == null || monarch.health.dead()) return;
    
    setBond(monarch, marries, BOND_MARRIED, BOND_MARRIED, 0);
    marries.assignBase(monarch.base());
    if (monarch.home() != null) monarch.home().setResident(marries, true);
    
    Mission party = marries.mission();
    if (party != null) {
      party.toggleRecruit(marries, false);
      party.toggleEnvoy(marries, false);
    }
  }
  
  
  static void signalVictory(
    Base victor, Base losing, Mission mission
  ) {
    if (victor == null || losing == null || mission == null) return;
    
    boolean report = false;
    float initVP = victor.prestige(), initLP = losing.prestige();
    
    losing.toggleRebellion(victor, false);
    incPrestige(victor, PRES_VICTORY_GAIN);
    incPrestige(losing, PRES_DEFEAT_LOSS );
    
    if (report) {
      I.say(victor+" prevailed over "+losing+"!!!");
      I.say(victor+" Prestige: "+initVP+" -> "+victor.prestige());
      I.say(losing+" Prestige: "+initLP+" -> "+losing.prestige());
    }
    
    mission.setMissionComplete(mission.base() == victor);
  }
  
  
  static void enterHostility(
    Base defends, Base attacks, boolean victory, float weight
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






