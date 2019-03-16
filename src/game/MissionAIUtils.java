


package game;
import static game.GameConstants.*;
import static game.BaseRelations.*;
import util.*;



public class MissionAIUtils {
  
  
  /**  General-purpose utilities-
    */
  static float owningValue(WorldLocale locale, Federation from) {
    return 1;
  }
  
  static float forceStrength(Base base) {
    float power = (POP_PER_CITIZEN + base.armyPower()) / 2f;
    float prestige = base.federation().relations.prestige() / PRESTIGE_MAX;
    return power * (1 + prestige);
  }
  
  static float exploreLevel(WorldLocale locale, Federation from) {
    return from.exploreLevel(locale);
  }
  
  static int preferredStance(Federation to, Federation from) {
    return RelationSet.BOND_NEUTRAL;
  }
  
  static void selectCapital(Federation from) {
    return;
  }
  
  
  static Series <Mission> allMissions(World world, Faction f) {
    Batch <Mission> all = new Batch();
    for (Base b : world.bases) {
      if (f != null && f != b.faction()) continue;
      for (Mission m : b.missions()) {
        all.add(m);
      }
    }
    return all;
  }
  
  static Series <Mission> allMissions(World world) {
    return allMissions(world, null);
  }
  
  static Series <Mission> federationMissions(Federation f) {
    return allMissions(f.world, f.faction);
  }
  
  static float federationPower(Federation f) {
    float power = 0;
    for (Base b : f.world.bases) {
      if (b.federation() == f) power += b.armyPower();
    }
    return power;
  }
  
  static void generateRecruits(Mission mission, float maxArmy, Type... types) {
    //
    //  For off-map bases, we can generate troops dynamically-
    if (mission.homeBase().locale.isOffmap()) {
      while (MissionForStrike.powerSum(mission) < maxArmy) {
        Type soldier = (Type) Rand.pickFrom(types);
        Actor fights = (Actor) soldier.generate();
        fights.assignBase(mission.homeBase);
        mission.toggleRecruit(fights, true);
      }
    }
    //
    //  If this is a base on a local map, we need to assign applicants from
    //  among the local population.
    else {
      Area localMap = mission.homeBase().activeMap();
      
      class Entry { Actor actor; float rating; }
      List <Entry> canApply = new List <Entry> () {
        protected float queuePriority(Entry r) {
          return r.rating;
        }
      };
      
      for (Actor a : localMap.actors) {
        if (a.base() != mission.homeBase()) continue;
        if ((! Visit.empty(types)) && ! Visit.arrayIncludes(types, a.type())) continue;
        
        Entry e = new Entry();
        e.actor = a;
        e.rating = mission.evalPriority * (0.5f + Rand.num());
        canApply.add(e);
      }
      canApply.queueSort();
      
      for (Entry e : canApply) {
        Actor fights = e.actor;
        mission.toggleRecruit(fights, true);
        if (MissionForStrike.powerSum(mission) >= maxArmy) break;
      }
    }
  }
  
  static boolean hasCompetition(Mission mission) {
    for (Mission m : mission.homeBase().missions()) {
      if (m != mission && m.active()) {
        return true;
      }
    }
    for (Mission m : federationMissions(mission.homeBase().federation())) {
      if (m == mission || ! m.active()          ) continue;
      if (m.objective != mission.objective      ) continue;
      if (m.worldFocus() != mission.worldFocus()) continue;
      return true;
    }
    return false;
  }
  
  
  
  /**  Strike-mission-
    */
  public static Mission setupStrikeMission(Base goes, Base from, float forceCap, boolean goesLimit) {
    
    float defence = forceStrength(goes);
    float maxArmy = goesLimit ? Nums.min(defence, forceCap) : forceCap;
    
    MissionForStrike mission = new MissionForStrike(from);
    mission.setWorldFocus(goes);
    mission.terms.assignTerms(RelationSet.BOND_LORD, null, null, null);
    mission.setEvalForce(maxArmy);
    return mission;
  }
  
  
  static float strikeAppeal(Base goes, Base from) {
    if (from.federation().hasTypeAI(Federation.AI_PACIFIST)) return -1;
    if (goes.faction() == from.faction()) return -1;
    
    float value = owningValue(goes.locale, from.federation());
    float enmity = 0 - from.relations.bondLevel(goes.faction());
    
    if (goes.isAllyOf (from)) enmity -= 0.5f;
    if (goes.isEnemyOf(from)) enmity += 0.5f;
    if (from.federation().hasTypeAI(Federation.AI_WARLIKE)) enmity += 1;
    
    return value * (0.5f + enmity);
  }
  
  
  static float strikeAppeal(Mission mission) {
    float appeal = strikeAppeal(mission.worldFocusBase(), mission.homeBase);
    if (hasCompetition(mission)) appeal *= 1.25f;
    return appeal;
  }
  
  
  static float strikeChance(Mission mission) {
    float defence = forceStrength(mission.worldFocusBase());
    float sumArmy = mission.evalForce() + 0;
    
    for (Mission m : federationMissions(mission.base().federation())) {
      if (m.objective != mission.objective || m == mission) continue;
      if (m.worldFocus() != mission.worldFocus()) continue;
      sumArmy += mission.evalForce();
    }
    
    return sumArmy / (sumArmy + defence);
  }
  
  
  public static void recruitStrikeMission(Mission mission, World world) {
    float maxArmy = mission.evalForce();
    Type  soldier = (Type) Visit.first(world.soldierTypes);
    generateRecruits(mission, maxArmy, soldier);
  }
  
  
  
