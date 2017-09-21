



public class ObjectType {
  
  
  String name;
  //Image sprite;
  int basicColor;
  
  int wide, high;
  boolean mobile;
  
  
  //  These are specific to buildings...
  ObjectType walkerType = null;
  int maxWalkers = 1;
  int walkerCountdown = 50;
  
  //  And these are specific to walkers...
  String names[];
}