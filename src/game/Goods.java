

package game;
import util.*;



public class Goods {
  
  
  
  final static Index <Good> INDEX = new Index();
  private static List <Good> GOODS_LIST = new List();
  
  static class Good extends Index.Entry implements Session.Saveable {
    
    String name;
    
    Good(String name, int ID) {
      super(INDEX, "_"+ID);
      GOODS_LIST.add(this);
      this.name = name;
    }
    
    public static Good loadConstant(Session s) throws Exception {
      return INDEX.loadEntry(s.input());
    }
    
    public void saveState(Session s) throws Exception {
      INDEX.saveEntry(this, s.output());
    }
    
    public String toString() {
      return name;
    }
  }
  
  
  final static Good
    CLAY       = new Good("Clay"      , 0 ),
    POTTERY    = new Good("Pottery"   , 1 ),
    
    IS_MARKET  = new Good("Is Market" , 22),
    IS_AMENITY = new Good("Is Amenity", 23),
    
    ALL_GOODS[] = (Good[]) GOODS_LIST.toArray(Good.class),
    NO_GOODS [] = new Good[0];
  
}



