

package game;
import util.*;




public class ObjectType extends Index.Entry implements Session.Saveable {
  
  
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
  
  
  
  String name;
  //Image sprite;
  int tint;
  
  int wide, high;
  boolean mobile;
  
  
  //  These are specific to buildings...
  ObjectType walkerType = null;
  int maxWalkers = 1;
  int walkerCountdown = 50;
  
  Goods.Good needed  [] = Goods.NO_GOODS;
  Goods.Good produced[] = Goods.NO_GOODS;
  Goods.Good consumed[] = Goods.NO_GOODS;
  Goods.Good features[] = Goods.NO_GOODS;
  
  int craftTime = 20, maxStock = 10;
  int maxDeliverRange = 100;
  int consumeTime = 500;
  
  
  //  And these are specific to walkers...
  String names[];
  
}


