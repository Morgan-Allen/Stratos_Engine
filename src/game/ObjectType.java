

package game;
import util.*;
import static game.GameConstants.*;




public class ObjectType extends Index.Entry implements Session.Saveable {
  
  
  /**  Indexing and save/load methods-
    */
  final static int
    IS_TERRAIN     = 0,
    IS_FIXTURE     = 1,
    IS_GOOD        = 2,
    IS_BUILDING    = 3,
    IS_DELIVER_BLD = 4,
    IS_GATHER_BLD  = 5,
    IS_TRADE_BLD   = 6,
    IS_HOME_BLD    = 7,
    IS_WALKER      = 8,
    IS_TRADE_WLK   = 9
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
  int tint = GameConstants.BLACK_COLOR;
  
  int category;
  int wide = 1, high = 1;
  boolean blocks = true ;
  boolean mobile = false;
  
  
  //  These are specific to buildings...
  ObjectType walkerTypes[] = NO_WALKERS;
  int maxWalkers = 1;
  int walkerCountdown = 50;
  int maxVisitors = 4;
  
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
  int attackScore = 2 ;
  int defendScore = 2 ;
  int maxHealth   = 5 ;
  
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
      case(IS_FIXTURE    ): return new Fixture(this);
      case(IS_BUILDING   ): return new Building(this);
      case(IS_DELIVER_BLD): return new BuildingForDelivery(this);
      case(IS_GATHER_BLD ): return new BuildingForGather(this);
      case(IS_TRADE_BLD  ): return new BuildingForTrade(this);
      case(IS_HOME_BLD   ): return new BuildingForHome(this);
      case(IS_WALKER     ): return new Walker(this);
      case(IS_TRADE_WLK  ): return new WalkerForTrade(this);
    }
    return null;
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    return name;
  }
}





