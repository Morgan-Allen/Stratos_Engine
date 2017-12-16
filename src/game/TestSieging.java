

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;
import static game.Task.*;



public class TestSieging extends Test {
  
  
  public static void main(String args[]) {
    testSieging(false);
  }
  
  
  static boolean testSieging(boolean graphics) {
    Test test = new TestSieging();
    
    World   world = new World();
    City    homeC = new City(world);
    City    awayC = new City(world);
    CityMap map   = CityMapTerrain.generateTerrain(
      homeC, 32, 0, MEADOW, JUNGLE
    );
    homeC.name = "Home City";
    awayC.name = "Away City";
    world.addCities(homeC, awayC);
    
    map.settings.toggleFog     = false;
    map.settings.toggleFatigue = false;
    map.settings.toggleHunger  = false;
    
    awayC.initBuildLevels(GARRISON, 5, HOUSE, 1);
    awayC.council.typeAI = CityCouncil.AI_OFF;
    
    City.setupRoute(homeC, awayC, 1);
    City.setPosture(homeC, awayC, City.POSTURE.ENEMY, true);
    
    CityMapPlanning.placeStructure(WALL, map, true, 4, 4, 20, 20);
    CityMapPlanning.markDemolish(map, true, 6, 6, 16, 16);
    
    Building gate = (Building) GATE.generate();
    gate.setFacing(TileConstants.E);
    gate.enterMap(map, 22, 9, 1);
    
    Building tower = (Building) TOWER.generate();
    tower.setFacing(TileConstants.E);
    tower.enterMap(map, 22, 12, 1);
    
    
    BuildingForArmy fort = (BuildingForArmy) GARRISON.generate();
    fort.enterMap(map, 10, 10, 1);
    fillWorkVacancies(fort);
    CityMapPlanning.placeStructure(ROAD, map, true, 10, 9, 12, 1 );
    CityMapPlanning.placeStructure(ROAD, map, true, 21, 9, 1 , 5 );
    CityMapPlanning.placeStructure(ROAD, map, true, 16, 9, 1 , 9 );
    CityMapPlanning.placeStructure(ROAD, map, true, 24, 9, 8 , 1 );
    
    for (int n = 3; n-- > 0;) {
      Building home = (Building) HOUSE.generate();
      home.enterMap(map, 17, 10 + (n * 3), 1);
      fillHomeVacancies(home, CITIZEN);
      for (Actor a : home.residents) if (fort.eligible(a, false)) {
        fort.toggleRecruit(a, true);
      }
    }
    for (Actor a : fort.workers) {
      fort.toggleRecruit(a, true);
    }
    
    Formation guarding = new Formation(new ObjectivePatrol(), homeC);
    fort.deployInFormation(guarding, true);
    guarding.beginSecuring(tower.at(), TileConstants.E, tower, map);
    
    Building store = (Building) PORTER_POST.generate();
    store.enterMap(map, 10, 6, 1);
    store.inventory.setWith(COTTON, 10);
    
    float initPrestige = awayC.prestige;
    float initLoyalty  = homeC.loyalty(awayC);
    Table <Actor, Tile> initPatrolPoints = new Table();
    Formation enemy = null;
    Tally <Good> tribute = null;
    
    boolean patrolInit  = false;
    boolean patrolDone  = false;
    boolean siegeComing = false;
    boolean siegeBegun  = false;
    boolean invadeFight = false;
    boolean defendFight = false;
    boolean victorious  = false;
    boolean tributePaid = false;
    boolean siegedOkay  = false;
    
    final int RUN_TIME = 1000;
    final int MIN_DEFENDERS = 4;
    final int MIN_INVADERS  = 8;
    
    while (map.time < RUN_TIME || graphics) {
      map = test.runLoop(map, 1, graphics, "saves/test_sieging.tlt");
      
      if (! patrolInit) {
        int numPatrol = 0;
        float avgMinDist = 0;
        
        for (Actor a : guarding.recruits) {
          if (a.at() != guarding.objective.standLocation(a, guarding)) continue;
          float minDist = 1000;
          for (Actor o : guarding.recruits) if (o != a) {
            minDist = Nums.min(minDist, distance(a, o));
          }
          avgMinDist += minDist;
          initPatrolPoints.put(a, a.at());
          numPatrol += 1;
        }
        avgMinDist /= guarding.recruits.size();
        
        if (avgMinDist > 1 && avgMinDist < 4 && numPatrol >= MIN_DEFENDERS) {
          patrolInit = true;
        }
      }
      
      if (patrolInit && ! patrolDone) {
        int numMoved = 0;
        for (Actor a : guarding.recruits) {
          Tile init = initPatrolPoints.get(a);
          if (init != null && a.at() != init) {
            numMoved += 1;
          }
        }
        if (numMoved >= MIN_DEFENDERS) {
          patrolDone = true;
          awayC.council.typeAI = CityCouncil.AI_WARLIKE;
        }
      }
      
      if (patrolDone && ! siegeComing) {
        for (World.Journey j : map.city.world.journeys) {
          Object goes = j.going.first();
          if (goes instanceof Formation) {
            enemy = (Formation) goes;
            tribute = enemy.tributeDemand;
            siegeComing = true;
          }
        }
      }
      
      if (siegeComing && enemy.map != null && ! siegeBegun) {
        Table <Tile, Actor> standing = new Table();
        boolean standWrong = false;
        
        for (Actor a : enemy.recruits) {
          if (a.jobType() == JOB.COMBAT) {
            TaskCombat task = (TaskCombat) a.task;
            if (task.primary != enemy.secureFocus) continue;
            Tile stands = (Tile) task.target;
            if (standing.get(stands) != null) standWrong = true;
            else standing.put(stands, a);
          }
        }
        
        if (standing.size() > MIN_INVADERS && ! standWrong) {
          siegeBegun = true;
        }
      }
      
      if (siegeComing && ! invadeFight) {
        int numFighting = 0;
        for (Actor a : enemy.recruits) {
          if (a.jobType() != JOB.COMBAT) continue;
          TaskCombat task = (TaskCombat) a.task;
          if (! task.primary.type.isActor()) continue;
          Actor struck = (Actor) task.primary;
          if (guarding.recruits.includes(struck)) numFighting += 1;
        }
        invadeFight = numFighting > MIN_INVADERS / 2;
      }
      
      if (siegeComing && ! defendFight) {
        int numFighting = 0;
        for (Actor a : guarding.recruits) {
          if (a.jobType() != JOB.COMBAT) continue;
          TaskCombat task = (TaskCombat) a.task;
          if (! task.primary.type.isActor()) continue;
          Actor struck = (Actor) task.primary;
          if (enemy.recruits.includes(struck)) numFighting += 1;
        }
        defendFight = numFighting >= MIN_DEFENDERS / 2;
      }
      
      if (siegeBegun && ! victorious) {
        if (homeC.isVassalOf(awayC)) {
          victorious = true;
          store.inventory.add(tribute);
          fillWorkVacancies(store);
        }
      }
      
      if (victorious && ! tributePaid) {
        
        City.Relation r = homeC.relationWith(awayC);
        boolean allSent = true;
        for (Good g : tribute.keys()) {
          float need = tribute.valueFor(g);
          float sent = r.suppliesSent.valueFor(g);
          if (sent < need) allSent = false;
        }
        tributePaid = allSent;
        
        if (homeC.currentFunds > 0) {
          I.say("\nShould not receive payment for tribute!");
          break;
        }
        
        if (awayC.prestige <= initPrestige) {
          I.say("\nPrestige should be boosted by conquest!");
          break;
        }
        
        if (homeC.loyalty(awayC) >= initLoyalty) {
          I.say("\nLoyalty should be reduced by conquest!");
          break;
        }
      }
      
      if (invadeFight && defendFight && tributePaid && ! siegedOkay) {
        siegedOkay = true;
        I.say("\nSIEGING TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return true;
      }
    }
    
    I.say("\nSIEGE TEST FAILED!");
    I.say("  Patrol init:  "+patrolInit );
    I.say("  Patrol done:  "+patrolDone );
    I.say("  Siege coming: "+siegeComing);
    I.say("  Siege begun:  "+siegeBegun );
    I.say("  Invade fight: "+invadeFight);
    I.say("  Defend fight: "+defendFight);
    I.say("  Victorious:   "+victorious );
    I.say("  Tribute paid: "+tributePaid);
    I.say("  Sieged okay:  "+siegedOkay );
    
    return false;
  }
  
  
  
  //  Older invasion-setup code, kept for posterity.
  /*
    Formation enemies = new Formation();
    enemies.setupFormation(GARRISON, cityB);
    
    for (int n = 8; n-- > 0;) {
      Actor fights = (Actor) ((n < 3) ? SOLDIER : CITIZEN).generate();
      fights.assignHomeCity(cityB);
      enemies.toggleRecruit(fights, true);
    }
    enemies.beginSecuring(cityA);
    Tally <Good> tribute = new Tally().setWith(COTTON, 10);
    enemies.assignDemands(City.POSTURE.VASSAL, null, tribute);

  //*/
  
}









