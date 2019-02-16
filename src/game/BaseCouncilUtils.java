


package game;
import static game.GameConstants.*;
import static game.BaseRelations.*;
import util.*;



public class BaseCouncilUtils {
  
  
  /**  General-purpose utilities-
    */
  static float owningValue(WorldLocale locale, Federation from) {
    return 1;
  }
  
  static float forceStrength(Base base) {
    float power = (1 + base.armyPower()) / 2f;
    float prestige = base.federation().relations.prestige() / PRESTIGE_MAX;
    return power * (1 + prestige);
  }
  
  static float exploreLevel(WorldLocale locale, Federation from) {
    return 1;
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
  
  static Series <Mission> federationMissions(Base base) {
    return allMissions(base.world, base.faction());
  }
  
  static float federationPower(Base base) {
    float power = 0;
    for (Base b : base.world.bases) {
      if (b.federation() == base.federation()) power += b.armyPower();
    }
    return power;
  }
  
  static boolean hasCompetition(Mission mission) {
    for (Mission m : mission.homeBase().missions()) {
      if (m != mission && m.active()) return true;
    }    
    for (Mission m : federationMissions(mission.homeBase())) {
      if (m == mission || ! m.active()          ) continue;
      if (m.objective != mission.objective      ) continue;
      if (m.worldFocus() != mission.worldFocus()) continue;
      return true;
    }
    return false;
  }
  
  
  
  /**  Strike-mission-
    */
  static Mission setupStrikeMission(Base goes, Base from) {
    
    float defence = forceStrength(goes);
    float reserve = from.armyPower() / 2;
    float maxArmy = Nums.min(defence, reserve);
    
    MissionForStrike mission = new MissionForStrike(from);
    mission.setWorldFocus(goes);
    mission.terms.assignTerms(RelationSet.BOND_VASSAL, null, null, null);
    mission.setEvalForce(maxArmy);
    return mission;
  }
  
  
  static float strikeAppeal(Base goes, Base from) {
    
    float value = owningValue(goes.locale, from.federation());
    float enmity = 0 - from.relations.bondLevel(goes.faction());
    
    if (goes.isAllyOf (from)) enmity -= 0.5f;
    if (goes.isEnemyOf(from)) enmity += 0.5f;
    
    return value * (0.5f + enmity);
  }
  
  
  static float strikeAppeal(Mission mission) {
    if (hasCompetition(mission)) return -1;
    return strikeAppeal(mission.worldFocus(), mission.homeBase);
  }
  
  
  static float strikeChance(Mission mission) {
    float defence = forceStrength(mission.worldFocus());
    float sumArmy = mission.evalForce() + 0;
    
    for (Mission m : federationMissions(mission.base())) {
      if (m.objective != mission.objective || m == mission) continue;
      if (m.worldFocus() != mission.worldFocus()) continue;
      sumArmy += mission.evalForce();
    }
    
    return sumArmy / (sumArmy + defence);
  }
  
  
  static void launchStrikeMission(Mission mission, World world) {
    float maxArmy = mission.evalForce();
    Type  soldier = (Type) Visit.first(world.soldierTypes);
    generateRecruits(mission, maxArmy, soldier);
    mission.beginMission(mission.homeBase());
  }
  
  
  
  /**  Security-mission-
    */
  static Mission setupDefendMission(Base goes, Base from) {
    
    float deficit = defenceDeficit(goes);
    float reserve = from.armyPower() / 2;
    float maxArmy = Nums.min(deficit, reserve);
    
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
  
  
  static float defendAppeal(Base base, Base from) {
    //  TODO:  Allow this for allies too.
    if (base.federation() != from.federation()) return -1;
    
    float deficit = defenceDeficit(base) + 0;
    return deficit / (AVG_ARMY_SIZE * POP_PER_CITIZEN);
  }
  
  
  static float defendAppeal(Mission mission) {
    if (hasCompetition(mission)) return -1;
    return defendAppeal(mission.worldFocus(), mission.homeBase);
  }
  
  
  static float defendChance(Mission mission) {
    return 1;
  }
  
  
  static void launchDefendMission(Mission mission, World world) {
    float maxArmy = mission.evalForce();
    Type  soldier = (Type) Visit.first(world.soldierTypes);
    generateRecruits(mission, maxArmy, soldier);
    mission.beginMission(mission.homeBase());
  }
  
  
  
  /**  Dialog-mission-
    */
  static Mission setupDialogMission(Base goes, Base from) {
    
    boolean isEnemy = from.isEnemyOf(goes);
    boolean isAlly  = from.isAllyOf (goes);

    float maxForce = AVG_ARMY_SIZE / 4;
    float reserve  = from.armyPower() / 2;
    float maxArmy  = Nums.min(reserve, maxForce);
    
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
    
    float basePower = goes.armyPower() + federationPower(goes);
    float fromPower = from.armyPower() + federationPower(from);
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
    peaceDesire *= basePower / (basePower + fromPower);
    peaceDesire *= synergyVal;
    
    return peaceDesire;
  }
  
  
  static float dialogAppeal(Mission mission) {
    if (hasCompetition(mission)) return -1;
    return dialogAppeal(mission.worldFocus(), mission.homeBase);
  }
  
  
  static float dialogChance(Mission mission) {
    return 1;
  }
  
  
  static void launchDialogMission(Mission mission, World world) {
    float maxArmy = mission.evalForce();
    Type  soldier = (Type) Visit.first(world.soldierTypes);
    Type  noble   = (Type) Visit.first(world.nobleTypes  );
    
    generateRecruits(mission, maxArmy, soldier);
    
    while (mission.envoys.size() < 2) {
      Actor talks = (Actor) noble.generate();
      talks.assignBase(mission.homeBase());
      mission.toggleEnvoy(talks, true);
    }
    
    mission.beginMission(mission.homeBase());
  }
  
  
  
  /**  Explore-mission-
    */
  //  TODO:  Ideally, these should really be keyed off WorldLocale, rather than
  //  a base.  ...Which is going to be needed for colonisation anyway.
  
  static Mission setupExploreMission(Base goes, Base from) {
    
    float maxForce = AVG_ARMY_SIZE / 4;
    
    MissionForRecon mission = new MissionForRecon(from);
    mission.setWorldFocus(goes);
    mission.setEvalForce(maxForce);
    return mission;
  }
  
  
  static float exploreAppeal(Base goes, Base from) {
    
    for (Mission m : federationMissions(from)) {
      if (m.objective != Mission.OBJECTIVE_RECON) continue;
      if (m.worldFocus() != goes) continue;
      return -1;
    }
    
    float exploreLevel = exploreLevel(goes.locale, from.federation());
    return 1 - exploreLevel;
  }
  
  
  static float exploreAppeal(Mission mission) {
    if (hasCompetition(mission)) return -1;
    return exploreAppeal(mission.worldFocus(), mission.homeBase);
  }
  
  
  static float exploreChance(Mission mission) {
    return 1;
  }
  
  
  static void launchExploreMission(Mission mission, World world) {
    float maxArmy = mission.evalForce();
    Type  soldier = (Type) Visit.first(world.soldierTypes);
    generateRecruits(mission, maxArmy, soldier);
    mission.beginMission(mission.homeBase());
  }
  
  
  
  
  /**  Generating trouble...
    */
  static class BaseEntry { Base base; float priority; }
  
  
  static void generateTrouble(Federation federation, Area activeMap) {
    
    World world = federation.world;
    final boolean report = true;
    
    if (report) {
      I.say("\nGenerating trouble from "+federation);
      I.say("  Total bases: "+world.bases.size());
    }
    
    List <BaseEntry> sorting = new List <BaseEntry> () {
      protected float queuePriority(BaseEntry r) {
        return r.priority;
      }
    };
    
    for (Base base : world.bases) {
      if (base.federation() != federation) continue;
      BaseEntry e = new BaseEntry();
      e.base = base;
      e.priority = Rand.num();
      sorting.add(e);
    }
    sorting.queueSort();
    
    for (BaseEntry e : sorting) {
      if (report) I.say("  Base From: "+e.base+" ("+e.base.faction()+")");
      
      Base from = e.base;
      Pick <Mission> selection = new Pick <Mission> (0) {
        public void compare(Mission next, float rating) {
          
          if (report) {
            I.say("    "+I.padToLength(next.toString(), 25));
            I.add("    Appeal: "+next.evalPriority()+" Chance: "+next.evalChance());
          }
          
          if (rating < 0) return;
          rating += Rand.num() / 2;
          super.compare(next, rating);
        }
      };
      
      for (Base goes : world.bases) if (goes != from) {
        if (report) I.say("  Base Goes: "+goes+" ("+goes.faction()+")");
        
        Mission strike = setupStrikeMission (goes, from);
        Mission secure = setupDefendMission (goes, from);
        Mission dialog = setupDialogMission (goes, from);
        Mission scouts = setupExploreMission(goes, from);
        
        strike.setEvalParams(strikeAppeal (strike), strikeChance (strike));
        secure.setEvalParams(defendAppeal (secure), defendChance (secure));
        dialog.setEvalParams(dialogAppeal (dialog), dialogChance (dialog));
        scouts.setEvalParams(exploreAppeal(scouts), exploreChance(scouts));
        
        selection.compare(strike, strike.evalPriority());
        selection.compare(secure, secure.evalPriority());
        selection.compare(dialog, dialog.evalPriority());
        selection.compare(scouts, scouts.evalPriority());
      }
      
      Mission selected = selection.result();
      
      if (report) {
        I.say("  Mission chosen: "+selected);
      }
      
      //
      //  If the mission has a reasonable chance of success (based on cumulative
      //  evaluation of help from other missions,) activate it and any similar
      //  missions.
      if (selected != null && selected.evalChance() >= 1) {
        activateMissionsMatching(selected, sorting);
      }
      else if (selected != null) {
        from.addMission(selected);
      }
    }
    
    //
    //  Any missions that haven't been activated should be removed-
    for (BaseEntry e : sorting) {
      for (Mission m : e.base.missions()) {
        if (! m.active()) {
          e.base.removeMission(m);
        }
      }
    }
  }
  
  
  static void generateRecruits(Mission mission, float maxArmy, Type... types) {
    //  TODO:  You are going to have to select troops locally if the mission is
    //  generated locally (and possibly from off-map visitors otherwise.)
    
    while (MissionForStrike.powerSum(mission) < maxArmy) {
      Type soldier = (Type) Rand.pickFrom(types);
      Actor fights = (Actor) soldier.generate();
      fights.assignBase(mission.homeBase);
      mission.toggleRecruit(fights, true);
    }
  }
  
  
  static void activateMissionsMatching(Mission mission, List <BaseEntry> owned) {
    for (BaseEntry e : owned) {
      Base base = e.base;
      World w = base.world;
      
      for (Mission m : base.missions()) {
        if (m.objective != mission.objective      ) continue;
        if (m.worldFocus() != mission.worldFocus()) continue;
        
        if (m.objective == Mission.OBJECTIVE_STRIKE ) launchStrikeMission (m, w);
        if (m.objective == Mission.OBJECTIVE_SECURE ) launchDefendMission (m, w);
        if (m.objective == Mission.OBJECTIVE_CONTACT) launchDialogMission (m, w);
        if (m.objective == Mission.OBJECTIVE_RECON  ) launchExploreMission(m, w);
      }
      
      //  We won't have evaluated past this point, so break here...
      if (base == mission.homeBase()) break;
    }
  }
  
  
  
}









