

package test;
import static content.GameContent.*;
import static content.GameWorld.*;
import static game.GameConstants.*;
import static game.BaseRelations.*;
import game.*;
import util.*;




public class TestWorld3 extends LogicTest {
  

  
  public static void main(String args[]) {
    testWorld(false);
  }
  
  
  static boolean testWorld(boolean graphics) {
    
    
    //  This tests for the basic outcomes of a single invasion attempt:
    {
      Base pair[] = configWeakStrongBasePair();
      Base goes = pair[0], from = pair[1];
      float oldPower = goes.armyPower();
      Mission strike = runCompleteInvasion(from, goes);
      float newPower = goes.armyPower();
      
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
      Base triple[] = configBases(FACTION_SETTLERS_A, FACTION_SETTLERS_A, FACTION_SETTLERS_B);
      Base a = triple[0], b = triple[1], c = triple[2];
      
      a.initBuildLevels(HOLDING, 9f, TROOPER_LODGE, 6f);
      b.initBuildLevels(HOLDING, 1f, TROOPER_LODGE, 0f);
      c.initBuildLevels(HOLDING, 4f, TROOPER_LODGE, 1f);
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
    
    // This tests for the outcome of a basic exploration attempt-
    {
      Base pair[] = configWeakStrongBasePair();
      Base goes = pair[0], from = pair[1];
      runCompleteExploration(from, goes);
      
      if (from.federation().exploreLevel(goes.area) < 0.5f) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Exploration did not reveal area!");
        return false;
      }
    }
    
    //  All done-
    return true;
  }
  

  
  
  
  /**  Individual subtests-
    */
  static Base[] configWeakStrongBasePair() {
    Base bases[] = configBases(FACTION_SETTLERS_A, FACTION_SETTLERS_B);
    Base a = bases[0], b = bases[1];
    a.setName("Victim Base");
    b.setName("Invader Base");
    b.federation().assignCapital(b);
    a.initBuildLevels(HOLDING, 3f, TROOPER_LODGE, 1f);
    b.initBuildLevels(HOLDING, 9f, TROOPER_LODGE, 6f);
    return bases;
  }
  
  static Base[] configBases(Faction... belong) {
    
    World world = new World(ALL_GOODS);
    world.assignTypes(
      ALL_BUILDINGS, ALL_SHIPS(), ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES()
    );

    Base bases[] = new Base[belong.length];
    for (int i = 0; i < bases.length; i++) {
      Base b = bases[i] = new Base(world, addArea(world, 0, i, i), belong[i]);
      b.setName("City No. "+i);
      b.federation().setTypeAI(Federation.AI_OFF);
      b.federation().relations.initPrestige(BaseRelations.PRESTIGE_MAX);
      
      for (int n = i; n-- > 0;) {
        AreaType.setupRoute(b.area.type, bases[n].area.type, 1, Type.MOVE_LAND);
      }
    }
    
    world.addBases(bases);
    return bases;
  }
  
  
  static Mission runCompleteInvasion(Base from, Base goes) {
    World world = from.world;
    
    Mission force = MissionAIUtils.setupStrikeMission(goes, from, from.armyPower(), false);
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
    
    Mission force = MissionAIUtils.setupDefendMission(goes, from, from.armyPower(), false);
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
    
    Mission force = MissionAIUtils.setupDialogMission(goes, from, from.armyPower(), true);
    MissionAIUtils.recruitDialogMission(force, world);
    force.beginMission();
    
    int time = 0;
    while (time < YEAR_LENGTH && force.active() && ! force.complete()) {
      world.updateWithTime(time++);
    }
    return force;
  }
  
  
  static Mission runCompleteExploration(Base from, Base goes) {
    World world = from.world;
    
    Mission force = MissionAIUtils.setupExploreMission(goes, from, from.armyPower(), true);
    MissionAIUtils.recruitExploreMission(force, world);
    force.beginMission();
    
    int time = 0;
    while (time < YEAR_LENGTH && force.active() && ! force.complete()) {
      world.updateWithTime(time++);
    }
    return force;
  }
}