  /**  Security-mission-
    */
  public static Mission setupDefendMission(Base goes, Base from, float forceCap, boolean goesLimit) {
    
    float deficit = defenceDeficit(goes);
    float maxArmy = goesLimit ? Nums.min(deficit, forceCap) : forceCap;
    
    MissionForSecure mission = new MissionForSecure(from);
    mission.setWorldFocus(goes);
    mission.setEvalForce(maxArmy);
    return mission;
  }
  
  
  static float defenceDeficit(Base base) {
    float sumDefence = base.armyPower();
    float sumDanger  = (AVG_ARMY_SIZE * POP_PER_CITIZEN) / 2f;
    
    sumDanger *= base.population() / AVG_POPULATION;
    
    for (Mission m : allMissions(base.world)) {
      if (m.worldFocus() == base) {
        if (m.objective == Mission.OBJECTIVE_SECURE) {
          sumDefence += MissionForStrike.powerSum(m);
        }
        if (m.objective == Mission.OBJECTIVE_STRIKE) {
          sumDanger += MissionForStrike.powerSum(m);
        }
      }
    }
    
    float deficit = (sumDanger - sumDefence);
    return deficit;
  }
  
  
  static float defendAppeal(Base goes, Base from) {
    ///if (exploreLevel(goes.locale, from.federation()) < 1) return -1;
    if (! goes.isAllyOf(from)) return -1;
    
    float deficit = defenceDeficit(goes) + 0;
    return deficit / (AVG_ARMY_SIZE * POP_PER_CITIZEN);
  }
  
  
  static float defendAppeal(Mission mission) {
    return defendAppeal(mission.worldFocusBase(), mission.homeBase);
  }
  
  
  static float defendChance(Mission mission) {
    return 1;
  }
  
  
  public static void recruitDefendMission(Mission mission, World world) {
    float maxArmy = mission.evalForce();
    Type  soldier = (Type) Visit.first(world.soldierTypes);
    generateRecruits(mission, maxArmy, soldier);
  }
  
  
  
