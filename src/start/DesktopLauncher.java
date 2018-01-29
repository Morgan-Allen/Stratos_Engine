
/*
package src.stratos.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import start.MyGdxGame;



public class DesktopLauncher {
  public static void main (String[] arg) {
    LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
    new LwjglApplication(new MyGdxGame(), config);
  }
}
//*/


//*

package start;
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
  
  
  public static void main (String[] arg) {
    //new LwjglApplication(new MyGdxGame(), getConfig());
    new LwjglApplication(new PlayLoop(), getConfig());
    ///MainGame.playScenario(new DebugStage());
  }
  
  
  private static LwjglApplicationConfiguration getConfig() {
    final Dimension SS = Toolkit.getDefaultToolkit().getScreenSize();
    final boolean report = true;
    
    final LwjglApplicationConfiguration
      config = new LwjglApplicationConfiguration()
    ;
    config.title = "Stratos";
    //config.useGL30      = false;
    //config.vSyncEnabled = true;
    config.width  = Nums.min(DEFAULT_WIDTH , SS.width  - 100);
    config.height = Nums.min(DEFAULT_HEIGHT, SS.height - 100);
    //config.foregroundFPS = DEFAULT_HERTZ;
    //config.backgroundFPS = DEFAULT_HERTZ;
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
//*/




