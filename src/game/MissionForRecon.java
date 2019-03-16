

package game;
import static game.GameConstants.*;
import static game.World.*;
import util.*;



public class MissionForRecon extends Mission {
  
  
  int exploreRange = AVG_EXPLORE_DIST;
  
  
  public MissionForRecon(Base belongs) {
    super(OBJECTIVE_RECON, belongs);
  }
  
  
  public MissionForRecon(Session s) throws Exception {
    super(s);
    exploreRange = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(exploreRange);
  }
  
  
  public void setExploreRange(int range) {
    this.exploreRange = range;
  }
  
  
  
  void update() {
    super.update();
    
    if (! complete()) {
      //
      //  In the case of exploring an entire foreign base...
      if (worldFocus() != null && ! onWrongMap()) {
        AreaMap  map   = localMap();
        AreaFog  fog   = map.fogMap(homeBase().faction(), true);
        AreaTile looks = (AreaTile) localFocus();
        
        if (looks == null || fog.maxSightLevel(looks) >= 1) {
          looks = fog.pickRandomFogPoint(transitTile(), -1);
          setLocalFocus(looks);
        }
        
        if (looks == null) setMissionComplete(true);
      }
      //
      //  In the case of local exploration, check for fulfillment:
      else if (localFocus() instanceof AreaTile) {
        AreaMap  map   = localMap();
        AreaFog  fog   = map.fogMap(homeBase().faction(), true);
        AreaTile looks = (AreaTile) localFocus();
        
        int r = (int) exploreRange;
        boolean allSeen = true;
        
        for (AreaTile t : map.tilesUnder(looks.x - r, looks.y - r, r * 2, r * 2)) {
          float dist = AreaMap.distance(looks, t);
          if (dist > r) continue;
          if (fog.maxSightLevel(t) == 0) allSeen = false;
        }
        
        if (allSeen) setMissionComplete(true);
      }
    }
  }
  
  
  public Task nextLocalMapBehaviour(Actor actor) {
    
    Target from = localFocus();
    TaskExplore recon = TaskExplore.configExploration(actor, from, exploreRange);
    if (recon != null) return recon;
    
    return null;
  }
  
  
  public boolean allowsFocus(Object newFocus) {
    if (newFocus instanceof AreaTile) return true;
    return false;
  }


  void handleOffmapArrival(Area goes, World.Journey journey) {
    if (goes == worldFocus()) {
      MissionUtils.handleRecon(this, goes, journey);
    }
    return;
  }
  
  
  void handleOffmapDeparture(Area from, Journey journey) {
    return;
  }
  
}


