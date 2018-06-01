

package content;
import game.*;
import static game.GameConstants.*;
import static content.GameContent.*;



public class ScenarioSectorA extends WorldScenario {
  
  
  final static SiteConfig
    SITE_A = WorldScenario.siteConfig(
      GameWorld.FACTION_ARTILECTS,
      GameContent.RUINS_LAIR, 1, 1
    );
  final static AreaConfig
    AREA_A = WorldScenario.areaConfig(
      64, new Terrain[] { MEADOW, JUNGLE, DESERT },
      SITE_A
    );
  
  
  protected ScenarioSectorA(World world, WorldLocale locale) {
    super(AREA_A, world, locale);
  }
  
  
  public ScenarioSectorA(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  

  
  protected int checkCompletion() {
    boolean allRazed = false;
    
    for (Building b : nests()) {
      if (! b.destroyed()) allRazed = false;
    }
    
    if (allRazed) return COMPLETE_SUCCESS;
    return COMPLETE_NONE;
  }
  
  
  
}











