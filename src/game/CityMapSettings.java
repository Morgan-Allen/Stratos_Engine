


package game;
import java.lang.reflect.*;




public class CityMapSettings {
  
  
  /**  Global toggle-settings used for debugging:
    */
  public boolean
    toggleFog     = true,
    toggleHunger  = true,
    toggleFatigue = true,
    toggleInjury  = true;
  
  
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
