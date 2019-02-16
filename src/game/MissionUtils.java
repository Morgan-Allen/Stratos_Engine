

package game;
import util.*;
import static game.ActorBonds.*;
import static game.BaseRelations.*;
import static game.FederationRelations.*;
import static game.Federation.*;
import static game.GameConstants.*;



public class MissionUtils {
  
  
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
    belongs.incArmyPower(0 - MissionForStrike.powerSum(mission.recruits(), null));
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
    float   chance    = BaseCouncilUtils.strikeChance(mission);
    float   fromPower = MissionForStrike.powerSum(mission) / POP_PER_CITIZEN;
    float   goesPower = goes.armyPower() / POP_PER_CITIZEN;
    float   fromLost  = 0;
    float   goesLost  = 0;
    boolean victory   = false;
    
    if (Rand.num() < chance) {
      fromLost = fromPower * Nums.clamp((Rand.num() + chance + 0.5f) / 2, 0, 1);
      goesLost = goesPower * Nums.clamp((Rand.num() + 0.5f - chance) / 2, 0, 1);
      victory  = true;
    }
    else {
      fromLost = fromPower * Nums.clamp((Rand.num() + chance - 0.5f) / 2, 0, 1);
      goesLost = goesPower * Nums.clamp((Rand.num() + 1.5f - chance) / 2, 0, 1);
      victory  = false;
    }
    
    if (report) {
      I.say("\n"+mission+" CONDUCTED ACTION AGAINST "+goes+", time "+time);
      I.say("  Victorious:    "+victory  );
      I.say("  Attack power:  "+fromPower);
      I.say("  Defend power:  "+goesPower);
      I.say("  Taken losses:  "+fromLost );
      I.say("  Dealt losses:  "+goesLost );
    }
    //
    //  We inflict the estimated casualties upon each party, and adjust posture
    //  and relations.  (We assume/pretend that 'barbarian' factions won't set
    //  up political ties.
    //  TODO:  Handle recall of forces in a separate decision-pass?
    fromLost = inflictCasualties(mission, fromLost);
    goesLost = inflictCasualties(goes   , goesLost);
    //world.recordEvent("attacked", from, goes);
    enterHostility(goes, from, victory, 1);
    
    if (victory && from.federation().government != GOVERNMENT.BARBARIAN) {
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
      RelationSet r = world.federation(goes.faction()).relations;
      I.say("  Adjusted loss: "+fromLost+"/"+goesLost);
      I.say("  "+from+" now: "+r.bondProperties(from.faction())+" of "+goes);
    }
  }
  
  
  static int inflictCasualties(Mission mission, float casualties) {
    
    int numLost = 0;
    float casualtiesLeft = casualties;
    
    while (casualtiesLeft > 0 && mission.recruits.size() > 0) {
      Actor lost = (Actor) Rand.pickFrom(mission.recruits);
      float lostPower = TaskCombat.attackPower(lost);
      
      if (lostPower < casualtiesLeft) {
        lost.health.setAsKilled("casualty of war");
        mission.toggleRecruit(lost, false);
        numLost += 1;
      }
      else {
        float damage = casualtiesLeft / lostPower;
        damage *= lost.health.maxHealth();
        lost.health.takeDamage(damage);
      }
      
      casualtiesLeft -= lostPower;
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
    //  TODO:  Implement this
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
    goes.incArmyPower(MissionForStrike.powerSum(mission.recruits(), null));
    mission.disbandMission();
  }
  
  
  
  /**  Note- these methods can also be called by formations on the map, so
    *  don't delete...
    */
  static void imposeTerms(
    Base upon, Base from, Mission mission
  ) {
    if (upon == null || from == null || mission == null) return;
    
    int p = mission.terms.postureDemand;
    upon.relations.setBondType(from.faction(), p);
    
    upon.relations.setSuppliesDue(from.faction(), mission.terms.tributeDemand);
    arrangeMarriage(upon, from, mission.terms.marriageDemand);
  }
  
  
  static void arrangeMarriage(Base city, Base other, Actor marries) {
    if (city == null || other == null) return;
    if (marries == null || marries.health.dead()) return;
    
    Actor monarch = city.council.memberWithRole(BaseCouncil.Role.MONARCH);
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
    float initVP = victor.federation().relations.prestige();
    float initLP = losing.federation().relations.prestige();
    
    losing.relations.toggleRebellion(victor.faction(), false);
    victor.federation().relations.incPrestige(PRES_VICTORY_GAIN);
    losing.federation().relations.incPrestige(PRES_DEFEAT_LOSS );
    
    if (report) {
      I.say(victor+" prevailed over "+losing+"!!!");
      I.say(victor+" Prestige: "+initVP+" -> "+victor.federation().relations.prestige());
      I.say(losing+" Prestige: "+initLP+" -> "+losing.federation().relations.prestige());
    }
    
    mission.setMissionComplete(mission.base() == victor);
  }
  
  
  static void enterHostility(
    Base defends, Base attacks, boolean victory, float weight
  ) {
    if (defends == null) return;
    World world = defends.world;
    
    //  TODO:  It should ideally take time for the news of a given assault to
    //  reach more distant cities...
    
    Federation.setPosture(attacks.faction(), defends.faction(), BOND_ENEMY, world);
    float hate = (victory ? LOY_CONQUER_PENALTY : LOY_ATTACK_PENALTY) * weight;
    defends.relations.incBond(attacks.faction(), hate);
  }
  
}






