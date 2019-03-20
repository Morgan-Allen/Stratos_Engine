


package test;
import game.*;
import content.*;
import util.*;
import static game.Base.*;
import static game.Task.*;
import static game.GameConstants.*;
import static content.GameContent.*;
import static content.GameWorld.*;



public class TestRetreat extends LogicTest {
  
  
  public static void main(String args[]) {
    testRetreat(true);
  }
  
  
  static boolean testRetreat(boolean graphics) {
    TestRetreat test = new TestRetreat();
    
    Base base = LogicTest.setupTestBase(FACTION_SETTLERS_A, ALL_GOODS, 32, false);
    AreaMap map = base.activeMap();
    World world = base.world;
    
    Base enemyBase = new Base(world, map.area, FACTION_SETTLERS_B, "Enemy Base");
    enemyBase.area.attachMap(map);
    map.area.addBase(enemyBase);
    Federation.setPosture(
      base.faction(), enemyBase.faction(),
      RelationSet.BOND_ENEMY, world
    );
    
    world.settings.toggleFatigue = false;
    world.settings.toggleInjury  = false;
    world.settings.toggleFog     = false;
    
    //  Generate an actor.
    Building home = (Building) ECOLOGIST_STATION.generate();
    home.enterMap(map, 2, 2, 1, base);
    Actor subject = ActorUtils.spawnActor(home, ECOLOGIST, false);
    subject.setInside(subject.inside(), false);
    subject.setLocation(map.tileAt(20, 20), map);
    float nearRange = subject.sightRange() - 1;
    
    //  Generate some enemies.  See if s/he runs.
    Batch <Actor> enemies = new Batch();
    for (int n = 3; n-- > 0;) {
      Actor enemy = (Actor) Trooper.TROOPER.generate();
      enemy.assignBase(enemyBase);
      enemies.add(enemy);
    }
    
    //  Generate some allies.  See if s/he stands their ground.
    Batch <Actor> allies = new Batch();
    for (int n = 5; n-- > 0;) {
      Actor ally = (Actor) Trooper.TROOPER.generate();
      ally.assignBase(base);
      allies.add(ally);
    }
    
    for (int n = 3; n-- > 0;) map.update(1);
    
    boolean braveOkay   = false;
    boolean enemiesDone = false;
    boolean scareOkay   = false;
    boolean alliesDone  = false;
    boolean braveAgain  = false;
    boolean testOkay    = false;
    
    while (map.time() < 100 || graphics) {
      test.runLoop(base, 1, graphics, "saves/test_retreat.str");
      
      if (! braveOkay) {
        braveOkay = subject.jobType() != JOB.RETREAT;
      }
      
      if (braveOkay && ! enemiesDone) {
        for (Actor enemy : enemies) {
          AreaTile goes = ActorUtils.randomTileNear(subject.at(), nearRange, map, true);
          enemy.enterMap(map, goes.x, goes.y, 1, enemy.base());
        }
        enemiesDone = true;
      }
      
      if (enemiesDone && ! scareOkay) {
        scareOkay = subject.jobType() == JOB.RETREAT;
      }
      
      if (scareOkay && ! alliesDone) {
        for (Actor friend : allies) {
          AreaTile goes = ActorUtils.randomTileNear(subject.at(), nearRange, map, true);
          friend.enterMap(map, goes.x, goes.y, 1, friend.base());
        }
        alliesDone = true;
      }
      
      if (alliesDone && ! braveAgain) {
        braveAgain = subject.jobType() != JOB.RETREAT;
      }
      
      if (braveAgain && ! testOkay) {
        I.say("\nRETREAT TEST CONCLUDED SUCCESSFULLY!");
        testOkay = true;
        if (! graphics) return true;
      }
    }
    
    I.say("\nRETREAT TEST FAILED!");
    I.say("  Brave okay:   "+braveOkay  );
    I.say("  Enemies done: "+enemiesDone);
    I.say("  Scare okay:   "+scareOkay  );
    I.say("  Allies done:  "+alliesDone );
    I.say("  Brave again:  "+braveAgain );
    
    return false;
  }
  
  
}










