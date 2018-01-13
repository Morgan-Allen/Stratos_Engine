


package game;
import java.lang.reflect.*;




public class WorldSettings {
  
  
  /**  Global toggle-settings used for debugging:
    */
  final World world;
  
  public boolean
    toggleFog     = true,
    toggleHunger  = true,
    toggleFatigue = true,
    toggleInjury  = true,
    
    toggleAging       = true,
    toggleChildMort   = true,
    
    toggleMigrate     = true,
    toggleBuildEvolve = true,
    
    slowed    = false,
    speedUp   = false,
    paused    = false,
    worldView = false,
    
    viewPathMap     = false,
    reportBattle    = false,
    reportPathCache = false;
  
  
  WorldSettings(World world) {
    this.world = world;
  }
  
  
  void loadState(Session s) throws Exception {
    for (Field f : WorldSettings.class.getFields()) {
      f.set(this, s.loadBool());
    }
  }
  
  
  void saveState(Session s) throws Exception {
    for (Field f : WorldSettings.class.getFields()) {
      s.saveBool(f.getBoolean(this));
    }
  }
  
}


