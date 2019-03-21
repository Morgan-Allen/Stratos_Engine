

package test;
import game.*;
import game.Task.JOB;
import util.*;
import static content.GameContent.*;
import static content.GameWorld.*;
import static game.GameConstants.*;



public class TestBounties extends LogicTest {
  
  
  public static void main(String args[]) {
    testAttackBuildingMission(false);
    testAttackActorMission   (false);
    testExploreAreaMission   (false);
    testDefendBuildingMission(false);
  }
  
  
  //  TODO:
  //  What are the cases I need to cover here?
  
  //  Defend  bounty on actor.
  //  Explore bounty on actor.
  //  Contact bounty on actor.
  //  Contact bounty on building.
  
  
  
  static boolean testAttackBuildingMission(boolean graphics) {
    TestBounties test = new TestBounties() {
      
      Mission setupMission(AreaMap map, Base base) {
        BuildingForNest nest = (BuildingForNest) RUINS_LAIR.generate();
        nest.enterMap(map, 20, 20, 1, map.area.locals);
        
        Mission mission;
        mission = new MissionForStrike(base);
        mission.setLocalFocus(nest);
        return mission;
      }
      
      boolean checkVictory(AreaMap map, Base base, Object focus) {
        Building nest = (Building) focus;
        return TaskCombat.beaten(nest);
      }
    };
    return test.bountyTest(graphics, "ATTACK BUILDING MISSION");
  }

  
  static boolean testAttackActorMission(boolean graphics) {
    TestBounties test = new TestBounties() {
      
      Mission setupMission(AreaMap map, Base base) {
        Actor creature = (Actor) MICOVORE.generate();
        creature.enterMap(map, 20, 20, 1, map.area.locals);
        creature.takeDamage(creature.health.maxHealth() * 0.7f);
        creature.health.incBleed(-1000);
        
        MissionForStrike mission = new MissionForStrike(base);
        mission.setLocalFocus(creature);
        return mission;
      }
      
      boolean checkVictory(AreaMap map, Base base, Object focus) {
        Actor creature = (Actor) focus;
        return TaskCombat.beaten(creature);
      }
    };
    return test.bountyTest(graphics, "ATTACK ACTOR MISSION");
  }
  
  
  static boolean testExploreAreaMission(boolean graphics) {
    TestBounties test = new TestBounties() {
      int RANGE = 8;
      
      Mission setupMission(AreaMap map, Base base) {
        AreaTile looks = map.tileAt(20, 20);
        
        MissionForRecon mission = new MissionForRecon(base);
        mission.setLocalFocus(looks);
        mission.setExploreRange(RANGE);
        return mission;
      }
      
      boolean checkVictory(AreaMap map, Base base, Object focus) {
        AreaTile looks = (AreaTile) focus;
        AreaFog fog = map.fogMap(base.faction(), true);
        int r = RANGE;
        boolean allSeen = true;
        
        for (AreaTile t : map.tilesUnder(looks.x - r, looks.y - r, r * 2, r * 2)) {
          float dist = AreaMap.distance(looks, t);
          if (dist > r) continue;
          if (fog.maxSightLevel(t) == 0) allSeen = false;
        }
        return allSeen;
      }
    };
    return test.bountyTest(graphics, "EXPLORE AREA MISSION");
  }
  
  
  static boolean testDefendBuildingMission(boolean graphics) {
    TestBounties test = new TestBounties() {
      
      Building guarded;
      MissionForSecure mission;
      
      Actor threat;
      boolean triedAttack;
      boolean didRespond;
      
      
      Mission setupMission(AreaMap map, Base base) {
        
        map.world.settings.toggleFog = false;
        
        guarded = (Building) STOCK_EXCHANGE.generate();
        guarded.enterMap(map, 2, 10, 1, base);
        
        threat = (Actor) TRIPOD.generate();
        threat.enterMap(map, 30, 30, 1, map.area.locals);
        threat.takeDamage(threat.health.maxHealth() * 0.7f);
        
        mission = new MissionForSecure(base);
        mission.setLocalFocus(guarded);
        return mission;
      }
      
      void onMapUpdate(AreaMap map, Base base) {
        if (threat.jobType() != JOB.COMBAT) {
          TaskCombat siege = TaskCombat.configCombat(threat, guarded);
          threat.assignTask(siege, threat);
        }
      }
      
      boolean checkVictory(AreaMap map, Base base, Object focus) {
        if (TaskCombat.killed((Building) focus)) return false;
        
        if (! triedAttack) {
          if (threat.jobType() == JOB.COMBAT) {
            TaskCombat task = (TaskCombat) threat.task();
            if (task.primary == guarded) triedAttack = true;
          }
        }
        
        if (! didRespond) for (Actor a : mission.recruits()) {
          if (a.jobType() == JOB.COMBAT) {
            TaskCombat task = (TaskCombat) a.task();
            if (task.primary == threat) didRespond = true;
          }
        }
        
        return triedAttack && didRespond;
      }
    };
    return test.bountyTest(graphics, "DEFEND BUILDING MISSION");
  }
  
  
  
