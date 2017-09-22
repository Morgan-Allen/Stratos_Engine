

package game;
import static game.Terrains.*;
import util.*;




public class GatherBuilding extends CraftBuilding {
  
  
  /**  Data fields, construction and save/load methods-
    */
  
  
  public GatherBuilding(ObjectType type) {
    super(type);
  }
  
  
  public GatherBuilding(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Life-cycle, update and economic functions-
    */
  public static class Crop extends Fixture {
    
    
    public Crop(ObjectType type) {
      super(type);
    }
    
    
    public Crop(Session s) throws Exception {
      super(s);
    }
    
    
    public void saveState(Session s) throws Exception {
      super.saveState(s);
    }


    void updateGrowth() {
      buildLevel += SCAN_PERIOD * 1f / RIPEN_PERIOD;
      if (buildLevel >= 1) buildLevel = 1;
      //I.say("Updating growth at "+x+" "+y+": "+buildLevel);
    }
    
  }
  
  
  
  
  
  void advanceProduction() {
    super.advanceProduction();
  }
  
  
  void selectWalkerBehaviour(Walker walker) {
    super.selectWalkerBehaviour(walker);
  }
  
  
}







