

package game;
import static game.GameConstants.*;
import game.World.Journey;



public class MissionRecon extends Mission {
  
  
  int exploreRange = AVG_EXPLORE_DIST;
  
  
  public MissionRecon(Base belongs) {
    super(OBJECTIVE_RECON, belongs);
  }
  
  
  public MissionRecon(Session s) throws Exception {
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
        Area map = localBase.activeMap();
        AreaTile looks = (AreaTile) localFocus();
        int r = (int) exploreRange;
        boolean allSeen = true;
        
        for (AreaTile t : map.tilesUnder(looks.x - r, looks.y - r, r * 2, r * 2)) {
          float dist = Area.distance(looks, t);
          if (dist > r) continue;
          if (map.fog.maxSightLevel(t) == 0) allSeen = false;
        }
        
        if (allSeen) setMissionComplete(true);
      }
    }
  }
  
  
  public Task nextLocalMapBehaviour(Actor actor) {
    TaskExplore recon = TaskExplore.configExploration(
      actor, localFocus(), exploreRange
    );
    if (recon != null) return recon;
    return null;
  }
  
  
  public boolean allowsFocus(Object newFocus) {
    if (newFocus instanceof AreaTile) return true;
    return false;
  }
  
  
  boolean objectiveComplete() {
    if (localFocus() instanceof AreaTile) {
      Area map = localBase.activeMap();
      AreaTile looks = (AreaTile) localFocus();
      int r = (int) exploreRange;
      boolean allSeen = true;
      
      for (AreaTile t : map.tilesUnder(looks.x - r, looks.y - r, r * 2, r * 2)) {
        float dist = Area.distance(looks, t);
        if (dist > r) continue;
        if (map.fog.maxSightLevel(t) == 0) allSeen = false;
      }
      
      return allSeen;
    }
    return false;
  }
  
  
  
  void handleOffmapArrival(Base goes, World.Journey journey) {
    //  TODO:  Fill this in...
    return;
  }
  
  
  void handleOffmapDeparture(Base from, Journey journey) {
    //  TODO:  Fill this in...
    return;
  }
  
}









