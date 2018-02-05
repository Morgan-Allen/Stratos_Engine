

package test;
import game.*;
import content.*;
import util.*;
import static content.GameContent.*;
import static game.GameConstants.*;



public class TestMilitary extends Test {
  
  
  public static void main(String args[]) {
    testMilitary(true);
  }
  
  
  static boolean testMilitary(boolean graphics) {
    Test test = new TestMilitary();

    World   world = new World(ALL_GOODS);
    City    baseC = new City(world, world.addLocale(2, 2));
    City    awayC = new City(world, world.addLocale(3, 3));
    CityMap map   = CityMapTerrain.generateTerrain(
      baseC, 32, 0, MEADOW, JUNGLE
    );
    world.assignTypes(ALL_BUILDINGS, ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES());
    world.addCities(baseC, awayC);
    baseC.setName("Home City");
    awayC.setName("Away City");
    awayC.council.setTypeAI(CityCouncil.AI_OFF);
    world.settings.toggleFog = false;
    
    World.setupRoute(baseC.locale, awayC.locale, 1);
    City.setPosture(baseC, awayC, City.POSTURE.ENEMY, true);
    
    
    BuildingForArmy fort = (BuildingForArmy) TROOPER_LODGE.generate();
    fort.enterMap(map, 10, 10, 1, baseC);
    fillWorkVacancies(fort);
    CityMapPlanning.placeStructure(WALKWAY, baseC, true, 2, 9, 30, 1);
    
    for (int n = 8; n-- > 0;) {
      Building house = (Building) HOLDING.generate();
      house.enterMap(map, 2 + (n * 3), 7, 1, baseC);
      fillHomeVacancies(house, Vassals.PYON);
      for (Actor a : house.residents()) a.setSexData(SEX_MALE);
    }
    
    float initPrestige = baseC.prestige();
    float initLoyalty  = awayC.loyalty(baseC);
    
    
    Mission troops  = null;
    Mission enemies = new Mission(Mission.OBJECTIVE_CONQUER, awayC, true);
    for (int n = 4; n-- > 0;) {
      Actor fights = (Actor) ((n == 0) ? Trooper.TROOPER : Vassals.PYON).generate();
      fights.assignHomeCity(awayC);
      enemies.toggleRecruit(fights, true);
    }
    awayC.setArmyPower(AVG_ARMY_POWER / 4);
    
    
    boolean recruited = false;
    boolean invaded   = false;
    boolean homeWin   = false;
    boolean invading  = false;
    boolean awayWin   = false;
    boolean backHome  = false;
    
    Trait COMBAT_SKILLS[] = { SKILL_MELEE, SKILL_RANGE, SKILL_EVADE };
    Table initSkills = null;
    Batch <Actor> fromTroops = new Batch();
    
    while (map.time() < 1000 || graphics) {
      test.runLoop(baseC, 10, graphics, "saves/test_military.tlt");
      
      if (fort.recruits().size() >= 8 && ! recruited) {
        troops = new Mission(Mission.OBJECTIVE_GARRISON, baseC, false);
        fort.deployOnMission(troops, true);
        troops.setFocus(map.tileAt(25, 25), TileConstants.E, map);
        
        Visit.appendTo(fromTroops, troops.recruits());
        initSkills = recordSkills(fromTroops, COMBAT_SKILLS);
        recruited = true;
      }
      
      if (troops != null && troops.formationReady() && ! invaded) {
        enemies.setFocus(baseC);
        World.Journey j = world.journeyFor(enemies);
        world.completeJourney(j);
        enemies.setFocus(troops, TileConstants.W, map);
        invaded = true;
      }
      
      if (recruited && ! homeWin) {
        boolean survivors = false;
        for (Actor w : enemies.recruits()) {
          if (w.alive()) survivors = true;
        }
        homeWin = ! survivors;
        if (homeWin) {
          troops.disbandFormation();
          fillAllVacancies(map, Vassals.PYON);
        }
      }
      
      if (homeWin && fort.recruits().size() >= 12 && ! invading) {
        troops = new Mission(Mission.OBJECTIVE_CONQUER, baseC, false);
        fort.deployOnMission(troops, true);
        troops.setFocus(awayC);
        troops.assignTerms(City.POSTURE.VASSAL, null, null, null);
        invading = true;
      }
      
      if (invading && baseC.isLordOf(awayC)) {
        awayWin = true;
      }
      
      if (awayWin && ! backHome) {
        boolean someAway = false;
        for (Actor w : fort.recruits()) {
          if (w.map() != map) someAway = true;
        }
        backHome = fort.recruits().size() > 8 && ! someAway;
        
        if (baseC.prestige() <= initPrestige) {
          I.say("\nPrestige should be boosted by conquest!");
          break;
        }
        
        if (awayC.loyalty(baseC) >= initLoyalty) {
          I.say("\nLoyalty should be reduced by conquest!");
          break;
        }
        
        if (backHome) {
          I.say("\nMILITARY TEST CONCLUDED SUCCESSFULLY!");
          reportSkills(fromTroops, COMBAT_SKILLS, initSkills);
          if (! graphics) return true;
        }
      }
    }
    
    I.say("\nMILITARY TEST FAILED!");
    I.say("  Recruited: "+recruited);
    I.say("  Invaded:   "+invaded  );
    I.say("  Home win:  "+homeWin  );
    I.say("  Invading:  "+invading );
    I.say("  Away win:  "+awayWin  );
    I.say("  Back home: "+backHome );
    I.say("  Current recuits: "+fort.recruits().size());
    reportSkills(fromTroops, COMBAT_SKILLS, initSkills);
    
    return false;
  }
  
  
  static Table recordSkills(Series <Actor> actors, Trait skills[]) {
    Table <String, Float> record = new Table();
    for (Actor a : actors) {
      for (Trait skill : skills) {
        String keyL = a.ID()+"_"+skill+"_L";
        record.put(keyL, a.levelOf(skill));
      }
    }
    return record;
  }
  
  
  static void reportSkills(Series <Actor> actors, Trait skills[], Table init) {
    Table ends = recordSkills(actors, skills);
    
    I.say("\nReporting experience gained.");
    for (Actor a : actors) {
      for (Trait skill : skills) {
        String keyL = a.ID()+"_"+skill+"_L";
        float startS = (Float) init.get(keyL);
        float endS   = (Float) ends.get(keyL);
        if (endS <= startS) continue;
        I.say("  "+a+" "+skill+": "+startS+" -> "+endS);
      }
    }
  }
  
  
  
}











