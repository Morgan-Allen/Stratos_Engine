

package content;
import game.*;
import util.*;
import static content.GameContent.*;
import static game.CityMap.*;
import static game.GameConstants.*;
import static game.Task.*;



public class TestSieging extends Test {
  
  
  public static void main(String args[]) {
    testSieging(true);
  }
  
  
  static boolean testSieging(boolean graphics) {
    Test test = new TestSieging();
    
    World   world = new World(ALL_GOODS);
    City    homeC = new City(world);
    City    awayC = new City(world);
    CityMap map   = CityMapTerrain.generateTerrain(
      homeC, 32, 0, MEADOW, JUNGLE
    );
    homeC.setName("Home City");
    awayC.setName("Away City");
    world.assignCitizenTypes(ALL_CITIZENS, ALL_SOLDIERS, ALL_NOBLES);
    world.addCities(homeC, awayC);
    
    world.settings.toggleFog     = false;
    world.settings.toggleFatigue = false;
    world.settings.toggleHunger  = false;
    
    awayC.initBuildLevels(TROOPER_LODGE, 5, HOLDING, 1);
    awayC.council.setTypeAI(CityCouncil.AI_OFF);
    
    City.setupRoute(homeC, awayC, 1);
    City.setPosture(homeC, awayC, City.POSTURE.ENEMY, true);
    
    CityMapPlanning.placeStructure(SHIELD_WALL, map, true, 4, 4, 20, 20);
    CityMapPlanning.markDemolish(map, true, 6, 6, 16, 16);
    
    Building gate = (Building) BLAST_DOOR.generate();
    gate.setFacing(TileConstants.E);
    gate.enterMap(map, 22, 9, 1);
    
    Building tower = (Building) TURRET.generate();
    tower.setFacing(TileConstants.E);
    tower.enterMap(map, 22, 12, 1);
    
    
    BuildingForArmy fort = (BuildingForArmy) TROOPER_LODGE.generate();
    fort.enterMap(map, 10, 10, 1);
    fillWorkVacancies(fort);
    CityMapPlanning.placeStructure(WALKWAY, map, true, 10, 9, 12, 1 );
    CityMapPlanning.placeStructure(WALKWAY, map, true, 21, 9, 1 , 5 );
    CityMapPlanning.placeStructure(WALKWAY, map, true, 16, 9, 1 , 9 );
    CityMapPlanning.placeStructure(WALKWAY, map, true, 24, 9, 8 , 1 );
    
    for (int n = 3, s = 0; n-- > 0;) {
      Building home = (Building) HOLDING.generate();
      home.enterMap(map, 17, 10 + (n * 3), 1);
      fillHomeVacancies(home, CITIZEN);
      for (Actor a : home.residents()) {
        a.setSexData((s++ % 2 == 0) ? SEX_MALE : SEX_FEMALE);
        if (fort.eligible(a, false)) fort.toggleRecruit(a, true);
      }
    }
    for (Actor a : fort.workers()) {
      fort.toggleRecruit(a, true);
    }
    
    Formation guarding;
    guarding = new Formation(Formation.OBJECTIVE_GARRISON, homeC, false);
    fort.deployInFormation(guarding, true);
    guarding.beginSecuring(tower, TileConstants.E, map);
    
    Building store = (Building) SUPPLY_DEPOT.generate();
    store.enterMap(map, 10, 6, 1);
    store.setInventory(MEDICINE, 10);
    
    float initPrestige = awayC.prestige();
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
    
    while (map.time() < RUN_TIME || graphics) {
      map = test.runLoop(map, 1, graphics, "saves/test_sieging.tlt");
      
      if (! patrolInit) {
        int numPatrol = 0;
        float avgMinDist = 0;
        
        for (Actor a : guarding.recruits()) {
          if (a.at() != guarding.standLocation(a)) continue;
          float minDist = 1000;
          for (Actor o : guarding.recruits()) if (o != a) {
            minDist = Nums.min(minDist, distance(a, o));
          }
          avgMinDist += minDist;
          initPatrolPoints.put(a, a.at());
          numPatrol += 1;
        }
        avgMinDist /= guarding.recruits().size();
        
        if (avgMinDist > 1 && avgMinDist < 4 && numPatrol >= MIN_DEFENDERS) {
          patrolInit = true;
        }
      }
      
      if (patrolInit && ! patrolDone) {
        int numMoved = 0;
        for (Actor a : guarding.recruits()) {
          Tile init = initPatrolPoints.get(a);
          if (init != null && a.at() != init) {
            numMoved += 1;
          }
        }
        if (numMoved >= MIN_DEFENDERS) {
          patrolDone = true;
          awayC.council.setTypeAI(CityCouncil.AI_WARLIKE);
        }
      }
      
      if (patrolDone && ! siegeComing) {
        for (World.Journey j : map.city.world.journeys()) {
          for (Journeys g : j.going()) if (g instanceof Formation) {
            enemy = (Formation) g;
            tribute = enemy.tributeDemand();
            siegeComing = true;
            
            //  TODO:  Add explicit test for this...
            /*
            I.say("Enemy recruits are: ");
            for (Actor a : enemy.recruits) I.say("  "+a);
            I.say("  Total: "+enemy.recruits.size());
            //*/
          }
        }
        if (siegeComing && enemy.objective != Formation.OBJECTIVE_CONQUER) {
          I.say("\nEnemies should be here to conquer!");
          break;
        }
      }
      
      if (siegeComing && enemy.onMap() && ! siegeBegun) {
        
        Table <Tile, Actor> standing = new Table();
        boolean standWrong = false;
        Actor testPathing = null;
        
        for (Actor a : enemy.recruits()) {
          if (a.jobType() == JOB.COMBAT) {
            TaskCombat task = (TaskCombat) a.task();
            
            Element target = task.primary;
            Series <Actor> guards = guarding.recruits();
            boolean isGuard = false, isWall = false;
            
            if (target.type().isActor() && guards.includes((Actor) target)) {
              isGuard = true;
            }
            if (target.type().isWall) {
              isWall = true;
            }
            if (! (isGuard || isWall)) {
              continue;
            }
            
            Tile stands = (Tile) task.target();
            if (standing.get(stands) != null) standWrong = true;
            else standing.put(stands, a);
            testPathing = a;
          }
        }
        
        if (standing.size() > MIN_INVADERS && ! standWrong) {
          siegeBegun = true;
        }
        
        if (testPathing != null) {
          ActorPathSearch search = new ActorPathSearch(testPathing, fort);
          search.doSearch();
          if (search.success()) {
            I.say("\nGatehouse should not allow entry to invaders!");
            break;
          }
        }
      }
      
      if (siegeComing && ! invadeFight) {
        int numFighting = 0;
        for (Actor a : enemy.recruits()) {
          if (a.jobType() != JOB.COMBAT) continue;
          TaskCombat task = (TaskCombat) a.task();
          if (! task.primary.type().isActor()) continue;
          Actor struck = (Actor) task.primary;
          if (guarding.recruits().includes(struck)) numFighting += 1;
        }
        invadeFight = numFighting > MIN_INVADERS / 2;
      }
      
      if (siegeComing && ! defendFight) {
        int numFighting = 0;
        for (Actor a : guarding.recruits()) {
          if (a.jobType() != JOB.COMBAT) continue;
          TaskCombat task = (TaskCombat) a.task();
          if (! task.primary.type().isActor()) continue;
          Actor struck = (Actor) task.primary;
          if (enemy.recruits().includes(struck)) numFighting += 1;
        }
        defendFight = numFighting >= MIN_DEFENDERS / 2;
      }
      
      if (siegeBegun && ! victorious) {
        if (homeC.isVassalOf(awayC)) {
          victorious = true;
          store.addInventory(tribute);
          fillWorkVacancies(store);
        }
      }
      
      if (victorious && ! tributePaid) {
        
        boolean allSent = true;
        for (Good g : tribute.keys()) {
          float need = tribute.valueFor(g);
          float sent = City.suppliesDue(homeC, awayC, g);
          if (sent < need) allSent = false;
        }
        tributePaid = allSent;
        
        if (homeC.funds() > 0) {
          I.say("\nShould not receive payment for tribute!");
          break;
        }
        
        if (awayC.prestige() <= initPrestige) {
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









