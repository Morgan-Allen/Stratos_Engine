

package test;
import game.*;
import content.*;
import util.*;
import static content.GameContent.*;
import static content.GameWorld.*;
import static game.GameConstants.*;



public class TestMilitary extends LogicTest {
  
  
  public static void main(String args[]) {
    testMilitary(true);
  }
  
  
  static boolean testMilitary(boolean graphics) {
    LogicTest test = new TestMilitary();
    
    World world = new World(ALL_GOODS);
    Base  baseC = new Base(world, world.addLocale(2, 2), FACTION_SETTLERS_A);
    Base  awayC = new Base(world, world.addLocale(3, 3), FACTION_SETTLERS_B);
    Area  map   = AreaTerrain.generateTerrain(
      baseC, 32, 0, MEADOW, JUNGLE
    );
    world.assignTypes(
      ALL_BUILDINGS, ALL_SHIPS(), ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES()
    );
    world.addBases(baseC, awayC);
    world.setPlayerFaction(FACTION_SETTLERS_A);
    baseC.setName("Home City");
    awayC.setName("Away City");
    awayC.federation().setTypeAI(Federation.AI_OFF);
    
    world.settings.toggleFog = false;
    
    World.setupRoute(baseC.locale, awayC.locale, 1, Type.MOVE_LAND);
    Federation.setPosture(
      baseC.faction(), awayC.faction(),
      RelationSet.BOND_ENEMY, world
    );
    awayC.setArmyPower(0);
    
    
    BuildingForArmy fort1 = (BuildingForArmy) TROOPER_LODGE.generate();
    fort1.enterMap(map, 10, 10, 1, baseC);
    ActorUtils.fillWorkVacancies(fort1);
    BuildingForArmy fort2 = (BuildingForArmy) TROOPER_LODGE.generate();
    fort2.enterMap(map, 14, 10, 1, baseC);
    ActorUtils.fillWorkVacancies(fort2);
    AreaPlanning.placeStructure(WALKWAY, baseC, true, 2, 9, 30, 1);
    
    {
      Actor troop = fort1.workers().first();
      I.say("\nReporting stats for "+troop);
      I.say("  Melee damage:   "+troop.meleeDamage());
      I.say("  Range damage:   "+troop.rangeDamage());
      I.say("  Range distance: "+troop.attackRange());
      I.say("  Armour class:   "+troop.armourClass());
    }
    
    for (int n = 8; n-- > 0;) {
      Building house = (Building) HOLDING.generate();
      house.enterMap(map, 2 + (n * 3), 7, 1, baseC);
    }
    
    float initPrestige = baseC.federation().relations.prestige();
    float initLoyalty  = awayC.relations.bondLevel(baseC.faction());
    int numHome = 0, numTroops = 0;
    
    MissionForSecure defence = null;
    MissionForStrike offence = null;
    Mission enemies = new MissionForStrike(awayC);
    Batch <Actor> enemyRecruits = new Batch();
    for (int n = 2; n-- > 0;) {
      Actor fights = (Actor) Trooper.TROOPER.generate();
      fights.assignBase(awayC);
      enemies.toggleRecruit(fights, true);
      enemyRecruits.add(fights);
    }
    
    boolean recruited = false;
    boolean invaded   = false;
    boolean homeWin   = false;
    boolean invading  = false;
    boolean awayWin   = false;
    boolean backHome  = false;
    
    Trait COMBAT_SKILLS[] = { SKILL_MELEE, SKILL_SIGHT, SKILL_EVADE };
    Faction HOME_FACTION = FACTION_SETTLERS_A;
    Table initSkills = null;
    Batch <Actor> fromTroops = new Batch();
    
    while (map.time() < 1000 || graphics) {
      test.runLoop(baseC, 10, graphics, "saves/test_military.tlt");
      
      if (! recruited) {
        defence = new MissionForSecure(baseC);
        for (Actor w : fort1.workers()) defence.toggleRecruit(w, true);
        for (Actor w : fort2.workers()) defence.toggleRecruit(w, true);
        defence.setLocalFocus(map.tileAt(25, 25));
        defence.beginMission();
        
        Visit.appendTo(fromTroops, defence.recruits());
        initSkills = recordSkills(fromTroops, COMBAT_SKILLS);
        recruited = true;
      }
      
      if (defence != null && defence.assembled() && ! invaded) {
        enemies.setWorldFocus(baseC);
        enemies.beginMission();
        map.update(1);
        
        World.Journey j = world.journeyFor(enemies);
        world.completeJourney(j);
        enemies.setLocalFocus((Actor) Rand.pickFrom(defence.recruits()));
        invaded = true;
      }
      
      if (! homeWin) {
        boolean survivors = false;
        for (Actor w : enemyRecruits) {
          if (w.health.alive()) survivors = true;
        }
        homeWin = ! survivors;
        if (homeWin) {
          defence.disbandMission();
          ActorUtils.fillAllWorkVacancies(map);
        }
      }
      
      if (homeWin && ! invading) {
        offence = new MissionForStrike(baseC);
        for (Actor w : fort1.workers()) offence.toggleRecruit(w, true);
        for (Actor w : fort2.workers()) offence.toggleRecruit(w, true);
        offence.setWorldFocus(awayC);
        offence.terms.assignTerms(RelationSet.BOND_VASSAL, null, null, null);
        offence.beginMission();
        invading = true;
      }
      
      if (invading && ! awayWin) {
        awayWin = awayC.faction() == HOME_FACTION;
      }
      
      if (awayWin && ! backHome) {
        
        numHome = 0;
        for (Actor w : fort1.workers()) if (w.map() == map) numHome += 1;
        for (Actor w : fort2.workers()) if (w.map() == map) numHome += 1;
        numTroops = fort1.workers().size() + fort2.workers().size();
        backHome = numTroops > 0 && numHome > (numTroops / 2);
        
        if (baseC.federation().relations.prestige() <= initPrestige) {
          I.say("\nPrestige should be boosted by conquest!");
          I.say("  "+baseC+" From "+initPrestige+" -> "+baseC.federation().relations.prestige());
          break;
        }
        
        if (awayC.relations.bondLevel(baseC.faction()) >= initLoyalty) {
          I.say("\nLoyalty should be reduced by conquest!");
          break;
        }
        
        if (backHome) {
          I.say("\nMILITARY TEST CONCLUDED SUCCESSFULLY!");
          reportSkills(fromTroops, COMBAT_SKILLS, initSkills);
          I.say("  Troops home: "+numHome+"/"+numTroops);
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
    reportSkills(fromTroops, COMBAT_SKILLS, initSkills);
    I.say("  Troops home: "+numHome+"/"+numTroops);
    
    return false;
  }
  
  
  static Table recordSkills(Series <Actor> actors, Trait skills[]) {
    Table <String, Float> record = new Table();
    for (Actor a : actors) {
      for (Trait skill : skills) {
        String keyL = a.ID()+"_"+skill+"_L";
        record.put(keyL, a.traits.levelOf(skill));
      }
      String keyC = a.ID()+"_"+a.type()+"_L";
      record.put(keyC, (float) a.traits.classLevel());
    }
    return record;
  }
  
  
  static void reportSkills(Series <Actor> actors, Trait skills[], Table init) {
    Table ends = recordSkills(actors, skills);
    
    I.say("\nReporting experience gained.");
    for (Actor a : actors) {

      String keyC = a.ID()+"_"+a.type()+"_L";
      int oldCL = (int) (float) (Float) init.get(keyC);
      int newCL = a.traits.classLevel();
      float prog = a.traits.classLevelProgress();
      I.say("  "+a+" (level "+oldCL+" -> "+newCL+" +"+I.percent(prog)+")");
      
      for (Trait skill : skills) {
        String keyL = a.ID()+"_"+skill+"_L";
        float startS = (Float) init.get(keyL);
        float endS   = (Float) ends.get(keyL);
        if (endS <= startS) continue;
        I.say("    "+skill+": "+startS+" -> "+endS);
      }
    }
  }
  
  
  
}












