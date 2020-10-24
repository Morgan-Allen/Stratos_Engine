

package game;
import java.lang.reflect.Constructor;
import util.*;



public class Constant extends Index.Entry implements Session.Saveable {
  
  
  final public static int
    
    IS_STORY       = -500,
    IS_AREA        = -400,
    IS_MEDIA       = -300,
    IS_DEITY       = -200,
    IS_TRAIT       = -100,
    
    IS_TERRAIN     =  0,
    IS_FIXTURE     =  1,
    IS_STRUCTURAL  =  2,
    IS_GOOD        =  3,
    
    IS_BUILDING    =  4,
    IS_UPGRADE     =  5,
    IS_CRAFTS_BLD  =  6,
    IS_GATHER_BLD  =  7,
    IS_MARKET_BLD  =  8,
    IS_TRADE_BLD   =  9,
    IS_DOCK_BLD    =  10,
    IS_HOME_BLD    =  11,
    IS_AMENITY_BLD =  12,
    IS_GOVERN_BLD  =  13,
    IS_HUNTS_BLD   =  14,
    IS_ARMY_BLD    =  15,
    IS_WATER_BLD   =  16,
    IS_WALLS_BLD   =  17,
    IS_FAITH_BLD   =  18,
    IS_NATIVE_BLD  =  19,
    
    IS_ACTOR       =  20,
    IS_PERSON_ACT  =  21,
    IS_MULE_ACT    =  22,
    IS_ANIMAL_ACT  =  23,
    IS_VESSEL_ACT  =  24
  ;
  
  final static Index <Constant> INDEX = new Index();
  
  public Class baseClass;
  public int category;
  
  public String name = "";
  public String info = "";
  
  
  
  public Constant(Class baseClass, String ID, int category) {
    super(INDEX, ID);
    this.baseClass = baseClass;
    this.category  = category ;
  }
  
  
  public static Constant loadConstant(Session s) throws Exception {
    return INDEX.loadEntry(s.input());
  }
  
  
  public void loadState(Session s) throws Exception {
    return;
  }
  
  
  public void saveState(Session s) throws Exception {
    INDEX.saveEntry(this, s.output());
  }
  
  
  
  public Object generate() {
    if (baseClass == null) {
      return null;
    }
    try {
      if (! Element.class.isAssignableFrom(baseClass)) return null;
      Constructor c = null;
      for (Constructor n : baseClass.getConstructors()) {
        Class params[] = n.getParameterTypes();
        if (params.length == 1 && Type.class.isAssignableFrom(params[0])) {
          c = n;
          break;
        }
      }
      return c.newInstance(this);
    }
    catch (NullPointerException e) {
      I.say(
        "\n  WARNING: NO TYPE CONSTRUCTOR FOR: "+baseClass.getName()+
        "\n  All Elements should implement a public constructor taking a Type "+
        "\n  as the sole argument, or else their Type should override the "+
        "\n  generate() method.  Thank you.\n"
      );
      return null;
    }
    catch (Exception e) {
      I.say("ERROR INSTANCING "+baseClass.getSimpleName()+": "+e);
      e.printStackTrace();
      return null;
    }
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  public String name() {
    return name;
  }
  
  
  public String toString() {
    return name;
  }
}





