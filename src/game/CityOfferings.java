

package game;
import util.*;
import static game.GameConstants.*;



public class CityOfferings {
  
  
  public static enum OFFER {
    GOODS, ANIMAL, BLOOD,
    CAPTIVE, GLADIATOR, VOLUNTEER
  }
  
  static class Offering {
    
    Type deityHonoured;
    Building site;
    
    OFFER type;
    Object given;
    float amount;
  }
  
  static class Festival {
    
    int eventID;
    int timeStarts;
    int timeEnds;
    
    List <Offering> offerings = new List();
  }
  
  City city;
  List <Festival> schedule = new List();
  
  
  
}