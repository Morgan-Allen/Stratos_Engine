

package test;
import game.*;
import content.*;
import util.*;
import static content.GameContent.*;
import static game.AreaMap.*;
import static game.GameConstants.*;
import static game.Task.*;



public class TestSieging extends LogicTest {
  
  
  public static void main(String args[]) {
    testSieging(true);
  }
  
  
  static boolean testSieging(boolean graphics) {
    LogicTest test = new TestSieging();
    
    World   world = new World(ALL_GOODS);
    Base    baseC = new Base(world, world.addLocale(2, 2));
    Base    awayC = new Base(world, world.addLocale(3, 3));
    AreaMap map   = CityMapTerrain.generateTerrain(
      baseC, 32, 0, MEADOW, JUNGLE
    );
    baseC.setName("Home City");
    awayC.setName("Away City");
    world.assignTypes(ALL_BUILDINGS, ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES());
    world.addBases(baseC, awayC);
    
    world.settings.toggleFog     = false;
    world.settings.toggleFatigue = false;
    world.settings.toggleHunger  = false;
    
    
    awayC.initBuildLevels(TROOPER_LODGE, 9, HOLDING, 1);
    awayC.council.setTypeAI(CityCouncil.AI_OFF);
    
    World.setupRoute(baseC.locale, awayC.locale, 1);
    Base.setPosture(baseC, awayC, Base.POSTURE.ENEMY, true);
    
    
    CityMapPlanning.placeStructure(SHIELD_WALL, baseC, true, 4, 4, 20, 20);
    CityMapPlanning.markDemolish(map, true, 6, 6, 16, 16);
    
    map.planning.updatePlanning();
    
    BuildingForWalls gate = (BuildingForWalls) BLAST_DOOR.generate();
    gate.setFacing(TileConstants.E);
    gate.enterMap(map, 22, 9, 1, baseC);
    
    BuildingForWalls tower = (BuildingForWalls) TURRET.generate();
    tower.setFacing(TileConstants.E);
    tower.enterMap(map, 22, 12, 1, baseC);
    
    Actor foe = (Actor) Trooper.TROOPER.generate();
    foe.enterMap(map, 25, 12, 1, awayC);
    
    map.update(1);
    
    if (! Task.inCombat(tower)) {
      I.say("\nTOWER DID NOT REACT TO ENEMIES!");
      return false;
    }
    
    foe.exitMap(map);
    
    
    BuildingForArmy fort = (BuildingForArmy) TROOPER_LODGE.generate();
    fort.enterMap(map, 10, 10, 1, baseC);
    ActorUtils.fillWorkVacancies(fort);
    CityMapPlanning.placeStructure(WALKWAY, baseC, true, 10, 9, 12, 1 );
    CityMapPlanning.placeStructure(WALKWAY, baseC, true, 21, 9, 1 , 5 );
    CityMapPlanning.placeStructure(WALKWAY, baseC, true, 16, 9, 1 , 9 );
    CityMapPlanning.placeStructure(WALKWAY, baseC, true, 24, 9, 8 , 1 );
    
    for (int n = 3; n-- > 0;) {
      Building home = (Building) HOLDING.generate();
      home.enterMap(map, 17, 10 + (n * 3), 1, baseC);
    }
    
    Mission guarding;
    guarding = new Mission(Mission.OBJECTIVE_GARRISON, baseC, false);
    for (Actor w : fort.workers()) guarding.toggleRecruit(w, true);
    guarding.setFocus(tower, TileConstants.E, map);
    
    BuildingForTrade store = (BuildingForTrade) SUPPLY_DEPOT.generate();
    store.enterMap(map, 12, 14, 1, baseC);
    store.setInventory(MEDICINE, 10);
    
    float initPrestige = awayC.prestige();
    float initLoyalty  = baseC.loyalty(awayC);
    Table <Actor, Tile> initPatrolPoints = new Table();
    Mission enemy = null;
    Tally <Good> tribute = null;
    
    boolean patrolInit  = false;
    boolean patrolDone  = false;
    boolean siegeComing = false;
    boolean siegeBegun  = false;
    boolean invadeFight = false;
    boolean towerFight  = false;
    boolean defendFight = false;
    boolean victorious  = false;
    boolean tributePaid = false;
    boolean siegedOkay  = false;
    
    
    int NUM_T = (int) TROOPER_LODGE.workerTypes.valueFor(Trooper.TROOPER);
    final int RUN_TIME = 1000;
    final int MIN_INVADERS  = NUM_T * 2;
    final int MIN_DEFENDERS = NUM_T - 1;
    
    
    while (map.time() < RUN_TIME || graphics) {
      test.runLoop(baseC, 1, graphics, "saves/test_sieging.tlt");
      
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
        for (World.Journey j : map.world.journeys()) {
          for (Journeys g : j.going()) if (g instanceof Mission) {
            enemy = (Mission) g;
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
        if (siegeComing && enemy.objective != Mission.OBJECTIVE_CONQUER) {
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
          if (task.primary.base() == baseC) numFighting += 1;
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
      
      if (siegeComing && ! towerFight) {
        towerFight = Task.inCombat(tower);
      }
      
      if (siegeBegun && ! victorious) {
        if (baseC.isVassalOf(awayC)) {
          victorious = true;
          store.addInventory(tribute);
          store.prodLevels().add(tribute);
          ActorUtils.fillWorkVacancies(store);
        }
      }
      
      if (victorious && ! tributePaid) {
        
        boolean allSent = true;
        for (Good g : tribute.keys()) {
          float need = tribute.valueFor(g);
          float sent = Base.goodsSent(baseC, awayC, g);
          if (sent < need) allSent = false;
        }
        tributePaid = allSent;
        
        if (baseC.funds() > 0) {
          I.say("\nShould not receive payment for tribute!");
          break;
        }
        
        if (awayC.prestige() <= initPrestige) {
          I.say("\nPrestige should be boosted by conquest!");
          break;
        }
        
        if (baseC.loyalty(awayC) >= initLoyalty) {
          I.say("\nLoyalty should be reduced by conquest!");
          break;
        }
      }
      
      if (
        invadeFight && defendFight && towerFight &&
        tributePaid && ! siegedOkay
      ) {
        siegedOkay = true;
        I.say("\nSIEGING TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return true;
      }
    }
    
    I.say("\nSIEGING TEST FAILED!");
    I.say("  Patrol init:  "+patrolInit );
    I.say("  Patrol done:  "+patrolDone );
    I.say("  Siege coming: "+siegeComing);
    I.say("  Siege begun:  "+siegeBegun );
    I.say("  Invade fight: "+invadeFight);
    I.say("  Defend fight: "+defendFight);
    I.say("  Tower fight:  "+towerFight );
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
      fights.assignBase(cityB);
      enemies.toggleRecruit(fights, true);
    }
    enemies.beginSecuring(cityA);
    Tally <Good> tribute = new Tally().setWith(COTTON, 10);
    enemies.assignDemands(City.POSTURE.VASSAL, null, tribute);

  //*/









