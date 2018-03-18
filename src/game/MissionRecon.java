

package game;
import static game.GameConstants.*;



public class MissionRecon extends Mission {
  
  
  public MissionRecon(Base belongs, boolean activeAI) {
    super(OBJECTIVE_RECON, belongs, activeAI);
  }
  
  
  public MissionRecon(Session s) throws Exception {
    super(s);
  }
  
  
  public Task selectActorBehaviour(Actor actor) {
    
    //  TODO:  What about leaving the map?
    //  You'll need to use the stand-point for that, once the objective is
    //  complete.
    
    TaskExplore recon = TaskExplore.configExploration(
      actor, (Target) focus(), (int) exploreRange
    );
    if (recon != null) {
      return recon;
    }
    return null;
  }
  
  
  public boolean allowsFocus(Object newFocus) {
    if (newFocus instanceof AreaTile) return true;
    return false;
  }
  
  
  boolean objectiveComplete() {
    if (focus() instanceof AreaTile) {
      AreaTile looks = (AreaTile) focus();
      int r = (int) exploreRange;
      boolean allSeen = true;
      
      for (AreaTile t : map().tilesUnder(looks.x - r, looks.y - r, r * 2, r * 2)) {
        float dist = Area.distance(looks, t);
        if (dist > r) continue;
        if (map().fog.maxSightLevel(t) == 0) allSeen = false;
      }
      
      return allSeen;
    }
    return false;
  }
  
  
  
  void handleArrival(Base goes, World.Journey journey) {
    //  TODO:  Fill this in...
  }
  
}





