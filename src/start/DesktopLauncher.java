


package start;
import content.*;
import util.*;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import java.awt.Dimension;
import java.awt.Toolkit;



public class DesktopLauncher {
  
  public final static int
    DEFAULT_WIDTH  = 1200,
    DEFAULT_HEIGHT = 720,
    DEFAULT_HERTZ  = 60;
  
  
  public static void main(String[] arg) {
    launchScenario(new ScenarioBlankMap());
  }
  
  
  public static void launchScenario(Scenario s) {
    
    //Assets.callsVerbose = true;
    //Assets.extraVerbose = true;
    //PlayLoop.verbose    = true;
    
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






