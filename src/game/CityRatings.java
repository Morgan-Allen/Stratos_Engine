

package game;
import util.*;



public class CityRatings {
  
  
  City city;
  
  //  So... What am I measuring here?
  
  // Population / Nature
  //  Knowledge / Aesthetics
  //      Power / Peace
  //    Freedom / Order
  //       Fire / Water
  //      Earth / Air
  
  //  Jobs/Homes available/filled.
  //  Slaves/Commoners/Merchants/Nobles.
  //  Men & Women.
  
  
  Tally <Type> supply = new Tally();
  Tally <Type> demand = new Tally();
  
  
  
  void updateRatings(CityMap map) {
    int totalPop = 0;
    
    for (Actor a : map.actors) {
      if (a.homeCity != city) continue;
      totalPop += 1;
    }
  }
  
  
}