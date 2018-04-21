


package game;
import java.lang.reflect.*;




public class WorldSettings {
  
  
  /**  Global toggle-settings used for debugging:
    */
  final World world;
  
  public boolean
    toggleFog         = true,
    toggleHunger      = true,
    toggleFatigue     = true,
    toggleInjury      = true,
    toggleReacts      = true,
    toggleCombat      = true,
    togglePurchases   = true,
    toggleDialog      = true,
    
    toggleMigrate     = true,
    toggleShipping    = true,
    toggleAging       = true,
    toggleChildMort   = true,
    
    toggleBuilding    = true,
    toggleSalvage     = true,
    toggleBuildEvolve = true,
    toggleAutoBuild   = true,
    
    toggleEasyHunger  = true,
    toggleEasyCraft   = true,
    toggleEasyBuild   = true,
    
    slowed            = false,
    speedUp           = false,
    paused            = false,
    worldView         = false,
    
    reportBattle      = false,
    reportPathCache   = false;
  
  
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


