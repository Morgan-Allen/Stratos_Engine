

package test;
import static content.GameContent.*;
import static content.GameWorld.*;
import static game.GameConstants.*;
import static game.BaseRelations.*;
import game.*;
import util.*;




public class TestOffmapMissions extends LogicTest {
  

  
  public static void main(String args[]) {
    testWorld(false);
  }
  
  
  static boolean testWorld(boolean graphics) {
    
    
    //  This tests for the basic outcomes of a single invasion attempt:
    {
      Base pair[] = configWeakStrongBasePair();
      Base goes = pair[0], from = pair[1];
      float oldPower = goes.growth.armyPower();
      Mission strike = runCompleteInvasion(from, goes);
      float newPower = goes.growth.armyPower();
      
      if (newPower >= oldPower) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Invasion inflicted no casualties!");
        return false;
      }
      if (! strike.success()) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Invasion did not succeed!");
        return false;
      }
      if (! goes.isVassalOf(from)) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Invasion did not impose vassal status!");
        return false;
      }
      if (goes.relations.bondLevel(from.faction()) >= LOY_CIVIL) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Invasion did not sour relations!");
        return false;
      }
    }
    
    //  This tests for the outcome of a security mission-
    {
      Base triple[] = configBases(
        BASE, FACTION_SETTLERS_A,
        AWAY, FACTION_SETTLERS_A,
        NEUT, FACTION_SETTLERS_B
      );
      Base a = triple[0], b = triple[1], c = triple[2];
      
      a.growth.initBuildLevels(HOLDING, 9f, TROOPER_LODGE, 6f);
      b.growth.initBuildLevels(HOLDING, 1f, TROOPER_LODGE, 0f);
      c.growth.initBuildLevels(HOLDING, 4f, TROOPER_LODGE, 1f);
      a.setName("Ally Base");
      b.setName("Victim Base");
      c.setName("Invading Base");
      
      dispatchDefence(a, b);
      runCompleteInvasion(c, b);
      
      if (b.faction() == c.faction()) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Base was not defended correctly!");
        return false;
      }
      if (b.relations.bondLevel(a.faction()) <= LOY_CIVIL) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Defence did not boost relations!");
        return false;
      }
    }
    
    //  This tests for the outcome of a basic dialog attempt:
    {
      Base pair[] = configWeakStrongBasePair();
      Base goes = pair[0], from = pair[1];
      goes.federation().setTypeAI(Federation.AI_PACIFIST);
      goes.federation().assignCapital(goes);
      runCompleteDialog(from, goes);
      
      if (! from.isAllyOf(goes)) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Dialog did not create ally!");
        return false;
      }
      if (goes.relations.bondLevel(from.faction()) <= LOY_CIVIL) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Dialog did not boost relations!");
        return false;
      }
    }
    
    //  This tests for the outcome of a dialog attempt aimed at conversion:
    {
      Base pair[] = configWeakStrongBasePair();
      Base goes = pair[0], from = pair[1];
      goes.federation().setTypeAI(Federation.AI_PACIFIST);
      goes.relations.incBond(from.faction(), 100);
      runCompleteDialog(from, goes);
      
      if (from.faction() != goes.faction()) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Dialog did not create vassal!");
        return false;
      }
    }
    
    // This tests for the outcome of a basic exploration attempt-
    {
      Base pair[] = configWeakStrongBasePair();
      Base goes = pair[0], from = pair[1];
      runCompleteExploration(from, goes.area);
      
      if (from.federation().exploreLevel(goes.area) < 0.5f) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Exploration did not reveal area!");
        return false;
      }
    }
    
    //  And this tests for the outcome of a colony-expedition-
    {
      Base home = configBases(BASE, FACTION_SETTLERS_A)[0];
      Area away = home.world.addArea(AWAY);
      home.growth.initBuildLevels(HOLDING, 3, TROOPER_LODGE, 1);
      runCompleteExpedition(home, away);
      
      Base atAway = away.firstBaseFor(FACTION_SETTLERS_A);
      if (atAway == null) {
        I.say("\nWORLD-EVENTS TESTING FAILED- No base established by expedition!");
        return false;
      }
      
      int time = home.world.time();
      while (time < (YEAR_LENGTH * POP_MAX_YEARS)) {
        home.world.updateWithTime(time++);
      }
      
      if (atAway.growth.population() < MAX_POPULATION / 2) {
        I.say("\nWORLD-EVENTS TESTING FAILED- No long-term offmap base-growth!");
        return false;
      }
      if (atAway.growth.employment() < MAX_POPULATION / 2) {
        I.say("\nWORLD-EVENTS TESTING FAILED- No offmap employment growth!");
        return false;
      }
    }
    
    //  All done-
    return true;
  }
  

  
  
  
  /**  Individual subtests-
    */
  static Base[] configWeakStrongBasePair() {
    Base bases[] = configBases(
      BASE, FACTION_SETTLERS_A,
      AWAY, FACTION_SETTLERS_B
    );
    Base a = bases[0], b = bases[1];
    a.setName("Victim Base");
    b.setName("Invader Base");
    b.federation().assignCapital(b);
    a.growth.initBuildLevels(HOLDING, 3f, TROOPER_LODGE, 1f);
    b.growth.initBuildLevels(HOLDING, 9f, TROOPER_LODGE, 6f);
    return bases;
  }
  
  
  static Base[] configBases(Object... args) {
    
    World world = new World(ALL_GOODS);
    world.assignTypes(
      ALL_BUILDINGS, ALL_SHIPS(), ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES()
    );
    Batch <Base> bases = new Batch();
    
    for (int i = 0; i < args.length;) {
      AreaType typeA = (AreaType) args[i++];
      Faction belongs = (Faction) args[i++];
      Base b = new Base(world, world.addArea(typeA), belongs);
      b.setName("City No. "+i);
      b.assignTechTypes(belongs.buildTypes());
      b.federation().setTypeAI(Federation.AI_OFF);
      b.federation().relations.initPrestige(BaseRelations.PRESTIGE_MAX);
      world.addBases(b);
      bases.add(b);
    }
    
    return bases.toArray(Base.class);
  }
  
  
  static Mission runCompleteInvasion(Base from, Base goes) {
    World world = from.world;
    
    Mission force = MissionAIUtils.setupStrikeMission(goes, from, from.growth.armyPower(), false);
    MissionAIUtils.recruitStrikeMission(force, world);
    force.beginMission();
    
    int time = 0;
    while (time < YEAR_LENGTH && force.active() && ! force.complete()) {
      world.updateWithTime(time++);
    }
    return force;
  }
  
  
  static Mission dispatchDefence(Base from, Base goes) {
    World world = from.world;
    
    Mission force = MissionAIUtils.setupDefendMission(goes, from, from.growth.armyPower(), false);
    MissionAIUtils.recruitDefendMission(force, world);
    force.beginMission();

    int time = 0;
    while (time < YEAR_LENGTH && force.active() && ! force.arrived()) {
      world.updateWithTime(time++);
    }
    return force;
  }
  
  
  static Mission runCompleteDialog(Base from, Base goes) {
    World world = from.world;
    
    Mission force = MissionAIUtils.setupDialogMission(goes, from, from.growth.armyPower(), true);
    MissionAIUtils.recruitDialogMission(force, world);
    force.beginMission();
    
    int time = 0;
    while (time < YEAR_LENGTH && force.active() && ! force.complete()) {
      world.updateWithTime(time++);
    }
    return force;
  }
  
  
  static Mission runCompleteExploration(Base from, Area goes) {
    World world = from.world;
    
    Mission force = MissionAIUtils.setupExploreMission(goes, from, from.growth.armyPower(), true);
    MissionAIUtils.recruitExploreMission(force, world);
    force.beginMission();
    
    int time = 0;
    while (time < YEAR_LENGTH && force.active() && ! force.complete()) {
      world.updateWithTime(time++);
    }
    return force;
  }
  
  
  static Mission runCompleteExpedition(Base from, Area goes) {
    World world = from.world;
    
    Mission force = MissionAIUtils.setupSettlerMission(goes, from, from.growth.armyPower(), false);
    MissionAIUtils.recruitSettlerMission(force, world);
    force.beginMission();
    
    int time = 0;
    while (time < YEAR_LENGTH && force.active() && ! force.complete()) {
      world.updateWithTime(time++);
    }
    return force;
  }
  
}











