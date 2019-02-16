

package test;
import game.*;
import game.GameConstants.Terrain;
import util.*;
import static game.GameConstants.*;
import static content.GameContent.*;
import static content.GameWorld.*;



public class TestWorld2 extends LogicTest {
  
  
  
  public static void main(String args[]) {
    testWorld(false);
  }
  
  
  static boolean testWorld(boolean graphics) {

    LogicTest test = new TestWorld2();
    boolean testOkay = true;
    
    
    World world = new World(ALL_GOODS);
    world.assignTypes(
      ALL_BUILDINGS, ALL_SHIPS(), ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES()
    );
    Base from[] = new Base[6];
    Base goes[] = new Base[4];
    float relations[][] = {
      {  0.5f, -0.5f, -0.5f, -1.0f },
      {  0.5f, -0.7f, -0.2f,  0.0f },
      {  0.5f, -0.7f, -0.2f,  0.0f },
      {  0.0f,  0.0f,  0.0f,  0.0f },
      {  0.0f,  0.0f,  0.0f,  0.0f },
      {  0.0f,  0.0f,  0.0f,  0.0f },
    };
    for (int i = from.length; i-- > 0;) {
      from[i] = new Base(world, world.addLocale(0, i), FACTION_SETTLERS_A, "F_"+i);
    }
    for (int i = goes.length; i-- > 0;) {
      goes[i] = new Base(world, world.addLocale(1, i), FACTION_SETTLERS_B, "G_"+i);
    }
    world.addBases(from);
    world.addBases(goes);
    
    
    for (int i = from.length; i-- > 0;) {
      for (int j = goes.length; j-- > 0;) {
        from[i].relations.incBond(goes[j], relations[i][j]);
        goes[j].relations.incBond(from[i], relations[i][j]);
      }
    }
    for (Base c : world.bases()) {
      c.initBuildLevels(HOLDING, 2f, TROOPER_LODGE, 2f);
      for (Base o : world.bases()) if (c != o) {
        World.setupRoute(c.locale, o.locale, 1, Type.MOVE_LAND);
      }
    }
    Base main = from[0];
    
    Area map = new Area(world, main.locale, main);
    map.performSetup(32, new Terrain[0]);
    
    
    
    //  TODO:  Test out some basic interactions first, then allow the world to
    //  interact at random for a while and see who comes out on top.
    final int RUN_TIME = YEAR_LENGTH * 20;
    
    //  TODO:  You need to report any events that occurred.
    
    while (map.time() < RUN_TIME || graphics) {
      test.runLoop(main, 1, graphics, "saves/test_world.tlt");
      
      for (WorldEvents.Event e : world.events.history()) {
        I.say(world.events.descFor(e));
      }
      world.events.clearHistory();
    }
    
    return testOkay;
  }
  
  
  
}

















