

package game;
import static game.GameConstants.*;



public class TestDiplomacy extends Test {
  
  
  public static void main(String args[]) {
    testDiplomacy(false);
  }
  
  static boolean testDiplomacy(boolean graphics) {
    Test test = new TestDiplomacy();
    
    World   world = GameConstants.setupDefaultWorld();
    City    homeC = world.cities.atIndex(0);
    City    awayC = world.cities.atIndex(1);
    CityMap map   = CityMapTerrain.generateTerrain(
      homeC, 32, 0, MEADOW, JUNGLE
    );
    homeC.name = "Home City";
    awayC.name = "Away City";
    awayC.council.typeAI = CityCouncil.AI_OFF;
    map.settings.toggleFog = false;
    
    
    //  TODO:  You need to introduce tests for both on-map diplomacy and
    //  off-map diplomacy in order for this to work.
    
    //  So, step 1-
    //  Schedule a diplomatic formation from the foreign city.
    //  Wait until they arrive.
    //  Allow the envoy/s to enter the city (even through walls) and conduct
    //  their business.
    //  Wait until a diplomatic offer is lodged.
    
    //  Accept the offer.
    //  OR
    //  Reject the offer.
    
    //  TODO:  If you reject the offer, they might attack right there!
    //  So... even regular military actions might allow for diplomacy, with
    //  force as a later option.
    
    //  Dispatch your own diplomatic formation from your own city.
    //  Wait until they arrive.
    //  Ensure your offer is accepted.
    //  Allow the formation to return home.  Job done.
    
    while (map.time < 1000 || graphics) {
      map = test.runLoop(map, 10, graphics, "saves/test_diplomacy.tlt");
    }
    
    return false;
  }
}









