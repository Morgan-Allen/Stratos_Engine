

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
  final static Objective
    MAIN_OBJECTIVE = new Objective(
      ScenarioSectorA.class, "main_ob_SA",
      "Destroy all ruins on the map.",
      "checkMainObjective"
    );
  
  
  
  protected ScenarioSectorA(World world, WorldLocale locale) {
    super(AREA_A, world, locale);
    assignObjectives(MAIN_OBJECTIVE);
  }
  
  
  public ScenarioSectorA(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  protected int checkMainObjective() {
    boolean allRazed = false;
    
    for (Building b : nests()) {
      if (! b.destroyed()) allRazed = false;
    }
    
    if (allRazed) return COMPLETE_SUCCESS;
    return COMPLETE_NONE;
  }
  
}











