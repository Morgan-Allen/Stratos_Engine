

package game;
import util.*;
import static game.BuildingSet.*;




public class ObjectType extends Index.Entry implements Session.Saveable {
  
  
  /**  Indexing and save/load methods-
    */
  final static Index <ObjectType> INDEX = new Index();
  
  
  ObjectType(String ID) {
    super(INDEX, ID);
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
  
  int wide = 1, high = 1;
  boolean blocks = true ;
  boolean mobile = false;
  
  
  //  These are specific to buildings...
  ObjectType walkerType = null;
  int maxWalkers = 1;
  int walkerCountdown = 50;
  
  Good needed  [] = NO_GOODS;
  Good produced[] = NO_GOODS;
  Good consumed[] = NO_GOODS;
  Good features[] = NO_GOODS;
  
  int craftTime = 20, maxStock = 10;
  int maxDeliverRange = 100;
  int consumeTime = 500;
  
  
  //  And these are specific to walkers...
  String names[];
  
  
  
  /**  General query utilities-
    */
  boolean hasFeature(Good feature) {
    return Visit.arrayIncludes(features, feature);
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    return name;
  }
}





