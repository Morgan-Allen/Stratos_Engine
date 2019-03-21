

package test;
import game.*;
import static game.GameConstants.*;
import static content.GameContent.*;
import start.*;



public abstract class ViewTest extends Scenario {
  
  
  int mapSize;
  Base initBase;
  String savePath;
  
  
  static void beginRenderTest(ViewTest t, int mapSize, String savePath) {
    t.mapSize = mapSize;
    t.savePath = savePath;
    t.initScenario(MainGame.mainGame());
    DesktopLauncher.launchScenario(t, null);
  }
  
  protected World createWorld() {
    Terrain gradient[] = new Terrain[] { MEADOW };
    initBase = LogicTest.setupTestBase(
      LogicTest.BASE, FACTION_NEUTRAL, new Good[0], mapSize, true, gradient
    );
    return initBase.world;
  }
  
  protected AreaMap createMap(World world) {
    return initBase.activeMap();
  }
  
  protected Base createBase(AreaMap map, World world) {
    return initBase;
  }
  
  protected String savePath() {
    return savePath;
  }
}









