

package start;
import content.*;
import static content.GameContent.*;
import game.*;
import static game.GameConstants.*;
import util.*;



public class LaunchWithMap {
  
  
  public static void main(String[] arg) {
    String savePath = "saves/test_scenario.str";
    
    if (! MainGame.loadGameState(savePath)) {
      I.say("\nGenerating scenario from scratch...");
      
      World world = GameWorld.setupDefaultWorld();
      world.assignSavePath(savePath);
      
      Base homeland = world.baseNamed("Homeworld Base");
      WorldScenario init = world.scenarios().first();
      
      MissionExpedition e = new MissionExpedition(homeland, true);
      Tally <Good> goods = Tally.with(PARTS, 20, PLASTICS, 20, CARBS, 20);
      e.configAssets(5000, goods, GameContent.BASTION);
      e.setWorldFocus(init.locale());
      
      init.assignExpedition(e, world);
      init.initScenario(MainGame.mainGame());
      
      DesktopLauncher.launchScenario(init, world);
    }
    else {
      I.say("\nLoaded scenario from file...");
      DesktopLauncher.launchGame(MainGame.mainGame());
    }
  }
}
