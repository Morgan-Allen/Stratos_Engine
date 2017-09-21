package game;




public class ObjectType {
  
  
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
  int craftTime = 20, maxStock = 10;
  int maxDeliverRange = 100;
  ObjectType legalStores[] = null;
  int consumeTime = 500;
  
  
  //  And these are specific to walkers...
  String names[];
  
}