

package game;
import static game.GameConstants.*;
import game.World.Journey;
import util.I;



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
        //  TODO:  ...Fill this in.
      }
      //
      //  In the case of local exploration, check for fulfillment:
      else if (localFocus() instanceof AreaTile) {
        Area     map   = localBase.activeMap();
        AreaFog  fog   = map.fogMap(homeBase().faction(), true);
        AreaTile looks = (AreaTile) localFocus();
        int r = (int) exploreRange;
        boolean allSeen = true;
        
        for (AreaTile t : map.tilesUnder(looks.x - r, looks.y - r, r * 2, r * 2)) {
          float dist = Area.distance(looks, t);
          if (dist > r) continue;
          if (fog.maxSightLevel(t) == 0) allSeen = false;
        }
        
        if (allSeen) setMissionComplete(true);
      }
    }
  }
  
  
  public Task nextLocalMapBehaviour(Actor actor) {
    
    Target from = localFocus();
    if (from == null) from = transitTile();
    
    TaskExplore recon = TaskExplore.configExploration(
      actor, from, exploreRange
    );
    if (recon != null) return recon;
    return null;
  }
  
  
  public boolean allowsFocus(Object newFocus) {
    if (newFocus instanceof AreaTile) return true;
    return false;
  }
  
  
  
  void handleOffmapArrival(Base goes, World.Journey journey) {
    Federation from = homeBase().federation();
    from.setExploreLevel(goes.locale, 1);
    return;
  }
  
  
  void handleOffmapDeparture(Base from, Journey journey) {
    //  TODO:  Fill this in?
    return;
  }
  
}








