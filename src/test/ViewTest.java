

package test;
import game.*;
import static game.GameConstants.*;
import start.*;



public abstract class ViewTest extends CityMapScenario {
  
  
  int mapSize;
  Base initBase;
  String savePath;
  
  
  static void beginRenderTest(ViewTest t, int mapSize, String savePath) {
    t.mapSize = mapSize;
    t.savePath = savePath;
    DesktopLauncher.launchScenario(t);
  }
  
  protected World createWorld() {
    initBase = LogicTest.setupTestBase(mapSize, new Good[0], false);
    return initBase.world;
  }
  
  protected AreaMap createStage(World world) {
    return initBase.activeMap();
  }
  
  protected Base createBase(AreaMap map, World world) {
    return initBase;
  }
  
  protected String savePath() {
    return savePath;
  }
}









