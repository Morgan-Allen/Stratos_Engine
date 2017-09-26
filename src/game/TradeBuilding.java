

package game;
import static game.BuildingSet.*;
import util.*;



public class TradeBuilding extends CraftBuilding {
  
  
  Tally <Good> stockLevels = new Tally();
  City tradePartner = null;
  Good imports[] = NO_GOODS, exports[] = NO_GOODS;
  
  
  public TradeBuilding(ObjectType type) {
    super(type);
  }
  
  
  public TradeBuilding(Session s) throws Exception {
    super(s);
    s.loadTally(stockLevels);
    tradePartner = (City) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveTally(stockLevels);
    s.saveObject(tradePartner);
  }

  
  
  void updateDemands() {
    
    Batch <Good> impB = new Batch(), expB = new Batch();
    for (Good g : stockLevels.keys()) {
      float level = stockLevels.valueFor(g);
      if (level > 0) impB.add(g);
      if (level < 0) expB.add(g);
    }
    imports = impB.toArray(Good.class);
    exports = expB.toArray(Good.class);
    
    super.updateDemands();
  }
  

  Good[] needed  () { return imports; }
  Good[] produced() { return exports; }
  float stockNeeded(Good imp) { return     stockLevels.valueFor(imp); }
  float stockLimit (Good exp) { return 0 - stockLevels.valueFor(exp); }

  
  
  void selectWalkerBehaviour(Walker walker) {
    //  In principle, the behaviour here is simple:  As before, you simply
    //  make deliveries to any structures that demand a particular good.  But
    //  there are a few twists:
    //    (1)  You can only deliver to trade structures.
    //    (2)  You should aggregate multiple goods into the same delivery.
    //    (3)  The 'structure' can be a foreign city OR another warehouse
    //         in the same city.
  }
  
  
}
















