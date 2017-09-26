

package game;
import util.*;
import static game.BuildingSet.*;




public class ObjectType extends Index.Entry implements Session.Saveable {
  
  
  /**  Indexing and save/load methods-
    */
  final static int
    IS_FIXTURE    = 0,
    IS_GOOD       = 1,
    IS_BUILDING   = 2,
    IS_CRAFT_BLD  = 3,
    IS_GATHER_BLD = 4,
    IS_TRADE_BLD  = 5,
    IS_HOME_BLD   = 6,
    IS_WALKER     = 7,
    IS_TRADE_WLK  = 8
  ;
  
  final static Index <ObjectType> INDEX = new Index();
  
  
  ObjectType(String ID, int category) {
    super(INDEX, ID);
    this.category = category;
  }
  
  
  public static ObjectType loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  
  /**  Data fields and setup functions-
    */
  String name;
  int tint = BuildingSet.BLACK_COLOR;
  
  int category;
  int wide = 1, high = 1;
  boolean blocks = true ;
  boolean mobile = false;
  
  
  //  These are specific to buildings...
  ObjectType walkerTypes[] = NO_WALKERS;
  int maxWalkers = 1;
  int walkerCountdown = 50;
  
  Good needed  [] = NO_GOODS;
  Good produced[] = NO_GOODS;
  Good consumed[] = NO_GOODS;
  Good features[] = NO_GOODS;
  
  int craftTime = 20, maxStock = 10;
  int maxDeliverRange = 100;
  int consumeTime = 500;
  
  
  void setWalkerTypes(ObjectType... types) {
    this.walkerTypes = types;
  }
  
  
  //  And these are specific to walkers...
  String names[];
  
  
  
  /**  General query utilities-
    */
  boolean hasFeature(Good feature) {
    return Visit.arrayIncludes(features, feature);
  }
  
  
  boolean isTradeBuilding() {
    return category == IS_TRADE_BLD;
  }
  
  
  Object generate() {
    switch (category) {
      case(IS_FIXTURE  ): return new Fixture(this);
      case(IS_BUILDING ): return new Building(this);
      case(IS_CRAFT_BLD): return new CraftBuilding(this);
      case(IS_TRADE_BLD): return new TradeBuilding(this);
      case(IS_HOME_BLD ): return new HomeBuilding(this);
      case(IS_WALKER   ): return new Walker(this);
      case(IS_TRADE_WLK): return new TradeWalker(this);
    }
    return null;
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    return name;
  }
}