  boolean bountyTest(boolean graphics, String title) {
    
    Base base = LogicTest.setupTestBase(BASE, FACTION_SETTLERS_A, ALL_GOODS, 32, false);
    base.setName("Client Base");
    AreaMap map = base.activeMap();
    
    int initFunds = 1000, reward = 500;
    base.initFunds(initFunds);
    
    BuildingForArmy fort = (BuildingForArmy) TROOPER_LODGE.generate();
    fort.enterMap(map, 2, 2, 1, base);
    ActorUtils.fillWorkVacancies(fort);
    
    float initActorFunds = 0;
    for (Actor a : fort.workers()) initActorFunds += a.outfit.carried(CASH);

    map.update(1);
    
    Mission mission = setupMission(map, base);
    mission.rewards.setAsBounty(reward);
    mission.beginMission();
    
    Target focus = mission.localFocus();
    Actor sample = fort.workers().first();
    Task given = mission.selectActorBehaviour(sample);
    
    
    if (given == null) {
      I.say("\n"+title+" TEST FAILED!");
      I.say("  No task provided for actor: "+sample);
      return false;
    }
    
    try {
      Session.saveSession("saves/test_save.tlt", map);
      Session session = Session.loadSession("saves/test_save.tlt", true);
      AreaMap loaded = (AreaMap) session.loaded()[0];
      
      Mission m = loaded.world.baseNamed("Client Base").missions().first();
      if (m.localFocus() == null) I.say("LOADED MISSION HAS NO FOCUS!");
      
      I.say("\nSuccessfully loaded map: "+loaded);
    }
    catch(Exception e) {
      I.report(e);
      return false;
    }
    
    
    boolean fundsTaken     = false;
    boolean missionTaken   = false;
    boolean targetFinished = false;
    boolean rewardSplit    = false;
    boolean testOkay       = false;
    
    while (map.time() < 1000 || graphics) {
      runLoop(base, 1, graphics, "saves/test_bounties.str");
      
      onMapUpdate(map, base);
      
      for (Actor a : fort.workers()) {
        if (a.health.injury() > 0) a.health.liftDamage(a.health.injury());
      }
      
      if (! fundsTaken) {
        fundsTaken = mission.rewards.cashReward() == initFunds - base.funds();
      }
      
      if (fundsTaken && ! missionTaken) {
        boolean someOn = false;
        for (Actor a : fort.workers()) {
          if (a.mission() == mission) someOn = true;
        }
        missionTaken = someOn;
      }
      
      if (missionTaken && ! targetFinished) {
        targetFinished = checkVictory(map, base, focus);
      }
      
      if (targetFinished && ! rewardSplit) {
        float actorFunds = 0;
        for (Actor a : fort.workers()) actorFunds += a.outfit.carried(CASH);
        float diff = Nums.abs(initActorFunds + reward - actorFunds);
        if (diff <= 1) rewardSplit = true;
      }
      
      if (rewardSplit && ! testOkay) {
        testOkay = true;
        I.say("\n"+title+" TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return true;
      }
    }
    
    I.say("\n"+title+" TEST FAILED!");
    I.say("  Funds taken:     "+fundsTaken    );
    I.say("  Mission taken:   "+missionTaken  );
    I.say("  Target finished: "+targetFinished);
    I.say("  Reward split:    "+rewardSplit   );
    
    return false;
  }
  
  
  Mission setupMission(AreaMap map, Base base) {
    return null;
  }
  
  
  void onMapUpdate(AreaMap map, Base base) {
    return;
  }
  

  boolean checkVictory(AreaMap map, Base base, Object focus) {
    return false;
  }
  
}









