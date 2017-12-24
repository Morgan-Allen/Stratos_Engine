

package game;
import util.*;
import static game.GameConstants.*;



public class TestMilitary extends Test {
  
  
  public static void main(String args[]) {
    testMilitary(true);
  }
  
  
  static boolean testMilitary(boolean graphics) {
    Test test = new TestMilitary();
    
    World   world = GameConstants.setupDefaultWorld();
    City    homeC = world.cities.atIndex(0);
    City    awayC = world.cities.atIndex(1);
    CityMap map   = CityMapTerrain.generateTerrain(
      homeC, 32, 0, MEADOW, JUNGLE
    );
    homeC.name = "Home City";
    awayC.name = "Away City";
    awayC.council.typeAI = CityCouncil.AI_OFF;
    map.settings.toggleFog = false;
    
    City.setupRoute(homeC, awayC, 1);
    City.setPosture(homeC, awayC, City.POSTURE.ENEMY, true);
    
    
    BuildingForArmy fort = (BuildingForArmy) GARRISON.generate();
    fort.enterMap(map, 10, 10, 1);
    fillWorkVacancies(fort);
    CityMapPlanning.placeStructure(ROAD, map, true, 2, 9, 30, 1);
    
    for (int n = 8; n-- > 0;) {
      Building house = (Building) HOUSE.generate();
      house.enterMap(map, 2 + (n * 3), 7, 1);
      fillHomeVacancies(house, CITIZEN);
      for (Actor a : house.residents) a.sexData = SEX_MALE;
    }
    
    float initPrestige = homeC.prestige;
    float initLoyalty  = awayC.loyalty(homeC);
    
    
    Formation troops  = null;
    Formation enemies = new Formation(Formation.OBJECTIVE_CONQUER, awayC, true);
    for (int n = 4; n-- > 0;) {
      Actor fights = (Actor) ((n == 0) ? SOLDIER : CITIZEN).generate();
      fights.assignHomeCity(awayC);
      enemies.toggleRecruit(fights, true);
    }
    awayC.armyPower = AVG_ARMY_POWER / 4;
    
    
    boolean recruited = false;
    boolean invaded   = false;
    boolean homeWin   = false;
    boolean invading  = false;
    boolean awayWin   = false;
    boolean backHome  = false;
    
    Trait COMBAT_SKILLS[] = { SKILL_MELEE, SKILL_RANGE, SKILL_EVADE };
    Table initSkills = null;
    Batch <Actor> fromTroops = new Batch();
    
    while (map.time < 1000 || graphics) {
      map = test.runLoop(map, 10, graphics, "saves/test_military.tlt");
      
      if (fort.recruits.size() >= 8 && ! recruited) {
        troops = new Formation(Formation.OBJECTIVE_GARRISON, homeC, false);
        fort.deployInFormation(troops, true);
        troops.beginSecuring(map.tileAt(25, 25), TileConstants.E, map);
        
        Visit.appendTo(fromTroops, troops.recruits);
        initSkills = recordSkills(fromTroops, COMBAT_SKILLS);
        recruited = true;
      }
      
      if (troops != null && troops.formationReady() && ! invaded) {
        enemies.beginSecuring(homeC);
        World.Journey j = world.journeyFor(enemies);
        world.completeJourney(j);
        enemies.beginSecuring(troops, TileConstants.W, map);
        invaded = true;
      }
      
      if (recruited && ! homeWin) {
        boolean survivors = false;
        for (Actor w : enemies.recruits) {
          if (w.state < Actor.STATE_DEAD) survivors = true;
        }
        homeWin = ! survivors;
        if (homeWin) {
          troops.disbandFormation();
          fillAllVacancies(map);
        }
      }
      
      if (homeWin && fort.recruits.size() >= 12 && ! invading) {
        troops = new Formation(Formation.OBJECTIVE_CONQUER, homeC, false);
        fort.deployInFormation(troops, true);
        troops.beginSecuring(awayC);
        troops.assignTerms(City.POSTURE.VASSAL, null, null, null);
        invading = true;
      }
      
      if (invading && homeC.isLordOf(awayC)) {
        awayWin = true;
      }
      
      if (awayWin && ! backHome) {
        boolean someAway = false;
        for (Actor w : fort.recruits) {
          if (w.map != map) someAway = true;
        }
        backHome = fort.recruits.size() > 8 && ! someAway;
        
        if (homeC.prestige <= initPrestige) {
          I.say("\nPrestige should be boosted by conquest!");
          break;
        }
        
        if (awayC.loyalty(homeC) >= initLoyalty) {
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
        String keyL = a.ID+"_"+skill+"_L";
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
        String keyL = a.ID+"_"+skill+"_L";
        float startS = (Float) init.get(keyL);
        float endS   = (Float) ends.get(keyL);
        if (endS <= startS) continue;
        I.say("  "+a+" "+skill+": "+startS+" -> "+endS);
      }
    }
  }
  
  
  
}