  /**  Dialog-mission-
    */
  public static Mission setupDialogMission(Base goes, Base from, float forceCap, boolean goesLimit) {
    
    boolean isEnemy = from.isEnemyOf(goes);
    boolean isAlly  = from.isAllyOf (goes);

    float maxTeam = AVG_ARMY_SIZE / 4;
    float maxArmy = goesLimit ? Nums.min(forceCap, maxTeam) : forceCap;
    
    MissionForContact mission = new MissionForContact(from);
    mission.setWorldFocus(goes);
    mission.setEvalForce(maxArmy);
    
    int posture = RelationSet.BOND_NEUTRAL;
    if (! isEnemy) posture = RelationSet.BOND_TRADING;
    if (! isAlly ) posture = RelationSet.BOND_ALLY;
    mission.terms.assignTerms(posture, null, null, null);
    
    return mission;
  }
  
  
  static float dialogAppeal(Base goes, Base from) {
    ///if (exploreLevel(goes.locale, from.federation()) < 1) return -1;
    if (from.federation().hasTypeAI(Federation.AI_WARLIKE)) return -1;
    if (from.federation() == goes.federation()) return -1;
    
    float goesPower = goes.armyPower() + federationPower(goes.federation());
    float fromPower = from.armyPower() + federationPower(from.federation());
    float synergyVal = 0, dot = 0, count = 0;
    
    //  Appeal of alliance depends on whether you have a good existing
    //  relationship and/or share similar enemies.
    
    for (RelationSet.Focus f : goes.relations.allBondedWith(0)) {
      float valueF = from.relations.bondLevel(f);
      float valueG = goes.relations.bondLevel(f);
      if (valueF == -100 || valueG == -100) continue;
      synergyVal += dot = valueF * valueG;
      count += (Nums.abs(dot) + 1) / 2;
    }
    synergyVal /= Nums.max(1, count);
    
    float peaceDesire = 1;
    peaceDesire *= goesPower / (goesPower + fromPower);
    peaceDesire *= 1 + synergyVal;
    if (from.federation().hasTypeAI(Federation.AI_PACIFIST)) peaceDesire += 1;
    
    return peaceDesire;
  }
  
  
  static float dialogAppeal(Mission mission) {
    if (hasCompetition(mission)) return -1;
    return dialogAppeal(mission.worldFocusBase(), mission.homeBase);
  }
  
  
  static float dialogChance(Mission mission) {
    return 1;
  }
  
  
  public static void recruitDialogMission(Mission mission, World world) {
    float maxArmy = mission.evalForce();
    Type  soldier = (Type) Visit.first(world.soldierTypes);
    Type  noble   = (Type) Visit.first(world.nobleTypes  );
    
    generateRecruits(mission, maxArmy, soldier);
    
    while (mission.envoys.size() < 2) {
      Actor talks = (Actor) noble.generate();
      talks.assignBase(mission.homeBase());
      mission.toggleEnvoy(talks, true);
    }
    
    //  TODO:  You also need to include tribute and marriage arrangements!
  }
  
  
  static float appealOfTerms(Base eval, Mission petition) {
    
    /*
        petition.base(), petition.localBase,
        petition.terms.postureDemand,
        petition.terms.actionDemand,
        petition.terms.marriageDemand,
        petition.terms.tributeDemand
    //*/
    
    float talkDesire = dialogAppeal(petition.homeBase(), eval);
    return talkDesire;
  }
  
  
  
  /**  Explore-mission-
    */
  //  TODO:  Ideally, these should really be keyed off WorldLocale, rather than
  //  a base.  ...Which is going to be needed for colonisation anyway.
  
  public static Mission setupExploreMission(Base goes, Base from, float forceCap, boolean goesLimit) {
    float maxTeam = AVG_ARMY_SIZE / 4;
    float maxArmy = goesLimit ? Nums.min(forceCap, maxTeam) : forceCap;
    
    MissionForRecon mission = new MissionForRecon(from);
    mission.setWorldFocus(goes);
    mission.setEvalForce(maxArmy);
    return mission;
  }
  
  
  static float exploreAppeal(WorldLocale goes, Base from) {
    float exploreLevel = exploreLevel(goes, from.federation());
    return (1 - exploreLevel) * 0.5f;
  }
  
  
  static float exploreAppeal(Mission mission) {
    if (hasCompetition(mission)) return -1;
    return exploreAppeal(mission.worldFocusLocale(), mission.homeBase);
  }
  
  
  static float exploreChance(Mission mission) {
    return 1;
  }
  
  
  public static void recruitExploreMission(Mission mission, World world) {
    float maxArmy = mission.evalForce();
    Type  soldier = (Type) Visit.first(world.soldierTypes);
    generateRecruits(mission, maxArmy, soldier);
  }
  
  
  
  /**  Updating internal politics...
    */
  public static void updateCapital(Federation federation, World world) {
    Base capital = federation.capital();
    
    if (capital == null || capital.federation() != federation) {
      Pick <Base> pick = new Pick(0);
      
      for (Base base : world.bases) if (base.federation() == federation) {
        float rating = base.armyPower() + base.population();
        rating *= 1 + (Rand.num() * 0.33f);
        pick.compare(base, rating);
      }
      
      capital = pick.result();
      federation.assignCapital(capital);
    }
  }
  
  
  
