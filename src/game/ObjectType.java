

package game;
import util.*;
import static game.GameConstants.*;
import java.lang.reflect.Array;




public class ObjectType extends Index.Entry implements Session.Saveable {
  
  
  /**  Indexing, categorisation, spawning and save/load methods-
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
    IS_ARMY_BLD    = 8,
    IS_WALKER      = 9,
    IS_TRADE_WLK   = 10
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
  
  
  Object generate() {
    switch (category) {
      case(IS_FIXTURE    ): return new Fixture(this);
      case(IS_BUILDING   ): return new Building           (this);
      case(IS_DELIVER_BLD): return new BuildingForCrafts(this);
      case(IS_GATHER_BLD ): return new BuildingForGather  (this);
      case(IS_TRADE_BLD  ): return new BuildingForTrade   (this);
      case(IS_HOME_BLD   ): return new BuildingForHome    (this);
      case(IS_ARMY_BLD   ): return new BuildingForMilitary(this);
      case(IS_WALKER     ): return new Walker        (this);
      case(IS_TRADE_WLK  ): return new WalkerForTrade(this);
    }
    return null;
  }
  
  
  
  /**  Common data fields and setup functions-
    */
  String name;
  int tint = BLACK_COLOR;
  
  int category;
  int wide = 1, high = 1, deep = 1;
  boolean blocks = true ;
  boolean mobile = false;
  float growRate = 0;
  
  
  void setDimensions(int w, int h, int d) {
    this.wide = w;
    this.high = h;
    this.deep = d;
    this.maxHealth = (int) (10 * w * h * (1 + ((d - 1) / 2f)));
  }
  
  
  Object[] castArray(Object arr[], Class c) {
    Object n[] = (Object[]) Array.newInstance(c, arr.length);
    for (int i = arr.length; i-- > 0;) n[i] = arr[i];
    return n;
  }
  
  
  
  /**  Building-specific data fields and setup methods-
    */
  Good    builtFrom  [] = NO_GOODS;
  Integer builtAmount[] = {};
  Good    buildsWith [] = NO_GOODS;
  
  Good needed  [] = NO_GOODS;
  Good produced[] = NO_GOODS;
  Good consumed[] = NO_GOODS;
  Good features[] = NO_GOODS;
  
  int updateTime      = 50 ;
  int gatherRange     = 4  ;
  int craftTime       = 20 ;
  int maxStock        = 10 ;
  int maxDeliverRange = 100;
  int consumeTime     = 500;
  
  ObjectType walkerTypes[] = NO_WALKERS;
  int maxWalkers  = 1 ;
  int maxVisitors = 4 ;
  int maxRecruits = 16;
  int numRanks    = 4 ;
  int numFile     = 4 ;
  
  
  void setBuildMaterials(Object... args) {
    Object split[][] = Visit.splitByModulus(args, 2);
    builtFrom   = (Good   []) castArray(split[0], Good   .class);
    builtAmount = (Integer[]) castArray(split[1], Integer.class);
  }
  
  
  int materialNeed(Good buildFrom) {
    int index = Visit.indexOf(buildFrom, this.builtFrom);
    return index == -1 ? 0 : builtAmount[index];
  }
  
  
  void setWalkerTypes(ObjectType... types) {
    this.walkerTypes = types;
  }
  
  
  boolean hasFeature(Good feature) {
    return Visit.arrayIncludes(features, feature);
  }
  
  
  boolean isTradeBuilding() {
    return category == IS_TRADE_BLD;
  }
  
  
  
  /**  Walker-specific stats and setup methods-
    */
  int attackScore =  2 ;
  int defendScore =  2 ;
  int maxHealth   =  5 ;
  int sightRange  =  6 ;
  int attackRange =  1 ;
  
  String names[];
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    return name;
  }
}





