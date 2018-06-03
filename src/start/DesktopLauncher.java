


package start;
import content.*;
import game.*;
import util.*;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import java.awt.Dimension;
import java.awt.Toolkit;



public class DesktopLauncher {
  
  public final static int
    DEFAULT_WIDTH  = 1200,
    DEFAULT_HEIGHT = 720,
    DEFAULT_HERTZ  = 60
  ;
  
  
  public static void main(String[] arg) {
    
    //Assets.callsVerbose = true;
    //Assets.extraVerbose = true;
    //PlayLoop.verbose    = true;
    
    String savePath = "test_scenario.str";
    Scenario s = MainGame.loadScenario(savePath);
    
    if (s == null) {
      I.say("\nGenerating scenario from start...");
      
      World world = GameWorld.setupDefaultWorld();
      WorldScenario init = world.scenarios().first();
      
      Base homeland = world.baseNamed("Homeworld Base");
      List <Actor> staff = new List();
      
      init.setPlayerLanding(
        GameWorld.FACTION_SETTLERS, 5000,
        homeland, staff, GameContent.BASTION
      );
      init.assignSavePath(savePath);
      init.initScenario(MainGame.mainGame());
      
      s = init;
    }
    else {
      I.say("\nLoaded scenario from file...");
    }
    
    launchScenario(s);
  }
  
  
  public static void launchScenario(Scenario s) {
    new LwjglApplication(new PlayLoop(), getConfig());
    MainGame.playScenario(s);
  }
  
  
  private static LwjglApplicationConfiguration getConfig() {
    final Dimension SS = Toolkit.getDefaultToolkit().getScreenSize();
    final boolean report = true;
    
    final LwjglApplicationConfiguration
      config = new LwjglApplicationConfiguration()
    ;
    config.title = "Stratos";
    config.width  = Nums.min(DEFAULT_WIDTH , SS.width  - 100);
    config.height = Nums.min(DEFAULT_HEIGHT, SS.height - 100);
    config.resizable  = false;
    config.fullscreen = false;
    
    if (report) {
      I.say("\nSetting up screen configuration...");
      I.say("  Default width/height: "+DEFAULT_WIDTH+"/"+DEFAULT_HEIGHT);
      I.say("  Screen  width/height: "+SS    .width +"/"+SS    .height );
      I.say("  Window  width/height: "+config.width +"/"+config.height );
      I.say("");
    }
    return config;
  }
  
}