  /**  Generating trouble...
    */
  static Mission generateTrouble(
    Federation federation, Base from, float forceCap, Series <Base> allGoes,
    boolean launch
  ) {
    
    if (federation == null || from == null || forceCap <= 0) return null;
    if (allGoes == null || allGoes.empty()) return null;
    
    if (federation.hasTypeAI(Federation.AI_OFF)) return null;
    if (federation.faction == from.world.playerFaction) return null;
    
    final boolean report = from.world.settings.reportMissionEval;
    if (report) {
      I.say("\n  Evaluating Missions From: "+from+" ("+from.faction()+")");
      I.say("  Force Cap: "+forceCap);
    }
    
    Pick <Mission> selection = new Pick <Mission> (0) {
      public void compare(Mission next, float rating) {
        
        rating = next.evalPriority();
        if (rating < 0) return;
        
        if (report) {
          I.say("    "+I.padToLength(next.toString(), 30));
          I.add("    Appeal: "+I.shorten(next.evalPriority(), 2)+" Chance: "+next.evalChance());
        }
        
        //rating *= 1 + next.evalChance();
        rating += Rand.num() * 0.5f;
        
        super.compare(next, rating);
      }
    };
    
    for (Base goes : allGoes) {
      boolean explored = exploreLevel(goes.locale, federation) > 0;
      
      //  TODO:  Use proper OOP for this, including a separate targetValid() 
      //  method.  (Explore-missions need to be assignable to locales in any 
      //  case.)
      
      if (explored) {
        Mission strike = setupStrikeMission(goes, from, forceCap, true);
        Mission secure = setupDefendMission(goes, from, forceCap, true);
        Mission dialog = setupDialogMission(goes, from, forceCap, true);
        strike.setEvalParams(strikeAppeal(strike), strikeChance(strike));
        secure.setEvalParams(defendAppeal(secure), defendChance(secure));
        dialog.setEvalParams(dialogAppeal(dialog), dialogChance(dialog));
        selection.compare(strike, 1);
        selection.compare(secure, 1);
        selection.compare(dialog, 1);
      }
      else {
        Mission scouts = setupExploreMission(goes, from, forceCap, true);
        scouts.setEvalParams(exploreAppeal(scouts), exploreChance(scouts));
        selection.compare(scouts, 1);
      }
    }
    
    Mission m = selection.result();
    World w = federation.world;
    
    if (m != null) {
      if (m.objective == Mission.OBJECTIVE_STRIKE ) recruitStrikeMission (m, w);
      if (m.objective == Mission.OBJECTIVE_SECURE ) recruitDefendMission (m, w);
      if (m.objective == Mission.OBJECTIVE_CONTACT) recruitDialogMission (m, w);
      if (m.objective == Mission.OBJECTIVE_RECON  ) recruitExploreMission(m, w);
      
      if (report) I.say("  Selected: "+m);
      
      if (launch) m.beginMission();
      return m;
    }
    else {
      return null;
    }
  }
  
  
  public static Mission generateLocalTrouble(Federation federation, Area activeMap, boolean launch) {
    if (federation == null || activeMap == null) return null;
    
    Base from = federation.capital();
    float forceCap = federationPower(federation);
    
    return generateTrouble(federation, from, forceCap, activeMap.bases(), launch);
  }
  
  
  public static Mission generateOffmapTrouble(Federation federation, World world, boolean launch) {
    if (federation == null || world == null) return null;

    Base from = federation.capital();
    float forceCap = federationPower(federation);
    Area map = world.activeBaseMap();
    
    Batch <Base> offmap = new Batch();
    for (Base b : world.bases) {
      if (map == null || b.locale != map.locale) offmap.add(b);
    }
    
    return generateTrouble(federation, from, forceCap, offmap, launch);
  }
  
  
  public static Mission generateLocalBaseTrouble(Base from, boolean launch) {
    if (from == null) return null;
    
    float forceCap = from.armyPower();
    Area map = from.activeMap();
    
    return generateTrouble(from.federation(), from, forceCap, map.bases(), launch);
  }

  
  
  /**  ...and rescinding orders.
    */
  static void considerMissionRecalls(Federation federation, World world) {
    
    float topEval = 0;
    for (Mission m : federationMissions(federation)) {
      topEval = Nums.max(topEval, m.evalPriority());
    }
    
    for (Mission m : federationMissions(federation)) {
      if (m.complete() || ! m.arrived()) continue;
      
      float oldEval = m.evalPriority();
      float newEval = 0;
      if (m.objective == Mission.OBJECTIVE_STRIKE ) newEval = strikeAppeal (m);
      if (m.objective == Mission.OBJECTIVE_SECURE ) newEval = defendAppeal (m);
      if (m.objective == Mission.OBJECTIVE_CONTACT) newEval = dialogAppeal (m);
      if (m.objective == Mission.OBJECTIVE_RECON  ) newEval = exploreAppeal(m);
      
      float threshold = Nums.max(oldEval, topEval - 0.5f) * 0.5f;
      if (newEval < threshold) {
        m.setMissionComplete(true);
      }
    }
    
  }
  
}







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


/*


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





