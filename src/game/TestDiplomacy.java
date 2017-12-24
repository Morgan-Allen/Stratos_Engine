

package game;
import util.*;
import static game.ActorAsPerson.*;
import static game.CityCouncil.*;
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
    
    
    
    //  TODO:  Surround with a small curtain wall...

    Building palace = (Building) PALACE.generate();
    CityCouncil council = map.city.council;
    
    ActorAsPerson monarch = (ActorAsPerson) NOBLE.generate();
    council.toggleMember(monarch, Role.MONARCH, true);
    palace.setResident(monarch, true);
    palace .enterMap(map, 10, 10, 1);
    monarch.enterMap(map, 12, 9 , 1);
    
    //  So, step 1-
    //  Schedule a diplomatic formation from the foreign city.
    //  Wait until they arrive.
    //  Allow the envoy/s to enter the city (even through walls) and conduct
    //  their business.
    //  Wait until a diplomatic offer is lodged.
    
    Formation entourage;
    entourage = new Formation(Formation.OBJECTIVE_DIALOG, awayC, true);
    for (int n = 4; n-- > 0;) {
      Actor s = (Actor) SOLDIER.generate();
      entourage.toggleRecruit(s, true);
    }
    
    Actor envoy = (Actor) NOBLE.generate();
    entourage.toggleEscorted(envoy, true);
    Actor bride = (Actor) CONSORT.generate();
    entourage.toggleEscorted(bride, true);
    
    entourage.assignTerms(City.POSTURE.ALLY, null, bride, null);
    entourage.beginSecuring(homeC);
    
    
    boolean escortArrived  = false;
    boolean offerGiven     = false;
    boolean offerAccepted  = false;
    boolean termsOkay      = false;
    boolean escortDeparted = false;
    boolean testOkay       = false;
    
    //  TODO:  Handle these permutations-
    //  Force is offensive/defensive/diplomatic.
    //  Terms accepted/rejected/ignored.
    //  Force returns/defeated/victorious.
    //  Force is on map/away.
    
    while (map.time < 1000 || graphics) {
      map = test.runLoop(map, 1, graphics, "saves/test_diplomacy.tlt");
      
      if (! escortArrived) {
        escortArrived = entourage.map == map;
      }
      
      if (escortArrived && ! offerGiven) {
        offerGiven = council.petitions.includes(entourage);
      }
      
      if (offerGiven && ! offerAccepted) {
        council.acceptTerms(entourage);
        offerAccepted = true;
      }
      
      if (offerAccepted && ! termsOkay) {
        boolean termsFilled = true;
        termsFilled &= monarch.hasBondType(bride, BOND_MARRIED);
        termsFilled &= homeC.isAllyOf(awayC);
        termsOkay = termsFilled;
      }
      
      if (termsOkay && ! escortDeparted) {
        escortDeparted = entourage.map == null;
      }
      
      if (escortDeparted && ! testOkay) {
        I.say("\nDIPLOMACY TEST CONCLUDED SUCCESSFULLY!");
        testOkay = true;
        if (! graphics) return true;
      }
    }
    
    I.say("\nDIPLOMACY TEST FAILED!");
    return false;
  }
}









