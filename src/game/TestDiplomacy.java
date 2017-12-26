

package game;
import util.*;
import static game.ActorAsPerson.*;
import static game.CityCouncil.*;
import static game.GameConstants.*;



public class TestDiplomacy extends Test {
  
  
  public static void main(String args[]) {
    testDiplomacy(true);
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
    
    
    CityMapPlanning.placeStructure(WALL, map, true, 7, 7, 12, 12);
    CityMapPlanning.markDemolish(map, true, 8, 8, 10, 10);
    
    Building gate = (Building) GATE.generate();
    gate.setFacing(TileConstants.N);
    gate.enterMap(map, 12, 17, 1);
    
    Building palace = (Building) PALACE.generate();
    CityCouncil council = map.city.council;
    palace.enterMap(map, 10, 10, 1);
    CityMapPlanning.placeStructure(ROAD, map, true, 12, 19, 1, 13);
    
    ActorAsPerson monarch = (ActorAsPerson) NOBLE.generate();
    council.toggleMember(monarch, Role.MONARCH, true);
    palace.setResident(monarch, true);
    monarch.enterMap(map, 12, 9, 1);
    
    
    
    Formation entourage;
    entourage = new Formation(Formation.OBJECTIVE_DIALOG, awayC, true);
    for (int n = 4; n-- > 0;) {
      Actor s = (Actor) SOLDIER.generate();
      s.assignHomeCity(awayC);
      entourage.toggleRecruit(s, true);
    }
    
    Actor envoy = (Actor) NOBLE.generate();
    entourage.toggleEscorted(envoy, true);
    Actor bride = (Actor) CONSORT.generate();
    entourage.toggleEscorted(bride, true);
    
    for (Actor e : entourage.escorted) e.assignHomeCity(awayC);
    
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
    
    
    //  Okay.  Now you need council AI for evaluating offers, and for generating
    //  diplomatic missions themselves.  TODO:  That.
    
    
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
    I.say("  Escort arrived:  "+escortArrived );
    I.say("  Offer given:     "+offerGiven    );
    I.say("  Offer accepted:  "+offerAccepted );
    I.say("  Terms okay:      "+termsOkay     );
    I.say("  Escort departed: "+escortDeparted);
    return false;
  }
}









