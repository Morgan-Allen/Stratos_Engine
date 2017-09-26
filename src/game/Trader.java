

package game;
import static game.BuildingSet.*;
import util.*;



public class Trader extends Walker {
  
  
  /**  Data fields, construction and save/load methods-
    */
  Tally <Good> carried = new Tally();
  
  
  public Trader(ObjectType type) {
    super(type);
  }
  
  
  public Trader(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  
  
}
