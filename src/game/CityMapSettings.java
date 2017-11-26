


package game;
import java.lang.reflect.*;




public class CityMapSettings {
  
  
  /**  Global toggle-settings used for debugging:
    */
  final CityMap map;
  
  public boolean
    toggleFog     = true,
    toggleHunger  = true,
    toggleFatigue = true,
    toggleInjury  = true,
    
    toggleMigrate     = true,
    toggleBuildEvolve = true,
    
    slowed    = false,
    speedUp   = false,
    paused    = false,
    worldView = false,
    
    reportBattle    = false,
    reportPathCache = false;
  
  
  CityMapSettings(CityMap map) {
    this.map = map;
  }
  
  
  void loadState(Session s) throws Exception {
    for (Field f : CityMapSettings.class.getFields()) {
      f.set(this, s.loadBool());
    }
  }
  
  
  void saveState(Session s) throws Exception {
    for (Field f : CityMapSettings.class.getFields()) {
      s.saveBool(f.getBoolean(this));
    }
  }
  
}


