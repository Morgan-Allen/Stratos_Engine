

package start;
import content.*;
import game.*;



public class LaunchWithGlobe {
  
  
  final public static String
    SAVE_PATH = "saves/test_world.str"
  ;
  
  public static void main(String[] arg) {
    
    if (! MainGame.loadGameState(SAVE_PATH)) {
      World world = GameWorld.setupDefaultWorld();
      DesktopLauncher.launchScenario(null, world);
    }
    else {
      DesktopLauncher.launchGame(MainGame.mainGame());
    }
  }
  
}
