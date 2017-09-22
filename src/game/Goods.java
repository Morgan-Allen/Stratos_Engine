package game;


import util.*;




public class Goods {
  
  
  
  private static List <Good> GOODS_LIST = new List();
  
  static class Good {
    String name;
    int index;
    
    Good(String name, int index) {
      this.name = name;
      this.index = index;
      GOODS_LIST.add(this);
    }
    
    public String toString() {
      return name;
    }
  }
  
  
  final static Good
    CLAY      = new Good("Clay"   , 0),
    POTTERY   = new Good("Pottery", 1),
    
    IS_MARKET = new Good("Is Market", 22),
    
    ALL_GOODS[] = (Good[]) GOODS_LIST.toArray(Good.class),
    NO_GOODS [] = new Good[0];
  
}











